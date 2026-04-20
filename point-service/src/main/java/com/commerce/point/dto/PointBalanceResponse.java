package com.commerce.point.dto;

import com.commerce.point.entity.PointWallet;

public record PointBalanceResponse(
        String userId,
        long totalPoint
) {
    public static PointBalanceResponse from(PointWallet wallet) {
        return new PointBalanceResponse(
                wallet.getUserId(),
                wallet.getTotalPoint()
        );
    }
}
