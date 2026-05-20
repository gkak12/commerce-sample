package com.commerce.bff.dto.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 판매자 정보 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class SellerUpdateRequest {

    @NotBlank
    private String businessName;

    @NotBlank
    private String phone;

    @Email
    @NotBlank
    private String email;
}
