package com.commerce.bff.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
    private String url;
    private boolean thumbnail;
    private int sortOrder;
}
