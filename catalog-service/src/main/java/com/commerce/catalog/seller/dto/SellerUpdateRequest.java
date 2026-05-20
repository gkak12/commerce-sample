package com.commerce.catalog.seller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerUpdateRequest {

    @NotBlank
    private String businessName;

    @NotBlank
    private String phone;

    @NotBlank
    @Email
    private String email;
}
