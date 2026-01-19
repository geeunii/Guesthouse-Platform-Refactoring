<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { fetchThemes } from '@/api/theme'

const props = defineProps({
  isOpen: Boolean,
  currentMin: Number,
  currentMax: Number,
  currentThemes: {
    type: Array,
    default: () => []
  },
  currentGuestCount: {
    type: Number,
    default: 1
  }
})

const emit = defineEmits(['close', 'apply'])

// Constants for slider
const MIN_LIMIT = 0
const MAX_LIMIT = 300000
const STEP = 5000

// Local state
const minPrice = ref(props.currentMin ?? MIN_LIMIT)
const maxPrice = ref(props.currentMax ?? MAX_LIMIT)
const themes = ref([])
const selectedThemeIds = ref([...props.currentThemes])
const guestCount = ref(props.currentGuestCount ?? 1)
const isLoadingThemes = ref(false)
const activeThumb = ref('max')

// Sync with props
watch(() => props.isOpen, (newVal) => {
  if (newVal) {
    minPrice.value = props.currentMin ?? MIN_LIMIT
    maxPrice.value = props.currentMax ?? MAX_LIMIT
    selectedThemeIds.value = [...props.currentThemes]
    guestCount.value = props.currentGuestCount ?? 1

    if (!themes.value.length) {
      loadThemes()
    }
  }
})

const loadThemes = async () => {
  if (isLoadingThemes.value) return
  isLoadingThemes.value = true
  try {
    const { ok, data } = await fetchThemes()
    if (ok && Array.isArray(data)) {
      themes.value = data
    }
  } catch (e) {
    console.error('Failed to load themes', e)
  } finally {
    isLoadingThemes.value = false
  }
}

const toggleTheme = (id) => {
  if (selectedThemeIds.value.includes(id)) {
    selectedThemeIds.value = selectedThemeIds.value.filter(item => item !== id)
  } else {
    selectedThemeIds.value = [...selectedThemeIds.value, id]
  }
}

const increaseGuest = () => {
  guestCount.value += 1
}

const decreaseGuest = () => {
  guestCount.value = Math.max(1, guestCount.value - 1)
}

onMounted(() => {
  loadThemes()
})

// Ensure min <= max
const handleMinChange = () => {
  if (minPrice.value > maxPrice.value) {
    minPrice.value = maxPrice.value
  }
}

const handleMaxChange = () => {
  if (maxPrice.value < minPrice.value) {
    maxPrice.value = minPrice.value
  }
}

const onMinInput = () => {
  activeThumb.value = 'min'
  handleMinChange()
}

const onMaxInput = () => {
  activeThumb.value = 'max'
  handleMaxChange()
}

const maxPriceLabel = computed(() => {
  if (maxPrice.value >= MAX_LIMIT) return `${MAX_LIMIT.toLocaleString()}원+`
  return `${maxPrice.value.toLocaleString()}원`
})

const minThumbOnTop = computed(() => {
  if ((maxPrice.value - minPrice.value) > STEP) return false
  return activeThumb.value === 'min'
})

const CATEGORY_LABELS = {
  NATURE: '자연',
  CULTURE: '문화',
  ACTIVITY: '액티비티',
  VIBE: '분위기',
  PARTY: '파티',
  MEETING: '만남/로맨스',
  PERSONA: '성향/동반',
  FACILITY: '시설/편의',
  FOOD: '음식',
  PLAY: '놀거리',
  AROUND_THEME: '주변테마',
  ACTIVITY_COMMUNITY: '액티비티/커뮤니티',
  LOCATION: '위치',
  LIFESTYLE: '라이프스타일',
  OTHER: '기타'
}

const normalizeCategoryKey = (value) => {
  const raw = value === undefined || value === null ? '' : String(value).trim()
  return raw || 'OTHER'
}

const normalizeThemeName = (value) => {
  return value === undefined || value === null ? '' : String(value).trim()
}

const moveCultureHeritageToEnd = (items) => {
  return [...items].sort((a, b) => {
    const isACulture = normalizeThemeName(a?.themeName) === '문화유적'
    const isBCulture = normalizeThemeName(b?.themeName) === '문화유적'
    if (isACulture === isBCulture) {
      return 0
    }
    return isACulture ? 1 : -1
  })
}

const themesByCategory = computed(() => {
  const groups = new Map()
  themes.value.forEach((theme) => {
    const categoryKey = normalizeCategoryKey(theme?.themeCategory)
    if (!groups.has(categoryKey)) {
      groups.set(categoryKey, [])
    }
    groups.get(categoryKey).push(theme)
  })
  const result = Array.from(groups, ([key, items]) => ({
    key,
    label: CATEGORY_LABELS[key] ?? key,
    items: moveCultureHeritageToEnd(items)
  }))
  const groupContainsCultureHeritage = (group) =>
    group.items.some((item) => normalizeThemeName(item?.themeName) === '문화유적')
  result.sort((a, b) => {
    const aHasCulture = groupContainsCultureHeritage(a)
    const bHasCulture = groupContainsCultureHeritage(b)
    if (aHasCulture === bHasCulture) {
      return 0
    }
    return aHasCulture ? 1 : -1
  })
  return result
})

// Calculate percentages for slider track background
const minPercent = computed(() => ((minPrice.value - MIN_LIMIT) / (MAX_LIMIT - MIN_LIMIT)) * 100)
const maxPercent = computed(() => ((maxPrice.value - MIN_LIMIT) / (MAX_LIMIT - MIN_LIMIT)) * 100)

const applyFilter = () => {
  emit('apply', {
    min: minPrice.value,
    max: maxPrice.value >= MAX_LIMIT ? null : maxPrice.value,
    themeIds: [...selectedThemeIds.value],
    guestCount: guestCount.value
  })
}

const resetFilter = () => {
  minPrice.value = MIN_LIMIT
  maxPrice.value = MAX_LIMIT
  selectedThemeIds.value = []
  guestCount.value = 1
  activeThumb.value = 'max'
}
</script>

<template>
  <div v-if="isOpen" class="modal-wrapper">
    <!-- No overlay background -->
    <div class="modal-content">
      <div class="modal-header">
        <h3>필터</h3>
        <button class="close-btn" @click="$emit('close')">닫기</button>
      </div>
      
      <div class="modal-body">
        <div class="section-header">
          <span class="section-title">가격</span>
        </div>
        <div class="price-display">
          <span>{{ minPrice.toLocaleString() }}원</span>
          <span>~</span>
          <span>{{ maxPriceLabel }}</span>
        </div>

        <div class="slider-container">
          <div class="slider-track" :style="{ background: `linear-gradient(to right, #ddd ${minPercent}%, #222 ${minPercent}%, #222 ${maxPercent}%, #ddd ${maxPercent}%)` }"></div>
          <input 
            type="range" 
            :min="MIN_LIMIT" 
            :max="MAX_LIMIT" 
            :step="STEP" 
            v-model.number="minPrice" 
            @input="onMinInput"
            @pointerdown="activeThumb = 'min'"
            @focus="activeThumb = 'min'"
            class="range-input"
            :style="{ zIndex: minThumbOnTop ? 2 : 1 }"
          />
          <input 
            type="range" 
            :min="MIN_LIMIT" 
            :max="MAX_LIMIT" 
            :step="STEP" 
            v-model.number="maxPrice" 
            @input="onMaxInput"
            @pointerdown="activeThumb = 'max'"
            @focus="activeThumb = 'max'"
            class="range-input"
            :style="{ zIndex: minThumbOnTop ? 1 : 2 }"
          />
        </div>

        <div class="section-header space-top">
          <span class="section-title">게스트 인원</span>
        </div>
        <div class="guest-row">
          <div class="guest-controls">
            <button class="guest-btn" type="button" @click="decreaseGuest" :disabled="guestCount <= 1">-</button>
            <span class="guest-count">{{ guestCount }}</span>
            <button class="guest-btn" type="button" @click="increaseGuest">+</button>
          </div>
        </div>

        <div class="section-header space-top">
          <span class="section-title">테마</span>
          <span class="section-hint">복수 선택 가능</span>
        </div>
        <div class="theme-groups">
          <div v-if="themesByCategory.length">
            <div v-for="group in themesByCategory" :key="group.key" class="theme-section">
              <div class="theme-section-title">{{ group.label }}</div>
              <div class="theme-grid">
                <button
                  v-for="theme in group.items"
                  :key="theme.id"
                  type="button"
                  class="theme-chip"
                  :class="{ active: selectedThemeIds.includes(theme.id) }"
                  @click="toggleTheme(theme.id)"
                >
                  <img
                    v-if="theme.themeImageUrl"
                    :src="theme.themeImageUrl"
                    :alt="theme.themeName"
                    class="theme-chip__icon"
                  />
                  <span v-else class="theme-chip__icon theme-chip__icon--empty"></span>
                  <span class="theme-chip__label">{{ theme.themeName }}</span>
                </button>
              </div>
            </div>
          </div>
          <div v-else-if="isLoadingThemes" class="theme-empty">테마를 불러오는 중...</div>
          <div v-else class="theme-empty">테마 정보를 불러올 수 없습니다.</div>
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-reset" type="button" @click="resetFilter">초기화</button>
        <button class="btn-apply" @click="applyFilter">적용</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-wrapper {
  position: fixed;
  --modal-offset-top: 104px;
  top: var(--modal-offset-top);
  left: 1rem;
  z-index: 150;
  /* Ensure it doesn't block interaction with the rest of the page outside the modal */
  pointer-events: none; 
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 320px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid #eee;
  pointer-events: auto; /* Re-enable pointer events for the modal itself */
  animation: fadeIn 0.2s ease-out;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - var(--modal-offset-top) - 16px - env(safe-area-inset-bottom));
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.modal-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  padding: 4px;
  color: #999;
}

.modal-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.price-display {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.slider-container {
  position: relative;
  height: 30px;
  margin-bottom: 16px;
}

.slider-track {
  position: absolute;
  top: 50%;
  left: 6px;
  right: 6px;
  transform: translateY(-50%);
  height: 4px;
  border-radius: 2px;
  background: #ddd;
}

.range-input {
  position: absolute;
  top: 50%;
  left: 6px;
  right: 6px;
  transform: translateY(-50%);
  width: auto;
  height: 0;
  -webkit-appearance: none;
  pointer-events: none; /* Allow clicking through to track/other slider */
  background: transparent;
}

/* Helper for thumb styling */
.range-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  border: 2px solid #222;
  cursor: pointer;
  pointer-events: auto; /* Catch clicks on thumb */
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  margin-top: -8px; /* Offset for track */
}

/* For Firefox */
.range-input::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  border: 2px solid #222;
  cursor: pointer;
  pointer-events: auto;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.modal-footer {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.btn-reset {
  background: #f5f5f5;
  color: #222;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 8px 16px;
  font-weight: 600;
  cursor: pointer;
  width: 100%;
}

.btn-reset:hover {
  background: #eee;
}

.btn-apply {
  background: #222;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-weight: 600;
  cursor: pointer;
  width: 100%;
}

.btn-apply:hover {
  background: #000;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 8px;
}

.section-header:first-of-type {
  margin-top: 0;
}

.section-header.space-top {
  margin-bottom: 0;
}

.section-title {
  font-weight: 700;
  color: #222;
}

.section-hint {
  font-size: 0.85rem;
  color: #777;
}

.space-top {
  margin-top: 12px;
}

.guest-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0 4px;
}

.guest-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.guest-type {
  font-weight: 600;
  color: #222;
}

.guest-desc {
  font-size: 0.85rem;
  color: #777;
}

.guest-controls {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.guest-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #ddd;
  background: #fff;
  font-weight: 700;
  cursor: pointer;
}

.guest-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.guest-count {
  min-width: 24px;
  text-align: center;
  font-weight: 700;
  color: #222;
}

.theme-groups {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 12px;
}

.theme-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.theme-section-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #4b5563;
  margin: 14px 0 2px;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  min-height: 36px;
}

.theme-chip {
  display: flex;
  width: 100%;
  justify-content: center;
  align-items: center;
  gap: 6px;
  border: 1px solid #ddd;
  border-radius: 18px;
  background: #fff;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.15s ease;
}

.theme-chip:hover {
  border-color: #222;
}

.theme-chip.active {
  background: #222;
  color: #fff;
  border-color: #222;
}

.theme-chip__icon {
  width: 18px;
  height: 18px;
  object-fit: contain;
  border-radius: 4px;
  flex-shrink: 0;
}

.theme-chip__icon--empty {
  background: #f3f4f6;
}

.theme-chip__label {
  line-height: 1;
}

.theme-empty {
  color: #999;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .modal-wrapper {
    --modal-offset-top: calc(128px + env(safe-area-inset-top));
    top: var(--modal-offset-top);
    left: 12px;
  }
}
</style>
