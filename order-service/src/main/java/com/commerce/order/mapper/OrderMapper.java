package com.commerce.order.mapper;

import com.commerce.order.dto.OrderItemResponse;
import com.commerce.order.dto.OrderResponse;
import com.commerce.order.entity.Order;
import com.commerce.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    @Mapping(target = "items", source = "orderItems")
    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem item);
}
