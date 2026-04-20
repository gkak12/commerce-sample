package com.commerce.delivery.mapper;

import com.commerce.delivery.dto.DeliveryResponse;
import com.commerce.delivery.entity.Delivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeliveryMapper {

    @Mapping(target = "status", expression = "java(delivery.getStatus().name())")
    DeliveryResponse toResponse(Delivery delivery);
}
