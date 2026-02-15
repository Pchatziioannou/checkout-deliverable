package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentGatewayService Tests")
class PaymentGatewayServiceTest {

  @Mock
  private BankClient bankClient;

  @Mock
  private PaymentsRepository paymentsRepository;

  private PaymentGatewayService service;

  @BeforeEach
  void setUp() {
    service = new PaymentGatewayService(paymentsRepository, bankClient);
  }

  @Test
  @DisplayName("Should process payment successfully when bank authorizes")
  void testProcessPaymentAuthorized() {
    PostPaymentRequest request = createValidPaymentRequest();
    BankResponse bankResponse = new BankResponse(true, "AUTH12345");

    when(bankClient.authorize(request)).thenReturn(bankResponse);

    PostPaymentResponse response = service.processPayment(request);

    assertNotNull(response.getId());
    assertEquals("AUTH12345", response.getAuthorizationCode());
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    assertEquals(366, response.getCardNumberLastFour());
    assertEquals(12, response.getExpiryMonth());
    assertEquals(2027, response.getExpiryYear());
    assertEquals("USD", response.getCurrency());
    assertEquals(1050, response.getAmount());
    verify(paymentsRepository, times(1)).add(any(PostPaymentResponse.class));
    verify(bankClient, times(1)).authorize(any(PostPaymentRequest.class));
  }

  @Test
  @DisplayName("Should set DECLINED status when bank declines payment")
  void testProcessPaymentDeclined() {
    PostPaymentRequest request = createValidPaymentRequest();
    BankResponse bankResponse = new BankResponse(false, null);

    when(bankClient.authorize(request)).thenReturn(bankResponse);

    PostPaymentResponse response = service.processPayment(request);

    assertNotNull(response.getId());
    assertEquals(PaymentStatus.DECLINED, response.getStatus());
    assertNull(response.getAuthorizationCode());
    verify(paymentsRepository, times(1)).add(any(PostPaymentResponse.class));
    verify(bankClient, times(1)).authorize(any(PostPaymentRequest.class));
  }


  @Test
  @DisplayName("Should retrieve payment by ID successfully")
  void testGetPaymentById() {
    UUID paymentId = UUID.randomUUID();
    PostPaymentResponse expectedPayment = PostPaymentResponse.builder()
        .setId(paymentId)
        .setAmount(1050)
        .setStatus(PaymentStatus.AUTHORIZED)
        .build();

    when(paymentsRepository.get(paymentId)).thenReturn(java.util.Optional.of(expectedPayment));

    PostPaymentResponse response = service.getPaymentById(paymentId);

    assertEquals(paymentId, response.getId());
    assertEquals(1050, response.getAmount());
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    verify(paymentsRepository, times(1)).get(paymentId);
  }

  @Test
  @DisplayName("Should throw exception when payment not found")
  void testGetPaymentByIdNotFound() {
    UUID paymentId = UUID.randomUUID();

    when(paymentsRepository.get(paymentId)).thenReturn(java.util.Optional.empty());

    assertThrows(
        EventProcessingException.class,
        () -> service.getPaymentById(paymentId)
    );
    verify(paymentsRepository, times(1)).get(paymentId);
  }

  @Test
  @DisplayName("Should generate unique ID for each payment")
  void testPaymentIdIsUnique() {
    PostPaymentRequest request = createValidPaymentRequest();
    BankResponse bankResponse = new BankResponse(true, "AUTH12345");

    when(bankClient.authorize(request)).thenReturn(bankResponse);

    PostPaymentResponse response1 = service.processPayment(request);
    PostPaymentResponse response2 = service.processPayment(request);

    assertNotEquals(response1.getId(), response2.getId());
  }


  private PostPaymentRequest createValidPaymentRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(123);
    return request;
  }
}
