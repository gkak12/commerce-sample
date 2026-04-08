package com.commerce.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotBlank
    private String productId;

    @NotBlank
    private String productName;

    @Positive
    private int quantity;

    @Positive
    private BigDecimal price;
}
