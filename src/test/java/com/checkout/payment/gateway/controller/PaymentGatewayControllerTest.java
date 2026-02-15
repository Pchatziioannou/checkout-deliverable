package com.checkout.payment.gateway.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("PaymentGatewayController Tests")
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private PaymentsRepository paymentsRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    paymentsRepository.getPayments().clear();
  }

  @Test
  @DisplayName("Should retrieve payment by ID successfully")
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {

    paymentsRepository.getPayments().clear();
    UUID paymentId = UUID.randomUUID();
    PostPaymentResponse payment = PostPaymentResponse.builder()
        .setId(paymentId)
        .setAmount(1050)
        .setCurrency("USD")
        .setStatus(PaymentStatus.AUTHORIZED)
        .setExpiryMonth(12)
        .setExpiryYear(2027)
        .setCardNumberLastFour(4321)
        .setAuthorizationCode("AUTH123")
        .build();

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId.toString()))
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()))
        .andExpect(jsonPath("$.authorizationCode").value(payment.getAuthorizationCode()));
  }

  @Test
  @DisplayName("Should return 404 when payment not found")
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));
  }

  @Test
  @DisplayName("Should create payment via POST and retrieve it via GET using repository")
  void testCreatePaymentThenRetrieveFromRepository() throws Exception {
    String paymentJson = """
        {
          "card_number": "4532015112830366",
          "expiry_month": 12,
          "expiry_year": 2027,
          "currency": "USD",
          "amount": 1050,
          "cvv": 123
        }
        """;

    // Step 1: Create payment via POST
    var createResult = mvc.perform(MockMvcRequestBuilders.post("/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(paymentJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.cardNumberLastFour").value(366))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2027))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(1050))
        .andReturn();

    // Step 2: Extract payment ID from POST response
    String responseContent = createResult.getResponse().getContentAsString();
    PostPaymentResponse createdPayment = objectMapper.readValue(responseContent,
        PostPaymentResponse.class);
    UUID paymentId = createdPayment.getId();

    // Step 3: Verify payment was stored in repository
    assertEquals(1, paymentsRepository.getPayments().size());
    assertTrue(paymentsRepository.get(paymentId).isPresent());

    // Step 4: Retrieve payment via GET
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId.toString()))
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.cardNumberLastFour").value(366))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2027))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(1050));

    // Step 5: Verify repository still has the payment
    assertEquals(1, paymentsRepository.getPayments().size());
    assertTrue(paymentsRepository.get(paymentId).isPresent());

  }
}
