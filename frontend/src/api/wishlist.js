import { authenticatedRequest } from './authClient'

// 내 위시리스트 ID 목록 조회 (메인페이지용)
export const fetchWishlistIds = () => {
    return authenticatedRequest('/api/wishlist/accommodation-ids', { method: 'GET' })
}

// 내 위시리스트 상세 조회 (마이페이지용)
export const fetchMyWishlist = () => {
    return authenticatedRequest('/api/wishlist', { method: 'GET' })
}

// 위시리스트 추가
export const addWishlist = async (accommodationsId) => {
    console.log('[위시리스트 추가] 요청 시작 - accommodationsId:', accommodationsId)
    try {
        const response = await authenticatedRequest('/api/wishlist', {
            method: 'POST',
            body: JSON.stringify({ accommodationsId })
        })
        console.log('[위시리스트 추가] 응답 받음 - status:', response.status, 'data:', response.data)
        if (!response.ok) {
            console.error('[위시리스트 추가] 실패 - status:', response.status, 'data:', response.data)
        }
        return response
    } catch (error) {
        console.error('[위시리스트 추가] 에러 발생:', error)
        throw error
    }
}

// 위시리스트 삭제
export const removeWishlist = async (accommodationId) => {
    console.log('[위시리스트 삭제] 요청 시작 - accommodationId:', accommodationId)
    try {
        const response = await authenticatedRequest(`/api/wishlist/${accommodationId}`, { method: 'DELETE' })
        console.log('[위시리스트 삭제] 응답 받음 - status:', response.status, 'data:', response.data)
        if (!response.ok) {
            console.error('[위시리스트 삭제] 실패 - status:', response.status, 'data:', response.data)
        }
        return response
    } catch (error) {
        console.error('[위시리스트 삭제] 에러 발생:', error)
        throw error
    }
}
