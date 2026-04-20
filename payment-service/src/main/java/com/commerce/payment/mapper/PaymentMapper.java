package com.commerce.payment.mapper;

import com.commerce.payment.dto.PaymentConfirmResponse;
import com.commerce.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "status", expression = "java(payment.getStatus().name())")
    PaymentConfirmResponse toConfirmResponse(Payment payment);
}
