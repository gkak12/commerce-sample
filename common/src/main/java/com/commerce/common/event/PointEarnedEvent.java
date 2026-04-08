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
public class PointEarnedEvent {

    private String pointId;
    private String userId;
    private String orderId;
    private long earnedPoint;
    private long totalPoint;

    @Builder.Default
    private LocalDateTime earnedAt = LocalDateTime.now();
}
