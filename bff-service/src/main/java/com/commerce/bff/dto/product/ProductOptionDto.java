package com.commerce.bff.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionDto {
    private String optionName;
    private String optionValue;
    private String additionalPrice;
    private int stockQuantity;
}
