package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankRequest;
import com.checkout.payment.gateway.model.BankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankClient Tests")
class BankClientTest {

  @Mock
  private RestTemplate restTemplate;

  private BankClient bankClient;
  private static final String BANK_URL = "http://localhost:8080";

  @BeforeEach
  void setUp() {
    bankClient = new BankClient(restTemplate, BANK_URL);
  }

  @Test
  @DisplayName("Should successfully authorize payment with bank")
  void testAuthorizePaymentSuccess() {
    PostPaymentRequest request = createPaymentRequest();
    BankResponse bankResponse = new BankResponse(true, "AUTH12345");

    when(restTemplate.postForObject(
        eq("http://localhost:8080/payments"),
        any(BankRequest.class),
        eq(BankResponse.class)
    )).thenReturn(bankResponse);

    BankResponse response = bankClient.authorize(request);

    assertTrue(response.isAuthorized());
    assertEquals("AUTH12345", response.getAuthorizationCode());
  }

  @Test
  @DisplayName("Should handle declined payment from bank")
  void testAuthorizePaymentDeclined() {
    PostPaymentRequest request = createPaymentRequest();
    BankResponse bankResponse = new BankResponse(false, null);

    when(restTemplate.postForObject(
        anyString(),
        any(BankRequest.class),
        eq(BankResponse.class)
    )).thenReturn(bankResponse);

    BankResponse response = bankClient.authorize(request);

    assertFalse(response.isAuthorized());
    assertNull(response.getAuthorizationCode());
  }

  @Test
  @DisplayName("Should convert payment request to bank request correctly")
  void testPaymentRequestConversion() {
    PostPaymentRequest request = createPaymentRequest();

    when(restTemplate.postForObject(
        anyString(),
        any(BankRequest.class),
        eq(BankResponse.class)
    )).thenReturn(new BankResponse(true, "AUTH123"));

    bankClient.authorize(request);

    ArgumentCaptor<BankRequest> captor = ArgumentCaptor.forClass(BankRequest.class);
    verify(restTemplate).postForObject(anyString(), captor.capture(), eq(BankResponse.class));

    BankRequest bankRequest = captor.getValue();
    assertEquals("4532015112830366", bankRequest.getCardNumber());
    assertEquals("12/25", bankRequest.getExpiryDate());
    assertEquals("USD", bankRequest.getCurrency());
    assertEquals(1050, bankRequest.getAmount());
    assertEquals(123, bankRequest.getCvv());
  }

  @Test
  @DisplayName("Should format single-digit month with leading zero")
  void testSingleDigitMonthFormatting() {
    PostPaymentRequest request = createPaymentRequest();
    request.setExpiryMonth(5);
    request.setExpiryYear(2026);

    when(restTemplate.postForObject(
        anyString(),
        any(BankRequest.class),
        eq(BankResponse.class)
    )).thenReturn(new BankResponse(true, "AUTH123"));

    bankClient.authorize(request);

    ArgumentCaptor<BankRequest> captor = ArgumentCaptor.forClass(BankRequest.class);
    verify(restTemplate).postForObject(anyString(), captor.capture(), eq(BankResponse.class));

    assertEquals("05/26", captor.getValue().getExpiryDate());
  }

  @Test
  @DisplayName("Should throw exception on RestClientException")
  void testHandleRestClientException() {
    PostPaymentRequest request = createPaymentRequest();

    when(restTemplate.postForObject(
        anyString(),
        any(BankRequest.class),
        eq(BankResponse.class)
    )).thenThrow(new RestClientException("Connection timeout"));

    assertThrows(
        EventProcessingException.class,
        () -> bankClient.authorize(request)
    );
  }

  private PostPaymentRequest createPaymentRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(12);
    request.setExpiryYear(2025);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(123);
    return request;
  }
}
