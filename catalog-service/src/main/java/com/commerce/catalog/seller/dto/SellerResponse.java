package com.commerce.catalog.seller.dto;

import com.commerce.catalog.seller.entity.Seller;
import com.commerce.catalog.seller.entity.SellerStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SellerResponse {

    private final String sellerId;
    private final String businessName;
    private final String businessNumber;
    private final String ownerName;
    private final String phone;
    private final String email;
    private final SellerStatus status;
    private final LocalDateTime createdAt;

    private SellerResponse(Seller seller) {
        this.sellerId        = seller.getSellerId();
        this.businessName    = seller.getBusinessName();
        this.businessNumber  = seller.getBusinessNumber();
        this.ownerName       = seller.getOwnerName();
        this.phone           = seller.getPhone();
        this.email           = seller.getEmail();
        this.status          = seller.getStatus();
        this.createdAt       = seller.getCreatedAt();
    }

    public static SellerResponse from(Seller seller) {
        return new SellerResponse(seller);
    }
}
