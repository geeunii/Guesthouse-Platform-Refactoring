<script setup>
import GuesthouseCard from '../../components/GuesthouseCard.vue'
import SkeletonCard from '../../components/SkeletonCard.vue'
import FilterModal from '../../components/FilterModal.vue'
import { useRouter, useRoute } from 'vue-router'
import { searchList } from '@/api/list'
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useSearchStore } from '@/stores/search'
import { useListingFilters } from '@/composables/useListingFilters'

import { fetchWishlistIds, addWishlist, removeWishlist } from '@/api/wishlist'
import { isAuthenticated } from '@/api/authClient'

const router = useRouter()
const route = useRoute()
const searchStore = useSearchStore()
const { applyRouteFilters, buildFilterQuery } = useListingFilters()
const items = ref([])
const wishlistIds = ref(new Set())
const page = ref(0)
const totalPages = ref(1)
const totalCount = ref(0)
const isLoading = ref(false)
const isLoadingMore = ref(false)
const loadMoreTrigger = ref(null)
const paginationRef = ref(null)
const isMapBtnFixed = ref(true)

const PAGE_SIZE = 16
const MOBILE_BREAKPOINT = 1024

let observer = null
let paginationObserver = null

// 화면 크기 감지 (PC vs Mobile)
const isMobile = ref(typeof window !== 'undefined' ? window.innerWidth < MOBILE_BREAKPOINT : false)

const handleResize = () => {
  isMobile.value = window.innerWidth < MOBILE_BREAKPOINT
}

// Filter State
const isFilterModalOpen = ref(false)

// Sort State
const SORT_OPTIONS = [
  { value: 'recommended', label: '추천순' },
  { value: 'reviews', label: '리뷰 많은순' },
  { value: 'rating', label: '평점 높은순' },
  { value: 'priceHigh', label: '가격 높은순' },
  { value: 'priceLow', label: '가격 낮은순' }
]
const currentSort = ref('recommended')
const isSortOpen = ref(false)

const currentSortLabel = computed(() => {
  const option = SORT_OPTIONS.find(opt => opt.value === currentSort.value)
  return option?.label || '추천순'
})

const toggleSort = () => {
  isSortOpen.value = !isSortOpen.value
}

const selectSort = (value) => {
  if (currentSort.value !== value) {
    router.replace({ query: { ...route.query, sort: value } })
  }
  isSortOpen.value = false
}

import { getCardDescription } from '@/utils/accommodationUtils'

const normalizeItem = (item) => {
  const id = item.accommodationsId ?? item.accommodationId ?? item.id
  const title = item.accommodationsName ?? item.accommodationName ?? item.title ?? ''
  const description = getCardDescription(item)
  const rating = item.rating ?? null
  const reviewCount = item.reviewCount ?? item.review_count ?? null
  const location = [item.city, item.district, item.township].filter(Boolean).join(' ')
  const price = Number(item.minPrice ?? item.price ?? 0)
  const imageUrl = item.imageUrl || 'https://placehold.co/400x300'
  const maxGuestsValue = Number(item.maxGuests ?? item.capacity ?? item.maxGuest ?? 0)
  const maxGuests = Number.isFinite(maxGuestsValue) ? maxGuestsValue : 0
  return { id, title, description, rating, reviewCount, location, price, imageUrl, maxGuests }
}

const getKeywordFromRoute = () => {
  const raw = route.query.keyword
  if (Array.isArray(raw)) {
    return raw[0] ?? ''
  }
  return raw ?? ''
}

// URL에서 페이지 번호 추출 헬퍼 함수
const getPageFromRoute = () => {
  const savedPage = parseInt(route.query.page, 10)
  return Number.isFinite(savedPage) && savedPage >= 0 ? savedPage : 0
}

const applyRouteKeyword = () => {
  const nextKeyword = getKeywordFromRoute()
  if (nextKeyword !== searchStore.keyword) {
    searchStore.setKeyword(nextKeyword)
  }
}

const applyRouteSort = () => {
  const sortParam = route.query.sort
  if (sortParam && SORT_OPTIONS.some(opt => opt.value === sortParam)) {
    currentSort.value = sortParam
  } else {
    currentSort.value = 'recommended'
  }
}

const loadWishlist = async () => {
  try {
    const res = await fetchWishlistIds()
    if (res.status === 200 && Array.isArray(res.data)) {
      wishlistIds.value = new Set(res.data)
    }
  } catch (e) {
    console.error('Failed to load wishlist', e)
  }
}

const toggleWishlist = async (id) => {
  if (!isAuthenticated()) {
    if (confirm('로그인이 필요한 서비스입니다.\n로그인 페이지로 이동하시겠습니까?')) {
      router.push('/login')
    }
    return
  }

  const isAdded = wishlistIds.value.has(id)
  if (isAdded) {
    wishlistIds.value.delete(id)
    try {
      await removeWishlist(id)
    } catch (e) {
      wishlistIds.value.add(id)
      console.error(e)
    }
  } else {
    wishlistIds.value.add(id)
    try {
      await addWishlist(id)
    } catch (e) {
      wishlistIds.value.delete(id)
      console.error(e)
    }
  }
}

const loadList = async ({
  themeIds = searchStore.themeIds,
  keyword = searchStore.keyword,
  checkin = searchStore.startDate,
  checkout = searchStore.endDate,
  guestCount = searchStore.guestCount,
  minPrice = searchStore.minPrice,
  maxPrice = searchStore.maxPrice,
  page: pageParam = 0,
  sort = currentSort.value,
  reset = false,
  appendData = true
} = {}) => {
  if (isLoading.value || isLoadingMore.value) return
  if (reset) {
    isLoading.value = true
    page.value = 0
  } else {
    isLoadingMore.value = true
  }
  try {
    const response = await searchList({
      themeIds,
      keyword,
      checkin,
      checkout,
      guestCount,
      minPrice,
      maxPrice,
      page: pageParam,
      size: PAGE_SIZE,
      sort,
      includeUnavailable: true
    })
    if (response.ok) {
      const payload = response.data
      const list = Array.isArray(payload?.items) ? payload.items : []
      const normalized = list.map(normalizeItem)
      
      // 모바일: 데이터 추가, PC: 데이터 교체
      if (reset || !appendData) {
        items.value = normalized
      } else {
        items.value = [...items.value, ...normalized]
      }
      
      const meta = payload?.page
      if (meta) {
        page.value = meta.number ?? pageParam
        totalPages.value = meta.totalPages ?? totalPages.value
        totalCount.value = meta.totalElements ?? 0
      }
    } else {
      console.error('Failed to load list', response.status)
    }
  } catch (error) {
    console.error('Failed to load list', error)
  } finally {
    isLoading.value = false
    isLoadingMore.value = false
  }
}

// 정렬은 서버에서 처리됨
const filteredItems = computed(() => {
  return items.value
})

const handleApplyFilter = ({ min, max, themeIds = [], guestCount = 1 }) => {
  searchStore.setPriceRange(min, max)
  searchStore.setThemeIds(themeIds)
  searchStore.setGuestCount(guestCount)
  isFilterModalOpen.value = false
  loadList({ themeIds, reset: true })
}

const goToDetail = (id) => {
  if (!id) return
  const query = { from: 'list', sort: currentSort.value, page: page.value, ...buildFilterQuery() }
  router.push({ path: `/room/${id}`, query })
}

const goToMap = () => {
  router.push({ path: '/map', query: { ...buildFilterQuery(), from: 'list' } })
}

const hasMore = computed(() => page.value + 1 < totalPages.value)

// 모바일용 무한 스크롤
const loadMore = () => {
  if (!isMobile.value) return
  if (!hasMore.value || isLoading.value || isLoadingMore.value) return
  const nextPage = page.value + 1
  loadList({ page: nextPage, appendData: true })
}

// PC용 페이징
const goToPage = (targetPage) => {
  if (isMobile.value) return
  const validPage = Math.min(Math.max(targetPage, 0), totalPages.value - 1)
  if (validPage === page.value) return
  // URL에 페이지 번호 저장
  router.replace({ query: { ...route.query, page: validPage } })
  // 스켈레톤 표시를 위해 아이템 초기화
  items.value = []
  window.scrollTo({ top: 0, behavior: 'auto' })
  // reset: true로 호출하여 내부에서 isLoading 설정
  loadList({ page: validPage, reset: true, appendData: false })
}

// 페이지 번호 목록 계산 (ReviewSection과 동일한 로직)
const pageNumbers = computed(() => {
  const total = totalPages.value
  const current = page.value + 1 // 0-based를 1-based로 변환

  if (total <= 5) {
    return Array.from({ length: total }, (_, index) => index + 1)
  }

  if (current <= 2) {
    return [1, 2, 3, 'ellipsis', total]
  }

  if (current === 3) {
    return [1, 2, 3, 4, 'ellipsis', total]
  }

  if (current >= total - 1) {
    return [1, 'ellipsis', total - 2, total - 1, total]
  }

  return [1, 'ellipsis', current - 1, current, current + 1, 'ellipsis', total]
})

const teardownObserver = () => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
}

const setupObserver = () => {
  // PC에서는 Observer 사용 안함
  if (!isMobile.value) {
    teardownObserver()
    return
  }
  if (typeof window === 'undefined' || !('IntersectionObserver' in window)) return
  teardownObserver()
  observer = new IntersectionObserver(
    (entries) => {
      if (!entries.length) return
      if (entries[0].isIntersecting) {
        loadMore()
      }
    },
    { root: null, rootMargin: '200px', threshold: 0.1 }
  )
  if (loadMoreTrigger.value) {
    observer.observe(loadMoreTrigger.value)
  }
}

const refreshObserver = async () => {
  if (!isMobile.value || !hasMore.value) {
    teardownObserver()
    return
  }
  await nextTick()
  setupObserver()
}

const handleClickOutside = (e) => {
  if (!e.target.closest('.sort-wrapper')) {
    isSortOpen.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  document.addEventListener('click', handleClickOutside)
  loadWishlist()
  applyRouteFilters(route.query)
  applyRouteKeyword()
  applyRouteSort()
  loadList({ page: getPageFromRoute(), reset: true })
  await nextTick()
  refreshObserver()
  setupPaginationObserver()
})

// PC에서 페이징 영역 감지 Observer 설정
const setupPaginationObserver = () => {
  if (isMobile.value) return
  if (paginationObserver) {
    paginationObserver.disconnect()
  }
  if (!paginationRef.value) return
  
  paginationObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach(entry => {
        // 페이징이 보이면 버튼을 static으로, 안 보이면 fixed로
        isMapBtnFixed.value = !entry.isIntersecting
      })
    },
    { rootMargin: '-50px 0px 0px 0px', threshold: 0 }
  )
  paginationObserver.observe(paginationRef.value)
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', handleClickOutside)
  teardownObserver()
  if (paginationObserver) {
    paginationObserver.disconnect()
    paginationObserver = null
  }
})

// 페이징이 렌더링될 때 observer 재설정
watch(
  () => paginationRef.value,
  (newVal) => {
    if (newVal && !isMobile.value) {
      nextTick(() => setupPaginationObserver())
    }
  }
)

watch(
  () => [
    route.query.keyword,
    route.query.themeIds,
    route.query.min,
    route.query.max,
    route.query.minPrice,
    route.query.maxPrice,
    route.query.guestCount,
    route.query.checkin,
    route.query.checkout,
    route.query.sort,
    route.query.page
  ],
  () => {
    applyRouteFilters(route.query)
    applyRouteKeyword()
    applyRouteSort()
    loadList({ page: getPageFromRoute(), reset: true })
  }
)

watch(
  () => hasMore.value,
  () => {
    refreshObserver()
  }
)

watch(
  () => loadMoreTrigger.value,
  () => {
    refreshObserver()
  }
)

// 화면 크기 변경 시 Observer 재설정
watch(
  () => isMobile.value,
  (mobile) => {
    if (mobile) {
      refreshObserver()
    } else {
      teardownObserver()
    }
  }
)
</script>

<template>
  <main class="container main-content">
    <div class="header">
      <h1 v-if="filteredItems.length > 0">검색결과 {{ totalCount }}건</h1>
      <h1 v-else>숙소 목록</h1>
    </div>

    <!-- Fixed Filter & Sort Buttons -->
    <div class="fixed-btns" @click.stop>
      <div class="sort-wrapper">
        <button class="sort-btn" @click="toggleSort">
          <span class="sort-label">{{ currentSortLabel }}</span>
          <span class="sort-arrow" :class="{ open: isSortOpen }">▼</span>
        </button>
        <div class="sort-dropdown" v-if="isSortOpen">
          <button
            v-for="option in SORT_OPTIONS"
            :key="option.value"
            class="sort-option"
            :class="{ active: currentSort === option.value }"
            @click="selectSort(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
      <button class="filter-btn" @click="isFilterModalOpen = !isFilterModalOpen"><span class="icon">🔍</span>필터</button>
    </div>

    <!-- Skeleton Loading -->
    <div class="list-container" v-if="isLoading && filteredItems.length === 0">
      <SkeletonCard v-for="n in 8" :key="'skeleton-' + n" class="list-item" />
    </div>

    <div class="list-container" v-else-if="filteredItems.length > 0">
      <GuesthouseCard
        v-for="item in filteredItems"
        :key="item.id"
        :id="item.id"
        :title="item.title"
        :description="item.description"
        :rating="item.rating"
        :review-count="item.reviewCount"
        :location="item.location"
        :price="item.price"
        :image-url="item.imageUrl"
        :is-favorite="wishlistIds.has(item.id)"
        @toggle-favorite="toggleWishlist"
        @click="goToDetail(item.id)"
        class="list-item"
      />
    </div>

    <!-- Empty State -->
    <div class="empty-state" v-else-if="!isLoading && filteredItems.length === 0">
      <div class="empty-icon-wrapper">
        <span class="empty-icon">🧐</span>
      </div>
      <h2 class="empty-title">조건에 맞는 숙소를 찾을 수 없어요</h2>
      <p class="empty-description">
        <span>적용한 필터를 변경하거나</span>
        <span>다른 검색어로 다시 시도해보세요.</span>
      </p>
    </div>

    <!-- 모바일: 무한 스크롤 트리거 -->
    <div ref="loadMoreTrigger" class="list-footer list-footer--observer" v-if="isMobile && hasMore">
      <span v-if="isLoadingMore" class="load-more-status">불러오는 중...</span>
    </div>

    <!-- Floating Map Button -->
    <div class="map-btn-wrapper" :class="{ 'map-btn-wrapper--static': !isMobile && !isMapBtnFixed }">
      <button class="map-floating-btn" @click="goToMap">
        <span class="text">지도에서 보기</span>
      </button>
    </div>

    <!-- PC: 페이지네이션 -->
    <div v-if="!isMobile && totalPages > 1 && filteredItems.length > 0" ref="paginationRef" class="list-pagination">
      <button
        type="button"
        class="page-btn nav"
        :disabled="page === 0"
        @click="goToPage(page - 1)"
      >
        이전
      </button>
      <div class="page-list">
        <template v-for="pageNum in pageNumbers" :key="`page-${pageNum}`">
          <span v-if="pageNum === 'ellipsis'" class="page-ellipsis">…</span>
          <button
            v-else
            type="button"
            class="page-btn number"
            :class="{ active: pageNum === page + 1 }"
            :aria-current="pageNum === page + 1 ? 'page' : null"
            @click="goToPage(pageNum - 1)"
          >
            {{ pageNum }}
          </button>
        </template>
      </div>
      <button
        type="button"
        class="page-btn nav"
        :disabled="!hasMore"
        @click="goToPage(page + 1)"
      >
        다음
      </button>
    </div>

    <!-- Filter Modal -->
    <FilterModal 
      :is-open="isFilterModalOpen"
      :current-min="searchStore.minPrice"
      :current-max="searchStore.maxPrice"
      :current-themes="searchStore.themeIds"
      :current-guest-count="searchStore.guestCount"
      @close="isFilterModalOpen = false"
      @apply="handleApplyFilter"
    />
  </main>
</template>

<style scoped>
.main-content {
  padding-top: 4px;
  padding-bottom: 1rem;
  padding-left: 1rem;
  padding-right: 1rem;
  --card-width: 300px;
}

/* PC 웹에서 header와 동일한 max-width 적용 */
@media (min-width: 769px) {
  .main-content {
    max-width: 1400px;
    margin: 0 auto;
    padding-left: 24px;
    padding-right: 24px;
  }
}

.header {
  margin-bottom: 0;
  padding-bottom: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
}

/* Sort Button Styles */
.sort-wrapper {
  position: relative;
}

.sort-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  background: white;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.2s;
}

.sort-btn:hover {
  border-color: #6DC3BB;
  background-color: #f0f7f6;
}

.sort-arrow {
  font-size: 0.65rem;
  transition: transform 0.2s;
}

.sort-arrow.open {
  transform: rotate(180deg);
}

.sort-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 140px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 6px 0;
  z-index: 130;
  animation: dropdownFadeIn 0.15s ease;
}

@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sort-option {
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: transparent;
  text-align: left;
  font-size: 0.9rem;
  color: var(--text-main);
  cursor: pointer;
  transition: background-color 0.15s;
}

.sort-option:hover {
  background-color: #f5f5f5;
}

.sort-option.active {
  color: #6DC3BB;
  font-weight: 600;
  background-color: #f0f7f6;
}

/* Fixed Buttons Group */
.fixed-btns {
  position: fixed;
  top: 95px;
  right: 1rem;
  z-index: 120;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* PC 웹에서 컨테이너 안쪽에 위치 */
@media (min-width: 769px) {
  .fixed-btns {
    right: max(24px, calc((100vw - 1400px) / 2 + 24px));
  }
}

@media (max-width: 768px) {
  .fixed-btns {
    top: calc(112px + env(safe-area-inset-top));
    right: 12px;
  }
}

.filter-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-main);
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.filter-btn:hover {
  background-color: #f5f5f5;
}

.list-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
  justify-content: center;
}

.list-item {
  cursor: pointer;
  transition: transform 0.2s;
  width: 100%;
  max-width: var(--card-width);
  justify-self: center;
}

.list-item:hover {
  transform: translateY(-5px);
}

/* 태블릿 (3열) */
@media (min-width: 640px) {
  .list-container {
    grid-template-columns: repeat(3, 1fr);
    gap: 1.5rem;
  }
}

/* PC (4열) */
@media (min-width: 1024px) {
  .list-container {
    grid-template-columns: repeat(4, 1fr);
    gap: 2.5rem;
  }
}

.list-footer {
  display: flex;
  justify-content: center;
  margin: 2rem 0 1rem;
  min-height: 48px;
}

.list-footer--observer {
  align-items: center;
}

.load-more-status {
  padding: 10px 18px;
  border: 1px solid #ddd;
  border-radius: 24px;
  background: white;
  font-weight: 600;
  color: #6b7280;
}

/* Floating Button Styles */
.map-btn-wrapper {
  position: fixed;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 50;
}

/* PC에서 페이징 영역에 도달하면 static으로 전환 */
.map-btn-wrapper--static {
  position: relative;
  bottom: auto;
  left: auto;
  transform: none;
  display: flex;
  justify-content: center;
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
}

@media (max-width: 768px) {
  .map-btn-wrapper {
    bottom: 1rem;
  }
}

.map-floating-btn {
  background-color: #222;
  color: white;
  border: none;
  border-radius: 24px;
  width: 138px;
  justify-content: center;
  padding: 12px 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  box-shadow: 0 4px 12px rgba(0,0,0,0.25);
  cursor: pointer;
  transition: transform 0.2s, background-color 0.2s;
}

.map-floating-btn:hover {
  background-color: #000;
  transform: scale(1.05);
}

.map-floating-btn .icon {
  font-size: 1.1rem;
}

/* Empty State Styles */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 6rem 1rem;
  text-align: center;
  min-height: 400px;
}

.empty-icon-wrapper {
  width: 80px;
  height: 80px;
  background-color: var(--bg-gray, #f3f4f6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.empty-icon {
  font-size: 2.5rem;
  line-height: 1;
}

.empty-title {
  font-size: 1.25rem;
  font-weight: 700;
  font-family: 'NanumSquareRound', sans-serif;
  color: var(--text-main, #1f2937);
  margin: 0 0 0.75rem 0;
  word-break: keep-all;
}

.empty-description {
  font-size: 0.95rem;
  color: var(--text-sub, #6b7280);
  margin: 0;
  line-height: 1.6;
  word-break: keep-all;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

@media (min-width: 480px) {
  .empty-description {
    display: block;
  }
  
  .empty-description span {
    display: inline-block;
    margin: 0 0.2rem;
  }
}

/* PC 페이지네이션 스타일 (ReviewSection과 동일) */
.list-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin: 2rem 0 1rem;
  padding-bottom: 2rem;
}

.page-list {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.page-btn {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  padding: 0.35rem 0.9rem;
  font-size: 0.85rem;
  cursor: pointer;
  color: var(--text-main);
  transition: all 0.2s ease;
}

.page-btn.number {
  min-width: 36px;
  height: 36px;
  padding: 0 0.65rem;
  font-weight: 600;
  background: #fff;
}

.page-btn.number:hover:not(.active) {
  background: #f0f7f6;
  border-color: #6DC3BB;
}

.page-btn.number.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #004d40;
}

.page-btn.nav {
  background: #fff;
  border-color: #e5e7eb;
  font-weight: 600;
}

.page-btn.nav:hover:not(:disabled) {
  background: #f0f7f6;
  border-color: #6DC3BB;
  color: #6DC3BB;
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.page-ellipsis {
  color: var(--text-sub);
  font-size: 0.9rem;
  padding: 0 0.25rem;
}
</style>
