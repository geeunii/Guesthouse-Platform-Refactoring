// 리뷰 태그 카테고리 (하드코딩)
// 태그 이름으로 매칭 (DB ID와 무관하게 동작)

export const REVIEW_TAG_CATEGORIES = [
  {
    name: '서비스 · 직원 응대',
    tagNames: ['친절해요', '매너타임이 잘 지켜져요', '픽업서비스가 편해요', '룸서비스가 잘 되어있어요']
  },
  {
    name: '청결 · 위생',
    tagNames: ['깨끗해요', '화장실이 잘 되어있어요', '벌레 걱정 없어요', '공용시설 관리가 잘돼요', '매장이 청결해요', '재료가 신선해요']
  },
  {
    name: '객실 · 시설 품질',
    tagNames: ['침구가 좋아요', '방음이 잘돼요', '냉난방이 잘돼요', '전기 사용이 편해요', '온수가 잘 나와요', '수영장이 잘 되어있어요', '개수대가 잘 되어있어요']
  },
  {
    name: '편의시설 · 인프라',
    tagNames: ['편의시설이 잘 되어있어요', '주차하기 편해요', '대중교통이 편해요', '마트 이용이 편해요', '근처에 갈 곳이 많아요']
  },
  {
    name: '분위기 · 감성',
    tagNames: ['인테리어가 멋져요', '컨셉이 독특해요', '뷰가 좋아요', '사진이 잘 나와요', '차분한 분위기에요', '아늑해요', '숙소가 넓어요', '신나는 분위기에요', '사장님이 재밌으세요', '솔로탈출 하기 좋아요']
  },
  {
    name: '액티비티 · 즐길 거리',
    tagNames: ['파티가 재밌어요', '파티하기 좋아요', '어울려 놀기 좋아요', '즐길 거리가 많아요', '물놀이하기 좋아요', '바비큐 해먹기 좋아요', '단체모임 하기 좋아요', '호캉스하기 좋아요']
  }
]

/**
 * API에서 받은 태그 목록을 카테고리별로 그룹화
 * 태그 이름으로 매칭하므로 DB ID가 달라도 동작
 * @param {Array} tags - API에서 받은 태그 목록 [{reviewTagId, reviewTagName}, ...]
 * @returns {Array} - 카테고리별로 그룹화된 태그 목록
 */
export function groupTagsByCategory(tags) {
  if (!Array.isArray(tags) || tags.length === 0) return []

  // 태그 이름으로 매핑
  const tagByName = new Map(tags.map(tag => [tag.reviewTagName, tag]))
  const usedTagNames = new Set()

  // 카테고리별로 그룹화
  const categorized = REVIEW_TAG_CATEGORIES.map(category => {
    const categoryTags = category.tagNames
      .map(name => {
        const tag = tagByName.get(name)
        if (tag) usedTagNames.add(name)
        return tag
      })
      .filter(Boolean)

    return {
      name: category.name,
      tags: categoryTags
    }
  }).filter(category => category.tags.length > 0)

  // 카테고리에 속하지 않은 태그들 (기타)
  const uncategorizedTags = tags.filter(tag => !usedTagNames.has(tag.reviewTagName))
  if (uncategorizedTags.length > 0) {
    categorized.push({
      name: '기타',
      tags: uncategorizedTags
    })
  }

  // 카테고리가 하나도 없으면 전체 태그를 하나의 그룹으로
  if (categorized.length === 0) {
    return [{
      name: '리뷰 태그',
      tags: tags
    }]
  }

  return categorized
}

export default {
  REVIEW_TAG_CATEGORIES,
  groupTagsByCategory
}
