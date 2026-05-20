package com.commerce.catalog.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductCreateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Positive
    private BigDecimal basePrice;

    @NotNull
    private Long categoryId;

    private List<ProductImageRequest> images;

    private List<ProductOptionRequest> options;

    @Getter
    @Setter
    public static class ProductImageRequest {
        private String url;
        private boolean isThumbnail;
        private int sortOrder;
    }

    @Getter
    @Setter
    public static class ProductOptionRequest {
        private String optionName;
        private String optionValue;
        private BigDecimal additionalPrice;
        private int stockQuantity;
    }
}
