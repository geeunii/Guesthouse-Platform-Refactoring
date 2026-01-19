<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createReview, updateReview, getReviewTags, getMyReviews } from '@/api/reviewApi'
import { groupTagsByCategory } from '@/constants/reviewTagCategories'

const router = useRouter()
const route = useRoute()

// Reservation data from router state
const reservation = ref({
  accommodationId: null,
  accommodationName: '산속 독채 숙소',
  dates: '2025-11-20 ~ 2025-11-22'
})

// Edit Mode State
const isEditMode = ref(false)
const targetReviewId = ref(null)

onMounted(async () => {
  if (history.state) {
    if (history.state.reservationData) {
      reservation.value = history.state.reservationData
    }
    if (history.state.mode === 'edit') {
      isEditMode.value = true
      await fetchAndFillReviewData()
    }
  }

  // 태그 목록 로드
  try {
    const tags = await getReviewTags()
    // 카테고리별로 그룹화
    groupedTags.value = groupTagsByCategory(tags)

    // 수정 모드인 경우, 태그 로드 후 선택된 태그 복원
    if (isEditMode.value && tempSelectedTagIds.value.length > 0) {
      selectedTagIds.value = [...tempSelectedTagIds.value]
    }
  } catch (error) {
    console.error('태그 로드 실패:', error)
  }
})

// 임시 저장용 (태그 로드 전)
const tempSelectedTagIds = ref([])

const fetchAndFillReviewData = async () => {
  try {
    const myReviews = await getMyReviews()
    // 현재 숙소에 대한 리뷰 찾기
    const review = myReviews.find(r => r.accommodationsId === reservation.value.accommodationId)
    
    if (review) {
      targetReviewId.value = review.reviewId
      rating.value = review.rating
      reviewContent.value = review.content
      
      // 이미지 복원
      if (review.images && review.images.length > 0) {
        images.value = review.images.map(img => ({
            preview: img.imageUrl,
            base64: img.imageUrl, // 기존 URL 그대로 사용
            isExisting: true
        }))
      }
      
      // 태그 복원 (ID 저장해뒀다가 태그 리스트 로드 후 매핑)
      if (review.tags) {
        tempSelectedTagIds.value = review.tags.map(t => t.reviewTagId)
      }
      
      agreed.value = true // 수정 시에는 동의한 것으로 간주
    } else {
      openModal('리뷰 정보를 찾을 수 없습니다.', 'error', () => router.back())
    }
  } catch (error) {
    console.error('리뷰 정보 불러오기 실패:', error)
    openModal('리뷰 정보를 불러오는데 실패했습니다.', 'error')
  }
}

// Form data
const agreed = ref(false)
const rating = ref(0)
const reviewContent = ref('')
const selectedTagIds = ref([])  // 선택된 태그 ID 배열
const isSubmitting = ref(false)

// 카테고리별로 그룹화된 태그
const groupedTags = ref([])

// Image upload
const MAX_IMAGES = 5
const images = ref([])  // { file: File, preview: string, base64: string }
const fileInput = ref(null)

// 파일 선택 트리거
const triggerFileInput = () => {
  fileInput.value?.click()
}

// 파일을 Base64로 변환
const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = () => resolve(reader.result)
    reader.onerror = (error) => reject(error)
  })
}

// 파일 선택 처리
const handleFileSelect = async (event) => {
  const files = event.target.files
  if (!files || files.length === 0) return

  const remainingSlots = MAX_IMAGES - images.value.length
  if (remainingSlots <= 0) {
    openModal(`최대 ${MAX_IMAGES}장까지 업로드 가능합니다.`, 'error')
    return
  }

  const filesToProcess = Array.from(files).slice(0, remainingSlots)

  for (const file of filesToProcess) {
    // 이미지 파일인지 확인
    if (!file.type.startsWith('image/')) {
      openModal('이미지 파일만 업로드 가능합니다.', 'error')
      continue
    }

    // 파일 크기 제한 (5MB)
    if (file.size > 5 * 1024 * 1024) {
      openModal('이미지 크기는 5MB 이하만 가능합니다.', 'error')
      continue
    }

    try {
      const base64 = await fileToBase64(file)
      images.value.push({
        file,
        preview: URL.createObjectURL(file),
        base64
      })
    } catch (error) {
      console.error('이미지 변환 실패:', error)
    }
  }

  // input 초기화 (같은 파일 다시 선택 가능하도록)
  event.target.value = ''
}

// 이미지 삭제
const removeImage = (index) => {
  const image = images.value[index]
  if (image.preview && !image.isExisting) {
    URL.revokeObjectURL(image.preview)
  }
  images.value.splice(index, 1)
}

// Modal State
const showModal = ref(false)
const modalMessage = ref('')
const modalType = ref('info')
const modalCallback = ref(null)

// Coupon Modal State
const showCouponModal = ref(false)

const closeCouponModal = () => {
  showCouponModal.value = false
  router.push('/reviews')
}

const goToCouponPage = () => {
  showCouponModal.value = false
  router.push('/coupons')
}

const openModal = (message, type = 'info', callback = null) => {
  modalMessage.value = message
  modalType.value = type
  modalCallback.value = callback
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  if (modalCallback.value) {
    modalCallback.value()
    modalCallback.value = null
  }
}

// 태그 선택/해제
const toggleTag = (tagId) => {
  const idx = selectedTagIds.value.indexOf(tagId)
  if (idx === -1) {
    selectedTagIds.value.push(tagId)
  } else {
    selectedTagIds.value.splice(idx, 1)
  }
}

const isTagSelected = (tagId) => {
  return selectedTagIds.value.includes(tagId)
}

const clampRating = (value) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return 0
  return Math.min(Math.max(parsed, 0), 5)
}

const formatRating = (value) => clampRating(value).toFixed(1)

const getStarFillWidth = (value) => `${(clampRating(value) / 5) * 100}%`

const setRating = (value) => {
  const stepped = Math.round(Number(value) * 2) / 2
  rating.value = clampRating(stepped)
}

const setRatingFromEvent = (event) => {
  const rect = event.currentTarget.getBoundingClientRect()
  if (!rect.width) return
  const offsetX = event.clientX - rect.left
  const ratio = Math.min(Math.max(offsetX / rect.width, 0), 1)
  const raw = ratio * 5
  const stepped = Math.ceil(raw * 2) / 2
  rating.value = clampRating(Math.max(0.5, stepped))
}

const handleRatingKeydown = (event) => {
  if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
    event.preventDefault()
    setRating((Number(rating.value) || 0) + 0.5)
    if (rating.value === 0) rating.value = 0.5
    return
  }
  if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
    event.preventDefault()
    setRating((Number(rating.value) || 0) - 0.5)
    return
  }
  if (event.key === 'Home') {
    event.preventDefault()
    rating.value = 0
  }
  if (event.key === 'End') {
    event.preventDefault()
    rating.value = 5
  }
}

// 방문일 추출 (dates에서 체크인 날짜, YYYY.MM.DD -> YYYY-MM-DD 변환)
const getVisitDate = () => {
  const dates = reservation.value.dates || ''
  const checkinDate = dates.split(' ~ ')[0]
  // YYYY.MM.DD 형식을 YYYY-MM-DD로 변환
  if (checkinDate) {
    return checkinDate.replace(/\./g, '-')
  }
  return new Date().toISOString().split('T')[0]
}

const handleSubmit = async () => {
  if (!agreed.value) {
    openModal('리뷰 작성에 동의해주세요.', 'error')
    return
  }
  if (rating.value === 0) {
    openModal('별점을 선택해주세요.', 'error')
    return
  }
  if (!reviewContent.value.trim()) {
    openModal('리뷰 내용을 입력해주세요.', 'error')
    return
  }
  if (!reservation.value.accommodationId) {
    openModal('숙소 정보가 없습니다. 다시 시도해주세요.', 'error')
    return
  }

  isSubmitting.value = true

  try {
    const imageUrls = images.value.map(img => img.base64)
    
    if (isEditMode.value) {
        // 수정
        const updateData = {
           content: reviewContent.value.trim(),
           imageUrls: imageUrls,
           rating: rating.value,
           tagIds: selectedTagIds.value
        }
        await updateReview(targetReviewId.value, updateData)
        openModal('리뷰가 수정되었습니다!', 'success', () => router.push('/reviews'))
    } else {
        // 등록
        const reviewData = {
          accommodationsId: reservation.value.accommodationId,
          rating: rating.value,
          content: reviewContent.value.trim(),
          visitDate: getVisitDate(),
          tagIds: selectedTagIds.value,
          imageUrls: imageUrls
        }
        const response = await createReview(reviewData)

        // 쿠폰 발급 여부 확인
        if (response && response.couponIssued) {
          showCouponModal.value = true
        } else {
          openModal('리뷰가 등록되었습니다!', 'success', () => router.push('/reviews'))
        }
    }
  } catch (error) {
    console.error('리뷰 전송 실패:', error)
    openModal(error.message || '리뷰 전송에 실패했습니다.', 'error')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="write-review-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="router.back()">←</button>
      <h1>{{ isEditMode ? '리뷰 수정' : '리뷰 작성' }}</h1>
    </div>



    <!-- Accommodation Info -->
    <div class="info-section">
      <div class="info-row">
        <span class="label">숙소 이름:</span>
        <span class="value">{{ reservation.accommodationName || reservation.title }}</span>
      </div>
      <div class="info-row">
        <span class="label">체크인/아웃:</span>
        <span class="value">{{ reservation.dates || reservation.date }}</span>
      </div>
    </div>

    <!-- Rating -->
    <div class="rating-section">
      <span class="label">별점:</span>
      <div
        class="stars"
        role="slider"
        aria-label="별점"
        :aria-valuemin="0"
        :aria-valuemax="5"
        :aria-valuenow="rating || 0"
        :aria-valuetext="`${formatRating(rating)}점`"
        tabindex="0"
        @click="setRatingFromEvent"
        @keydown="handleRatingKeydown"
      >
        <span class="stars-base" aria-hidden="true">★★★★★</span>
        <span
          class="stars-fill"
          :style="{ width: getStarFillWidth(rating) }"
          aria-hidden="true"
        >
          ★★★★★
        </span>
      </div>
    </div>

    <!-- Review Content -->
    <div class="content-section">
      <label class="label">리뷰 내용:</label>
      <textarea 
        v-model="reviewContent"
        placeholder="숙소에 대한 리뷰를 작성해주세요."
      ></textarea>
    </div>

    <!-- Photo Upload -->
    <div class="photo-section">
      <span class="label">사진 첨부 (최대 {{ MAX_IMAGES }}장)</span>
      <div class="photo-list">
        <!-- 업로드된 이미지들 -->
        <div
          v-for="(image, index) in images"
          :key="index"
          class="photo-item"
        >
          <img :src="image.preview" alt="리뷰 이미지" />
          <button class="remove-btn" @click="removeImage(index)">×</button>
        </div>

        <!-- 이미지 추가 버튼 -->
        <div
          v-if="images.length < MAX_IMAGES"
          class="photo-placeholder"
          @click="triggerFileInput"
        >
          <span class="plus-icon">+</span>
          <span class="add-text">사진 추가</span>
        </div>
      </div>

      <!-- 숨겨진 파일 input -->
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        multiple
        style="display: none"
        @change="handleFileSelect"
      />
    </div>

    <!-- Tags -->
    <div class="tags-section">
      <span class="section-title">리뷰 태그를 선택해주세요</span>

      <div v-for="category in groupedTags" :key="category.name" class="tag-category">
        <h3 class="category-name">{{ category.name }}</h3>
        <div class="tags-grid">
          <button
            v-for="tag in category.tags"
            :key="tag.reviewTagId"
            class="tag-btn"
            :class="{ selected: isTagSelected(tag.reviewTagId) }"
            @click="toggleTag(tag.reviewTagId)"
          >
            {{ tag.reviewTagName }}
          </button>
        </div>
      </div>
    </div>

    <!-- Notice Box -->
    <div class="notice-box">
      <h3>리뷰 작성 전 확인해 주세요.</h3>
      <ul>
        <li>개인정보(실명, 얼굴사진 등)와 허위·비방 내용은 등록할 수 없습니다.</li>
        <li>리뷰와 사진은 서비스 노출 및 운영 목적에 활용될 수 있습니다.</li>
        <li>부정 리뷰(보상 목적, 방문 이력 없음 등)는 제한되며 삭제될 수 있습니다.</li>
      </ul>
      <label class="agree-label">
        <input type="checkbox" v-model="agreed" />
        <span>위 내용을 확인하고 리뷰 작성에 동의합니다.</span>
      </label>
    </div>

    <!-- Submit Button -->
    <button
      class="submit-btn"
      @click="handleSubmit"
      :disabled="isSubmitting"
    >
      {{ isSubmitting ? (isEditMode ? '수정 중...' : '등록 중...') : (isEditMode ? '리뷰 수정' : '리뷰 제출') }}
    </button>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-icon" :class="modalType">
          <span v-if="modalType === 'success'">✓</span>
          <span v-else-if="modalType === 'error'">!</span>
          <span v-else>i</span>
        </div>
        <p class="modal-message">{{ modalMessage }}</p>
        <button class="modal-btn" @click="closeModal">확인</button>
      </div>
    </div>

    <!-- Coupon Modal -->
    <div v-if="showCouponModal" class="modal-overlay">
      <div class="coupon-modal-content">
        <button class="coupon-modal-close" @click="closeCouponModal">&times;</button>
        <div class="coupon-modal-icon">
          <span>🎉</span>
        </div>
        <h2 class="coupon-modal-title">쿠폰이 발급되었습니다!</h2>
        <p class="coupon-modal-message">리뷰 작성 감사 쿠폰이 발급되었습니다.<br/>쿠폰함을 확인해주세요!</p>
        <button class="coupon-modal-btn" @click="goToCouponPage">쿠폰함으로 가기</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.write-review-page {
  padding-top: 1rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.page-header h1 {
  font-size: 1.2rem;
  font-weight: 700;
}

/* Notice Box */
.notice-box {
  border: 1px solid #ddd;
  border-radius: 12px;
  padding: 1.2rem;
  margin-bottom: 1.5rem;
  background: #fafafa;
}

.notice-box h3 {
  font-size: 0.95rem;
  margin-bottom: 0.8rem;
}

.notice-box ul {
  list-style: disc inside;
  font-size: 0.85rem;
  color: #555;
  line-height: 1.6;
  margin-bottom: 1rem;
}

.agree-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  cursor: pointer;
}

.agree-label input {
  width: 18px;
  height: 18px;
}

/* Info Section */
.info-section {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.info-row {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.label {
  font-weight: 600;
  font-size: 0.9rem;
  color: #333;
}

.value {
  font-size: 0.9rem;
  color: #555;
}

/* Rating */
.rating-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stars {
  position: relative;
  display: inline-block;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  user-select: none;
}

.stars:focus {
  outline: 2px solid #8FCFC1;
  outline-offset: 2px;
}

.stars-base {
  color: #ddd;
}

.stars-fill {
  color: #fbbf24;
  position: absolute;
  left: 0;
  top: 0;
  overflow: hidden;
  white-space: nowrap;
  width: 0;
}

/* Content */
.content-section {
  margin-bottom: 1.5rem;
}

.content-section textarea {
  width: 100%;
  height: 120px;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 1rem;
  font-size: 0.95rem;
  resize: none;
  outline: none;
  margin-top: 0.5rem;
}

.content-section textarea:focus {
  border-color: var(--primary);
}

/* Photo */
.photo-section {
  margin-bottom: 1.5rem;
}

.photo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.photo-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.photo-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-item .remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.photo-item .remove-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.photo-placeholder {
  width: 80px;
  height: 80px;
  border: 2px dashed #ccc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #888;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;
}

.photo-placeholder:hover {
  border-color: var(--primary);
  background-color: #f9fffe;
}

.photo-placeholder .plus-icon {
  font-size: 1.5rem;
  line-height: 1;
}

.photo-placeholder .add-text {
  font-size: 0.7rem;
  margin-top: 2px;
}

/* Tags */
.tags-section {
  margin-bottom: 2rem;
}

.section-title {
  display: block;
  font-weight: 700;
  font-size: 1rem;
  color: #333;
  margin-bottom: 1rem;
}

.tag-category {
  margin-bottom: 1.2rem;
}

.category-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #555;
  margin-bottom: 0.5rem;
  padding-left: 0.25rem;
}

.tags-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-btn:hover {
  border-color: var(--primary);
  background: #f9fffe;
}

.tag-btn.selected {
  background: var(--primary);
  border-color: var(--primary);
  color: #004d40;
  font-weight: 500;
}

/* Submit */
.submit-btn {
  width: 100%;
  padding: 1rem;
  background: var(--primary);
  color: #004d40;
  font-weight: bold;
  font-size: 1rem;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  max-width: 320px;
  width: 90%;
  text-align: center;
}

.modal-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-size: 1.5rem;
  font-weight: bold;
}

.modal-icon.success {
  background: var(--primary);
  color: #004d40;
}

.modal-icon.error {
  background: #fee2e2;
  color: #dc2626;
}

.modal-icon.info {
  background: #e0f2fe;
  color: #0284c7;
}

.modal-message {
  font-size: 1rem;
  color: #333;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.modal-btn {
  width: 100%;
  padding: 0.8rem;
  background: var(--primary);
  color: #004d40;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
}

/* Coupon Modal */
.coupon-modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  max-width: 340px;
  width: 90%;
  text-align: center;
  position: relative;
}

.coupon-modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 1.5rem;
  line-height: 1;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.coupon-modal-close:hover {
  background: #eee;
  color: #333;
}

.coupon-modal-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.coupon-modal-title {
  font-size: 1.2rem;
  font-weight: 700;
  color: #333;
  margin-bottom: 0.8rem;
}

.coupon-modal-message {
  font-size: 0.95rem;
  color: #666;
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.coupon-modal-btn {
  width: 100%;
  padding: 0.9rem;
  background: var(--primary);
  color: #004d40;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

.coupon-modal-btn:hover {
  opacity: 0.9;
}
</style>
