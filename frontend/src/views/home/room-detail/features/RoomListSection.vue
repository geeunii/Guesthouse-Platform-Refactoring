<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  rooms: {
    type: Array,
    default: () => []
  },
  selectedRoom: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:selectedRoom'])

const showAllRooms = ref(false)
const isUnavailableModalOpen = ref(false)
const isRoomImageModalOpen = ref(false)
const roomModalImage = ref('')
const currentScale = ref(1)

const displayedRooms = computed(() => {
  if (showAllRooms.value) return props.rooms
  return props.rooms.slice(0, 3)
})

const hasMoreRooms = computed(() => props.rooms.length > 3)

const formatPrice = (price) => {
  return Number(price).toLocaleString()
}

const handleRoomClick = (room) => {
  if (!room.available) {
    isUnavailableModalOpen.value = true
  } else {
    emit('update:selectedRoom', room)
  }
}

// Image Modal Logic
const handleWheel = (event) => {
  const delta = event.deltaY > 0 ? -0.2 : 0.2
  currentScale.value = Math.max(1, Math.min(currentScale.value + delta, 5))
}

const openRoomImageModal = (url) => {
  if (!url) return
  roomModalImage.value = url
  isRoomImageModalOpen.value = true
}

const closeRoomImageModal = () => {
  isRoomImageModalOpen.value = false
  roomModalImage.value = ''
  currentScale.value = 1
}
const isDescModalOpen = ref(false)
const currentRoomDesc = ref('')

const openDescModal = (desc) => {
  currentRoomDesc.value = desc
  isDescModalOpen.value = true
}

const closeDescModal = () => {
  isDescModalOpen.value = false
  currentRoomDesc.value = ''
}

const shouldShowMoreRoom = (room) => {
  const intro = room?.introduction || ''
  const desc = room?.description || ''
  if (!intro && !desc) return false
  const combined = [intro, desc].filter(Boolean).join('\n\n')
  return combined.length > 80 || combined.includes('\n')
}

const shouldShowMore = (desc) => {
  if (!desc) return false
  // ??듭쟻??湲몄씠 泥댄겕 ?먮뒗 以꾨컮轅?臾몄옄 泥댄겕
  return desc.length > 60 || desc.includes('\n')
}

const getFullDescription = (room) => {
  const parts = []
  if (room.introduction) parts.push(room.introduction)
  if (room.description) parts.push(room.description)
  return parts.join('\n\n')
}
</script>

<template>
  <div class="room-list-container">
    <div v-if="!rooms.length" class="room-empty">
      조건에 맞는 객실이 없습니다.
    </div>

    <div v-else class="room-list">
      <div
        v-for="room in displayedRooms"
        :key="room.id"
        class="room-card"
        :class="{ selected: selectedRoom?.id === room.id, unavailable: !room.available }"
        @click="handleRoomClick(room)"
      >
        <div class="room-media" @click.stop="openRoomImageModal(room.thumbnailUrl)">
          <img :src="room.thumbnailUrl" :alt="room.name" loading="lazy" />
          <span v-if="!room.available" class="room-unavailable-badge">사용 중지</span>
        </div>

        <div class="room-content">
          <div class="room-info">
            <h3>{{ room.name }}</h3>
            <div class="room-text-info">
              <p v-if="room.description" class="room-intro-text">{{ room.description }}</p>
              <div v-if="room.introduction" class="room-desc-wrapper">
                <p class="room-desc">{{ room.introduction }}</p>
                <button 
                  v-if="shouldShowMoreRoom(room)" 
                  class="more-desc-btn" 
                  @click.stop="openDescModal(getFullDescription(room))"
                >
                  더보기
                </button>
              </div>
            </div>
            <span class="capacity">최대 {{ room.capacity }}명</span>
          </div>
          <div class="room-action">
            <div class="price">₩{{ formatPrice(room.price) }}</div>
            <button
              class="select-btn"
              :class="{ active: selectedRoom?.id === room.id, unavailable: !room.available }"
              @click.stop="handleRoomClick(room)"
            >
              {{ !room.available ? '마감' : (selectedRoom?.id === room.id ? '선택됨' : '객실') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="hasMoreRooms" class="room-toggle-row">
      <button type="button" class="room-toggle-btn" @click="showAllRooms = !showAllRooms">
        {{ showAllRooms ? '접기' : '더보기' }}
      </button>
    </div>

        <!-- Unavailable Modal -->
    <div v-if="isUnavailableModalOpen" class="modal-overlay" @click.self="isUnavailableModalOpen = false">
      <div class="modal-content unavailable-modal">
        <div class="modal-icon">🚫</div>
        <h3>예약 불가능</h3>
        <p class="modal-desc">선택하신 날짜에는 이미 예약이 완료된 객실입니다.<br>다른 날짜나 객실을 선택해주세요.</p>
        <button class="close-modal-btn" @click="isUnavailableModalOpen = false">확인</button>
      </div>
    </div>

        <!-- Room Image Modal -->
    <div v-if="isRoomImageModalOpen" class="image-modal" @click="closeRoomImageModal">
      <div class="image-modal-content" @click.stop>
        <button class="image-modal-close" @click="closeRoomImageModal">닫기</button>
        <img 
          :src="roomModalImage" 
          alt="객실 이미지" 
          class="image-modal-image" 
          @wheel.prevent="handleWheel"
          :style="{ transform: `scale(${currentScale})`, transition: 'transform 0.1s ease-out' }"
        />
      </div>
    </div>

    <!-- Description Modal -->
    <div v-if="isDescModalOpen" class="modal-overlay" @click.self="closeDescModal">
      <div class="modal-content desc-modal">
        <h3>객실 소개</h3>
        <p class="full-desc">{{ currentRoomDesc }}</p>
        <button class="close-modal-btn" @click="closeDescModal">닫기</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.room-list-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.room-empty {
  padding: 1.5rem;
  border: 1px dashed #d1d5db;
  border-radius: 8px; /* var(--radius-md) fallback */
  background: #f9fafb;
  color: #6b7280;
  text-align: center;
  font-weight: 600;
}

.room-card {
  border: 2px solid #ddd;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 0; /* list container gap handles spacing */
  display: flex;
  gap: 1.5rem;
  align-items: stretch;
  cursor: pointer;
  transition: border-color 0.2s;
  background: #fff;
}

.room-card:hover { border-color: #BFE7DF; }
.room-card.selected { border-color: #0f4c44; background-color: #f9fdfc; }

.room-media {
  flex: 0 0 320px; /* PC Fixed 320px */
  height: auto;
  aspect-ratio: 4 / 3;
  border-radius: 10px;
  overflow: hidden;
  background: #e5e7eb;
  position: relative;
}

.room-media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: smooth;
  transform: translateZ(0);
}

.room-unavailable-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: #ef4444;
  color: white;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.room-content {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  flex: 1;
  min-width: 0;
}

.room-info h3 { margin: 0 0 0.5rem 0; font-size: 1.2rem; font-family: 'NanumSquareRound', sans-serif; }
.room-text-info {
  margin: 0 0 0.5rem 0;
}
.room-intro-text {
  font-size: 0.95rem;
  font-weight: 600;
  font-family: 'NanumSquareRound', sans-serif;
  color: #374151; /* gray-700 */
  margin: 0 0 0.4rem 0;
  line-height: 1.4;
  white-space: pre-wrap;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.room-desc-wrapper {
  margin: 0;
}
.room-desc { 
  color: #6b7280; 
  font-size: 0.9rem; 
  margin: 0; 
  line-height: 1.5;
  word-break: break-all; /* 한글/영문 혼용 시 줄바꿈 처리 */
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 2줄까지만 표시 */
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: pre-wrap; /* 줄바꿈은 인식되되 line-clamp로 자름 */
}
.more-desc-btn {
  background: none;
  border: none;
  font-size: 0.8rem;
  color: #999;
  text-decoration: underline;
  cursor: pointer;
  padding: 0;
  margin-top: 2px;
}
.more-desc-btn:hover {
  color: #666;
}

.capacity { font-size: 0.8rem; background: #eee; padding: 2px 6px; border-radius: 4px; }

.room-action { text-align: right; display: flex; flex-direction: column; justify-content: space-between; align-items: flex-end; }
.price { font-weight: bold; font-size: 1.1rem; margin-bottom: 0.5rem; font-family: 'NanumSquareRound', sans-serif; }

.select-btn {
  padding: 0.5rem 1rem;
  background: #eee;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  min-width: 72px;
  text-align: center;
  font-weight: 600;
  font-family: 'NanumSquareRound', sans-serif;
}
.select-btn.active {
  background: #0f4c44;
  color: #fff;
}
.select-btn.unavailable {
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.room-card.unavailable {
  opacity: 0.7;
  cursor: not-allowed;
}
.room-card.unavailable:hover { border-color: #ddd; }

.room-toggle-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
}
.room-toggle-btn {
  background: #BFE7DF;
  border: 1px solid #8FCFC1;
  color: #0f4c44;
  font-weight: 700;
  cursor: pointer;
  padding: 0.3rem 0.7rem;
  border-radius: 999px;
  white-space: nowrap;
}

/* Modals */
.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-content.unavailable-modal {
  background: white;
  padding: 2rem;
  border-radius: 16px;
  text-align: center;
  max-width: 320px;
  width: 90%;
}
.modal-icon { font-size: 3rem; margin-bottom: 1rem; }
.modal-desc { margin-bottom: 1.5rem; line-height: 1.5; color: #555; }
.close-modal-btn {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.close-modal-btn:hover { background: #f5f5f5; }

/* Description Modal specific */
.modal-content.desc-modal {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  text-align: left;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}
.desc-modal h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  font-size: 1.1rem;
  font-weight: 700;
  text-align: center;
}
.full-desc {
  white-space: pre-wrap; /* 원본 줄바꿈 적용 */
  word-break: break-all;
  line-height: 1.6;
  color: #444;
  font-size: 0.95rem;
  overflow-y: auto;
  flex: 1;
  margin-bottom: 1rem;
}

/* Image Modal (Unified Style) */
.image-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 0;
}
.image-modal-content {
  background: transparent;
  width: auto;
  max-width: 90vw;
  max-height: 90vh;
  padding: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center; /* 닫기 버튼 상단 우측 정렬 등을 위해 */
}
.image-modal-image {
  width: auto;
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 8px;
  background: #fff;
  display: block;
}
.image-modal-close {
  align-self: flex-end;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  color: #333;
  font-weight: 600;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  cursor: pointer;
}

/* Tablet breakpoint */
@media (min-width: 769px) and (max-width: 1024px) {
  .room-media {
    flex: 0 0 200px;
    aspect-ratio: 4 / 3;
  }
  .room-card {
    gap: 1rem;
    padding: 1rem;
  }
}

@media (max-width: 768px) {
  .room-card {
    flex-direction: column;
    padding: 1rem;
    gap: 0.75rem;
  }
  .room-media {
    flex: none;
    width: 100%;
    max-width: 400px;
    height: auto;
    aspect-ratio: 4 / 3;
    margin: 0 auto;
  }
  .room-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  .room-action {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
  .room-info h3 { margin: 0; }
  .room-info p { margin: 0; line-height: 1.25; }
}
</style>
