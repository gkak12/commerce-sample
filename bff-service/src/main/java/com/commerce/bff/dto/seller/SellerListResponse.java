package com.commerce.bff.dto.seller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerListResponse {
    private List<SellerDto> sellers;
    private int totalCount;
    private int totalPages;
    private int currentPage;
}
