package com.ecommerce.paymentservice.mapper;

import com.ecommerce.paymentservice.dto.response.PaymentInitiatedResponse;
import com.ecommerce.paymentservice.dto.response.PaymentResponse;
import com.ecommerce.paymentservice.dto.response.PaymentStatusHistoryResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    PaymentInitiatedResponse toPaymentInitiatedResponse(Payment payment);

    PaymentResponse toPaymentResponse(Payment payment);

    @Mapping(target = "changedAt", source = "createdAt")
    PaymentStatusHistoryResponse toPaymentStatusHistoryResponse(PaymentStatusHistory history);
}
