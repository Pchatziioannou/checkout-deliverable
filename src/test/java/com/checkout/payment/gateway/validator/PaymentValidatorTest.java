package com.checkout.payment.gateway.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentValidator Tests")
class PaymentValidatorTest {

  @Test
  @DisplayName("Should validate a valid payment request successfully")
  void testValidPaymentRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(123);

    assertDoesNotThrow(() -> PaymentValidator.validatePayment(request));
  }

  @Test
  @DisplayName("Should reject null or empty card number")
  void testNullCardNumber() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber(null);
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(123);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertEquals("Card number is required", exception.getMessage());

    request.setCardNumber("");
    exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertEquals("Card number is required", exception.getMessage());
  }

  @Test
  @DisplayName("Should reject card number with less than 14 digits or more than 19")
  void testCardNumberTooShort() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("453201511283036");  // 15 digits, but testing below min
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(123);

    PostPaymentRequest shortCard = new PostPaymentRequest();
    shortCard.setCardNumber("4532015112830");  // 13 digits
    shortCard.setExpiryMonth(12);
    shortCard.setExpiryYear(2027);
    shortCard.setCurrency("USD");
    shortCard.setAmount(1050);
    shortCard.setCvv(123);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(shortCard)
    );
    assertTrue(exception.getMessage().contains("must be 14 to 19 digits long"));

    request.setCardNumber("45320151128303661234");  // 20 digits
    exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("must be 14 to 19 digits long"));
  }

  @Test
  @DisplayName("Should reject card number with non-numeric characters")
  void testCardNumberWithNonNumeric() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("453201511283036A");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(123);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertEquals("Card number must contain only numeric characters", exception.getMessage());
  }

  @Test
  @DisplayName("Should reject invalid expiry month (less than 1 or greater than 12)")
  void testInvalidExpiryMonthTooLow() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(0);
    request.setExpiryYear(2027);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(123);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("must be between 1 and 12"));

    request.setExpiryMonth(13);
    exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("must be between 1 and 12"));
  }

  @Test
  @DisplayName("Should reject expired card")
  void testExpiredCard() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(1);
    request.setExpiryYear(2020);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(123);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("is in the past"));
  }

  @Test
  @DisplayName("Should reject null or lowercase currency")
  void testNullCurrency() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency(null);
    request.setAmount(1050);
    request.setCvv(123);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertEquals("Currency is required", exception.getMessage());


    request.setCurrency("usd");
    exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("must contain only uppercase letters"));
  }

  @Test
  @DisplayName("Should validate all supported currencies")
  void testAllSupportedCurrencies() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setAmount(1050);
    request.setCvv(123);

    for (String currency : new String[]{"USD", "EUR", "GBP"}) {
      request.setCurrency(currency);
      assertDoesNotThrow(() -> PaymentValidator.validatePayment(request));
    }
  }

  @Test
  @DisplayName("Should reject zero or negative amount")
  void testZeroOrNegativeAmount() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("USD");
    request.setAmount(0);
    request.setCvv(123);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("must be a positive integer"));

    request.setAmount(-100);
    exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("must be a positive integer"));
  }

  @Test
  @DisplayName("Should reject CVV with less than 3 digits or more than 4")
  void testCvvTooShortOrTooLong() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4532015112830366");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("USD");
    request.setAmount(1050);
    request.setCvv(12);  // 2 digits

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("must be 3 to 4 digits long"));


    request.setCvv(12345);  // 5 digits
    exception = assertThrows(
    EventProcessingException.class,
        () -> PaymentValidator.validatePayment(request)
    );
    assertTrue(exception.getMessage().contains("must be 3 to 4 digits long"));
  }

}
