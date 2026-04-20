package com.commerce.point.mapper;

import com.commerce.point.dto.PointBalanceResponse;
import com.commerce.point.entity.PointWallet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PointMapper {

    PointBalanceResponse toResponse(PointWallet wallet);
}
