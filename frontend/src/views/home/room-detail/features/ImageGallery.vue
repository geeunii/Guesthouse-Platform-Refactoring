<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  images: {
    type: Array,
    default: () => []
  },
  name: {
    type: String,
    default: ''
  }
})

const DEFAULT_IMAGE = 'https://placehold.co/800x600'

const imagesList = computed(() => {
  const list = Array.isArray(props.images) ? props.images.filter(Boolean) : []
  return list.length ? list : [DEFAULT_IMAGE]
})

// 실제 이미지 개수
const imageCount = computed(() => imagesList.value.length)

// 이미지 개수에 따른 그리드 클래스
const gridClass = computed(() => {
  const count = imageCount.value
  if (count === 1) return 'image-grid images-1'
  if (count === 2) return 'image-grid images-2'
  if (count === 3) return 'image-grid images-3'
  if (count === 4) return 'image-grid images-4'
  return 'image-grid images-5plus'
})

// 표시할 서브 이미지 (최대 4장)
const subImages = computed(() => imagesList.value.slice(1, 5))

// 오버레이를 표시할 마지막 서브 이미지 인덱스 (0-based, subImages 기준)
const lastVisibleSubIndex = computed(() => {
  const count = imageCount.value
  if (count <= 1) return -1 // 서브 이미지 없음, 메인에 오버레이
  if (count === 2) return 0 // 서브 1장
  if (count === 3) return 1 // 서브 2장
  if (count === 4) return 2 // 서브 3장
  return 2 // 5장 이상: 서브 3번째 (4번째 표시 안함)
})

const isImageModalOpen = ref(false)
const selectedImageIndex = ref(0)

const currentModalImage = computed(() => {
  return imagesList.value[selectedImageIndex.value] || imagesList.value[0]
})

const currentScale = ref(1)

const handleWheel = (event) => {
  const delta = event.deltaY > 0 ? -0.2 : 0.2
  currentScale.value = Math.max(1, Math.min(currentScale.value + delta, 5))
}

const openImageModal = (index) => {
  const maxIndex = Math.max(0, imagesList.value.length - 1)
  selectedImageIndex.value = Math.min(Math.max(index, 0), maxIndex)
  isImageModalOpen.value = true
}

const closeImageModal = () => {
  isImageModalOpen.value = false
  currentScale.value = 1
}

const showPrevImage = () => {
  const total = imagesList.value.length
  if (!total) return
  selectedImageIndex.value = (selectedImageIndex.value - 1 + total) % total
  currentScale.value = 1
}

const showNextImage = () => {
  const total = imagesList.value.length
  if (!total) return
  selectedImageIndex.value = (selectedImageIndex.value + 1) % total
  currentScale.value = 1
}

watch(
  () => props.images,
  () => {
    selectedImageIndex.value = 0
    isImageModalOpen.value = false
    currentScale.value = 1
  }
)
</script>

<template>
  <section :class="gridClass">
    <div
      class="main-img"
      :style="{ backgroundImage: `url(${imagesList[0]})` }"
      role="button"
      tabindex="0"
      @click="openImageModal(0)"
      @keydown.enter.prevent="openImageModal(0)"
    >
      <!-- 1장일 때 우측 하단 버튼 -->
      <button
        v-if="imageCount === 1"
        type="button"
        class="view-more-btn"
        @click.stop="openImageModal(0)"
      >
        + 사진 크게보기
      </button>
    </div>
    <div v-if="subImages.length > 0" class="sub-imgs">
      <div
        v-for="(img, index) in subImages"
        :key="`sub-${index}`"
        :class="['sub-img', { 'has-overlay': index === lastVisibleSubIndex }]"
        :style="{ backgroundImage: `url(${img})` }"
        role="button"
        tabindex="0"
        @click="openImageModal(index + 1)"
        @keydown.enter.prevent="openImageModal(index + 1)"
      ></div>
    </div>
  </section>

  <div v-if="isImageModalOpen" class="image-modal" @click="closeImageModal">
    <div class="image-modal-content" @click.stop>
      <div class="image-modal-header">
        <button type="button" class="image-modal-close" @click="closeImageModal">닫기</button>
      </div>
      <div class="image-modal-body">
        <button
          v-if="imagesList.length > 1"
          type="button"
          class="image-modal-nav prev"
          @click="showPrevImage"
          aria-label="이전 사진"
        >
          ‹
        </button>
        <img 
          :src="currentModalImage" 
          :alt="name" 
          class="image-modal-image"
          @wheel.prevent="handleWheel"
          :style="{ transform: `scale(${currentScale})`, transition: 'transform 0.1s ease-out' }"
        />
        <button
          v-if="imagesList.length > 1"
          type="button"
          class="image-modal-nav next"
          @click="showNextImage"
          aria-label="다음 사진"
        >
          ›
        </button>
      </div>
      <div class="image-modal-caption">{{ selectedImageIndex + 1 }} / {{ imagesList.length }}</div>
    </div>
  </div>
</template>

<style scoped>
.image-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  height: 500px;
  gap: 8px;
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-bottom: 1rem;
}

/* === 이미지 1장: 전체 너비 === */
.image-grid.images-1 {
  grid-template-columns: 1fr;
}
.images-1 .main-img {
  border-radius: var(--radius-md);
}

/* === 이미지 2장: 1:1 분할 === */
.images-2 .main-img {
  border-radius: var(--radius-md) 0 0 var(--radius-md);
}
.images-2 .sub-imgs {
  display: flex;
}
.images-2 .sub-img {
  flex: 1;
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
}

/* === 이미지 3장: 메인 + 서브 2장 (상하 분할) === */
.images-3 .sub-imgs {
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr 1fr;
}
.images-3 .sub-img:nth-child(1) {
  grid-column: 1;
  grid-row: 1;
  border-radius: 0 var(--radius-md) 0 0;
}
.images-3 .sub-img:nth-child(2) {
  grid-column: 1;
  grid-row: 2;
  border-radius: 0 0 var(--radius-md) 0;
}

/* === 이미지 4장: 메인 + 서브 3장 (1+2 그리드) === */
.images-4 .sub-imgs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
}
.images-4 .sub-img:nth-child(1) {
  grid-column: 1 / 3;
  grid-row: 1;
  border-radius: 0 var(--radius-md) 0 0;
}
.images-4 .sub-img:nth-child(2) {
  grid-column: 1 / 2;
  grid-row: 2;
  border-radius: 0;
}
.images-4 .sub-img:nth-child(3) {
  grid-column: 2 / 3;
  grid-row: 2;
  border-radius: 0 0 var(--radius-md) 0;
}

/* === 이미지 5장 이상 (기존 레이아웃) === */
.images-5plus .sub-imgs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 8px;
}
.images-5plus .sub-img:nth-child(1) {
  grid-column: 1 / 3;
  grid-row: 1;
  border-radius: 0 var(--radius-md) 0 0;
}
.images-5plus .sub-img:nth-child(2) {
  grid-column: 1 / 2;
  grid-row: 2;
  border-radius: 0;
}
.images-5plus .sub-img:nth-child(3) {
  grid-column: 2 / 3;
  grid-row: 2;
  border-radius: 0 0 var(--radius-md) 0;
}
.images-5plus .sub-img:nth-child(4) {
  display: none;
}

.main-img {
  background-size: cover;
  background-position: center;
  border-radius: var(--radius-md) 0 0 var(--radius-md);
  cursor: pointer;
  position: relative;
}
.sub-imgs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 8px;
}
.sub-img {
  background-size: cover;
  background-position: center;
  cursor: pointer;
  position: relative;
}

/* === 오버레이 스타일 (has-overlay 클래스 기반) === */
.has-overlay::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  border-radius: inherit;
}
.has-overlay::after {
  content: '+ 사진 크게보기';
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 1.1rem;
  font-weight: 700;
}

/* === 1장일 때 우측 하단 버튼 스타일 === */
.view-more-btn {
  position: absolute;
  right: 0;
  bottom: 0;
  padding: 10px 18px;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  border: none;
  border-radius: var(--radius-md) 0 var(--radius-md) 0;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
  backdrop-filter: blur(4px);
}
.view-more-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.image-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85); /* 배경 더 어둡게 */
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: 0;
}
.image-modal-content {
  background: transparent; /* 배경 투명 */
  border-radius: 0;
  padding: 0;
  width: auto;
  max-width: 90vw;
  max-height: 90vh;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0;
  align-items: center;
}
.image-modal-body {
  display: flex;
  align-items: center;
  gap: 0;
  position: relative;
  justify-content: center;
  width: 100%;
}
.image-modal-header {
  display: flex;
  justify-content: flex-end;
  padding: 0 0 10px 0;
  background: transparent; /* 헤더 배경 제거 */
  width: 100%;
}
.image-modal-image {
  width: auto; /* width 자동 */
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 8px; /* 둥근 모서리 */
  background: #fff;
  display: block;
}
.image-modal-close {
  background: rgba(255, 255, 255, 0.9);
  border: none;
  color: #333;
  font-weight: 600;
  padding: 0.5rem 1rem;
  border-radius: 8px; /* 버튼 스타일 통일 */
  cursor: pointer;
}
.image-modal-nav {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 1.8rem;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  transition: background 0.2s;
}
.image-modal-nav:hover {
  background: rgba(255, 255, 255, 0.3);
}
.image-modal-nav.prev {
  left: -50px; /* 이미지 밖으로 */
}
.image-modal-nav.next {
  right: -50px; /* 이미지 밖으로 */
}
.image-modal-caption {
  text-align: center;
  color: #fff; /* 캡션 흰색 */
  font-size: 0.9rem;
  padding: 1rem 0 0 0;
}

@media (max-width: 768px) {
  .image-grid {
    grid-template-columns: 1fr 1fr;
    height: 230px;
  }

  /* === 모바일: 이미지 1장 === */
  .image-grid.images-1 {
    grid-template-columns: 1fr;
  }
  .images-1 .main-img {
    height: 100%;
    border-radius: var(--radius-md);
  }

  /* === 모바일: 이미지 2장 === */
  .images-2 .main-img {
    height: 100%;
    border-radius: var(--radius-md) 0 0 var(--radius-md);
  }
  .images-2 .sub-imgs {
    height: 100%;
  }
  .images-2 .sub-img {
    border-radius: 0 var(--radius-md) var(--radius-md) 0;
  }

  /* === 모바일: 이미지 3장 === */
  .images-3 .main-img {
    height: 100%;
    border-radius: var(--radius-md) 0 0 var(--radius-md);
  }
  .images-3 .sub-imgs {
    height: 100%;
  }
  .images-3 .sub-img:nth-child(1) {
    border-radius: 0 var(--radius-md) 0 0;
  }
  .images-3 .sub-img:nth-child(2) {
    border-radius: 0 0 var(--radius-md) 0;
  }

  /* === 모바일: 이미지 4장 === */
  .images-4 .main-img {
    height: 100%;
    border-radius: var(--radius-md) 0 0 var(--radius-md);
  }
  .images-4 .sub-imgs {
    height: 100%;
  }
  .images-4 .sub-img:nth-child(1) {
    border-radius: 0 var(--radius-md) 0 0;
  }
  .images-4 .sub-img:nth-child(2) {
    border-radius: 0;
  }
  .images-4 .sub-img:nth-child(3) {
    border-radius: 0 0 var(--radius-md) 0;
  }

  /* === 모바일: 이미지 5장 이상 === */
  .images-5plus .main-img {
    height: 100%;
    border-radius: var(--radius-md) 0 0 var(--radius-md);
  }
  .images-5plus .sub-imgs {
    height: 100%;
  }
  .images-5plus .sub-img:nth-child(1) {
    border-radius: 0 var(--radius-md) 0 0;
  }
  .images-5plus .sub-img:nth-child(2) {
    border-radius: 0;
  }
  .images-5plus .sub-img:nth-child(3) {
    border-radius: 0 0 var(--radius-md) 0;
  }
  .images-5plus .sub-img:nth-child(4) {
    display: none;
  }

  /* === 모바일 오버레이 스타일 === */
  .has-overlay::after {
    content: '+ 사진\A크게보기';
    font-size: 0.9rem;
    white-space: pre;
    text-align: center;
  }

  /* === 모바일 우측 하단 버튼 스타일 === */
  .view-more-btn {
    right: 0;
    bottom: 0;
    padding: 8px 14px;
    font-size: 0.8rem;
  }

  .image-modal-content {
    width: 100%;
  }
  .image-modal-image {
    max-height: 60vh;
  }
  .image-modal-nav {
    width: 32px;
    height: 32px;
    font-size: 1.2rem;
  }
  .image-modal-nav.prev {
    left: 4px;
  }
  .image-modal-nav.next {
    right: 4px;
  }
}
</style>
