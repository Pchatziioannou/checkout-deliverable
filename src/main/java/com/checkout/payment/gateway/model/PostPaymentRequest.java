package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  private String cardNumber;
  @JsonProperty("expiry_month")
  private int expiryMonth;
  @JsonProperty("expiry_year")
  private int expiryYear;
  private String currency;
  @JsonPropertyDescription("Amount in minor currency units (e.g., cents for USD). " +
      "Example: $10.50 should be sent as 1050")
  private int amount;
  private int cvv;

  @JsonIgnore
  public String getCardNumberLastFour() {
    if (cardNumber != null && cardNumber.length() >= 4) {
      return cardNumber.substring(cardNumber.length() - 4);
    }
    return "";
  }

  @JsonIgnore
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumber=" + maskCardNumber(cardNumber) +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=" + cvv +
        '}';
  }

  private static String maskCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < 4) {
      return cardNumber;
    }
    String lastFour = cardNumber.substring(cardNumber.length() - 4);
    return "*".repeat(cardNumber.length() - 4) + lastFour;
  }
}
