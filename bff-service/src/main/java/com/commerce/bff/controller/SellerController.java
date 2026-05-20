package com.commerce.bff.controller;

import com.commerce.bff.grpc.CatalogGrpcClient;
import com.commerce.grpc.catalog.*;
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

    // ── 판매자 신청 / 내 정보 ─────────────────────────────────────────────────

    @Operation(summary = "판매자 신청")
    @PostMapping("/api/sellers")
    public ResponseEntity<SellerResponse> register(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RegisterSellerRequest request) {
        SellerResponse response = catalogGrpcClient.registerSeller(userDetails.getUsername(), request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "내 판매자 정보 조회")
    @GetMapping("/api/sellers/me")
    public ResponseEntity<SellerResponse> getMyInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(catalogGrpcClient.getMySellerInfo(userDetails.getUsername()));
    }

    @Operation(summary = "내 판매자 정보 수정")
    @PutMapping("/api/sellers/me")
    public ResponseEntity<SellerResponse> updateMyInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateSellerInfoRequest request) {
        return ResponseEntity.ok(
                catalogGrpcClient.updateSellerInfo(userDetails.getUsername(), request));
    }

    // ── 관리자 전용 ───────────────────────────────────────────────────────────

    @Operation(summary = "판매자 목록 조회 (관리자)")
    @GetMapping("/api/admin/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GetSellerListResponse> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(catalogGrpcClient.getSellerList(status, page, size));
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
