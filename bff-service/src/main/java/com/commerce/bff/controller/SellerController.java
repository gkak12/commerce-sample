package com.commerce.bff.controller;

import com.commerce.bff.dto.seller.SellerDetailResponse;
import com.commerce.bff.dto.seller.SellerListResponse;
import com.commerce.bff.dto.seller.SellerRegisterRequest;
import com.commerce.bff.dto.seller.SellerUpdateRequest;
import com.commerce.bff.grpc.CatalogGrpcClient;
import com.commerce.bff.mapper.SellerMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "판매자")
@RestController
@RequiredArgsConstructor
public class SellerController {

    private final CatalogGrpcClient catalogGrpcClient;
    private final SellerMapper sellerMapper;

    // ── 판매자 신청 / 내 정보 ─────────────────────────────────────────────────

    @Operation(summary = "판매자 신청")
    @PostMapping("/api/sellers")
    public ResponseEntity<SellerDetailResponse> register(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SellerRegisterRequest request) {
        return ResponseEntity.status(201)
                .body(sellerMapper.toSellerDetailResponse(
                        catalogGrpcClient.registerSeller(userDetails.getUsername(), request)));
    }

    @Operation(summary = "내 판매자 정보 조회")
    @GetMapping("/api/sellers/me")
    public ResponseEntity<SellerDetailResponse> getMyInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(sellerMapper.toSellerDetailResponse(
                catalogGrpcClient.getMySellerInfo(userDetails.getUsername())));
    }

    @Operation(summary = "내 판매자 정보 수정")
    @PutMapping("/api/sellers/me")
    public ResponseEntity<SellerDetailResponse> updateMyInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SellerUpdateRequest request) {
        return ResponseEntity.ok(sellerMapper.toSellerDetailResponse(
                catalogGrpcClient.updateSellerInfo(userDetails.getUsername(), request)));
    }

    // ── 관리자 전용 ───────────────────────────────────────────────────────────

    @Operation(summary = "판매자 목록 조회 (관리자)")
    @GetMapping("/api/admin/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerListResponse> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(sellerMapper.toSellerListResponse(
                catalogGrpcClient.getSellerList(status, page, size)));
    }

    @Operation(summary = "판매자 승인 (관리자)")
    @PutMapping("/api/admin/sellers/{sellerId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approve(@PathVariable String sellerId) {
        catalogGrpcClient.approveSeller(sellerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "판매자 거절 (관리자)")
    @PutMapping("/api/admin/sellers/{sellerId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reject(@PathVariable String sellerId) {
        catalogGrpcClient.rejectSeller(sellerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "판매자 정지 (관리자)")
    @PutMapping("/api/admin/sellers/{sellerId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspend(@PathVariable String sellerId) {
        catalogGrpcClient.suspendSeller(sellerId);
        return ResponseEntity.noContent().build();
    }
}
