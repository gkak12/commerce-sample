package com.commerce.bff.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListResponse {
    private String userId;
    private List<OrderSummaryDto> orders;
    private int currentPage;
    private int totalCount;
    private int totalPages;
}
