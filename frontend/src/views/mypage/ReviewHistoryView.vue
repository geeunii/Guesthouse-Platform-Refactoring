<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyReviews } from '@/api/reviewApi'

const router = useRouter()

const reviews = ref([])
const isLoading = ref(false)
const errorMessage = ref('')
const activeMenuId = ref(null)

const toggleMenu = (id) => {
  activeMenuId.value = activeMenuId.value === id ? null : id
}

// 클릭 외부 감지해서 닫기 (선택사항, 간단하게 구현)
const closeMenu = () => {
  activeMenuId.value = null
}

// 날짜 포맷 함수
const formatDate = (dateValue) => {
  if (!dateValue) return ''
  const date = new Date(dateValue)
  if (Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}.${month}.${day}`
}

// 백엔드 응답을 프론트엔드 형식으로 변환
const normalizeReview = (review) => {
  const images = Array.isArray(review?.images) ? review.images : []
  const imageUrls = images
    .map((img) => img?.imageUrl ?? img)
    .filter((url) => typeof url === 'string' && url.trim())

  const tags = Array.isArray(review?.tags) ? review.tags : []
  const tagNames = tags
    .map((tag) => tag?.reviewTagName ?? tag?.name ?? tag)
    .filter((tag) => typeof tag === 'string' && tag.trim())

  const ratingValue = Number(review?.rating)

  return {
    id: review?.reviewId ?? review?.id,
    accommodationId: review?.accommodationsId,
    accommodationName: review?.accommodationName ?? '',
    accommodationImage: review?.accommodationImage,
    visitDate: review?.visitDate,
    checkin: review?.checkin,
    checkout: review?.checkout,
    rating: Number.isFinite(ratingValue) ? Math.round(ratingValue) : 0,
    date: formatDate(review?.createdAt),
    stayPeriod: (review?.checkin && review?.checkout)
      ? `${formatDate(review.checkin)} ~ ${formatDate(review.checkout)}`
      : (review?.visitDate || formatDate(review?.createdAt)),
    content: review?.content ?? '',
    images: imageUrls,
    tags: tagNames,
    replyContent: review?.replyContent ?? '',
    replyUpdatedAt: formatDate(review?.replyUpdatedAt)
  }
}

// 내 리뷰 조회
const loadMyReviews = async () => {
  isLoading.value = true
  errorMessage.value = ''

  try {
    const data = await getMyReviews()
    reviews.value = Array.isArray(data) ? data.map(normalizeReview) : []
  } catch (error) {
    console.error('내 리뷰 조회 실패:', error)
    errorMessage.value = error.message || '리뷰를 불러오는데 실패했습니다.'
    reviews.value = []
  } finally {
    isLoading.value = false
  }
}

const goToDetail = (id) => {
  router.push(`/room/${id}`)
}

const renderStars = (rating) => {
  const safeRating = Math.min(Math.max(Number(rating) || 0, 0), 5)
  return '★'.repeat(safeRating) + '☆'.repeat(5 - safeRating)
}

// 리뷰 수정
const handleEditReview = (review) => {
  router.push({
    name: 'write-review',
    state: {
      mode: 'edit',
      reservationData: {
        accommodationId: review.accommodationId,
        accommodationName: review.accommodationName,
        dates: review.stayPeriod,
        image: review.accommodationImage
      }
    }
  })
}

// 리뷰 삭제
import { deleteReview } from '@/api/reviewApi'

const handleDeleteReview = async (reviewId) => {
  if (!confirm('정말로 리뷰를 삭제하시겠습니까?')) return

  try {
    await deleteReview(reviewId)
    // 목록 새로고침
    await loadMyReviews()
  } catch (error) {
    console.error('리뷰 삭제 실패:', error)
    alert(error.message || '리뷰 삭제 중 오류가 발생했습니다.')
  }
}

onMounted(loadMyReviews)
</script>

<template>
  <div class="review-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="router.back()">←</button>
      <h1>리뷰 내역</h1>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="loading-state">
      <p>리뷰를 불러오는 중...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="errorMessage" class="error-state">
      <p>{{ errorMessage }}</p>
      <button class="retry-btn" @click="loadMyReviews">다시 시도</button>
    </div>

    <!-- Empty State -->
    <div v-else-if="reviews.length === 0" class="empty-state">
      <div class="empty-icon">☆</div>
      <h2>아직 작성한 리뷰가 없어요</h2>
      <p>숙소를 이용한 후 리뷰를 남겨보세요.</p>
      <p>다른 여행자들에게 도움이 될 수 있어요.</p>
    </div>

    <!-- Review List -->
    <div v-else class="review-list">
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <!-- Top Row: Accommodation Info & Actions -->
        <div class="review-top">
          <div class="info-left">
            <img :src="review.accommodationImage || 'https://via.placeholder.com/60'" class="review-thumb" />
            <div class="review-info">
              <h3 class="acc-name" @click="goToDetail(review.accommodationId)">
                {{ review.accommodationName }}
              </h3>
              <div class="rating-row">
                <span class="stars">{{ renderStars(review.rating) }}</span>
                <span class="date">{{ review.stayPeriod }}</span>
              </div>
            </div>
          </div>
          
          <!-- Kebab Menu (Actions) -->
          <div class="review-actions-menu">
            <button class="more-btn" @click.stop="toggleMenu(review.id)">⋮</button>
            <div v-if="activeMenuId === review.id" class="menu-dropdown">
              <button class="menu-item" @click="handleEditReview(review)">수정</button>
              <button class="menu-item delete" @click="handleDeleteReview(review.id)">삭제</button>
            </div>
          </div>
        </div>

        <!-- Review Images (Moved Above Content) -->
        <div v-if="review.images && review.images.length > 0" class="review-images">
          <img v-for="(img, idx) in review.images" :key="idx" :src="img" class="review-img" />
        </div>

        <!-- Review Content -->
        <p class="review-content">{{ review.content }}</p>

        <!-- Review Tags -->
        <div v-if="review.tags && review.tags.length > 0" class="review-tags">
          <span v-for="tag in review.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>

        <!-- Host Reply -->
        <div v-if="review.replyContent" class="host-reply">
          <p class="reply-title">호스트 답글</p>
          <p class="reply-content">{{ review.replyContent }}</p>
          <p v-if="review.replyUpdatedAt" class="reply-date">{{ review.replyUpdatedAt }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.review-page {
  padding-top: 1rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
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

/* Empty State */
.empty-state {
  text-align: center;
  padding: 6rem 2rem;
}

.empty-icon {
  width: 80px;
  height: 80px;
  background: #f5f5f5;
  border-radius: 50%;
  margin: 0 auto 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: #bbb;
}

.empty-state h2 {
  font-size: 1.3rem;
  margin-bottom: 1rem;
  color: #333;
}

.empty-state p {
  color: #888;
  font-size: 0.95rem;
  line-height: 1.6;
}

/* Review List */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.review-card {
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #eee;
}

/* Review Top Layout */
.review-top {
  display: flex;
  justify-content: space-between; /* Space between info and menu */
  align-items: flex-start;
  margin-bottom: 1rem;
}

.info-left {
  display: flex;
  gap: 12px;
  flex: 1;
}

.review-thumb {
  width: 54px;
  height: 54px;
  border-radius: 8px;
  object-fit: cover;
  background: #eee;
  flex-shrink: 0;
  border: 1px solid #f0f0f0;
}

.review-info {
  display: flex;
  flex-direction: column;
  justify-content: center; /* Center text vertically relative to image */
  min-height: 54px;
}

.acc-name {
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  color: #333;
  margin-bottom: 4px;
  line-height: 1.3;
}

.acc-name:hover {
  color: var(--primary);
  text-decoration: underline;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.stars {
  color: #fbbf24;
  font-size: 0.85rem;
  padding-bottom: 1px;
}

.date {
  font-size: 0.8rem;
  color: #999;
}

/* Actions Menu */
.review-actions-menu {
  position: relative;
}

.more-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #888;
  cursor: pointer;
  line-height: 1;
  padding: 0 0.5rem;
}

.more-btn:hover {
  color: #333;
}

.menu-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border: 1px solid #eee;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  overflow: hidden;
  z-index: 10;
  width: 80px;
  display: flex;
  flex-direction: column;
}

.menu-item {
  width: 100%;
  padding: 0.8rem;
  text-align: center;
  border: none;
  background: white;
  font-size: 0.85rem;
  cursor: pointer;
  color: #333;
  transition: background 0.2s;
}

.menu-item:not(:last-child) {
  border-bottom: 1px solid #f5f5f5;
}

.menu-item:hover {
  background: #f9f9f9;
}

.menu-item.delete {
  color: #e11d48;
}

.menu-item.delete:hover {
  background: #fff1f2;
}

.review-content {
  font-size: 0.95rem;
  line-height: 1.6;
  color: #333;
  margin-bottom: 1rem;
}


/* Review Tags */
.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.tag {
  background: var(--primary);
  color: #004d40;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 0.8rem;
  font-weight: 500;
}

/* Review Images */
.review-images {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  overflow-x: auto;
}

.review-img {
  width: 100px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}

/* Host Reply */
.host-reply {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 0.75rem 0.9rem;
  background: #f8fafc;
  display: grid;
  gap: 0.35rem;
  margin-top: 0.5rem;
}

.reply-title {
  margin: 0;
  font-weight: 700;
  color: #0f172a;
  font-size: 0.9rem;
}

.reply-content {
  margin: 0;
  color: #334155;
  font-size: 0.9rem;
  line-height: 1.5;
}

.reply-date {
  margin: 0;
  color: #64748b;
  font-size: 0.82rem;
}

/* Loading & Error States */
.loading-state,
.error-state {
  text-align: center;
  padding: 4rem 2rem;
  color: #888;
}

.error-state p {
  margin-bottom: 1rem;
  color: #ef4444;
}

.retry-btn {
  background: var(--primary);
  color: #004d40;
  border: none;
  padding: 0.5rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

</style>
