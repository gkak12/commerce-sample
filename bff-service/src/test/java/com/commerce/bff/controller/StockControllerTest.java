package com.commerce.bff.controller;

import com.commerce.bff.stock.StockRedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockController.class)    // 보안 설정을 제외하지 않습니다.
@DisplayName("StockController 단위 테스트")
class StockControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    StockRedisService stockRedisService;

    // ── POST /api/stocks/{productId}/init ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN") // 관리자 권한 부여
    @DisplayName("재고 초기화 성공 - 200 반환")
    void initStock_success() throws Exception {
        mockMvc.perform(post("/api/stocks/product-1/init")
                        .with(csrf())
                        .param("quantity", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.productId").value("product-1"))
                .andExpect(jsonPath("$.quantity").value(100));

        verify(stockRedisService).initStock("product-1", 100L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("quantity=0 이면 400 반환")
    void initStock_zeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/stocks/product-1/init")
                        .with(csrf())
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(stockRedisService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("quantity 음수면 400 반환")
    void initStock_negativeQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/stocks/product-1/init")
                        .with(csrf())
                        .param("quantity", "-5"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stockRedisService);
    }

    // ── PUT /api/stocks/{productId}/init ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("재고 재설정 성공 - quantity=0 허용")
    void resetStock_zeroAllowed() throws Exception {
        mockMvc.perform(put("/api/stocks/product-1/init")
                        .with(csrf())
                        .param("quantity", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(stockRedisService).initStock("product-1", 0L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("재고 재설정 - 음수 수량은 400")
    void resetStock_negativeQuantity_returns400() throws Exception {
        mockMvc.perform(put("/api/stocks/product-1/init")
                        .with(csrf())
                        .param("quantity", "-1"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/stocks/{productId} ───────────────────────────────────────────

    @Test
    @WithMockUser
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
    @WithMockUser
    @DisplayName("재고 조회 - 재고 소진(0)")
    void getStock_outOfStock() throws Exception {
        when(stockRedisService.getStock("product-1")).thenReturn(0L);

        mockMvc.perform(get("/api/stocks/product-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(0))
                .andExpect(jsonPath("$.inStock").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("재고 조회 - 미초기화 상품(-1)")
    void getStock_notInitialized() throws Exception {
        when(stockRedisService.getStock("product-999")).thenReturn(-1L);

        mockMvc.perform(get("/api/stocks/product-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("재고 정보가 없습니다. 초기화가 필요합니다."));
    }
}
