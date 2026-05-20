package com.commerce.catalog.grpc;

import com.commerce.catalog.product.dto.ProductCreateRequest;
import com.commerce.catalog.product.dto.ProductResponse;
import com.commerce.catalog.product.service.ProductService;
import com.commerce.catalog.seller.dto.SellerRegisterRequest;
import com.commerce.catalog.seller.dto.SellerUpdateRequest;
import com.commerce.catalog.seller.entity.SellerStatus;
import com.commerce.catalog.seller.service.SellerService;
import com.commerce.grpc.catalog.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * catalog-service gRPC 서버 구현체
 *
 * CatalogServiceGrpc.CatalogServiceImplBase 는 proto에서 생성된 하나의 서비스 정의이므로
 * @GrpcService 빈은 반드시 하나만 존재해야 합니다.
 * → SellerService / ProductService 에 각각 위임하여 관심사를 분리합니다.
 */
@GrpcService
@RequiredArgsConstructor
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcService.class);

    private final SellerService sellerService;
    private final ProductService productService;

    // ═══════════════════════════════════════════════════════════════════════════
    // 판매자 (Seller)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void registerSeller(RegisterSellerRequest request,
                               StreamObserver<SellerResponse> responseObserver) {
        try {
            SellerRegisterRequest dto = new SellerRegisterRequest();
            dto.setBusinessName(request.getBusinessName());
            dto.setBusinessNumber(request.getBusinessNumber());
            dto.setOwnerName(request.getOwnerName());
            dto.setPhone(request.getPhone());
            dto.setEmail(request.getEmail());

            com.commerce.catalog.seller.dto.SellerResponse result =
                    sellerService.register(request.getUserId(), dto);

            responseObserver.onNext(toSellerResponse(result));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[gRPC] registerSeller failed. userId={}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void getMySellerInfo(GetMySellerInfoRequest request,
                                StreamObserver<SellerResponse> responseObserver) {
        try {
            com.commerce.catalog.seller.dto.SellerResponse result =
                    sellerService.getMyInfo(request.getUserId());
            responseObserver.onNext(toSellerResponse(result));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void updateSellerInfo(UpdateSellerInfoRequest request,
                                 StreamObserver<SellerResponse> responseObserver) {
        try {
            SellerUpdateRequest dto = new SellerUpdateRequest();
            dto.setBusinessName(request.getBusinessName());
            dto.setPhone(request.getPhone());
            dto.setEmail(request.getEmail());

            com.commerce.catalog.seller.dto.SellerResponse result =
                    sellerService.updateMyInfo(request.getUserId(), dto);

            responseObserver.onNext(toSellerResponse(result));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void getSellerList(GetSellerListRequest request,
                              StreamObserver<GetSellerListResponse> responseObserver) {
        try {
            SellerStatus status = request.getStatus().isBlank()
                    ? null : SellerStatus.valueOf(request.getStatus());

            Page<com.commerce.catalog.seller.dto.SellerResponse> page =
                    sellerService.findAll(status, PageRequest.of(request.getPage(), request.getSize()));

            GetSellerListResponse response = GetSellerListResponse.newBuilder()
                    .addAllSellers(page.getContent().stream().map(this::toSellerProto).toList())
                    .setTotalCount((int) page.getTotalElements())
                    .setTotalPages(page.getTotalPages())
                    .setCurrentPage(page.getNumber())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void approveSeller(SellerActionRequest request,
                              StreamObserver<EmptyResponse> responseObserver) {
        handleAction(() -> sellerService.approve(request.getSellerId()), responseObserver);
    }

    @Override
    public void rejectSeller(SellerActionRequest request,
                             StreamObserver<EmptyResponse> responseObserver) {
        handleAction(() -> sellerService.reject(request.getSellerId()), responseObserver);
    }

    @Override
    public void suspendSeller(SellerActionRequest request,
                              StreamObserver<EmptyResponse> responseObserver) {
        handleAction(() -> sellerService.suspend(request.getSellerId()), responseObserver);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 상품 (Product)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void getProduct(GetProductRequest request,
                           StreamObserver<GetProductResponse> responseObserver) {
        try {
            ProductResponse product = productService.getProduct(request.getProductId());
            responseObserver.onNext(GetProductResponse.newBuilder()
                    .setFound(true)
                    .setProduct(toProductProto(product))
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onNext(GetProductResponse.newBuilder().setFound(false).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getProductList(GetProductListRequest request,
                               StreamObserver<GetProductListResponse> responseObserver) {
        try {
            Long categoryId = request.getCategoryId() == 0 ? null : request.getCategoryId();
            Page<ProductResponse> page = productService.getProducts(
                    categoryId, PageRequest.of(request.getPage(), request.getSize()));

            responseObserver.onNext(GetProductListResponse.newBuilder()
                    .addAllProducts(page.getContent().stream().map(this::toProductProto).toList())
                    .setTotalCount((int) page.getTotalElements())
                    .setTotalPages(page.getTotalPages())
                    .setCurrentPage(page.getNumber())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void createProduct(CreateProductRequest request,
                              StreamObserver<com.commerce.grpc.catalog.ProductResponse> responseObserver) {
        try {
            ProductResponse result = productService.create(request.getUserId(), toCreateRequest(request));
            responseObserver.onNext(toProductProtoResponse(result));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[gRPC] createProduct failed. userId={}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void updateProduct(UpdateProductRequest request,
                              StreamObserver<com.commerce.grpc.catalog.ProductResponse> responseObserver) {
        try {
            ProductResponse result = productService.update(
                    request.getUserId(), request.getProductId(), toUpdateRequest(request));
            responseObserver.onNext(toProductProtoResponse(result));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void deleteProduct(DeleteProductRequest request,
                              StreamObserver<EmptyResponse> responseObserver) {
        handleAction(() -> productService.delete(request.getUserId(), request.getProductId()),
                responseObserver);
    }

    @Override
    public void getMyProducts(GetMyProductsRequest request,
                              StreamObserver<GetProductListResponse> responseObserver) {
        try {
            Page<ProductResponse> page = productService.getMyProducts(
                    request.getUserId(), PageRequest.of(request.getPage(), request.getSize()));

            responseObserver.onNext(GetProductListResponse.newBuilder()
                    .addAllProducts(page.getContent().stream().map(this::toProductProto).toList())
                    .setTotalCount((int) page.getTotalElements())
                    .setTotalPages(page.getTotalPages())
                    .setCurrentPage(page.getNumber())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void suspendProduct(SuspendProductRequest request,
                               StreamObserver<EmptyResponse> responseObserver) {
        handleAction(() -> productService.suspendByAdmin(request.getProductId()), responseObserver);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 공통 헬퍼
    // ═══════════════════════════════════════════════════════════════════════════

    private void handleAction(Runnable action, StreamObserver<EmptyResponse> observer) {
        try {
            action.run();
            observer.onNext(EmptyResponse.newBuilder().setSuccess(true).build());
            observer.onCompleted();
        } catch (Exception e) {
            observer.onError(Status.INTERNAL.withDescription(e.getMessage()).asException());
        }
    }

    // ── Seller 변환 ──────────────────────────────────────────────────────────

    private SellerResponse toSellerResponse(com.commerce.catalog.seller.dto.SellerResponse s) {
        return SellerResponse.newBuilder()
                .setFound(true)
                .setSeller(toSellerProto(s))
                .build();
    }

    private SellerProto toSellerProto(com.commerce.catalog.seller.dto.SellerResponse s) {
        return SellerProto.newBuilder()
                .setSellerId(s.getSellerId())
                .setBusinessName(s.getBusinessName())
                .setBusinessNumber(s.getBusinessNumber())
                .setOwnerName(s.getOwnerName())
                .setPhone(s.getPhone())
                .setEmail(s.getEmail())
                .setStatus(s.getStatus().name())
                .setCreatedAt(s.getCreatedAt().toString())
                .build();
    }

    // ── Product 변환 ─────────────────────────────────────────────────────────

    private ProductProto toProductProto(ProductResponse p) {
        List<ProductImageProto> images = p.getImages().stream()
                .map(img -> ProductImageProto.newBuilder()
                        .setUrl(img.getUrl())
                        .setIsThumbnail(img.isThumbnail())
                        .setSortOrder(img.getSortOrder())
                        .build())
                .toList();

        List<ProductOptionProto> options = p.getOptions().stream()
                .map(opt -> ProductOptionProto.newBuilder()
                        .setOptionName(opt.getOptionName())
                        .setOptionValue(opt.getOptionValue())
                        .setAdditionalPrice(opt.getAdditionalPrice().toPlainString())
                        .setStockQuantity(opt.getStockQuantity())
                        .build())
                .toList();

        return ProductProto.newBuilder()
                .setProductId(p.getProductId())
                .setSellerId(p.getSellerId())
                .setCategoryId(p.getCategoryId())
                .setName(p.getName())
                .setDescription(p.getDescription() != null ? p.getDescription() : "")
                .setBasePrice(p.getBasePrice().toPlainString())
                .setStatus(p.getStatus().name())
                .setCreatedAt(p.getCreatedAt().toString())
                .addAllImages(images)
                .addAllOptions(options)
                .build();
    }

    private com.commerce.grpc.catalog.ProductResponse toProductProtoResponse(ProductResponse p) {
        return com.commerce.grpc.catalog.ProductResponse.newBuilder()
                .setFound(true)
                .setProduct(toProductProto(p))
                .build();
    }

    private ProductCreateRequest toCreateRequest(CreateProductRequest req) {
        ProductCreateRequest dto = new ProductCreateRequest();
        dto.setName(req.getName());
        dto.setDescription(req.getDescription());
        dto.setBasePrice(new BigDecimal(req.getBasePrice()));
        dto.setCategoryId(req.getCategoryId());
        dto.setImages(req.getImagesList().stream()
                .map(img -> {
                    ProductCreateRequest.ProductImageRequest i = new ProductCreateRequest.ProductImageRequest();
                    i.setUrl(img.getUrl());
                    i.setThumbnail(img.getIsThumbnail());
                    i.setSortOrder(img.getSortOrder());
                    return i;
                }).toList());
        dto.setOptions(req.getOptionsList().stream()
                .map(opt -> {
                    ProductCreateRequest.ProductOptionRequest o = new ProductCreateRequest.ProductOptionRequest();
                    o.setOptionName(opt.getOptionName());
                    o.setOptionValue(opt.getOptionValue());
                    o.setAdditionalPrice(new BigDecimal(opt.getAdditionalPrice()));
                    o.setStockQuantity(opt.getStockQuantity());
                    return o;
                }).toList());
        return dto;
    }

    private ProductCreateRequest toUpdateRequest(UpdateProductRequest req) {
        ProductCreateRequest dto = new ProductCreateRequest();
        dto.setName(req.getName());
        dto.setDescription(req.getDescription());
        dto.setBasePrice(new BigDecimal(req.getBasePrice()));
        dto.setCategoryId(req.getCategoryId());
        dto.setImages(req.getImagesList().stream()
                .map(img -> {
                    ProductCreateRequest.ProductImageRequest i = new ProductCreateRequest.ProductImageRequest();
                    i.setUrl(img.getUrl());
                    i.setThumbnail(img.getIsThumbnail());
                    i.setSortOrder(img.getSortOrder());
                    return i;
                }).toList());
        dto.setOptions(req.getOptionsList().stream()
                .map(opt -> {
                    ProductCreateRequest.ProductOptionRequest o = new ProductCreateRequest.ProductOptionRequest();
                    o.setOptionName(opt.getOptionName());
                    o.setOptionValue(opt.getOptionValue());
                    o.setAdditionalPrice(new BigDecimal(opt.getAdditionalPrice()));
                    o.setStockQuantity(opt.getStockQuantity());
                    return o;
                }).toList());
        return dto;
    }
}
