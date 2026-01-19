// 예약 API 클라이언트
import { authenticatedRequest } from './authClient'

/**
 * 예약 생성
 * @param {Object} data - 예약 요청 데이터
 * @returns {Promise<Object>} - 생성된 예약 정보
 */
export async function createReservation(data) {
    const response = await authenticatedRequest('/api/reservations', {
        method: 'POST',
        body: JSON.stringify(data)
    })

    if (!response.ok) {
        // 백엔드에서 전달된 에러 메시지가 있으면 사용, 없으면 상태 코드 반환
        const errorMsg = response.data?.message || `예약 생성 실패: ${response.status}`
        throw new Error(errorMsg)
    }

    return response.data
}

/**
 * 예약 단건 조회
 * @param {number} reservationId - 예약 ID
 * @returns {Promise<Object>} - 예약 정보
 */
export async function getReservation(reservationId) {
    const response = await authenticatedRequest(`/api/reservations/${reservationId}`)

    if (!response.ok) {
        throw new Error(`예약 조회 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 사용자별 예약 목록 조회 (userId 파라미터 방식)
 * @param {number} userId - 사용자 ID (없으면 기본값 1 사용)
 * @returns {Promise<Array>} - 예약 목록
 */
export async function getUserReservations(userId = 1) {
    const response = await authenticatedRequest(`/api/reservations/user/${userId}`)

    if (!response.ok) {
        throw new Error(`예약 목록 조회 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 현재 로그인된 사용자의 예약 목록 조회 (토큰 기반)
 * @returns {Promise<Array>} - 예약 목록
 */
export async function getMyReservations() {
    const response = await authenticatedRequest('/api/reservations/my')

    if (!response.ok) {
        throw new Error(`예약 목록 조회 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 숙소별 예약 목록 조회
 * @param {number} accommodationsId - 숙소 ID
 * @returns {Promise<Array>} - 예약 목록
 */
export async function getAccommodationReservations(accommodationsId) {
    const response = await authenticatedRequest(`/api/reservations/accommodation/${accommodationsId}`)

    if (!response.ok) {
        throw new Error(`숙소 예약 목록 조회 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 대기 상태 예약 삭제 (결제 취소 시)
 * @param {number} reservationId - 예약 ID
 * @returns {Promise<void>}
 */
export async function deletePendingReservation(reservationId) {
    const response = await authenticatedRequest(`/api/reservations/pending/${reservationId}`, {
        method: 'DELETE'
    })

    if (!response.ok) {
        console.error(`대기 예약 삭제 실패: ${response.status}`)
    }
}

/**
 * 이용 완료된 예약 삭제 (내역에서 삭제)
 * 체크인 날짜가 지난 확정된 예약만 삭제 가능
 * @param {number} reservationId - 예약 ID
 * @returns {Promise<void>}
 */
export async function deleteCompletedReservation(reservationId) {
    const response = await authenticatedRequest(`/api/reservations/completed/${reservationId}`, {
        method: 'DELETE'
    })


    if (!response.ok) {
        const errorMsg = response.data?.message || `이용 완료된 예약만 삭제할 수 있습니다. (${response.status})`
        throw new Error(errorMsg)
    }
}

/**
 * 취소된 예약 삭제 (내역에서 삭제)
 * @param {number} reservationId - 예약 ID
 * @returns {Promise<void>}
 */
export async function deleteCancelledReservation(reservationId) {
    const response = await authenticatedRequest(`/api/reservations/cancelled/${reservationId}`, {
        method: 'DELETE'
    })

    if (!response.ok) {
        const errorMsg = response.data?.message || `예약 내역 삭제 실패 (${response.status})`
        throw new Error(errorMsg)
    }
}
