package com.neekostar.adsystem.mapper;

import com.neekostar.adsystem.dto.PaymentResponseDto;
import com.neekostar.adsystem.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {AdMapper.class})
public interface PaymentMapper {
    @Mapping(target = "adId", source = "payment.ad.id")
    PaymentResponseDto toDto(Payment payment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ad", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "user", ignore = true)
    Payment toEntity(PaymentResponseDto paymentResponseDto);
}
