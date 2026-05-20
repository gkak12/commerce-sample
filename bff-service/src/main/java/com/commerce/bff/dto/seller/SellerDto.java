package com.commerce.bff.dto.seller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDto {
    private String sellerId;
    private String businessName;
    private String businessNumber;
    private String ownerName;
    private String phone;
    private String email;
    private String status;
    private String createdAt;
}
