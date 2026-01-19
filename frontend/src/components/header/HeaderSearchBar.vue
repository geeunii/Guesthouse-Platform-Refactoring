<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useSearchStore } from '@/stores/search'
import { useCalendarStore } from '@/stores/calendar'
import { fetchSearchSuggestions, resolveSearchAccommodation } from '@/api/list'
import HeaderCalendar from './HeaderCalendar.vue'
import HeaderGuestPicker from './HeaderGuestPicker.vue'

const POPULAR_DESTINATIONS = [
  { value: '협재', type: 'REGION', description: '에메랄드빛 바다와 비양도' },
  { value: '애월', type: 'REGION', description: '낭만적인 카페거리와 산책로' },
  { value: '성산', type: 'REGION', description: '유네스코 자연유산 성산일출봉' },
  { value: '서귀포', type: 'REGION', description: '제주의 남쪽, 따뜻한 휴양지' },
  { value: '함덕', type: 'REGION', description: '야자수와 백사장이 펼쳐진 곳' }
]

const router = useRouter()
const route = useRoute()
const searchStore = useSearchStore()
const calendarStore = useCalendarStore()

// Search Keyword State
const searchKeyword = ref(searchStore.keyword || '')
const suggestKeyword = ref(searchKeyword.value || '')

const keywordDisplay = computed(() => {
  const keyword = String(searchStore.keyword ?? '').trim()
  return keyword || '어디로 갈까?'
})

// Suggestion State
const suggestions = ref([])
const isSuggestOpen = ref(false)
const isSuggestLoading = ref(false)
const hasSuggestFetched = ref(false)
const isShowingPopular = ref(false) // 인기 검색어 노출 여부
const isComposing = ref(false)
const MIN_SUGGEST_LENGTH = 2
const SUGGEST_LIMIT = 10
let suggestTimer = null
let suggestRequestId = 0

// UI State
const isSearchExpanded = ref(false)
const isGuestOpen = ref(false)
const isCalendarOpen = computed(() => calendarStore.activeCalendar === 'header')

// Mobile check
const isMobile = () => window.innerWidth <= 768

// Suggestion Logic
const normalizeSuggestKeyword = (value) => {
  const trimmed = String(value ?? '').trim()
  return trimmed.length >= MIN_SUGGEST_LENGTH ? trimmed : ''
}

const canShowSuggestions = computed(() => {
  // API 로딩 중에도 suggestions div를 유지하여 클릭이 투과되는 것 방지
  return isSuggestOpen.value && (suggestions.value.length > 0 || isSuggestLoading.value)
})

const clearSuggestionTimer = () => {
  if (suggestTimer) {
    clearTimeout(suggestTimer)
    suggestTimer = null
  }
}

const resetSuggestions = () => {
  clearSuggestionTimer()
  suggestions.value = []
  isSuggestLoading.value = false
  hasSuggestFetched.value = false
  isShowingPopular.value = false
}

const isSelecting = ref(false)

const scheduleSuggestionFetch = (value) => {
  if (isSelecting.value) return // 선택 중이면 무시

  const trimmed = String(value ?? '').trim()
  if (trimmed.length === 0 && isSuggestOpen.value) {
    clearSuggestionTimer()
    suggestions.value = [...POPULAR_DESTINATIONS]
    isSuggestLoading.value = false
    hasSuggestFetched.value = true
    isShowingPopular.value = true
    return
  }

  const keyword = normalizeSuggestKeyword(value)

  // 즉시 리셋하지 않고 타이머 설정 (입력 중간 단계에서 리스트 깜빡임 방지)
  clearSuggestionTimer()
  hasSuggestFetched.value = false
  isShowingPopular.value = false

  suggestTimer = setTimeout(async () => {
    // suggestions가 닫혀있으면 중단
    if (!isSuggestOpen.value) {
      resetSuggestions()
      return
    }

    // 키워드가 유효하지 않으면 리셋
    if (!keyword) {
      resetSuggestions()
      return
    }

    const requestId = ++suggestRequestId
    isSuggestLoading.value = true
    try {
      const response = await fetchSearchSuggestions(keyword, SUGGEST_LIMIT)
      if (requestId !== suggestRequestId) return
      if (response.ok && Array.isArray(response.data)) {
        suggestions.value = response.data
      } else {
        suggestions.value = []
      }
    } catch (error) {
      console.error('Failed to load search suggestions', error)
      if (requestId === suggestRequestId) {
        suggestions.value = []
      }
    } finally {
      if (requestId === suggestRequestId) {
        isSuggestLoading.value = false
        hasSuggestFetched.value = true
      }
    }
  }, 250)
}

const openSuggestions = () => {
  if (isSuggestOpen.value) return // 이미 열려있으면 무시
  if (isSelecting.value) return
  isSuggestOpen.value = true
  scheduleSuggestionFetch(suggestKeyword.value || searchKeyword.value)
}

const closeSuggestions = () => {
  isSuggestOpen.value = false
  resetSuggestions()
}

const getSuggestionLabel = (type) => {
  const normalized = String(type || '').toUpperCase()
  return normalized === 'REGION' ? '지역' : '숙소'
}

// Navigation & Search Execution
const resolveAccommodation = async (value) => {
  const keyword = String(value ?? '').trim()
  if (!keyword) return null
  try {
    const response = await resolveSearchAccommodation(keyword)
    if (response.ok && response.data?.accommodationsId) {
      return response.data
    }
  } catch (error) {
    console.error('Failed to resolve accommodation', error)
  }
  return null
}

const isAccommodationSuggestion = (suggestion) => {
  return String(suggestion?.type || '').toUpperCase() === 'ACCOMMODATION'
}

const formatDateParam = (date) => {
  if (!(date instanceof Date)) return null
  if (Number.isNaN(date.getTime())) return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const isMapContext = () => {
  if (route.path === '/map') return true
  const from = route.query.from
  if (Array.isArray(from)) return from.includes('map')
  return from === 'map'
}

const buildSearchQuery = () => {
  const query = {}
  const keyword = String(searchKeyword.value ?? '').trim()
  searchStore.setKeyword(keyword)
  searchKeyword.value = keyword
  if (keyword) query.keyword = keyword
  if (searchStore.guestCount > 0) query.guestCount = String(searchStore.guestCount)
  const checkin = formatDateParam(searchStore.startDate)
  const checkout = formatDateParam(searchStore.endDate)
  if (checkin && checkout) {
    query.checkin = checkin
    query.checkout = checkout
  }
  if (route.path === '/list' || isMapContext()) {
    if (searchStore.minPrice !== null) query.min = String(searchStore.minPrice)
    if (searchStore.maxPrice !== null) query.max = String(searchStore.maxPrice)
    if (searchStore.themeIds.length) query.themeIds = searchStore.themeIds.join(',')
  }
  return query
}

const handleSuggestionInteract = () => {
  isSelecting.value = true
}

const selectSuggestion = async (suggestion) => {
  // 선택 시작 플래그 설정
  isSelecting.value = true
  
  const val = suggestion?.value
  const type = suggestion?.type
  if (!val) {
    isSelecting.value = false
    return
  }

  const nextValue = String(val)
  
  // 입력 상태 정리
  isComposing.value = false
  searchKeyword.value = nextValue
  suggestKeyword.value = nextValue
  searchStore.setKeyword(nextValue)
  
  // 강제로 포커스 해제하여 가상 키보드 닫기
  if (document.activeElement instanceof HTMLElement) {
    document.activeElement.blur()
  }

  // 네비게이션
  try {
    if (String(type || '').toUpperCase() === 'ACCOMMODATION') {
      const resolved = await resolveAccommodation(nextValue)
      if (resolved?.accommodationsId) {
        await router.push({ path: `/room/${resolved.accommodationsId}` })
        return
      }
    }

    const targetPath = isMapContext() ? '/map' : '/list'
    await router.push({ path: targetPath, query: buildSearchQuery() })
  } catch (error) {
    console.error('Navigation error:', error)
  } finally {
    // 네비게이션 완료 후 닫기 (이벤트 전파 이슈 방지 및 자연스러운 전환)
    isSearchExpanded.value = false
    closeSuggestions()

    // 약간의 지연 후 선택 플래그 해제 (이벤트 루프 처리 대기)
    setTimeout(() => {
      isSelecting.value = false
    }, 500)
  }
}

const handleSearch = async () => {
  const keyword = String(searchKeyword.value ?? '').trim()
  if (keyword) {
    const resolved = await resolveAccommodation(keyword)
    if (resolved?.accommodationsId) {
      searchStore.setKeyword(keyword)
      searchKeyword.value = keyword
      isSearchExpanded.value = false
      closeSuggestions()
      router.push({ path: `/room/${resolved.accommodationsId}` })
      return
    }
  }
  const targetPath = isMapContext() ? '/map' : '/list'
  router.push({ path: targetPath, query: buildSearchQuery() })
  isSearchExpanded.value = false
  closeSuggestions()
}

// Input Handlers
const handleInput = (event) => {
  if (isSelecting.value) return
  const value = event?.target?.value ?? ''
  suggestKeyword.value = value
  if (!isSuggestOpen.value) return
  if (event?.isComposing || isComposing.value) {
    scheduleSuggestionFetch(value)
  }
}

const handleCompositionStart = () => { isComposing.value = true }
const handleCompositionUpdate = (event) => {
  if (isSelecting.value) return
  const value = event?.target?.value ?? ''
  suggestKeyword.value = value
  scheduleSuggestionFetch(value)
}
const handleCompositionEnd = (event) => {
  if (isSelecting.value) return
  isComposing.value = false
  const value = event?.target?.value ?? searchKeyword.value
  suggestKeyword.value = value
  scheduleSuggestionFetch(value)
}

// Toggle Handlers
const toggleSearch = (event) => {
  if (!isMobile()) return
  if (event?.target?.closest?.('.search-btn-mini')) {
    handleSearch()
    return
  }
  isSearchExpanded.value = !isSearchExpanded.value
  if (!isSearchExpanded.value) {
    closeSuggestions()
  }
}

const toggleCalendar = (e) => {
  // suggestions가 열려있으면 캘린더 토글 무시
  if (isSuggestOpen.value) {
    return
  }
  e.stopPropagation()
  calendarStore.toggleCalendar('header')
  isGuestOpen.value = false
  closeSuggestions()
}

const toggleGuestPicker = (e) => {
  // suggestions가 열려있으면 게스트 피커 토글 무시
  if (isSuggestOpen.value) return
  e.stopPropagation()
  isGuestOpen.value = !isGuestOpen.value
  calendarStore.closeCalendar('header')
  closeSuggestions()
}

// Click Outside & Resize
const handleClickOutside = (e) => {
  const closestWrapper = e.target.closest('.search-keyword-wrapper')
  const closestSuggestions = e.target.closest('.search-suggestions')
  
  if (isSelecting.value) {
    return
  }
  
  if (!e.target.closest('.date-picker-wrapper')) {
    calendarStore.closeCalendar('header')
  }
  if (!e.target.closest('.guest-picker-wrapper')) {
    isGuestOpen.value = false
  }
  // suggestions 영역 클릭 시에도 닫지 않음
  if (!closestWrapper && !closestSuggestions) {
    closeSuggestions()
  }
}

const handleResize = () => {
  if (!isMobile()) {
    isSearchExpanded.value = false
    closeSuggestions()
  }
}

// Lifecycle & Watchers
watch(() => searchStore.keyword, (value) => {
  const next = value || ''
  if (next !== searchKeyword.value) {
    searchKeyword.value = next
  }
})

watch(() => searchKeyword.value, (value) => {
  if (isComposing.value) return
  suggestKeyword.value = value || ''
  if (!isSuggestOpen.value) return
  scheduleSuggestionFetch(value)
})

watch(() => route.fullPath, () => {
  if (isMobile()) {
    isSearchExpanded.value = false
    closeSuggestions()
  }
})

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
  clearSuggestionTimer()
})
</script>

<template>
  <div class="search-bar" :class="{ expanded: isSearchExpanded }">
    <!-- Collapsed Mobile View - Click to expand -->
    <div class="search-bar-collapsed" @click="toggleSearch" v-if="!isSearchExpanded">
      <span class="collapsed-text collapsed-text--keyword">{{ keywordDisplay }}</span>
      <span class="collapsed-divider">|</span>
      <span class="collapsed-text">{{ searchStore.dateDisplayText }}</span>
      <span class="collapsed-divider">|</span>
      <span class="collapsed-text">{{ searchStore.guestDisplayText }}</span>
      <button class="search-btn-mini" type="button" aria-label="검색">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
        </svg>
      </button>
    </div>

    <!-- Expanded Mobile View -->
    <div class="search-bar-expanded" :class="{ 'suggestions-open': canShowSuggestions }" v-if="isSearchExpanded" @click.stop>
      <div class="expanded-close" @click="isSearchExpanded = false">×</div>
      <div class="search-item-full search-keyword-wrapper">
        <label>여행지</label>
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="어디로 갈까?"
          @input="handleInput"
          @keydown.enter.prevent="handleSearch"
          @focus="openSuggestions"
          @compositionstart="handleCompositionStart"
          @compositionupdate="handleCompositionUpdate"
          @compositionend="handleCompositionEnd"
        >
        <div v-if="canShowSuggestions" class="search-suggestions" 
             @mousedown.capture.prevent="handleSuggestionInteract"
             @touchstart.capture="handleSuggestionInteract"
             @click.stop>
          <div v-if="isShowingPopular" class="suggestions-header">✨ 추천 검색어</div>
          <button
            v-for="(suggestion, idx) in suggestions"
            :key="`${suggestion.type}-${suggestion.value}-${idx}`"
            type="button"
            :class="[
              'search-suggestion',
              String(suggestion.type || '').toUpperCase() === 'REGION'
                ? 'search-suggestion--region'
                : 'search-suggestion--accommodation'
            ]"
            @click.stop="selectSuggestion(suggestion)"
          >
            <div class="suggestion-info">
              <span class="suggestion-text">{{ suggestion.value }}</span>
              <span v-if="suggestion.description" class="suggestion-desc">{{ suggestion.description }}</span>
            </div>
            <span
              class="suggestion-tag"
              :class="String(suggestion.type || '').toUpperCase() === 'REGION' ? 'suggestion-tag--region' : 'suggestion-tag--accommodation'"
            >
              {{ getSuggestionLabel(suggestion.type) }}
            </span>
          </button>
          
          <div v-if="isSuggestLoading && suggestions.length === 0" class="suggestion-loading">검색 중...</div>
          <div v-if="!isSuggestLoading && hasSuggestFetched && !suggestions.length" class="suggestion-empty">검색 결과 없음</div>
        </div>
      </div>

      <div class="search-item-full" 
           :style="{ 'pointer-events': isSuggestOpen ? 'none' : 'auto' }"
           @click="toggleCalendar">
        <label>날짜</label>
        <input type="text" :placeholder="searchStore.dateDisplayText" readonly>
      </div>

      <!-- Mobile Calendar -->
      <HeaderCalendar v-if="isCalendarOpen" mode="mobile" />

      <div class="search-item-full" 
           :style="{ 'pointer-events': isSuggestOpen ? 'none' : 'auto' }"
           @click="toggleGuestPicker">
        <label>여행자</label>
        <input type="text" :placeholder="searchStore.guestDisplayText" readonly>
      </div>

      <!-- Mobile Guest Picker -->
      <HeaderGuestPicker v-if="isGuestOpen" mode="mobile" />

      <button class="search-btn-full" @click="handleSearch">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="18" height="18">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
        </svg>
      </button>
    </div>

    <!-- Desktop View -->
    <div class="search-bar-desktop">
      <div class="search-item search-keyword-wrapper">
        <label>여행지</label>
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="어디로 갈까?"
          @input="handleInput"
          @keydown.enter.prevent="handleSearch"
          @focus="openSuggestions"
          @compositionstart="handleCompositionStart"
          @compositionupdate="handleCompositionUpdate"
          @compositionend="handleCompositionEnd"
        >
        <div v-if="canShowSuggestions" class="search-suggestions" 
             @mousedown.capture.prevent="handleSuggestionInteract"
             @touchstart.capture="handleSuggestionInteract"
             @click.stop>
          <div v-if="isShowingPopular" class="suggestions-header">✨ 추천 검색어</div>
          <button
            v-for="(suggestion, idx) in suggestions"
            :key="`${suggestion.type}-${suggestion.value}-${idx}`"
            type="button"
            :class="[
              'search-suggestion',
              String(suggestion.type || '').toUpperCase() === 'REGION'
                ? 'search-suggestion--region'
                : 'search-suggestion--accommodation'
            ]"
            @click.stop="selectSuggestion(suggestion)"
          >
            <div class="suggestion-info">
              <span class="suggestion-text">{{ suggestion.value }}</span>
              <span v-if="suggestion.description" class="suggestion-desc">{{ suggestion.description }}</span>
            </div>
            <span
              class="suggestion-tag"
              :class="String(suggestion.type || '').toUpperCase() === 'REGION' ? 'suggestion-tag--region' : 'suggestion-tag--accommodation'"
            >
              {{ getSuggestionLabel(suggestion.type) }}
            </span>
          </button>

          <div v-if="isSuggestLoading && suggestions.length === 0" class="suggestion-loading">검색 중...</div>
          <div v-if="!isSuggestLoading && hasSuggestFetched && !suggestions.length" class="suggestion-empty">검색 결과 없음</div>
        </div>
      </div>

      <div class="search-divider"></div>

      <div class="date-picker-wrapper" @click.stop>
        <div class="search-item" @click="toggleCalendar">
          <label>날짜</label>
          <input type="text" :placeholder="searchStore.dateDisplayText" readonly>
        </div>

        <!-- Desktop Calendar -->
        <HeaderCalendar v-if="isCalendarOpen" mode="desktop" />
      </div>

      <div class="search-divider"></div>

      <div class="guest-picker-wrapper" @click.stop>
        <div class="search-item" @click="toggleGuestPicker">
          <label>여행자</label>
          <input type="text" :placeholder="searchStore.guestDisplayText" readonly>
        </div>

        <!-- Desktop Guest Picker -->
        <HeaderGuestPicker v-if="isGuestOpen" mode="desktop" />
      </div>

      <button class="search-btn" aria-label="검색" @click="handleSearch">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
/* Search Bar Styles */
.search-bar {
  width: 100%;
  background: white;
  border: 1px solid #e0e6eb;
  border-radius: 12px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.search-bar:hover {
  border-color: #6DC3BB;
  box-shadow: 0 4px 12px rgba(109, 195, 187, 0.1);
}

/* Hide mobile elements on desktop */
.search-bar-collapsed,
.search-bar-expanded {
  display: none !important;
}

/* Show desktop elements on desktop */
.search-bar-desktop {
  display: contents;
}

.search-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 6px 8px;
  border-radius: 6px;
  min-width: 0;
}

.search-keyword-wrapper {
  position: relative;
}

/* Search Suggestions */
.search-suggestions {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: auto;
  width: min(560px, calc(100vw - 32px));
  min-width: 100%;
  font-family: sans-serif;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  z-index: 99999;
  max-height: 480px;
  overflow-y: auto;
  pointer-events: auto;
  padding: 6px;
  overscroll-behavior: contain;
  animation: suggestionDrop 0.16s ease-out;
}

.search-suggestion {
  --suggest-accent: #0f766e;
  width: 100%;
  border: 1px solid transparent;
  background: #ffffff;
  text-align: left;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  font-size: 14px;
  color: #111827;
  border-radius: 10px;
  transition: background 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
  position: relative;
}

.search-suggestion + .search-suggestion {
  margin-top: 4px;
}

.search-suggestion::before {
  content: '';
  width: 3px;
  height: 18px;
  border-radius: 999px;
  background: var(--suggest-accent);
  opacity: 0.6;
  flex-shrink: 0;
}

.search-suggestion--region {
  --suggest-accent: #2563eb;
}

.search-suggestion--accommodation {
  --suggest-accent: #0f766e;
}

.search-suggestion:hover,
.search-suggestion:focus-visible {
  background: #f8fafc;
  border-color: #e2e8f0;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
  outline: none;
}

.suggestions-header {
  padding: 8px 14px 4px;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 4px;
}

.suggestion-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.suggestion-desc {
  font-size: 11px;
  color: #64748b;
  font-weight: 400;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.suggestion-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 999px;
  letter-spacing: 0.2px;
  flex-shrink: 0;
  border: 1px solid #e2e8f0;
  color: #475569;
  background: #f8fafc;
  margin-left: auto;
}

.search-suggestion--accommodation .suggestion-tag {
  background: #ecfdf5;
  color: #0f766e;
  border-color: #99f6e4;
}

.search-suggestion--region .suggestion-tag {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: #bfdbfe;
}

.suggestion-text {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.suggestion-empty,
.suggestion-loading {
  padding: 14px 8px;
  font-size: 13px;
  color: #6b7280;
  text-align: center;
}

.suggestion-loading {
  color: #0f766e;
  font-weight: 600;
}

.search-suggestions::-webkit-scrollbar {
  width: 6px;
}

.search-suggestions::-webkit-scrollbar-thumb {
  background: rgba(15, 23, 42, 0.2);
  border-radius: 999px;
}

.search-suggestions::-webkit-scrollbar-track {
  background: transparent;
}

@keyframes suggestionDrop {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (prefers-reduced-motion: reduce) {
  .search-suggestions { animation: none; }
  .search-suggestion { transition: none; }
}

.search-item:hover { background-color: #f0f7f6; }

.search-item label {
  font-size: 10px; font-weight: 600; color: #8a92a0; text-transform: uppercase;
  letter-spacing: 0.3px; margin-bottom: 2px; display: block; font-family: 'Poppins', sans-serif;
  white-space: nowrap;
}

.search-item input {
  background: transparent; border: none; font-size: 13px; color: #1a1f36; outline: none;
  font-weight: 500; font-family: 'Noto Sans KR', sans-serif; width: 100%;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.search-item input::placeholder { color: #c5cdd4; font-weight: 400; }

.search-divider {
  width: 1px; height: 20px; background-color: #e8ecf0; flex-shrink: 0;
}

.search-btn {
  display: flex; align-items: center; justify-content: center; width: 36px; height: 36px;
  background-color: #6DC3BB; border: none; border-radius: 50%; cursor: pointer; color: white;
  transition: all 0.3s ease; flex-shrink: 0; box-shadow: 0 2px 8px rgba(109, 195, 187, 0.2);
}

.search-btn:hover {
  background-color: #5aaca3; box-shadow: 0 4px 16px rgba(109, 195, 187, 0.3); transform: scale(1.05);
}
.search-btn:active { transform: scale(0.95); }
.search-btn svg { width: 16px; height: 16px; }

/* Mobile Expanded/Collapsed Styles */
@media (max-width: 768px) {
  .search-bar-desktop { display: none !important; }
  .search-bar .search-btn { display: none !important; }

  .search-bar {
    width: 100%; max-width: 100%; padding: 0; border: none; box-shadow: none; background: transparent;
  }
  
  .search-bar-collapsed {
    display: flex !important; align-items: center; background: white; border: 1px solid #e0e6eb;
    border-radius: 20px; padding: 8px 16px; gap: 8px; width: 100%; cursor: pointer;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
  
  .search-bar.expanded .search-bar-collapsed { display: none !important; }
  .search-bar.expanded .search-bar-expanded { display: flex !important; }

  .search-suggestions { left: 50%; transform: translateX(-50%); }

  .collapsed-text {
    flex: 1 1 0; min-width: 0; font-size: 13px; color: #6b7280; white-space: nowrap;
    overflow: hidden; text-overflow: ellipsis; font-family: revert;
  }
  .collapsed-text--keyword { flex: 1 1 0; min-width: 0; }
  .collapsed-divider { color: #e0e6eb; font-size: 12px; font-family: revert; }
  
  .search-btn-mini {
    display: flex; align-items: center; justify-content: center; width: 32px; height: 32px;
    background-color: #6DC3BB; border: none; border-radius: 50%; cursor: pointer; color: white;
    flex-shrink: 0;
  }
  
  /* Expanded Mobile Search Bar */
  .search-bar-expanded {
    display: flex; flex-direction: column; background: white; border: 1px solid #e0e6eb;
    border-radius: 20px; padding: 20px; width: 100%; position: relative;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }
  
  .expanded-close {
    position: absolute; top: 12px; right: 16px; font-size: 24px; color: #9ca3af;
    cursor: pointer; line-height: 1;
  }
  .expanded-close:hover { color: #374151; }
  
  .search-item-full {
    display: flex; flex-direction: column; padding: 16px 0; border-bottom: 1px solid #f0f0f0;
  }
  
  .search-item-full.search-keyword-wrapper {
    position: relative;
  }
  .search-item-full:last-of-type { border-bottom: none; }
  
  .search-item-full label {
    font-size: 12px; font-weight: 600; color: #374151; margin-bottom: 6px; font-family: sans-serif;
  }
  
  .search-item-full input {
    background: transparent; border: none; font-size: 16px; color: #1a1f36; outline: none;
    padding: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-family: revert;
  }
  .search-item-full input::placeholder { color: #9ca3af; }
  
  .search-btn-full {
    display: flex; align-items: center; justify-content: center; width: 100%; height: 48px;
    background-color: #6DC3BB; border: none; border-radius: 12px; cursor: pointer; color: white;
    margin-top: 16px; font-size: 16px; font-weight: 600; gap: 8px;
  }
  .search-btn-full:hover { background-color: #5aaca3; }

  /* suggestions가 열렸을 때 다른 요소들의 클릭 차단 */
  .search-bar-expanded.suggestions-open > *:not(.search-keyword-wrapper) {
    pointer-events: none;
    opacity: 0.5; /* 시각적 피드백 */
  }
}

@media (min-width: 769px) {
  .search-bar {
    max-width: 850px; width: min(850px, 100%); order: 2; justify-self: center;
    grid-column: 2; border-radius: 40px; padding: 8px 16px; gap: 12px;
  }
  
  .search-item { padding: 6px 10px; min-width: 150px; }
  .search-item label { font-size: 12px; display: block !important; }
  .search-item input { font-size: 14px; }
  
  .search-divider { display: block !important; height: 20px; }
  
  .search-btn { width: 32px; height: 32px; }
  .search-btn svg { width: 16px; height: 16px; }
  
  .date-picker-wrapper, .guest-picker-wrapper {
    position: relative; flex: 1; min-width: 0;
  }
}
</style>
