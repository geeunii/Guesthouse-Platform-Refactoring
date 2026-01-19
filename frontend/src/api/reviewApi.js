// 리뷰 API 클라이언트
import { authenticatedRequest } from './authClient'

/**
 * 리뷰 등록
 * @param {Object} reviewData - 리뷰 데이터
 * @param {number} reviewData.accommodationsId - 숙소 ID
 * @param {number} reviewData.rating - 별점 (1-5)
 * @param {string} reviewData.content - 리뷰 내용
 * @param {string} reviewData.visitDate - 방문일 (YYYY-MM-DD)
 * @param {Array<number>} reviewData.tagIds - 태그 ID 목록
 * @param {Array<string>} reviewData.imageUrls - 이미지 URL 또는 Base64 목록
 * @returns {Promise<string>} - 성공 메시지
 */
export async function createReview(reviewData) {
    const response = await authenticatedRequest('/api/reviews', {
        method: 'POST',
        body: JSON.stringify(reviewData)
    })

    if (!response.ok) {
        // 에러 메시지 추출
        let errorMessage = '리뷰 등록에 실패했습니다.'

        if (response.data) {
            if (typeof response.data === 'string') {
                errorMessage = response.data
            } else if (response.data.message) {
                errorMessage = response.data.message
            } else if (response.data.error) {
                errorMessage = response.data.error
            }
        }

        console.error('리뷰 등록 에러:', response.status, response.data)
        throw new Error(errorMessage)
    }

    return response.data
}

/**
 * 숙소별 리뷰 조회
 * @param {number} accommodationsId - 숙소 ID
 * @returns {Promise<Array>} - 리뷰 목록
 */
export async function getReviewsByAccommodation(accommodationsId) {
    const response = await authenticatedRequest(`/api/reviews/accommodations/${accommodationsId}`)

    if (!response.ok) {
        throw new Error(`리뷰 조회 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 전체 리뷰 태그 목록 조회
 * @returns {Promise<Array>} - 태그 목록 [{reviewTagId, reviewTagName}]
 */
export async function getReviewTags() {
    const response = await authenticatedRequest('/api/reviews/tags')

    if (!response.ok) {
        throw new Error(`태그 조회 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 리뷰 수정
 * @param {number} reviewId - 리뷰 ID
 * @param {Object} updateData - 수정할 데이터 { content, imageUrls }
 * @returns {Promise<string>} - 성공 메시지
 */
export async function updateReview(reviewId, updateData) {
    const response = await authenticatedRequest(`/api/reviews/${reviewId}`, {
        method: 'PUT',
        body: JSON.stringify(updateData)
    })

    if (!response.ok) {
        let errorMessage = '리뷰 수정에 실패했습니다.'
        if (response.data) {
            if (typeof response.data === 'string') {
                errorMessage = response.data
            } else if (response.data.message) {
                errorMessage = response.data.message
            } else if (response.data.error) {
                errorMessage = response.data.error
            }
        }
        console.error('리뷰 수정 에러:', response.status, response.data)
        throw new Error(errorMessage)
    }

    return response.data
}

/**
 * 내 리뷰 조회
 * @returns {Promise<Array>} - 리뷰 목록
 */
export async function getMyReviews() {
    const response = await authenticatedRequest('/api/reviews/my')

    if (!response.ok) {
        throw new Error(`내 리뷰 조회 실패: ${response.status}`)
    }

    return response.data
}

/**
 * 리뷰 삭제
 * @param {number} reviewId - 리뷰 ID
 * @returns {Promise<string>} - 성공 메시지
 */
export async function deleteReview(reviewId) {
    const response = await authenticatedRequest(`/api/reviews/${reviewId}`, {
        method: 'DELETE'
    })

    if (!response.ok) {
        let errorMessage = '리뷰 삭제에 실패했습니다.'
        if (response.data && response.data.message) {
            errorMessage = response.data.message
        }
        throw new Error(errorMessage)
    }

    return response.data
}

export default {
    createReview,
    updateReview,
    getReviewsByAccommodation,
    getReviewTags,
    getMyReviews,
    deleteReview
}
