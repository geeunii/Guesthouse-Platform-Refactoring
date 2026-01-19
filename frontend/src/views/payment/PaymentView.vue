<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getReservation } from '@/api/reservationApi'
import { confirmPayment } from '@/api/paymentApi'
import { getCurrentUser } from '@/api/userClient'

const router = useRouter()
const route = useRoute()

const reservationId = computed(() => route.params.reservationId)
const reservation = ref(null)
const currentUser = ref(null)
const isLoading = ref(true)
const isPaymentLoading = ref(false)
const errorMessage = ref('')
const paymentWidget = ref(null)
const paymentMethodWidget = ref(null)

// 토스페이먼츠 결제위젯 클라이언트 키
const clientKey = 'test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm'

// 주문번호 생성
const generateOrderId = () => {
  const timestamp = Date.now()
  return `ORDER_${reservationId.value}_${timestamp}`
}

onMounted(async () => {
  try {
    // 예약 정보 조회
    reservation.value = await getReservation(reservationId.value)
    
    // 현재 로그인한 사용자 정보 조회
    const userResponse = await getCurrentUser()
    if (userResponse.ok && userResponse.data) {
      currentUser.value = userResponse.data
    }
    
    // 토스페이먼츠 SDK 로드
    await loadTossPaymentsSDK()
    
    // 결제 위젯 초기화
    initPaymentWidget()
    
  } catch (error) {
    console.error('초기화 실패:', error)
    errorMessage.value = '결제 페이지를 불러오는데 실패했습니다.'
  } finally {
    isLoading.value = false
  }
})

onUnmounted(() => {
  // Toss Payments 위젯/오버레이 강제 제거
  try {
    // 1. iframe 제거
    const iframes = document.querySelectorAll('iframe')
    iframes.forEach(iframe => {
      if ((iframe.src && iframe.src.includes('tosspayments')) || 
          (iframe.id && iframe.id.includes('tosspayments')) ||
          (iframe.className && typeof iframe.className === 'string' && iframe.className.includes('tosspayments'))) {
        iframe.remove()
      }
    })
    
    // 2. 오버레이/모달 컨테이너 제거 (div)
    // Toss Payments는 보통 body 바로 아래에 div를 생성하여 오버레이를 띄움
    // id나 class에 toss가 포함된 div를 찾아서 제거
    const divs = document.querySelectorAll('div[id*="toss"], div[class*="toss"], div[class*="PaymentWidget"]')
    divs.forEach(div => {
       // 위젯 컨테이너 자체(#payment-method)는 제외하고, body 직계 자식인 모달 등만 제거
       if (div.parentElement === document.body) {
         div.remove()
       }
    })

    // 3. body 스타일 초기화 (스크롤 잠금 해제)
    document.body.style.overflow = ''
    document.body.style.overflowY = ''
    document.body.style.paddingRight = ''
    
  } catch (e) {
    console.warn('Toss Payments Cleanup Failed:', e)
  }
})

// 토스페이먼츠 SDK 동적 로드
const loadTossPaymentsSDK = () => {
  return new Promise((resolve, reject) => {
    if (window.TossPayments) {
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = 'https://js.tosspayments.com/v2/standard'
    script.onload = resolve
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// 결제 위젯 초기화
const initPaymentWidget = async () => {
  try {
    const tossPayments = window.TossPayments(clientKey)
    
    // 결제 위젯 인스턴스 생성
    paymentWidget.value = tossPayments.widgets({
      customerKey: `CUSTOMER_${reservation.value.userId || 'GUEST'}`
    })

    // 결제 금액 설정
    await paymentWidget.value.setAmount({
      currency: 'KRW',
      value: reservation.value.finalPaymentAmount
    })

    // 결제 수단 위젯 렌더링
    await paymentWidget.value.renderPaymentMethods({
      selector: '#payment-method',
      variantKey: 'DEFAULT'
    })

    // 약관 위젯 렌더링
    await paymentWidget.value.renderAgreement({
      selector: '#agreement',
      variantKey: 'AGREEMENT'
    })

  } catch (error) {
    console.error('위젯 초기화 실패:', error)
    errorMessage.value = '결제 위젯을 불러오는데 실패했습니다.'
  }
}

// 결제 요청
const handlePayment = async () => {
  if (!paymentWidget.value) {
    errorMessage.value = '결제 위젯이 초기화되지 않았습니다.'
    return
  }

  isPaymentLoading.value = true
  errorMessage.value = ''

  try {
    const orderId = generateOrderId()

    // 결제 요청
    await paymentWidget.value.requestPayment({
      orderId: orderId,
      orderName: `${reservation.value.accommodationName || '게스트하우스'} 예약`,
      successUrl: `${window.location.origin}/payment/success?reservationId=${reservationId.value}`,
      failUrl: `${window.location.origin}/payment/fail?reservationId=${reservationId.value}`,
      customerEmail: currentUser.value?.email || 'guest@example.com',
      customerName: currentUser.value?.email || reservation.value.reserverName || '예약자',
      customerMobilePhone: currentUser.value?.phone?.replace(/-/g, '') || '01000000000'
    })

  } catch (error) {
    console.error('결제 요청 실패:', error)
    errorMessage.value = error.message || '결제 요청에 실패했습니다.'
  } finally {
    isPaymentLoading.value = false
  }
}

// 뒤로가기 시 대기 예약 삭제
const goBack = async () => {
  // 결제하지 않고 나가면 대기 예약 삭제
  if (reservationId.value && reservation.value?.reservationStatus === 0) {
    try {
      const { deletePendingReservation } = await import('@/api/reservationApi')
      await deletePendingReservation(reservationId.value)
    } catch (e) {
      console.error('대기 예약 삭제 실패:', e)
    }
  }
  router.back()
}
</script>

<template>
  <div class="payment-page">
    <!-- Header -->
    <header class="header">
      <button class="icon-btn" @click="goBack">←</button>
      <h1>결제하기</h1>
    </header>

    <div class="container" v-if="!isLoading">
      <!-- 예약 정보 요약 -->
      <div class="reservation-summary" v-if="reservation">
        <h3>예약 정보</h3>
        <div class="info-row">
          <span>체크인</span>
          <span>{{ reservation.checkin?.split('T')[0] }}</span>
        </div>
        <div class="info-row">
          <span>체크아웃</span>
          <span>{{ reservation.checkout?.split('T')[0] }}</span>
        </div>
        <div class="info-row">
          <span>숙박</span>
          <span>{{ reservation.stayNights }}박</span>
        </div>
        <div class="info-row">
          <span>인원</span>
          <span>{{ reservation.guestCount }}명</span>
        </div>
        <div class="divider"></div>
        <div class="info-row">
          <span>예약자 이름</span>
          <span>{{ reservation.reserverName || '-' }}</span>
        </div>
        <div class="info-row">
          <span>예약자 연락처</span>
          <span>{{ currentUser?.phone || reservation.reserverPhone || '-' }}</span>
        </div>
        <div class="info-row total">
          <span>결제 금액</span>
          <span>₩{{ reservation.finalPaymentAmount?.toLocaleString() }}</span>
        </div>
      </div>

      <!-- 결제 수단 위젯 -->
      <div class="payment-widget-container">
        <div id="payment-method"></div>
        <div id="agreement"></div>
      </div>

      <!-- 에러 메시지 -->
      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

      <!-- 결제 버튼 -->
      <div class="payment-action">
        <button 
          class="pay-btn" 
          :disabled="isPaymentLoading"
          @click="handlePayment"
        >
          {{ isPaymentLoading ? '처리 중...' : `₩${reservation?.finalPaymentAmount?.toLocaleString()} 결제하기` }}
        </button>
      </div>
    </div>

    <!-- 로딩 -->
    <div class="loading" v-else>
      <p>결제 페이지를 불러오는 중...</p>
    </div>
  </div>
</template>

<style scoped>
.payment-page {
  background-color: #f8f9fa;
  min-height: 100vh;
  padding-bottom: 100px;
}

.header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: white;
  border-bottom: 1px solid #eee;
}

.header h1 {
  font-size: 1.2rem;
  margin: 0;
}

.icon-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0;
}

.container {
  max-width: 600px;
  margin: 0 auto;
  padding: 1rem;
}

.reservation-summary {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.reservation-summary h3 {
  margin: 0 0 1rem;
  font-size: 1.1rem;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  font-size: 0.95rem;
  color: #555;
}

.info-row.total {
  border-top: 1px solid #eee;
  margin-top: 0.5rem;
  padding-top: 1rem;
  font-weight: bold;
  color: #000;
  font-size: 1.1rem;
}

.divider {
  height: 1px;
  background: #eee;
  margin: 0.8rem 0;
}

.payment-widget-container {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

#payment-method {
  min-height: 200px;
}

.error-message {
  color: #e11d48;
  font-size: 0.9rem;
  text-align: center;
  padding: 1rem;
  background: #fef2f2;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.payment-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 1rem;
  background: white;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: center;
  z-index: 100;
}

.pay-btn {
  width: calc(100% - 2rem);
  max-width: 600px;
  background: linear-gradient(135deg, #0064FF 0%, #0052CC 100%);
  color: white;
  padding: 1rem;
  border: none;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.2s;
}

.pay-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 50vh;
  color: #666;
}
</style>
