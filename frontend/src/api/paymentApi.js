// 결제 API 클라이언트
import { authenticatedRequest } from './authClient'

/**
 * 결제 승인 요청
 * @param {Object} data - { paymentKey, orderId, amount }
 * @returns {Promise<Object>} - 결제 결과
 */
export async function confirmPayment(data) {
    const response = await authenticatedRequest('/api/payments/confirm', {
        method: 'POST',
        body: JSON.stringify(data)
    })

    if (!response.ok) {
        throw new Error(response.data?.message || `결제 승인 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 토스페이먼츠 클라이언트 키 조회
 * @returns {Promise<string>} - 클라이언트 키
 */
export async function getClientKey() {
    const response = await authenticatedRequest('/api/payments/client-key')

    if (!response.ok) {
        throw new Error(`클라이언트 키 조회 실패: ${response.status}`)
    }

    // response.data가 문자열 또는 객체일 수 있음
    return typeof response.data === 'string' ? response.data : response.data?.clientKey || response.data
}

/**
 * 주문번호로 결제 조회
 * @param {string} orderId - 주문번호
 * @returns {Promise<Object>} - 결제 정보
 */
export async function getPaymentByOrderId(orderId) {
    const response = await authenticatedRequest(`/api/payments/order/${orderId}`)

    if (!response.ok) {
        throw new Error(`결제 조회 실패: ${response.status}`)
    }

    return response.data
}
