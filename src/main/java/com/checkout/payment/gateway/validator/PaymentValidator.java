package com.checkout.payment.gateway.validator;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.YearMonth;

public class PaymentValidator {

  private static final String[] ALLOWED_CURRENCIES = {"USD", "EUR", "GBP"};
  private static final int MIN_CARD_LENGTH = 14;
  private static final int MAX_CARD_LENGTH = 19;
  private static final int MIN_CVV_LENGTH = 3;
  private static final int MAX_CVV_LENGTH = 4;

  public static void validatePayment(PostPaymentRequest request) {
    validateCardNumber(request.getCardNumber());
    validateExpiryDate(request.getExpiryMonth(), request.getExpiryYear());
    validateCurrency(request.getCurrency());
    validateAmount(request.getAmount());
    validateCvv(request.getCvv());
  }

  private static void validateCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.isEmpty()) {
      throw new EventProcessingException("Card number is required");
    }

    if (cardNumber.length() < MIN_CARD_LENGTH || cardNumber.length() > MAX_CARD_LENGTH) {
      throw new EventProcessingException(
          String.format("Card number must be %d to %d digits long, but got %d",
              MIN_CARD_LENGTH, MAX_CARD_LENGTH, cardNumber.length()));
    }

    if (!cardNumber.matches("\\d+")) {
      throw new EventProcessingException("Card number must contain only numeric characters");
    }
  }

  private static void validateExpiryDate(int expiryMonth, int expiryYear) {
    if (expiryMonth < 1 || expiryMonth > 12) {
      throw new EventProcessingException(
          String.format("Expiry month must be between 1 and 12, but got %d", expiryMonth));
    }

    YearMonth expiryYearMonth = YearMonth.of(expiryYear, expiryMonth);
    YearMonth currentYearMonth = YearMonth.now();

    if (expiryYearMonth.isBefore(currentYearMonth)) {
      throw new EventProcessingException(
          String.format("Card expiry date %d/%d is in the past", expiryMonth, expiryYear));
    }
  }

  private static void validateCurrency(String currency) {
    if (currency == null || currency.isEmpty()) {
      throw new EventProcessingException("Currency is required");
    }

    if (currency.length() != 3) {
      throw new EventProcessingException(
          String.format("Currency must be a 3-character code, but got '%s'", currency));
    }

    if (!currency.matches("[A-Z]+")) {
      throw new EventProcessingException(
          String.format("Currency must contain only uppercase letters, but got '%s'", currency));
    }

    boolean isAllowed = false;
    for (String allowed : ALLOWED_CURRENCIES) {
      if (allowed.equals(currency)) {
        isAllowed = true;
        break;
      }
    }

    if (!isAllowed) {
      throw new EventProcessingException(
          String.format("Currency '%s' is not supported. Allowed currencies: USD, EUR, GBP", currency));
    }
  }

  private static void validateAmount(int amount) {
    if (amount <= 0) {
      throw new EventProcessingException(
          String.format("Amount must be a positive integer, but got %d", amount));
    }
  }

  private static void validateCvv(int cvv) {
    String cvvStr = String.valueOf(cvv);

    if (cvvStr.length() < MIN_CVV_LENGTH || cvvStr.length() > MAX_CVV_LENGTH) {
      throw new EventProcessingException(
          String.format("CVV must be %d to %d digits long, but got %d",
              MIN_CVV_LENGTH, MAX_CVV_LENGTH, cvvStr.length()));
    }
  }
}
