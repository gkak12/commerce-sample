package com.commerce.bff.controller;

import com.commerce.bff.grpc.CatalogGrpcClient;
import com.commerce.bff.mapper.SellerMapperImpl;
import com.commerce.grpc.catalog.GetSellerListResponse;
import com.commerce.grpc.catalog.SellerProto;
import com.commerce.grpc.catalog.SellerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SellerController.class)
@Import({SellerMapperImpl.class, BaseControllerTest.MethodSecurityConfig.class})
@DisplayName("SellerController 단위 테스트")
class SellerControllerTest extends BaseControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CatalogGrpcClient catalogGrpcClient;

    // ── GET /api/sellers/me ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user-001")
    @DisplayName("내 판매자 정보 조회 - 존재하는 판매자")
    void getMyInfo_found() throws Exception {
        SellerResponse grpcResp = SellerResponse.newBuilder()
                .setFound(true)
                .setSeller(SellerProto.newBuilder()
                        .setSellerId("seller-1")
                        .setBusinessName("테스트 상점")
                        .setBusinessNumber("123-45-67890")
                        .setOwnerName("홍길동")
                        .setPhone("010-1234-5678")
                        .setEmail("seller@test.com")
                        .setStatus("APPROVED")
                        .setCreatedAt("2024-01-01T10:00:00")
                        .build())
                .build();

        when(catalogGrpcClient.getMySellerInfo("user-001")).thenReturn(grpcResp);

        mockMvc.perform(get("/api/sellers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.seller.sellerId").value("seller-1"))
                .andExpect(jsonPath("$.seller.businessName").value("테스트 상점"))
                .andExpect(jsonPath("$.seller.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "user-002")
    @DisplayName("내 판매자 정보 조회 - 미등록 사용자면 found=false, seller 필드 없음")
    void getMyInfo_notFound() throws Exception {
        when(catalogGrpcClient.getMySellerInfo("user-002"))
                .thenReturn(SellerResponse.newBuilder().setFound(false).build());

        mockMvc.perform(get("/api/sellers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.seller").doesNotExist());
    }

    // ── GET /api/admin/sellers ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("판매자 목록 조회 - 2건 반환")
    void findAll_success() throws Exception {
        GetSellerListResponse grpcResp = GetSellerListResponse.newBuilder()
                .addSellers(SellerProto.newBuilder()
                        .setSellerId("seller-1")
                        .setBusinessName("상점 A")
                        .setStatus("APPROVED")
                        .build())
                .addSellers(SellerProto.newBuilder()
                        .setSellerId("seller-2")
                        .setBusinessName("상점 B")
                        .setStatus("PENDING")
                        .build())
                .setTotalCount(2)
                .setTotalPages(1)
                .setCurrentPage(0)
                .build();

        when(catalogGrpcClient.getSellerList(null, 0, 10)).thenReturn(grpcResp);

        mockMvc.perform(get("/api/admin/sellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.sellers[0].sellerId").value("seller-1"))
                .andExpect(jsonPath("$.sellers[1].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("판매자 목록 조회 - status 필터 적용")
    void findAll_withStatusFilter() throws Exception {
        GetSellerListResponse grpcResp = GetSellerListResponse.newBuilder()
                .addSellers(SellerProto.newBuilder()
                        .setSellerId("seller-1")
                        .setStatus("PENDING")
                        .build())
                .setTotalCount(1)
                .setTotalPages(1)
                .setCurrentPage(0)
                .build();

        when(catalogGrpcClient.getSellerList("PENDING", 0, 10)).thenReturn(grpcResp);

        mockMvc.perform(get("/api/admin/sellers").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.sellers[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser
    @DisplayName("판매자 목록 조회 - 비관리자는 403")
    void findAll_forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/sellers"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(catalogGrpcClient);
    }

    // ── PUT /api/admin/sellers/{sellerId}/approve ─────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("판매자 승인 - 204 반환")
    void approve_success() throws Exception {
        mockMvc.perform(put("/api/admin/sellers/seller-1/approve").with(csrf()))
                .andExpect(status().isNoContent());

        verify(catalogGrpcClient).approveSeller("seller-1");
    }

    // ── PUT /api/admin/sellers/{sellerId}/reject ──────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("판매자 거절 - 204 반환")
    void reject_success() throws Exception {
        mockMvc.perform(put("/api/admin/sellers/seller-1/reject").with(csrf()))
                .andExpect(status().isNoContent());

        verify(catalogGrpcClient).rejectSeller("seller-1");
    }

    // ── PUT /api/admin/sellers/{sellerId}/suspend ─────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("판매자 정지 - 204 반환")
    void suspend_success() throws Exception {
        mockMvc.perform(put("/api/admin/sellers/seller-1/suspend").with(csrf()))
                .andExpect(status().isNoContent());

        verify(catalogGrpcClient).suspendSeller("seller-1");
    }
}
