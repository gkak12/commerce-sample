package com.commerce.bff.dto.mypage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDetailResponse {
    private boolean found;
    private String message;       // found=false 일 때만 포함
    private String orderId;
    private String status;
    private String totalAmount;
    private String createdAt;
    private List<OrderItemDto> items;
    private DeliveryDto delivery; // 배송 미시작이면 null → NON_NULL에 의해 응답에서 제외
}
