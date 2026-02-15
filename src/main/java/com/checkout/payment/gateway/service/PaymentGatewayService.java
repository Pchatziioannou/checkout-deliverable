package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.checkout.payment.gateway.validator.PaymentValidator.validatePayment;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    try {
      validatePayment(paymentRequest);
      BankResponse bankResponse = bankClient.authorize(paymentRequest);

      PostPaymentResponse paymentResponse = PostPaymentResponse.builder()
          .setId(UUID.randomUUID())
          .setAuthorizationCode(bankResponse.getAuthorizationCode())
          .setStatus(
              bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED)
          .setCardNumberLastFour(Integer.parseInt(paymentRequest.getCardNumberLastFour()))
          .setExpiryMonth(paymentRequest.getExpiryMonth())
          .setExpiryYear(paymentRequest.getExpiryYear())
          .setCurrency(paymentRequest.getCurrency())
          .setAmount(paymentRequest.getAmount())
          .build();

      paymentsRepository.add(paymentResponse);
      return paymentResponse;
    } catch (EventProcessingException e) {
      return PostPaymentResponse.builder()
          .setId(UUID.randomUUID())
          .setAuthorizationCode("NA")
          .setStatus(PaymentStatus.REJECTED)
          .setCardNumberLastFour(Integer.parseInt(paymentRequest.getCardNumberLastFour()))
          .setExpiryMonth(paymentRequest.getExpiryMonth())
          .setExpiryYear(paymentRequest.getExpiryYear())
          .setCurrency(paymentRequest.getCurrency())
          .setAmount(paymentRequest.getAmount())
          .build();
    }
  }
}

