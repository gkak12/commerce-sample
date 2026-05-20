package com.commerce.bff.grpc;

import com.commerce.bff.dto.seller.SellerRegisterRequest;
import com.commerce.bff.dto.seller.SellerUpdateRequest;
import com.commerce.grpc.catalog.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * catalog-service gRPC 클라이언트
 *
 * 상품 조회, 판매자 조회/등록/수정, 관리자 판매자/상품 관리
 */
@Component
public class CatalogGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcClient.class);

    @GrpcClient("catalog-service")
    private CatalogServiceGrpc.CatalogServiceBlockingStub stub;

    // ── 상품 조회 (공개) ──────────────────────────────────────────────────────

    @CircuitBreaker(name = "catalog-service")
    public GetProductResponse getProduct(String productId) {
        log.debug("[gRPC] getProduct. productId={}", productId);
        return stub.getProduct(GetProductRequest.newBuilder()
                .setProductId(productId)
                .build());
    }

    @CircuitBreaker(name = "catalog-service")
    public GetProductListResponse getProductList(Long categoryId, int page, int size) {
        log.debug("[gRPC] getProductList. categoryId={}, page={}", categoryId, page);
        return stub.getProductList(GetProductListRequest.newBuilder()
                .setCategoryId(categoryId != null ? categoryId : 0L)
                .setPage(page)
                .setSize(size)
                .build());
    }

    // ── 판매자 ────────────────────────────────────────────────────────────────

    @CircuitBreaker(name = "catalog-service")
    public SellerResponse registerSeller(String userId, SellerRegisterRequest request) {
        log.debug("[gRPC] registerSeller. userId={}", userId);
        return stub.registerSeller(RegisterSellerRequest.newBuilder()
                .setUserId(userId)
                .setBusinessName(request.getBusinessName())
                .setBusinessNumber(request.getBusinessNumber())
                .setOwnerName(request.getOwnerName())
                .setPhone(request.getPhone())
                .setEmail(request.getEmail())
                .build());
    }

    @CircuitBreaker(name = "catalog-service")
    public SellerResponse getMySellerInfo(String userId) {
        return stub.getMySellerInfo(GetMySellerInfoRequest.newBuilder()
                .setUserId(userId)
                .build());
    }

    @CircuitBreaker(name = "catalog-service")
    public SellerResponse updateSellerInfo(String userId, SellerUpdateRequest request) {
        return stub.updateSellerInfo(UpdateSellerInfoRequest.newBuilder()
                .setUserId(userId)
                .setBusinessName(request.getBusinessName())
                .setPhone(request.getPhone())
                .setEmail(request.getEmail())
                .build());
    }

    // ── 상품 관리 (판매자) ────────────────────────────────────────────────────

    @CircuitBreaker(name = "catalog-service")
    public com.commerce.grpc.catalog.ProductResponse createProduct(CreateProductRequest request) {
        return stub.createProduct(request);
    }

    @CircuitBreaker(name = "catalog-service")
    public com.commerce.grpc.catalog.ProductResponse updateProduct(UpdateProductRequest request) {
        return stub.updateProduct(request);
    }

    @CircuitBreaker(name = "catalog-service")
    public EmptyResponse deleteProduct(String userId, String productId) {
        return stub.deleteProduct(DeleteProductRequest.newBuilder()
                .setUserId(userId)
                .setProductId(productId)
                .build());
    }

    @CircuitBreaker(name = "catalog-service")
    public GetProductListResponse getMyProducts(String userId, int page, int size) {
        return stub.getMyProducts(GetMyProductsRequest.newBuilder()
                .setUserId(userId)
                .setPage(page)
                .setSize(size)
                .build());
    }

    // ── 관리자 ────────────────────────────────────────────────────────────────

    @CircuitBreaker(name = "catalog-service")
    public GetSellerListResponse getSellerList(String status, int page, int size) {
        return stub.getSellerList(GetSellerListRequest.newBuilder()
                .setStatus(status != null ? status : "")
                .setPage(page)
                .setSize(size)
                .build());
    }

    @CircuitBreaker(name = "catalog-service")
    public void approveSeller(String sellerId) {
        stub.approveSeller(SellerActionRequest.newBuilder().setSellerId(sellerId).build());
    }

    @CircuitBreaker(name = "catalog-service")
    public void rejectSeller(String sellerId) {
        stub.rejectSeller(SellerActionRequest.newBuilder().setSellerId(sellerId).build());
    }

    @CircuitBreaker(name = "catalog-service")
    public void suspendSeller(String sellerId) {
        stub.suspendSeller(SellerActionRequest.newBuilder().setSellerId(sellerId).build());
    }

    @CircuitBreaker(name = "catalog-service")
    public void suspendProduct(String productId) {
        stub.suspendProduct(SuspendProductRequest.newBuilder().setProductId(productId).build());
    }
}
