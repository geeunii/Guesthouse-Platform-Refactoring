<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyReservations, deleteCompletedReservation, deleteCancelledReservation } from '@/api/reservationApi'
import { isAuthenticated } from '@/api/authClient'

const router = useRouter()

// 로딩 및 에러 상태
const isLoading = ref(true)
const errorMessage = ref('')

// 예약 데이터
const reservations = ref([])

// 탭 상태
const activeTab = ref('ACTIVE') // ACTIVE | CANCELLED | COMPLETED

// 오늘 날짜
const today = new Date()
today.setHours(0, 0, 0, 0)

// 예정된 예약 (체크인 날짜가 오늘 이후)
const upcomingReservations = computed(() => {
  return reservations.value.filter(r => {
    const checkinDate = new Date(r.checkin)
    checkinDate.setHours(0, 0, 0, 0)
    return checkinDate >= today && r.reservationStatus === 2 // 확정된 예약만 (2: 확정)
  })
})

// 이용 완료 (체크인 날짜가 오늘 이전)
const pastReservations = computed(() => {
  return reservations.value.filter(r => {
    const checkinDate = new Date(r.checkin)
    checkinDate.setHours(0, 0, 0, 0)
    return checkinDate < today && r.reservationStatus === 2 // 확정된 예약만 (2: 확정)
  })
})

// 취소된 예약 (reservationStatus === 9)
const cancelledReservations = computed(() => {
  return reservations.value.filter(r => r.reservationStatus === 9)
})

// 탭별 필터링 (쿠폰함 스타일)
const filteredReservations = computed(() => {
  if (activeTab.value === 'ACTIVE') {
    return upcomingReservations.value
  }
  if (activeTab.value === 'CANCELLED') {
    return cancelledReservations.value
  }
  if (activeTab.value === 'COMPLETED') {
    return pastReservations.value
  }
  return []
})

// 날짜 포맷 (YYYY.MM.DD)
const formatDate = (dateString) => {
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}.${month}.${day}`
}

// 시간 포맷
const formatTime = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

// 이미지 URL (원본 사용)
const getThumbnailUrl = (url) => {
  if (!url) return ''
  return url
}

// 리뷰 작성 가능 여부 확인 (체크아웃 후 7일 이내)
const isReviewable = (checkoutDate) => {
  const checkout = new Date(checkoutDate)
  checkout.setHours(0, 0, 0, 0)
  
  const deadline = new Date(checkout)
  deadline.setDate(deadline.getDate() + 7)
  
  const now = new Date()
  now.setHours(0, 0, 0, 0)
  
  
  return now <= deadline
}

// 삭제 가능 여부 (체크아웃 시간 이후)
const isDeletable = (checkoutDate) => {
  return new Date() >= new Date(checkoutDate)
}

// 예약 목록 조회 (토큰 기반)
const fetchReservations = async () => {
  try {
    isLoading.value = true
    errorMessage.value = ''

    if (!isAuthenticated()) {
      router.push('/login')
      return
    }

    const data = await getMyReservations()
    reservations.value = data || []
  } catch (error) {
    console.error('예약 조회 실패:', error)
    errorMessage.value = '예약 내역을 불러오는데 실패했습니다.'
  } finally {
    isLoading.value = false
  }
}

// 예약 취소 (페이지 이동)
const handleCancel = (item) => {
  router.push({
    name: 'reservation-cancel',
    params: { id: item.reservationId },
    state: {
      reservationData: {
        id: item.reservationId,
        accommodationName: item.accommodationName,
        location: item.accommodationAddress,
        checkin: formatDate(item.checkin),
        checkout: formatDate(item.checkout),
        guests: item.guestCount,
        price: item.finalPaymentAmount,
        image: getThumbnailUrl(item.accommodationImageUrl) || `https://picsum.photos/seed/${item.accommodationsId}/200/200`
      }
    }
  })
}

// 이용 완료된 예약 내역에서 삭제
const handleDelete = async (id) => {
  if (confirm('내역에서 삭제하시겠습니까?')) {
    try {
      await deleteCompletedReservation(id)
      reservations.value = reservations.value.filter(r => r.reservationId !== id)
    } catch (error) {
      console.error('삭제 실패:', error)
      errorMessage.value = '이용 완료된 예약만 삭제할 수 있습니다.'
    }
  }
}

// 취소 내역에서 삭제
const handleDeleteCancelled = async (id) => {
  if (confirm('취소 내역에서 삭제하시겠습니까?')) {
    try {
      await deleteCancelledReservation(id)
      reservations.value = reservations.value.filter(r => r.reservationId !== id)
    } catch (error) {
      console.error('취소 내역 삭제 실패:', error)
      errorMessage.value = error.message || '취소 내역을 삭제하는데 실패했습니다.'
    }
  }
}

// 예정된 예약 카드 클릭 → 예약완료 상세 페이지
const handleUpcomingClick = (item) => {
  router.push({
    name: 'booking-success',
    query: { reservationId: item.reservationId }
  })
}

// 이용완료 카드 클릭 → 숙소 상세 페이지
const handlePastClick = (item) => {
  router.push(`/room/${item.accommodationsId}`)
}

// 리뷰 작성
const handleWriteReview = (item) => {
  router.push({
    name: 'write-review',
    state: {
      reservationData: {
        reservationId: item.reservationId,
        accommodationId: item.accommodationsId,
        accommodationName: item.accommodationName,
        dates: `${formatDate(item.checkin)} ~ ${formatDate(item.checkout)}`
      }
    }
  })
}



onMounted(() => {
  fetchReservations()
})
</script>

<template>
  <div class="reservation-page container">
    <div class="header-section">
      <button class="back-btn" @click="router.back()">←</button>
      <h1 class="page-title">예약 내역</h1>
    </div>

    <!-- 탭 내비게이션 -->
    <div class="tab-nav">
      <button 
        :class="['tab-btn', { active: activeTab === 'ACTIVE' }]"
        @click="activeTab = 'ACTIVE'"
      >
        예약내역
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'CANCELLED' }]"
        @click="activeTab = 'CANCELLED'"
      >
        취소내역
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'COMPLETED' }]"
        @click="activeTab = 'COMPLETED'"
      >
        이용완료
      </button>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="isLoading" class="loading-state">
      <p>예약 내역을 불러오는 중...</p>
    </div>

    <!-- 에러 상태 -->
    <div v-else-if="errorMessage" class="error-state">
      <p>{{ errorMessage }}</p>
      <button @click="fetchReservations" class="retry-btn">다시 시도</button>
    </div>

    <template v-else>
      <!-- 탭별 예약 목록 -->
      <section class="section">
        <!-- 빈 상태 -->
        <div v-if="filteredReservations.length === 0" class="empty-state">
          <span v-if="activeTab === 'ACTIVE'">예약 내역이 없습니다.</span>
          <span v-else-if="activeTab === 'CANCELLED'">취소 내역이 없습니다.</span>
          <span v-else>이용 완료된 내역이 없습니다.</span>
        </div>

        <!-- 예약 목록 -->
        <div v-else class="card-list">
          <!-- 예정된 예약 (ACTIVE 탭) -->
          <template v-if="activeTab ===  'ACTIVE'">
            <div 
              v-for="item in filteredReservations" 
              :key="item.reservationId" 
              class="res-card clickable" 
              role="link"
              tabindex="0"
              @click="handleUpcomingClick(item)"
              @keydown.enter="handleUpcomingClick(item)"
            >
              <div class="card-content">
                <img
                    :src="getThumbnailUrl(item.accommodationImageUrl) || `https://picsum.photos/seed/${item.accommodationsId}/200/200`"
                    class="card-img"
                    :alt="item.accommodationName || '숙소 이미지'"
                />
                <div class="card-info">
                  <h3 class="res-title">{{ item.accommodationName || '숙소명 없음' }}</h3>
                  <p class="res-loc">{{ item.accommodationAddress || '주소 없음' }}</p>
                  <div class="res-details">
                    <span>체크인</span> <span class="val">{{ formatDate(item.checkin) }}</span>
                  </div>
                  <div class="res-details">
                    <span>체크아웃</span> <span class="val">{{ formatDate(item.checkout) }}</span>
                  </div>
                  <div class="res-details">
                    <span>인원</span> <span class="val">{{ item.guestCount }}명</span>
                    <span class="spacer">숙박</span> <span class="val">{{ item.stayNights }}박</span>
                  </div>
                  <div class="res-price">
                    결제금액 <span class="price-val">{{ item.finalPaymentAmount?.toLocaleString() || 0 }}원</span>
                  </div>
                </div>
              </div>

              <div class="card-actions" @click.stop>
                <button class="action-btn outline" @click="handleCancel(item)">예약 취소</button>
              </div>
            </div>
          </template>

          <!-- 취소된 예약 (CANCELLED 탭) -->
          <template v-if="activeTab === 'CANCELLED'">
            <router-link 
              v-for="item in filteredReservations" 
              :key="item.reservationId" 
              :to="`/room/${item.accommodationsId}`"
              class="res-card clickable cancelled"
            >
              <div class="card-content">
                <img
                    :src="getThumbnailUrl(item.accommodationImageUrl) || `https://picsum.photos/seed/${item.accommodationsId}/200/200`"
                    class="card-img"
                    :alt="item.accommodationName || '숙소 이미지'"
                />
                <div class="card-info">
                  <div class="cancelled-badge">취소됨</div>
                  <h3 class="res-title">{{ item.accommodationName || '숙소명 없음' }}</h3>
                  <p class="res-loc">{{ item.accommodationAddress || '주소 없음' }}</p>
                  <div class="res-details">
                    <span>예약일</span> <span class="val">{{ formatDate(item.checkin) }} ~ {{ formatDate(item.checkout) }}</span>
                  </div>
                  <div class="res-details">
                    <span>인원</span> <span class="val">{{ item.guestCount }}명</span>
                    <span class="spacer">숙박</span> <span class="val">{{ item.stayNights }}박</span>
                  </div>
                  </div>

                <!-- 삭제 버튼 (이벤트 버블링 방지) -->
                <div class="card-action-overlay" @click.prevent.stop>
                  <button
                    class="icon-btn delete-small"
                    @click="handleDeleteCancelled(item.reservationId)"
                    title="내역 삭제"
                  >🗑</button>
                </div>
              </div>
            </router-link>
          </template>

          <!-- 이용 완료 (COMPLETED 탭) -->
          <template v-if="activeTab === 'COMPLETED'">
            <div 
              v-for="item in filteredReservations" 
              :key="item.reservationId" 
              class="res-card clickable" 
              role="link"
              tabindex="0"
              @click="handlePastClick(item)"
              @keydown.enter="handlePastClick(item)"
            >
              <div class="card-content">
                <img
                    :src="getThumbnailUrl(item.accommodationImageUrl) || `https://picsum.photos/seed/${item.accommodationsId}/200/200`"
                    class="card-img"
                    :alt="item.accommodationName || '숙소 이미지'"
                />
                <div class="card-info">
                  <h3 class="res-title">{{ item.accommodationName || '숙소명 없음' }}</h3>
                  <p class="res-loc">{{ item.accommodationAddress || '주소 없음' }}</p>
                  <div class="res-details">
                    <span>이용일</span> <span class="val">{{ formatDate(item.checkin) }} ~ {{ formatDate(item.checkout) }}</span>
                  </div>
                  <div class="res-details">
                    <span>인원</span> <span class="val">{{ item.guestCount }}명</span>
                    <span class="spacer">숙박</span> <span class="val">{{ item.stayNights }}박</span>
                  </div>
                  <div class="res-price">
                    결제금액 <span class="price-val">{{ item.finalPaymentAmount?.toLocaleString() || 0 }}원</span>
                  </div>
                </div>
              </div>

              <div class="card-actions" @click.stop>
                <template v-if="!item.hasReview">
                   <button
                    class="action-btn review"
                    :class="{ disabled: !isReviewable(item.checkout) }"
                    @click="handleWriteReview(item)"
                    :disabled="!isReviewable(item.checkout)"
                  >
                    {{ isReviewable(item.checkout) ? '리뷰 작성하기' : '작성 기한 만료' }}
                  </button>
                </template>
                <template v-else>
                  <button class="action-btn review completed" disabled>
                    리뷰 등록 완료
                  </button>
                </template>
                
                <button
                  class="icon-btn delete"
                  @click="handleDelete(item.reservationId)"
                  :disabled="!isDeletable(item.checkout)"
                  :title="isDeletable(item.checkout) ? '내역 삭제' : '이용 완료 후 삭제 가능'"
                >🗑</button>
              </div>
            </div>
          </template>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.reservation-page {
  padding-top: 1rem;
  padding-bottom: 4rem;
  max-width: 600px;
}

.header-section {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.back-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
}

.page-title {
  font-size: 1.3rem;
  font-weight: 700;
}

/* 탭 네비게이션 */
.tab-nav {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #eee;
}

.tab-btn {
  flex: 1;
  padding: 0.75rem 1rem;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 0.95rem;
  font-weight: 600;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  color: #333;
  border-bottom-color: #333;
}

.tab-btn:hover:not(.active) {
  color: #333;
  background: #f9f9f9;
}

.section {
  margin-bottom: 2.5rem;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 800;
  margin-bottom: 1rem;
  color: #333;
}

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 2rem;
  color: #888;
  background: #f9f9f9;
  border-radius: 12px;
}

.error-state {
  color: #e11d48;
}

.retry-btn {
  margin-top: 1rem;
  padding: 0.5rem 1rem;
  background: var(--primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.res-card {
  background: white;
  border: 1px solid #eee;
  border-radius: 16px;
  padding: 1rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}

.res-card.clickable {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.res-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.card-content {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.card-img {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  object-fit: cover;
  background: #eee;
  /* 이미지 축소 시 품질 개선 */
  image-rendering: -webkit-optimize-contrast;
  image-rendering: smooth;
  transform: translateZ(0);
}

.card-info {
  flex: 1;
}

.res-title {
  font-size: 1rem;
  font-weight: 800;
  margin-bottom: 0.3rem;
  color: #111;
}

.res-loc {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 0.5rem;
}

.res-details {
  font-size: 0.85rem;
  color: #444;
  margin-bottom: 2px;
}

.res-details .spacer {
  margin-left: 12px;
}

.res-details .val {
  font-weight: 500;
}

.res-price {
  margin-top: 0.5rem;
  font-size: 0.9rem;
  color: #2563eb;
  font-weight: bold;
}

.price-val {
  color: #2563eb;
}

.card-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  flex: 1;
  padding: 0.6rem;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
}

.action-btn.outline {
  background: white;
  color: #555;
  border: 1px solid #ddd;
}

.action-btn.outline:hover {
  background: #f5f5f5;
}

.action-btn.review {
  background: var(--primary);
  color: #004d40;
  border: 1px solid var(--primary);
}

.action-btn.review:hover:not(:disabled) {
  opacity: 0.9;
}

.action-btn.review.completed {
  background: #e0e0e0;
  color: #666;
  border: 1px solid #ccc;
  cursor: default;
}

.icon-btn.delete {
  background: var(--primary);
  border: 1px solid var(--primary);
  color: #e11d48;
  width: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  cursor: pointer;
}

.icon-btn.delete:hover {
  opacity: 0.9;
}

.icon-btn.delete:disabled {
  background: #ccc;
  border-color: #ccc;
  cursor: not-allowed;
  opacity: 0.6;
}

.action-btn.review.disabled {
  background: #ccc;
  border-color: #ccc;
  color: #666;
  cursor: not-allowed;
}

.action-btn.edit {
  background: white;
  border: 1px solid var(--primary);
  color: #004d40;
}

.action-btn.edit:hover {
  background: #f0fdf4; /* primary light color */
}

/* 취소된 예약 스타일 */
.res-card.cancelled {
  opacity: 0.7;
  border-color: #e0e0e0;
}

.res-card.cancelled .card-img {
  filter: grayscale(50%);
}

.cancelled-badge {
  display: inline-block;
  background: #fee2e2;
  color: #dc2626;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 4px;
}

/* router-link 기본 스타일 제거 */
a.res-card {
  text-decoration: none;
  color: inherit;
}

/* 카드 내 오버레이 액션 버튼 */
.card-action-overlay {
  display: flex;
  align-items: flex-start;
  padding-left: 0.5rem;
}

.icon-btn.delete-small {
  background: none;
  border: none;
  color: #999;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}

.icon-btn.delete-small:hover {
  color: #e11d48;
  background: #fee2e2;
}
</style>
