package com.commerce.bff.dto.mypage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDto {
    private String deliveryId;
    private String status;
    private String address;
    private String startedAt;
}
