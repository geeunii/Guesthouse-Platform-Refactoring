package com.ssg9th2team.geharbang.domain.payment.service;

import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmRequestDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentConfirmResponseDto;
import com.ssg9th2team.geharbang.domain.payment.dto.PaymentResponseDto;
import com.ssg9th2team.geharbang.domain.payment.dto.RefundResponseDto;

import java.util.List;

public interface PaymentService {

    /**
     * 결제 승인 요청 (토스페이먼츠 API 호출)
     */
    PaymentConfirmResponseDto confirmPayment(PaymentConfirmRequestDto requestDto);

    /**
     * 결제 조회 (주문번호)
     */
    PaymentResponseDto getPaymentByOrderId(String orderId);

    /**
     * 결제 조회 (예약번호)
     */
    PaymentResponseDto getPaymentByReservationId(Long reservationId);

    /**
     * 결제 취소 (환불) - 사용자용 (환불 정책 적용)
     * 
     * @param reservationId 예약 ID
     * @param cancelReason  취소 사유
     * @param refundAmount  환불 금액 (사용되지 않음, 정책에 따라 자동 계산됨)
     * @return 환불 처리 결과
     */
    PaymentResponseDto cancelPayment(Long reservationId, String cancelReason, Integer refundAmount);

    /**
     * 관리자 강제 환불 (정책 무시, 입력된 금액으로 환불)
     *
     * @param reservationId 예약 ID
     * @param cancelReason  취소 사유
     * @param refundAmount  환불 금액
     * @return 환불 처리 결과
     */
    PaymentResponseDto adminRefundPayment(Long reservationId, String cancelReason, Integer refundAmount);

    /**
     * 전체 환불 내역 조회 (관리자용)
     */
    List<RefundResponseDto> getAllRefunds();

    /**
     * 예약 ID와 관련된 모든 결제 및 환불 데이터 삭제 (예약 삭제 시 호출)
     */
    void deleteAllPaymentDataByReservationId(Long reservationId);
}
