package com.commerce.bff.dto.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 판매자 신청 요청 DTO
 *
 * userId는 JWT 토큰에서 추출한 인증 정보를 사용하므로
 * 요청 바디에 포함하지 않습니다.
 */
@Getter
@NoArgsConstructor
public class SellerRegisterRequest {

    @NotBlank
    private String businessName;

    @NotBlank
    private String businessNumber;

    @NotBlank
    private String ownerName;

    @NotBlank
    private String phone;

    @Email
    @NotBlank
    private String email;
}
