package com.commerce.catalog.seller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerRegisterRequest {

    @NotBlank
    private String businessName;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "사업자등록번호는 10자리 숫자입니다.")
    private String businessNumber;

    @NotBlank
    private String ownerName;

    @NotBlank
    private String phone;

    @NotBlank
    @Email
    private String email;
}
