package com.commerce.catalog.product.dto;

import com.commerce.catalog.product.entity.Product;
import com.commerce.catalog.product.entity.ProductStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ProductResponse {

    private final String productId;
    private final String sellerId;
    private final Long categoryId;
    private final String name;
    private final String description;
    private final BigDecimal basePrice;
    private final ProductStatus status;
    private final List<ImageResponse> images;
    private final List<OptionResponse> options;
    private final LocalDateTime createdAt;

    private ProductResponse(Product product) {
        this.productId   = product.getProductId();
        this.sellerId    = product.getSellerId();
        this.categoryId  = product.getCategoryId();
        this.name        = product.getName();
        this.description = product.getDescription();
        this.basePrice   = product.getBasePrice();
        this.status      = product.getStatus();
        this.createdAt   = product.getCreatedAt();
        this.images      = product.getImages().stream()
                .map(ImageResponse::new).toList();
        this.options     = product.getOptions().stream()
                .map(OptionResponse::new).toList();
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(product);
    }

    @Getter
    public static class ImageResponse {
        private final String url;
        private final boolean isThumbnail;
        private final int sortOrder;

        public ImageResponse(com.commerce.catalog.product.entity.ProductImage image) {
            this.url         = image.getUrl();
            this.isThumbnail = image.isThumbnail();
            this.sortOrder   = image.getSortOrder();
        }
    }

    @Getter
    public static class OptionResponse {
        private final String optionName;
        private final String optionValue;
        private final java.math.BigDecimal additionalPrice;
        private final int stockQuantity;

        public OptionResponse(com.commerce.catalog.product.entity.ProductOption option) {
            this.optionName      = option.getOptionName();
            this.optionValue     = option.getOptionValue();
            this.additionalPrice = option.getAdditionalPrice();
            this.stockQuantity   = option.getStockQuantity();
        }
    }
}
