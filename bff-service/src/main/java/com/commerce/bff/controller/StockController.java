package com.commerce.bff.controller;

import com.commerce.bff.stock.StockRedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 재고 관리 API
 *
 * - POST /api/stocks/{productId}/init  : 상품 재고 초기화 (관리자 전용)
 * - PUT  /api/stocks/{productId}/init  : 상품 재고 재설정 (관리자 전용)
 * - GET  /api/stocks/{productId}       : 현재 재고 조회
 */
@Tag(name = "재고", description = "Redis 기반 실시간 재고 관리 API")
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    private final StockRedisService stockRedisService;

    /**
     * 재고 초기화 (최초 등록 시 사용)
     * POST /api/stocks/{productId}/init?quantity=100
     */
    @Operation(summary = "재고 초기화", description = "상품 최초 등록 시 Redis에 재고를 설정합니다. ADMIN 권한 필요.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{productId}/init")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> initStock(
            @PathVariable String productId,
            @RequestParam long quantity) {

        if (quantity <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "수량은 1 이상이어야 합니다.",
                            "productId", productId
                    ));
        }

        stockRedisService.initStock(productId, quantity);
        log.info("[StockController] Stock initialized. productId={}, quantity={}", productId, quantity);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "재고가 초기화되었습니다.",
                "productId", productId,
                "quantity", quantity
        ));
    }

    /**
     * 재고 재설정 (덮어쓰기)
     * PUT /api/stocks/{productId}/init?quantity=200
     */
    @Operation(summary = "재고 재설정", description = "기존 재고를 덮어씁니다. ADMIN 권한 필요.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{productId}/init")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetStock(
            @PathVariable String productId,
            @RequestParam long quantity) {

        if (quantity < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "수량은 0 이상이어야 합니다.",
                            "productId", productId
                    ));
        }

        stockRedisService.initStock(productId, quantity);
        log.info("[StockController] Stock reset. productId={}, quantity={}", productId, quantity);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "재고가 재설정되었습니다.",
                "productId", productId,
                "quantity", quantity
        ));
    }

    /**
     * 현재 재고 조회
     * GET /api/stocks/{productId}
     */
    @Operation(summary = "재고 조회", description = "현재 재고 수량을 조회합니다. 인증 불필요.")
    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getStock(
            @Parameter(description = "상품 ID") @PathVariable String productId) {
        long stock = stockRedisService.getStock(productId);

        if (stock == -1L) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "재고 정보가 없습니다. 초기화가 필요합니다.",
                    "productId", productId,
                    "stock", -1
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "productId", productId,
                "stock", stock,
                "inStock", stock > 0
        ));
    }
}
