import { hostGet, hostPost } from './adminClient'

/**
 * 사용자 맞춤 추천 숙소 조회
 * @param {number} userId - 사용자 ID
 * @param {number} limit - 추천 개수 (기본값: 10)
 * @returns {Promise} 추천 숙소 목록
 */
export async function fetchRecommendations(userId, limit = 10) {
    return hostGet(`/recommendations?userId=${userId}&limit=${limit}`)
}

/**
 * AI 자연어 기반 숙소 추천
 * @param {string} query - 자연어 검색어 (예: "조용한 곳에서 풍경 보면서 여행하고 싶어")
 * @returns {Promise} AI 분석 결과 및 추천 숙소 목록
 */
export async function fetchAiRecommendations(query) {
    return hostPost('/recommendations/ai', { query })
}

