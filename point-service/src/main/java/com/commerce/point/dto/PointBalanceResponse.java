package com.commerce.point.dto;

public record PointBalanceResponse(
        String userId,
        long totalPoint
) {}
