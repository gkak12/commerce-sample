package com.commerce.bff.mapper;

import com.commerce.bff.dto.product.ProductDetailResponse;
import com.commerce.bff.dto.product.ProductDto;
import com.commerce.bff.dto.product.ProductImageDto;
import com.commerce.bff.dto.product.ProductListResponse;
import com.commerce.bff.dto.product.ProductOptionDto;
import com.commerce.grpc.catalog.GetProductListResponse;
import com.commerce.grpc.catalog.GetProductResponse;
import com.commerce.grpc.catalog.ProductImageProto;
import com.commerce.grpc.catalog.ProductOptionProto;
import com.commerce.grpc.catalog.ProductProto;
import com.commerce.grpc.catalog.ProductResponse;
import org.mapstruct.Mapper;

/**
 * 상품 gRPC 응답 → DTO 변환 매퍼
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    // ── ProductImageProto → ProductImageDto ──────────────────────────────────

    default ProductImageDto toProductImageDto(ProductImageProto proto) {
        return ProductImageDto.builder()
                .url(proto.getUrl())
                .thumbnail(proto.getIsThumbnail())
                .sortOrder(proto.getSortOrder())
                .build();
    }

    // ── ProductOptionProto → ProductOptionDto ─────────────────────────────────

    default ProductOptionDto toProductOptionDto(ProductOptionProto proto) {
        return ProductOptionDto.builder()
                .optionName(proto.getOptionName())
                .optionValue(proto.getOptionValue())
                .additionalPrice(proto.getAdditionalPrice())
                .stockQuantity(proto.getStockQuantity())
                .build();
    }

    // ── ProductProto → ProductDto ─────────────────────────────────────────────

    default ProductDto toProductDto(ProductProto proto) {
        if (proto == null) return null;
        return ProductDto.builder()
                .productId(proto.getProductId())
                .sellerId(proto.getSellerId())
                .categoryId(proto.getCategoryId())
                .name(proto.getName())
                .description(proto.getDescription())
                .basePrice(proto.getBasePrice())
                .status(proto.getStatus())
                .createdAt(proto.getCreatedAt())
                .images(proto.getImagesList().stream()
                        .map(this::toProductImageDto)
                        .toList())
                .options(proto.getOptionsList().stream()
                        .map(this::toProductOptionDto)
                        .toList())
                .build();
    }

    // ── GetProductResponse → ProductDetailResponse ────────────────────────────

    default ProductDetailResponse toProductDetailResponse(GetProductResponse resp) {
        return ProductDetailResponse.builder()
                .found(resp.getFound())
                .product(resp.getFound() ? toProductDto(resp.getProduct()) : null)
                .build();
    }

    // ── ProductResponse (Create/Update 결과) → ProductDetailResponse ──────────

    default ProductDetailResponse toProductDetailResponse(ProductResponse resp) {
        return ProductDetailResponse.builder()
                .found(resp.getFound())
                .product(resp.getFound() ? toProductDto(resp.getProduct()) : null)
                .build();
    }

    // ── GetProductListResponse → ProductListResponse ──────────────────────────

    default ProductListResponse toProductListResponse(GetProductListResponse resp) {
        return ProductListResponse.builder()
                .products(resp.getProductsList().stream()
                        .map(this::toProductDto)
                        .toList())
                .totalCount(resp.getTotalCount())
                .totalPages(resp.getTotalPages())
                .currentPage(resp.getCurrentPage())
                .build();
    }
}
