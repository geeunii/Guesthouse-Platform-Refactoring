<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCoupons } from '@/api/couponApi'

const router = useRouter()
const activeTab = ref('ISSUED')
const coupons = ref([])
const loading = ref(false)
const error = ref(null)

// 탭별 쿠폰 개수
const couponCounts = ref({
  ISSUED: 0,
  USED: 0,
  EXPIRED: 0
})

// 쿠폰 조회
async function fetchCoupons(status) {
  loading.value = true
  error.value = null
  try {
    coupons.value = await getMyCoupons(status)
  } catch (e) {
    console.error('쿠폰 조회 실패:', e)
    error.value = e.message
    coupons.value = []
  } finally {
    loading.value = false
  }
}

// 모든 탭의 개수 조회
async function fetchAllCounts() {
  try {
    const [issued, used, expired] = await Promise.all([
      getMyCoupons('ISSUED'),
      getMyCoupons('USED'),
      getMyCoupons('EXPIRED')
    ])
    couponCounts.value = {
      ISSUED: issued.length,
      USED: used.length,
      EXPIRED: expired.length
    }
  } catch (e) {
    console.error('쿠폰 개수 조회 실패:', e)
  }
}

// 탭 변경
function switchTab(tab) {
  activeTab.value = tab
  fetchCoupons(tab)
}

// 할인 표시 포맷
function formatDiscount(coupon) {
  if (coupon.discountType === 'PERCENT') {
    return `${coupon.discountValue}% 할인`
  }
  return `${coupon.discountValue.toLocaleString()}원 할인`
}

// 최소 금액 표시
function formatMinPrice(minPrice) {
  if (!minPrice || minPrice === 0) return '조건 없음'
  return `${minPrice.toLocaleString()}원 이상 구매 시`
}

// 만료일 표시
function formatExpiry(expiredAt) {
  if (!expiredAt) return ''
  const date = new Date(expiredAt)
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}까지`
}

onMounted(() => {
  fetchCoupons('ISSUED')
  fetchAllCounts()
})
</script>

<template>
  <div class="coupon-page container">
    <!-- Header -->
    <div class="page-header">
      <button class="back-btn" @click="router.back()">←</button>
      <h1>보유 쿠폰</h1>
    </div>

    <!-- Tabs -->
    <div class="tabs">
      <button
        class="tab"
        :class="{ active: activeTab === 'ISSUED' }"
        @click="switchTab('ISSUED')"
      >
        사용 가능<br><span class="count">{{ couponCounts.ISSUED }}개</span>
      </button>
      <button
        class="tab"
        :class="{ active: activeTab === 'USED' }"
        @click="switchTab('USED')"
      >
        사용 완료<br><span class="count">{{ couponCounts.USED }}개</span>
      </button>
      <button
        class="tab"
        :class="{ active: activeTab === 'EXPIRED' }"
        @click="switchTab('EXPIRED')"
      >
        만료됨<br><span class="count">{{ couponCounts.EXPIRED }}개</span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>쿠폰을 불러오는 중...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button @click="fetchCoupons(activeTab)">다시 시도</button>
    </div>

    <!-- Empty State -->
    <div v-else-if="coupons.length === 0" class="empty-state">
      <div class="empty-icon">🎟️</div>
      <h2 v-if="activeTab === 'ISSUED'">사용 가능한 쿠폰이 없어요</h2>
      <h2 v-else-if="activeTab === 'USED'">사용한 쿠폰이 없어요</h2>
      <h2 v-else>만료된 쿠폰이 없어요</h2>
      <p>이벤트와 프로모션을 확인해보세요.</p>
    </div>

    <!-- Coupon List -->
    <template v-else>
      <!-- Section Title -->
      <div class="section-title">
        <span v-if="activeTab === 'ISSUED'">✓ 사용 가능한 쿠폰</span>
        <span v-else-if="activeTab === 'USED'">✓ 사용 완료된 쿠폰</span>
        <span v-else>⏱ 만료된 쿠폰</span>
      </div>

      <!-- Coupon Cards -->
      <div class="coupon-list">
        <div
          v-for="coupon in coupons"
          :key="coupon.id"
          class="coupon-card"
          :class="{
            available: activeTab === 'ISSUED',
            expired: activeTab === 'EXPIRED',
            used: activeTab === 'USED'
          }"
        >
          <div class="coupon-icon">🏷️</div>
          <div class="coupon-info">
            <span class="coupon-name">{{ coupon.name }}</span>
            <span class="coupon-discount">{{ formatDiscount(coupon) }}</span>
            <span class="coupon-condition">{{ formatMinPrice(coupon.minPrice) }}</span>
            <span class="coupon-expiry">⏱ {{ formatExpiry(coupon.expiredAt) }}</span>
          </div>
          <div v-if="activeTab === 'EXPIRED'" class="status-badge expired-badge">만료됨</div>
          <div v-if="activeTab === 'USED'" class="status-badge used-badge">사용완료</div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.coupon-page {
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

/* Loading & Error */
.loading-state,
.error-state {
  text-align: center;
  padding: 4rem 2rem;
  color: #666;
}

.error-state button {
  margin-top: 1rem;
  padding: 0.5rem 1rem;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
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

/* Tabs */
.tabs {
  display: flex;
  border-bottom: 1px solid #eee;
  margin-bottom: 1.5rem;
}

.tab {
  flex: 1;
  padding: 1rem;
  background: none;
  border: none;
  text-align: center;
  font-size: 0.85rem;
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}

.tab.active {
  color: #333;
  font-weight: bold;
  border-bottom-color: #333;
}

.tab .count {
  font-weight: bold;
}

/* Section Title */
.section-title {
  font-size: 0.9rem;
  color: #555;
  margin-bottom: 1rem;
  font-weight: 500;
}

/* Coupon Cards */
.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.coupon-card {
  display: flex;
  gap: 1rem;
  padding: 1.2rem;
  border: 1px solid #eee;
  border-radius: 12px;
  background: white;
  position: relative;
}

.coupon-card.expired,
.coupon-card.used {
  opacity: 0.6;
}

.coupon-card.available {
  opacity: 1;
}

.coupon-card.available .coupon-name {
  color: #333;
  font-weight: 600;
}

.coupon-card.available .coupon-condition,
.coupon-card.available .coupon-expiry {
  color: #555;
}

.coupon-icon {
  width: 40px;
  height: 40px;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.coupon-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.coupon-name {
  font-size: 0.85rem;
  color: #666;
}

.coupon-discount {
  font-size: 1.1rem;
  font-weight: 800;
  color: #2563eb;
}

.coupon-condition {
  font-size: 0.8rem;
  color: #888;
}

.coupon-expiry {
  font-size: 0.8rem;
  color: #888;
}

.status-badge {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.75rem;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: bold;
}

.expired-badge {
  background: #f87171;
  color: white;
}

.used-badge {
  background: #9ca3af;
  color: white;
}
</style>
