package com.commerce.catalog.product.service.impl;

import com.commerce.catalog.product.dto.ProductCreateRequest;
import com.commerce.catalog.product.dto.ProductResponse;
import com.commerce.catalog.product.entity.*;
import com.commerce.catalog.product.repository.ProductRepository;
import com.commerce.catalog.product.service.ProductService;
import com.commerce.catalog.seller.entity.Seller;
import com.commerce.catalog.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final SellerRepository  sellerRepository;

    @Override
    @Transactional
    public ProductResponse create(String userId, ProductCreateRequest request) {
        Seller seller = findApprovedSeller(userId);

        Product product = Product.builder()
                .productId(UUID.randomUUID().toString())
                .sellerId(seller.getSellerId())
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .status(ProductStatus.ON_SALE)
                .build();

        addImages(product, request.getImages());
        addOptions(product, request.getOptions());

        productRepository.save(product);

        log.info("[Product] Created. productId={}, sellerId={}", product.getProductId(), seller.getSellerId());
        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    public ProductResponse update(String userId, String productId, ProductCreateRequest request) {
        Seller seller  = findApprovedSeller(userId);
        Product product = findMyProduct(productId, seller.getSellerId());

        product.update(request.getName(), request.getDescription(),
                request.getBasePrice(), request.getCategoryId());

        product.getImages().clear();
        product.getOptions().clear();
        addImages(product, request.getImages());
        addOptions(product, request.getOptions());

        log.info("[Product] Updated. productId={}", productId);
        return ProductResponse.from(product);
    }

    @Override
    @Transactional
    public void delete(String userId, String productId) {
        Seller seller  = findApprovedSeller(userId);
        Product product = findMyProduct(productId, seller.getSellerId());
        product.suspend();
        log.info("[Product] Deleted(SUSPENDED). productId={}", productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(String userId, Pageable pageable) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
        return productRepository.findAllBySellerId(seller.getSellerId(), pageable)
                .map(ProductResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(String productId) {
        Product product = productRepository.findByProductId(productId)
                .filter(p -> p.getStatus() != ProductStatus.SUSPENDED)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return ProductResponse.from(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Long categoryId, Pageable pageable) {
        if (categoryId != null) {
            return productRepository
                    .findAllByCategoryIdAndStatus(categoryId, ProductStatus.ON_SALE, pageable)
                    .map(ProductResponse::from);
        }
        return productRepository
                .findAllByStatus(ProductStatus.ON_SALE, pageable)
                .map(ProductResponse::from);
    }

    @Override
    @Transactional
    public void suspendByAdmin(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        product.suspend();
        log.info("[Product] Suspended by admin. productId={}", productId);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private Seller findApprovedSeller(String userId) {
        Seller seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
        if (!seller.isApproved()) {
            throw new IllegalStateException("승인된 판매자만 상품을 등록할 수 있습니다.");
        }
        return seller;
    }

    private Product findMyProduct(String productId, String sellerId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalStateException("본인 상품만 수정할 수 있습니다.");
        }
        return product;
    }

    private void addImages(Product product, List<ProductCreateRequest.ProductImageRequest> images) {
        if (images == null) return;
        images.forEach(img -> product.getImages().add(
                ProductImage.builder()
                        .product(product)
                        .url(img.getUrl())
                        .isThumbnail(img.isThumbnail())
                        .sortOrder(img.getSortOrder())
                        .build()));
    }

    private void addOptions(Product product, List<ProductCreateRequest.ProductOptionRequest> options) {
        if (options == null) return;
        options.forEach(opt -> product.getOptions().add(
                ProductOption.builder()
                        .product(product)
                        .optionName(opt.getOptionName())
                        .optionValue(opt.getOptionValue())
                        .additionalPrice(opt.getAdditionalPrice())
                        .stockQuantity(opt.getStockQuantity())
                        .build()));
    }
}
