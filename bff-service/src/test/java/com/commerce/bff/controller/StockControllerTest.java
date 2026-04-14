package com.commerce.bff.controller;

import com.commerce.bff.stock.StockRedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = StockController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@DisplayName("StockController 단위 테스트")
class StockControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StockRedisService stockRedisService;

    // ── POST /api/stocks/{productId}/init ─────────────────────────────────────

    @Test
    @DisplayName("재고 초기화 성공 - 200 반환")
    void initStock_success() throws Exception {
        mockMvc.perform(post("/api/stocks/product-1/init")
                        .param("quantity", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.productId").value("product-1"))
                .andExpect(jsonPath("$.quantity").value(100));

        verify(stockRedisService).initStock("product-1", 100L);
    }

    @Test
    @DisplayName("quantity=0 이면 400 반환")
    void initStock_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/stocks/product-1/init")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(stockRedisService);
    }

    @Test
    @DisplayName("quantity 음수면 400 반환")
    void initStock_negativeQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/stocks/product-1/init")
                        .param("quantity", "-5"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockRedisService);
    }

    // ── PUT /api/stocks/{productId}/init ──────────────────────────────────────

    @Test
    @DisplayName("재고 재설정 성공 - quantity=0 허용")
    void resetStock_zeroAllowed() throws Exception {
        mockMvc.perform(put("/api/stocks/product-1/init")
                        .param("quantity", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(stockRedisService).initStock("product-1", 0L);
    }

    @Test
    @DisplayName("재고 재설정 - 음수 수량은 400")
    void resetStock_negativeQuantity_returns400() throws Exception {
        mockMvc.perform(put("/api/stocks/product-1/init")
                        .param("quantity", "-1"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/stocks/{productId} ───────────────────────────────────────────

    @Test
    @DisplayName("재고 조회 - 재고 있음")
    void getStock_available() throws Exception {
        when(stockRedisService.getStock("product-1")).thenReturn(50L);

        mockMvc.perform(get("/api/stocks/product-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    @DisplayName("재고 조회 - 재고 소진(0)")
    void getStock_outOfStock() throws Exception {
        when(stockRedisService.getStock("product-1")).thenReturn(0L);

        mockMvc.perform(get("/api/stocks/product-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.inStock").value(false));
    }

    @Test
    @DisplayName("재고 조회 - 미초기화 상품(-1)")
    void getStock_notInitialized() throws Exception {
        when(stockRedisService.getStock("product-999")).thenReturn(-1L);

        mockMvc.perform(get("/api/stocks/product-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("재고 정보가 없습니다. 초기화가 필요합니다."));
    }
}
