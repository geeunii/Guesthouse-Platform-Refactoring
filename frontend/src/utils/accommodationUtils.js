/**
 * 숙소 관련 공통 유틸리티 함수
 */

/**
 * 숙소 카드에 표시될 설명을 정규화합니다.
 * shortDescription을 우선 사용하고, 없으면 description을 사용합니다.
 * 첫 번째 줄만 반환합니다.
 * 
 * @param {Object} item - 숙소 데이터 객체
 * @returns {string} 정규화된 설명 문자열
 */
export const getCardDescription = (item) => {
    const shortText = typeof item.shortDescription === 'string' ? item.shortDescription.trim() : ''
    const raw = shortText || item.description || ''
    if (typeof raw !== 'string') return ''
    return raw.split(/\r?\n/)[0].trim()
}
