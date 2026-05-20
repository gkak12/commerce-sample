package com.commerce.bff.mapper;

import com.commerce.bff.dto.seller.SellerDetailResponse;
import com.commerce.bff.dto.seller.SellerDto;
import com.commerce.bff.dto.seller.SellerListResponse;
import com.commerce.grpc.catalog.GetSellerListResponse;
import com.commerce.grpc.catalog.SellerProto;
import com.commerce.grpc.catalog.SellerResponse;
import org.mapstruct.Mapper;

/**
 * 판매자 gRPC 응답 → DTO 변환 매퍼
 */
@Mapper(componentModel = "spring")
public interface SellerMapper {

    // ── SellerProto → SellerDto ───────────────────────────────────────────────

    default SellerDto toSellerDto(SellerProto proto) {
        if (proto == null) return null;
        return SellerDto.builder()
                .sellerId(proto.getSellerId())
                .businessName(proto.getBusinessName())
                .businessNumber(proto.getBusinessNumber())
                .ownerName(proto.getOwnerName())
                .phone(proto.getPhone())
                .email(proto.getEmail())
                .status(proto.getStatus())
                .createdAt(proto.getCreatedAt())
                .build();
    }

    // ── SellerResponse → SellerDetailResponse ────────────────────────────────

    default SellerDetailResponse toSellerDetailResponse(SellerResponse resp) {
        return SellerDetailResponse.builder()
                .found(resp.getFound())
                .seller(resp.getFound() ? toSellerDto(resp.getSeller()) : null)
                .build();
    }

    // ── GetSellerListResponse → SellerListResponse ────────────────────────────

    default SellerListResponse toSellerListResponse(GetSellerListResponse resp) {
        return SellerListResponse.builder()
                .sellers(resp.getSellersList().stream()
                        .map(this::toSellerDto)
                        .toList())
                .totalCount(resp.getTotalCount())
                .totalPages(resp.getTotalPages())
                .currentPage(resp.getCurrentPage())
                .build();
    }
}
