package com.commerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerApprovedEvent {

    private String userId;      // bff-service에서 role 변경 대상
    private String sellerId;

    @Builder.Default
    private LocalDateTime approvedAt = LocalDateTime.now();
}
