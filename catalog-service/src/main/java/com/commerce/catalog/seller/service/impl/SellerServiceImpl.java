package com.commerce.catalog.seller.service.impl;

import com.commerce.catalog.kafka.SellerEventProducer;
import com.commerce.catalog.seller.dto.SellerRegisterRequest;
import com.commerce.catalog.seller.dto.SellerResponse;
import com.commerce.catalog.seller.dto.SellerUpdateRequest;
import com.commerce.catalog.seller.entity.Seller;
import com.commerce.catalog.seller.entity.SellerStatus;
import com.commerce.catalog.seller.repository.SellerRepository;
import com.commerce.catalog.seller.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerServiceImpl implements SellerService {

    private static final Logger log = LoggerFactory.getLogger(SellerServiceImpl.class);

    private final SellerRepository sellerRepository;
    private final SellerEventProducer sellerEventProducer;

    @Override
    @Transactional
    public SellerResponse register(String userId, SellerRegisterRequest request) {
        if (sellerRepository.existsByUserId(userId)) {
            throw new IllegalStateException("이미 판매자 신청을 완료한 계정입니다.");
        }
        if (sellerRepository.existsByBusinessNumber(request.getBusinessNumber())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        Seller seller = sellerRepository.save(Seller.builder()
                .sellerId(UUID.randomUUID().toString())
                .userId(userId)
                .businessName(request.getBusinessName())
                .businessNumber(request.getBusinessNumber())
                .ownerName(request.getOwnerName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(SellerStatus.PENDING)
                .build());

        log.info("[Seller] Register requested. userId={}, sellerId={}", userId, seller.getSellerId());
        return SellerResponse.from(seller);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerResponse getMyInfo(String userId) {
        Seller seller = findByUserId(userId);
        return SellerResponse.from(seller);
    }

    @Override
    @Transactional
    public SellerResponse updateMyInfo(String userId, SellerUpdateRequest request) {
        Seller seller = findByUserId(userId);
        seller.updateInfo(request.getBusinessName(), request.getPhone(), request.getEmail());
        return SellerResponse.from(seller);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellerResponse> findAll(SellerStatus status, Pageable pageable) {
        if (status != null) {
            return sellerRepository.findAllByStatus(status, pageable).map(SellerResponse::from);
        }
        return sellerRepository.findAll(pageable).map(SellerResponse::from);
    }

    @Override
    @Transactional
    public void approve(String sellerId) {
        Seller seller = findBySellerId(sellerId);
        seller.approve();
        log.info("[Seller] Approved. sellerId={}", sellerId);

        // 판매자 승인 → bff-service에 ROLE_SELLER 권한 부여 이벤트 발행
        sellerEventProducer.publishSellerApproved(seller.getUserId(), seller.getSellerId());
    }

    @Override
    @Transactional
    public void reject(String sellerId) {
        Seller seller = findBySellerId(sellerId);
        seller.reject();
        log.info("[Seller] Rejected. sellerId={}", sellerId);
    }

    @Override
    @Transactional
    public void suspend(String sellerId) {
        Seller seller = findBySellerId(sellerId);
        seller.suspend();
        log.info("[Seller] Suspended. sellerId={}", sellerId);

        // ↓ 판매자 정지 시 해당 판매자 상품 일괄 SUSPENDED 처리 위치
        // productService.suspendAllBySeller(sellerId);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private Seller findByUserId(String userId) {
        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("판매자 정보를 찾을 수 없습니다."));
    }

    private Seller findBySellerId(String sellerId) {
        return sellerRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다. sellerId=" + sellerId));
    }
}
