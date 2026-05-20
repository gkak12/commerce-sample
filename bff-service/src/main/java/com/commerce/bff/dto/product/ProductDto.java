package com.commerce.bff.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String productId;
    private String sellerId;
    private long categoryId;
    private String name;
    private String description;
    private String basePrice;
    private String status;
    private String createdAt;
    private List<ProductImageDto> images;
    private List<ProductOptionDto> options;
}
