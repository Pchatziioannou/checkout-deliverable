package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankRequest;
import com.checkout.payment.gateway.model.BankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {

  private static final Logger logger = LoggerFactory.getLogger(BankClient.class);

  private final RestTemplate restTemplate;
  private final String bankSimulatorUrl;

  public BankClient(RestTemplate restTemplate, @Value("${bank.simulator.url}") String bankSimulatorUrl) {
    this.restTemplate = restTemplate;
    this.bankSimulatorUrl = bankSimulatorUrl;
  }

  public BankResponse authorize(PostPaymentRequest paymentRequest) {
    // Bank expects the date in format MM/YY
    String expiryDate = String.format("%02d/%02d",
        paymentRequest.getExpiryMonth(),
        paymentRequest.getExpiryYear() % 100);

    BankRequest bankRequest = new BankRequest(
        paymentRequest.getCardNumber(),
        expiryDate,
        paymentRequest.getCurrency(),
        paymentRequest.getAmount(),
        paymentRequest.getCvv()
    );

    try {
      logger.info("Calling bank simulator at {}", bankSimulatorUrl);
      BankResponse response = restTemplate.postForObject(
          bankSimulatorUrl + "/payments",
          bankRequest,
          BankResponse.class
      );
      logger.info("Bank simulator response: authorized={}", response.isAuthorized());
      return response;
    } catch (HttpClientErrorException.BadRequest e) {
      logger.warn("Bank returned 400 Bad Request - treating as validation failure");
      return new BankResponse(false, null);
    } catch (RestClientException e) {
      logger.error("Bank simulator error: {}", e.getMessage());
      throw new EventProcessingException("Bank service unavailable: " + e.getMessage());
    }
  }
}
