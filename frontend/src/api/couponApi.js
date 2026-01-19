// 쿠폰 API 클라이언트
import { authenticatedRequest } from './authClient'

/**
 * 상태별 내 쿠폰 조회
 * @param {string} status - 쿠폰 상태 (ISSUED / USED / EXPIRED)
 * @returns {Promise<Array>} - 쿠폰 목록
 */
export async function getMyCoupons(status = 'ISSUED') {
    const response = await authenticatedRequest(`/api/coupons/my?status=${status}`)

    if (!response.ok) {
        throw new Error(`쿠폰 조회 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 쿠폰 발급 (숙소 상세페이지에서 쿠폰 받기)
 * @param {number} couponId - 쿠폰 ID
 * @returns {Promise<string>} - 성공 메시지
 */
export async function issueCoupon(couponId) {
    const response = await authenticatedRequest(`/api/coupons/issue?couponId=${couponId}`, {
        method: 'POST'
    })

    if (!response.ok) {
        let errorMessage = '쿠폰 발급에 실패했습니다.'
        if (response.data) {
            if (typeof response.data === 'string') {
                errorMessage = response.data
            } else if (response.data.message) {
                errorMessage = response.data.message
            }
        }
        throw new Error(errorMessage)
    }

    return response.data
}

/**
 * 쿠폰 사용 처리
 * @param {number} userCouponId - 유저 쿠폰 ID
 * @returns {Promise<string>} - 성공 메시지
 */
export async function useCoupon(userCouponId) {
    const response = await authenticatedRequest(`/api/coupons/${userCouponId}/use`, {
        method: 'POST'
    })

    if (!response.ok) {
        let errorMessage = '쿠폰 사용에 실패했습니다.'
        if (response.data) {
            if (typeof response.data === 'string') {
                errorMessage = response.data
            } else if (response.data.message) {
                errorMessage = response.data.message
            }
        }
        throw new Error(errorMessage)
    }

    return response.data
}

/**
 * 숙소 상세페이지에서 다운로드 가능한 쿠폰 목록 조회
 * @param {number} accommodationId
 * @returns {Promise<Array>}
 */
export async function getDownloadableCoupons(accommodationId) {
    const response = await authenticatedRequest(`/api/coupons/accommodation/${accommodationId}`)

    if (!response.ok) {
        throw new Error(`쿠폰 목록 조회 실패: ${response.status}`)
    }

    return response.data
}

export async function getMyCouponIds() {
    const response = await authenticatedRequest('/api/coupons/my/ids')
    if (!response.ok) {
        throw new Error(`쿠폰 ID 조회 실패: ${response.status}`)
    }
    return response.data || []
}

export default {
    getMyCoupons,
    issueCoupon,
    useCoupon,
    getDownloadableCoupons,
    getMyCouponIds
}
