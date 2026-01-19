<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authenticatedRequest } from '@/api/authClient'

const router = useRouter()
const route = useRoute()

const isLoading = ref(true)
const errorMessage = ref('')

const reservation = ref({
  id: route.params.id,
  accommodationName: '',
  location: '',
  checkin: '',
  checkout: '',
  guests: 1,
  price: 0,
  image: ''
})

const paymentInfo = ref({
  paymentMethod: '',
  approvedAmount: 0
})

const refundQuoteLoading = ref(false)
const refundQuoteError = ref('')
const refundQuote = ref(null)

// 예약 정보 로드
onMounted(async () => {
  try {
    // history.state에서 예약 정보 받아오기
    if (history.state && history.state.reservationData) {
      const data = history.state.reservationData
      reservation.value = {
        id: data.id,
        accommodationName: data.accommodationName,
        location: data.location,
        checkin: data.checkin,
        checkout: data.checkout,
        guests: data.guests,
        price: data.price,
        image: data.image
      }
    }

    // 결제 정보 조회
    const reservationId = route.params.id
    const response = await authenticatedRequest(`/api/payments/reservation/${reservationId}`)
    if (response.ok && response.data) {
      paymentInfo.value = {
        paymentMethod: response.data.paymentMethod || '카드',
        approvedAmount: response.data.approvedAmount || reservation.value.price
      }
    }
    await loadRefundQuote(reservationId)
  } catch (error) {
    console.error('결제 정보 조회 실패:', error)
    // 결제 정보가 없어도 진행 가능
    paymentInfo.value.approvedAmount = reservation.value.price
    await loadRefundQuote(route.params.id)
  } finally {
    isLoading.value = false
  }
})

const daysUntilCheckin = computed(() => {
  const value = Number(refundQuote.value?.daysBefore)
  return Number.isFinite(value) ? value : 0
})

const refundRate = computed(() => {
  const value = Number(refundQuote.value?.refundRate)
  return Number.isFinite(value) ? value : 0
})

const refundAmount = computed(() => {
  const value = Number(refundQuote.value?.refundAmount)
  return Number.isFinite(value) ? value : 0
})

const canRefund = computed(() => refundRate.value > 0)

const cancelReason = ref('')
const agreed = ref(false)

// Modal State
const showModal = ref(false)
const modalMessage = ref('')
const modalType = ref('info')
const modalCallback = ref(null)

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

// 결제 수단 표시 텍스트
const paymentMethodText = computed(() => {
  const method = paymentInfo.value.paymentMethod
  if (!method) return '결제 수단'
  if (method === '카드' || method === 'CARD') return '신용/체크카드'
  if (method === '간편결제') return '간편결제'
  if (method === '계좌이체') return '계좌이체'
  return method
})

// 환불 처리
const handleCancel = async () => {
  if (refundQuoteError.value) {
    openModal(refundQuoteError.value, 'error')
    return
  }
  if (!cancelReason.value.trim()) {
    openModal('환불 사유를 입력해주세요.', 'error')
    return
  }
  if (!agreed.value) {
    openModal('환불 규정에 동의해주세요.', 'error')
    return
  }
  
  try {
    const response = await authenticatedRequest('/api/payments/cancel', {
      method: 'POST',
      body: JSON.stringify({
        reservationId: Number(route.params.id),
        cancelReason: cancelReason.value,
        refundAmount: refundAmount.value
      })
    })

    if (response.ok) {
      openModal(`환불이 완료되었습니다.\n환불 금액: ${refundAmount.value.toLocaleString()}원`, 'success', () => router.push('/reservations'))
    } else {
      openModal('환불 처리에 실패했습니다.', 'error')
    }
  } catch (error) {
    console.error('환불 요청 실패:', error)
    openModal('환불 처리 중 오류가 발생했습니다.', 'error')
  }
}

// 환불 규정 페이지로 이동
const goToRefundPolicy = () => {
  window.open('/policy?tab=refund', '_blank')
}
const cancelReasons = [
  '방문불가/여행취소',
  '타 서비스에서 더 싼 상품 발견',
  '다시예약_같은숙소 일정/객실 변경',
  '다른 예약_다른숙소로 변경',
  '결제수단변경/쿠폰사용',
  '업체요청',
  '단순변심'
]

const loadRefundQuote = async (reservationId) => {
  if (!reservationId) return
  refundQuoteLoading.value = true
  refundQuoteError.value = ''
  const response = await authenticatedRequest(`/api/refunds/quote?reservationId=${reservationId}`)
  if (response.ok && response.data) {
    refundQuote.value = response.data
  } else {
    refundQuoteError.value = response?.data?.message || '환불 계산 불가'
  }
  refundQuoteLoading.value = false
}
</script>

<template>
  <div class="cancel-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="router.back()">←</button>
      <h1>예약 취소</h1>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="loading-state">
      <p>정보를 불러오는 중...</p>
    </div>

    <template v-else>
      <!-- Info Card -->
      <div class="info-card">
        <img :src="reservation.image || 'https://picsum.photos/200/200'" class="info-img" />
        <div class="info-content">
          <h3>{{ reservation.accommodationName }}</h3>
          <p class="loc">{{ reservation.location }}</p>
          <p class="date">{{ reservation.checkin }} ~ {{ reservation.checkout }}</p>
          <p class="guests">게스트 {{ reservation.guests }}명</p>
        </div>
      </div>

      <!-- Refund Guide -->
      <div class="refund-guide">
        <h3>환불 규정</h3>
        <div v-if="refundQuoteLoading" class="refund-status">환불 금액 계산 중...</div>
        <div v-else-if="refundQuoteError" class="refund-status refund-status--error">{{ refundQuoteError }}</div>
        <ul>
          <li :class="{ active: daysUntilCheckin >= 7 }">체크인 7일 전: <strong>100%</strong> 환불</li>
          <li :class="{ active: daysUntilCheckin >= 5 && daysUntilCheckin < 7 }">체크인 5~6일 전: <strong>90%</strong> 환불</li>
          <li :class="{ active: daysUntilCheckin >= 3 && daysUntilCheckin < 5 }">체크인 3~4일 전: <strong>70%</strong> 환불</li>
          <li :class="{ active: daysUntilCheckin >= 1 && daysUntilCheckin < 3 }">체크인 1~2일 전: <strong>50%</strong> 환불</li>
          <li :class="{ active: daysUntilCheckin < 1, 'no-refund': true }">체크인 당일 또는 노쇼: <strong>환불 불가</strong></li>
        </ul>
        <div class="days-info">
          <span>체크인까지</span>
          <span class="days">{{ daysUntilCheckin }}일</span>
        </div>
        <div class="refund-amount">
          <span>예상 환불 금액 ({{ refundRate }}%)</span>
          <span class="amount" :class="{ 'no-refund': !canRefund }">₩{{ refundAmount.toLocaleString() }}</span>
        </div>
      </div>

      <!-- Cancel Reason -->
      <div class="reason-section">
        <h3>취소 사유</h3>
        <div class="reason-list">
          <label 
            v-for="reason in cancelReasons" 
            :key="reason" 
            class="reason-item"
            :class="{ selected: cancelReason === reason }"
          >
            <input type="radio" v-model="cancelReason" :value="reason">
            <span>{{ reason }}</span>
          </label>
        </div>
      </div>

      <!-- Refund Method -->
      <div class="method-section">
        <h3>환불 수단</h3>
        <div class="method-box">
          <span>💳</span>
          <span>{{ paymentMethodText }}로 결제한 금액이 환불됩니다</span>
        </div>
      </div>

      <!-- Warning -->
      <div class="warning-box" v-if="!canRefund">
        <p>⚠️ 체크인 당일은 환불이 불가능합니다.</p>
      </div>
      <div class="warning-box" v-else>
        <p>⚠️ 취소 후에는 되돌릴 수 없습니다.</p>
      </div>

      <!-- Agreement -->
      <label class="agreement">
        <input type="checkbox" v-model="agreed" />
        <span>
          위 <a href="#" @click.prevent="goToRefundPolicy" class="policy-link">환불 규정</a>을 확인하고 동의합니다.
        </span>
      </label>

      <!-- Bottom Bar -->
      <div class="bottom-bar">
        <button class="cancel-btn outline" @click="router.back()">뒤로가기</button>
        <button 
          class="cancel-btn primary" 
          @click="handleCancel"
          :disabled="!canRefund"
          :class="{ disabled: !canRefund }"
        >
          환불 요청
        </button>
      </div>

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
    </template>
  </div>
</template>

<style scoped>
.cancel-page {
  padding-top: 1rem;
  padding-bottom: 120px;
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

.loading-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

/* Info Card */
.info-card {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 12px;
  margin-bottom: 1.5rem;
}

.info-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.info-content h3 {
  font-size: 1rem;
  margin-bottom: 0.3rem;
}

.info-content .loc {
  font-size: 0.85rem;
  color: #666;
}

.info-content .date,
.info-content .guests {
  font-size: 0.85rem;
  color: #888;
}

/* Refund Guide */
.refund-guide {
  padding: 1.2rem;
  border: 1px solid #eee;
  border-radius: 12px;
  margin-bottom: 1.5rem;
}

.refund-guide h3 {
  font-size: 0.95rem;
  margin-bottom: 0.8rem;
}

.refund-status {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 0.75rem;
  font-weight: 600;
}

.refund-status--error {
  color: #dc2626;
}

.refund-guide ul {
  list-style: none;
  font-size: 0.85rem;
  color: #555;
  line-height: 1.8;
  margin-bottom: 1rem;
}

.refund-guide ul li {
  padding: 0.3rem 0.5rem;
  border-radius: 4px;
  transition: background 0.2s;
}

.refund-guide ul li.active {
  background: var(--primary);
  color: #004d40;
  font-weight: 600;
}

.refund-guide ul li.no-refund.active {
  background: #fee2e2;
  color: #dc2626;
}

.days-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  padding: 0.8rem 0;
  border-top: 1px solid #eee;
  color: #666;
}

.days-info .days {
  font-weight: bold;
  color: #2563eb;
}

.refund-amount {
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  padding-top: 0.8rem;
  border-top: 1px solid #eee;
}

.amount {
  color: #2563eb;
  font-size: 1.1rem;
}

.amount.no-refund {
  color: #dc2626;
}

/* Reason */
.reason-section {
  margin-bottom: 1.5rem;
}

.reason-section h3 {
  font-size: 0.95rem;
  margin-bottom: 0.5rem;
}

.reason-list {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.reason-item {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  background: white;
  transition: all 0.2s;
}

.reason-item:hover {
  background: #f9f9f9;
  border-color: #ddd;
}

.reason-item.selected {
  border-color: var(--primary);
  background: #f0fdf9;
  font-weight: 500;
}

.reason-item input[type="radio"] {
  width: 18px;
  height: 18px;
  accent-color: var(--primary);
  cursor: pointer;
}

/* Method */
.method-section {
  margin-bottom: 1.5rem;
}

.method-section h3 {
  font-size: 0.95rem;
  margin-bottom: 0.5rem;
}

.method-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #f9f9f9;
  font-size: 0.9rem;
}

/* Warning */
.warning-box {
  padding: 1rem;
  background: #fff5f5;
  border: 1px solid #fecaca;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.warning-box p {
  font-size: 0.9rem;
  color: #b91c1c;
}

/* Agreement */
.agreement {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  cursor: pointer;
  margin-bottom: 2rem;
}

.agreement input {
  width: 18px;
  height: 18px;
}

.policy-link {
  color: #2563eb;
  text-decoration: underline;
}

/* Bottom Bar */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 1rem;
  background: white;
  border-top: 1px solid #eee;
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.cancel-btn {
  flex: 1;
  max-width: 280px;
  padding: 1rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: bold;
  cursor: pointer;
}

.cancel-btn.outline {
  background: white;
  border: 1px solid #ddd;
  color: #333;
}

.cancel-btn.primary {
  background: var(--primary);
  color: #004d40;
  border: none;
}

.cancel-btn.disabled {
  background: #ccc;
  color: #666;
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
  white-space: pre-line;
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
</style>
