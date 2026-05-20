package com.commerce.bff.controller;

import com.commerce.bff.grpc.CatalogGrpcClient;
import com.commerce.bff.mapper.ProductMapperImpl;
import com.commerce.grpc.catalog.GetProductListResponse;
import com.commerce.grpc.catalog.GetProductResponse;
import com.commerce.grpc.catalog.ProductImageProto;
import com.commerce.grpc.catalog.ProductOptionProto;
import com.commerce.grpc.catalog.ProductProto;
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

@WebMvcTest(controllers = ProductController.class)
@Import({ProductMapperImpl.class, BaseControllerTest.MethodSecurityConfig.class})
@DisplayName("ProductController 단위 테스트")
class ProductControllerTest extends BaseControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    CatalogGrpcClient catalogGrpcClient;

    // ── GET /api/products ─────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("상품 목록 조회 - 2건 반환")
    void getProducts_success() throws Exception {
        GetProductListResponse grpcResp = GetProductListResponse.newBuilder()
                .addProducts(ProductProto.newBuilder()
                        .setProductId("prod-1")
                        .setName("노트북")
                        .setBasePrice("1500000")
                        .setStatus("ON_SALE")
                        .build())
                .addProducts(ProductProto.newBuilder()
                        .setProductId("prod-2")
                        .setName("마우스")
                        .setBasePrice("30000")
                        .setStatus("ON_SALE")
                        .build())
                .setTotalCount(2)
                .setTotalPages(1)
                .setCurrentPage(0)
                .build();

        when(catalogGrpcClient.getProductList(null, 0, 10)).thenReturn(grpcResp);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.products[0].productId").value("prod-1"))
                .andExpect(jsonPath("$.products[0].name").value("노트북"))
                .andExpect(jsonPath("$.products[1].basePrice").value("30000"));
    }

    @Test
    @WithMockUser
    @DisplayName("상품 목록 조회 - 카테고리 필터")
    void getProducts_withCategoryFilter() throws Exception {
        GetProductListResponse grpcResp = GetProductListResponse.newBuilder()
                .addProducts(ProductProto.newBuilder()
                        .setProductId("prod-1")
                        .setCategoryId(10L)
                        .setName("노트북")
                        .build())
                .setTotalCount(1)
                .setTotalPages(1)
                .setCurrentPage(0)
                .build();

        when(catalogGrpcClient.getProductList(10L, 0, 10)).thenReturn(grpcResp);

        mockMvc.perform(get("/api/products").param("categoryId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.products[0].categoryId").value(10));
    }

    // ── GET /api/products/{productId} ─────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("상품 상세 조회 - 이미지/옵션 포함")
    void getProduct_found() throws Exception {
        GetProductResponse grpcResp = GetProductResponse.newBuilder()
                .setFound(true)
                .setProduct(ProductProto.newBuilder()
                        .setProductId("prod-1")
                        .setName("노트북")
                        .setBasePrice("1500000")
                        .setStatus("ON_SALE")
                        .addImages(ProductImageProto.newBuilder()
                                .setUrl("https://cdn.example.com/laptop.jpg")
                                .setIsThumbnail(true)
                                .setSortOrder(1)
                                .build())
                        .addOptions(ProductOptionProto.newBuilder()
                                .setOptionName("색상")
                                .setOptionValue("실버")
                                .setAdditionalPrice("0")
                                .setStockQuantity(10)
                                .build())
                        .build())
                .build();

        when(catalogGrpcClient.getProduct("prod-1")).thenReturn(grpcResp);

        mockMvc.perform(get("/api/products/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.product.productId").value("prod-1"))
                .andExpect(jsonPath("$.product.name").value("노트북"))
                .andExpect(jsonPath("$.product.images[0].thumbnail").value(true))
                .andExpect(jsonPath("$.product.images[0].url").value("https://cdn.example.com/laptop.jpg"))
                .andExpect(jsonPath("$.product.options[0].optionName").value("색상"))
                .andExpect(jsonPath("$.product.options[0].stockQuantity").value(10));
    }

    @Test
    @WithMockUser
    @DisplayName("상품 상세 조회 - 존재하지 않는 상품이면 found=false, product 필드 없음")
    void getProduct_notFound() throws Exception {
        when(catalogGrpcClient.getProduct("prod-999"))
                .thenReturn(GetProductResponse.newBuilder().setFound(false).build());

        mockMvc.perform(get("/api/products/prod-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false))
                .andExpect(jsonPath("$.product").doesNotExist()); // @JsonInclude(NON_NULL)
    }

    // ── GET /api/products/me ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "seller-user")
    @DisplayName("내 상품 목록 조회 - 판매자 본인 상품 반환")
    void getMyProducts_success() throws Exception {
        GetProductListResponse grpcResp = GetProductListResponse.newBuilder()
                .addProducts(ProductProto.newBuilder()
                        .setProductId("prod-1")
                        .setSellerId("seller-user")
                        .setName("내 상품")
                        .build())
                .setTotalCount(1)
                .setTotalPages(1)
                .setCurrentPage(0)
                .build();

        when(catalogGrpcClient.getMyProducts("seller-user", 0, 10)).thenReturn(grpcResp);

        mockMvc.perform(get("/api/products/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.products[0].sellerId").value("seller-user"));
    }

    // ── DELETE /api/products/{productId} ──────────────────────────────────────

    @Test
    @WithMockUser(username = "seller-user")
    @DisplayName("상품 삭제 - 204 반환")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/products/prod-1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(catalogGrpcClient).deleteProduct("seller-user", "prod-1");
    }

    // ── PUT /api/admin/products/{productId}/suspend ───────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 강제 중지 - 204 반환")
    void suspend_success() throws Exception {
        mockMvc.perform(put("/api/admin/products/prod-1/suspend").with(csrf()))
                .andExpect(status().isNoContent());

        verify(catalogGrpcClient).suspendProduct("prod-1");
    }

    @Test
    @WithMockUser // ADMIN 권한 없이
    @DisplayName("상품 강제 중지 - 비관리자는 403")
    void suspend_forbidden() throws Exception {
        mockMvc.perform(put("/api/admin/products/prod-1/suspend").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(catalogGrpcClient);
    }
}
