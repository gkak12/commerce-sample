package com.commerce.bff.service;

import com.commerce.bff.dto.OrderRequest;
import com.commerce.bff.dto.OrderResponse;

public interface OrderService {

    OrderResponse placeOrder(OrderRequest request);
}
