package com.checkout.payment.gateway.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentsRepository Tests")
class PaymentsRepositoryTest {

  private PaymentsRepository repository;

  @BeforeEach
  void setUp() {
    repository = new PaymentsRepository();
  }


  @Test
  @DisplayName("Should store multiple payments independently")
  void testAddMultiplePayments() {
    PostPaymentResponse payment1 = createPayment(1050, "USD", PaymentStatus.AUTHORIZED);
    PostPaymentResponse payment2 = createPayment(5000, "EUR", PaymentStatus.DECLINED);
    PostPaymentResponse payment3 = createPayment(2500, "GBP", PaymentStatus.REJECTED);

    repository.add(payment1);
    repository.add(payment2);
    repository.add(payment3);

    assertEquals(payment1.getAmount(), repository.get(payment1.getId()).get().getAmount());
    assertEquals(payment2.getAmount(), repository.get(payment2.getId()).get().getAmount());
    assertEquals(payment3.getAmount(), repository.get(payment3.getId()).get().getAmount());
  }

  @Test
  @DisplayName("Should return empty Optional when payment not found")
  void testGetNonExistentPayment() {
    Optional<PostPaymentResponse> retrieved = repository.get(UUID.randomUUID());
    assertTrue(retrieved.isEmpty());
  }

  private PostPaymentResponse createPayment(int amount, String currency, PaymentStatus status) {
    return PostPaymentResponse.builder()
        .setId(UUID.randomUUID())
        .setAmount(amount)
        .setCurrency(currency)
        .setStatus(status)
        .setExpiryMonth(12)
        .setExpiryYear(2025)
        .setCardNumberLastFour(1234)
        .setAuthorizationCode("AUTH123")
        .build();
  }
}
