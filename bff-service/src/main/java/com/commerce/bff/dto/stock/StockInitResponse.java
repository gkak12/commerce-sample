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
public class StockInitResponse {
    private boolean success;
    private String message;
    private String productId;
    private Long quantity; // null이면 응답에서 제외 (실패 케이스)
}
