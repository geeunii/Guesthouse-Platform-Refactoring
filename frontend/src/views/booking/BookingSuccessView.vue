<script setup>
import { useRouter, useRoute } from 'vue-router'
import { onMounted, ref } from 'vue'
import { getReservation } from '@/api/reservationApi'

const router = useRouter()
const route = useRoute()

// Default fallback data if accessed directly
const info = ref({
  bookingNumber: '',
  accommodationName: '',
  location: '',
  dates: '',
  nights: 0,
  guests: '',
  basePrice: 0,
  fees: 0,
  totalPrice: 0,
  paymentDate: '',
  reserverName: '',
  reserverPhone: '',
  paymentMethod: ''
})

// 결제 수단 한글 변환
const getPaymentMethodName = (method) => {
  const methodNames = {
    '카드': '신용/체크카드',
    'CARD': '신용/체크카드',
    '가상계좌': '가상계좌',
    'VIRTUAL_ACCOUNT': '가상계좌',
    '계좌이체': '계좌이체',
    'TRANSFER': '계좌이체',
    '휴대폰': '휴대폰 결제',
    'MOBILE_PHONE': '휴대폰 결제',
    '간편결제': '간편결제',
    'EASY_PAY': '간편결제',
    '토스페이': '토스페이',
    '토스결제': '토스페이'
  }
  return methodNames[method] || method
}

onMounted(async () => {
    // 1. state에서 데이터 확인 (booking-success 라우트로 이동 시 전달됨)
    const state = history.state
    
    // 2. 예약 ID 확인 (query 파라미터 우선, 없으면 state에서 확인)
    // view_file 결과를 보면 route가 정의되지 않았으므로 script setup 상단에 추가해야 함. 
    // 이미 추가했음 (const route = useRoute())
    const reservationId = route.query.reservationId || (state && state.bookingData ? state.bookingData.reservationId : null)

    if (reservationId) {
        try {
            const res = await getReservation(reservationId)
            
            // 날짜 포맷팅
            const checkin = new Date(res.checkin).toLocaleDateString()
            const checkout = new Date(res.checkout).toLocaleDateString()
            
            info.value = {
                bookingNumber: 'BK' + res.reservationId,
                accommodationName: res.accommodationName,
                location: res.accommodationAddress,
                dates: `${checkin} ~ ${checkout}`,
                nights: res.stayNights,
                guests: `게스트 ${res.guestCount}명`, // 상세 구분은 API에 없으므로 총 인원 표시
                basePrice: res.finalPaymentAmount, // 숙박 요금 (할인 후 최종 금액을 표시하거나, 원가를 표시하고 할인을 따로 표시할 수 있음. 여기선 최종 금액 사용)
                fees: 0, // 수수료 없음
                totalPrice: res.finalPaymentAmount,
                paymentDate: new Date(res.createdAt).toLocaleString(),
                reserverName: res.reserverName,
                reserverPhone: res.reserverPhone,
                paymentMethod: res.paymentMethod || ''
            }
        } catch (error) {
            console.error('예약 정보 조회 실패:', error)
        }
    } else if (state && state.bookingData) {
        // 백엔드 연동 전 fallback (기존 로직 유지하되 일부 필드 매핑)
        const data = state.bookingData
        info.value = {
            ...info.value,
            accommodationName: data.accommodationName,
            dates: data.dates,
            guests: data.guests,
            basePrice: data.basePrice,
            totalPrice: data.totalPrice,
            paymentDate: new Date().toLocaleString()
        }
    }
})

const goHome = () => router.push('/')
const goHistory = () => router.push('/reservations')
</script>

<template>
  <div class="success-page">
    <!-- Top Banner -->
    <div class="top-banner">
      <div class="check-icon">✓</div>
      <h1>예약이 완료되었습니다!</h1>
      <p>예약 확인 메일이 발송되었습니다</p>
    </div>

    <div class="container content">
      <!-- Booking Number -->
      <div class="card booking-number-card">
        <label>예약 번호</label>
        <div class="number">{{ info.bookingNumber }}</div>
      </div>

      <!-- Reservation Info -->
      <div class="card info-card">
        <h3>예약 정보</h3>
        <hr class="divider"/>
        
        <div class="info-item">
          <div class="text">
            <span class="label">숙소</span>
            <span class="value">{{ info.accommodationName }}</span>
          </div>
        </div>

        <div class="info-item">
          <div class="text">
            <span class="label">위치</span>
            <span class="value">{{ info.location }}</span>
          </div>
        </div>

        <div class="info-item">
          <div class="text">
            <span class="label">체크인 / 체크아웃</span>
            <span class="value">{{ info.dates }}</span>
            <span class="sub-text">{{ info.nights }}박</span>
          </div>
        </div>

        <div class="info-item">
          <div class="text">
            <span class="label">투숙객</span>
            <span class="value">{{ info.guests }}</span>
          </div>
        </div>

        <!-- Added: Applicant Info -->
        <div class="info-item" v-if="info.reserverName">
            <div class="text">
              <span class="label">예약자명</span>
              <span class="value">{{ info.reserverName }}</span>
            </div>
        </div>
        <div class="info-item" v-if="info.reserverPhone">
            <div class="text">
              <span class="label">연락처</span>
              <span class="value">{{ info.reserverPhone }}</span>
            </div>
        </div>

      </div>

      <!-- Payment Info -->
      <div class="card payment-card">
        <h3>결제 정보</h3>
        <hr class="divider"/>
        
        <div class="row">
          <span>숙박 요금</span>
          <span>{{ info.basePrice?.toLocaleString() }}원</span>
        </div>
        <!-- Commission Removed as requested -->
        
        <hr class="divider-light"/>
        <div class="row total">
          <span>총 결제 금액</span>
          <span>{{ info.totalPrice?.toLocaleString() }}원</span>
        </div>
        <div class="row" v-if="info.paymentMethod">
          <span>결제 수단</span>
          <span>{{ getPaymentMethodName(info.paymentMethod) }}</span>
        </div>

        <div class="payment-date">
          결제 완료<br/>
          {{ info.paymentDate }}
        </div>
      </div>

      <!-- Guide -->
      <div class="card guide-card">
        <h3>체크인 안내</h3>
        <ul>
          <li>체크인 시간: 오후 3시 이후</li>
          <li>체크아웃 시간: 오전 11시</li>
          <li>신분증을 지참해 주세요</li>
        </ul>
      </div>

      <!-- Buttons -->
      <div class="actions">
        <button class="primary-btn" @click="goHistory">예약 내역 보기</button>
        <button class="secondary-btn" @click="goHome">새로운 예약하기</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.success-page {
  min-height: 100vh;
  background-color: var(--bg-body);
  padding-bottom: 2rem;
}

.top-banner {
  background-color: var(--primary);
  color: #004d40;
  padding: 3rem 1rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.check-icon {
  width: 60px;
  height: 60px;
  border: 4px solid #004d40;
  border-radius: 50%;
  font-size: 2rem;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}

.top-banner h1 {
  font-size: 1.5rem;
  margin: 0;
}

.top-banner p {
  color: #00695c;
  font-size: 0.9rem;
}

.content {
  max-width: 500px; /* Narrower for mobile-like feel per screenshot */
  margin: -1.5rem auto 0;
  padding: 0 1rem;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  border: 1px solid #eee;
}

.booking-number-card {
  text-align: center;
  padding: 1.5rem;
}
.booking-number-card label {
  color: #888;
  font-size: 0.85rem;
  display: block;
  margin-bottom: 0.25rem;
}
.booking-number-card .number {
  font-weight: bold;
  font-size: 1.1rem;
  letter-spacing: 1px;
}

h3 {
  font-size: 1rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
}
.divider {
  border: 0;
  border-top: 2px solid #f5f5f5;
  margin: 0.8rem 0 1.2rem;
}
.divider-light {
  border: 0;
  border-top: 1px solid #eee;
  margin: 0.8rem 0;
}

.info-item {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.2rem;
}
.icon {
  color: #999;
  font-size: 1.1rem;
}
.text {
  display: flex;
  flex-direction: column;
}
.label {
  font-size: 0.8rem;
  color: #888;
  margin-bottom: 2px;
}
.value {
  font-size: 0.95rem;
  font-weight: 500;
  color: #333;
}
.sub-text {
  font-size: 0.8rem;
  color: #888;
}

.row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  color: #555;
}
.row.total {
  font-weight: bold;
  color: #000;
  font-size: 1rem;
  margin-top: 0.5rem;
}

.payment-date {
  margin-top: 1rem;
  font-size: 0.8rem;
  color: #888;
  line-height: 1.4;
}

.guide-card ul {
  list-style: none;
  padding: 0;
}
.guide-card li {
  position: relative;
  padding-left: 10px;
  font-size: 0.9rem;
  color: #555;
  margin-bottom: 0.3rem;
}
.guide-card li::before {
  content: "•";
  position: absolute;
  left: 0;
  color: #888;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
  margin-top: 1.5rem;
}

.primary-btn {
  background: var(--primary);
  color: #004d40; /* High contrast text */
  padding: 1rem;
  border-radius: 8px;
  font-weight: bold;
  border: none;
}
.secondary-btn {
  background: white;
  border: 1px solid #ddd;
  padding: 1rem;
  border-radius: 8px;
  font-weight: bold;
  color: #555;
}
</style>
