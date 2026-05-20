package com.commerce.bff.dto.stock;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockQueryResponse {
    private boolean success;
    private String productId;
    private Long stock;     // null이면 응답에서 제외
    private Boolean inStock; // null이면 응답에서 제외 (미초기화 케이스)
    private String message; // null이면 응답에서 제외 (정상 케이스)
}
