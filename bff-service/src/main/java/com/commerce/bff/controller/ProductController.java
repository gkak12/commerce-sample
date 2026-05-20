package com.commerce.bff.controller;

import com.commerce.bff.dto.product.ProductDetailResponse;
import com.commerce.bff.dto.product.ProductListResponse;
import com.commerce.bff.grpc.CatalogGrpcClient;
import com.commerce.bff.mapper.ProductMapper;
import com.commerce.grpc.catalog.CreateProductRequest;
import com.commerce.grpc.catalog.UpdateProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "상품")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final CatalogGrpcClient catalogGrpcClient;
    private final ProductMapper productMapper;

    // ── 공개 API ──────────────────────────────────────────────────────────────

    @Operation(summary = "상품 목록 조회")
    @GetMapping("/api/products")
    public ResponseEntity<ProductListResponse> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productMapper.toProductListResponse(
                catalogGrpcClient.getProductList(categoryId, page, size)));
    }

    @Operation(summary = "상품 상세 조회")
    @GetMapping("/api/products/{productId}")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productMapper.toProductDetailResponse(
                catalogGrpcClient.getProduct(productId)));
    }

    // ── 판매자 전용 ───────────────────────────────────────────────────────────

    @Operation(summary = "상품 등록 (판매자)")
    @PostMapping("/api/products")
    public ResponseEntity<ProductDetailResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateProductRequest request) {
        CreateProductRequest requestWithUser = CreateProductRequest.newBuilder()
                .mergeFrom(request)
                .setUserId(userDetails.getUsername())
                .build();
        return ResponseEntity.status(201)
                .body(productMapper.toProductDetailResponse(
                        catalogGrpcClient.createProduct(requestWithUser)));
    }

    @Operation(summary = "상품 수정 (판매자)")
    @PutMapping("/api/products/{productId}")
    public ResponseEntity<ProductDetailResponse> update(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId,
            @RequestBody UpdateProductRequest request) {
        UpdateProductRequest requestWithUser = UpdateProductRequest.newBuilder()
                .mergeFrom(request)
                .setUserId(userDetails.getUsername())
                .setProductId(productId)
                .build();
        return ResponseEntity.ok(productMapper.toProductDetailResponse(
                catalogGrpcClient.updateProduct(requestWithUser)));
    }

    @Operation(summary = "상품 삭제 (판매자)")
    @DeleteMapping("/api/products/{productId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String productId) {
        catalogGrpcClient.deleteProduct(userDetails.getUsername(), productId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내 상품 목록 (판매자)")
    @GetMapping("/api/products/me")
    public ResponseEntity<ProductListResponse> getMyProducts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productMapper.toProductListResponse(
                catalogGrpcClient.getMyProducts(userDetails.getUsername(), page, size)));
    }

    // ── 관리자 전용 ───────────────────────────────────────────────────────────

    @Operation(summary = "상품 강제 중지 (관리자)")
    @PutMapping("/api/admin/products/{productId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> suspend(@PathVariable String productId) {
        catalogGrpcClient.suspendProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
