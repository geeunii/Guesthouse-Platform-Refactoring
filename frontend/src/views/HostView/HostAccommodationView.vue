<script setup>
import { ref, computed, onMounted } from 'vue'
import HostAccommodationRegister from './HostAccommodationRegister.vue'
import { fetchHostAccommodations, deleteHostAccommodation } from '@/api/hostAccommodation'

const SERVER_BASE_URL = import.meta.env.VITE_API_BASE_URL?.replace('/api', '') || ''

// 이미지 URL을 전체 경로로 변환
const getFullImageUrl = (url) => {
  if (!url) return 'https://placehold.co/400x300'
  if (url.startsWith('blob:') || url.startsWith('http')) return url
  return `${SERVER_BASE_URL}${url}`
}

const viewMode = ref('list')
const accommodations = ref([])
const isLoading = ref(false)
const loadError = ref('')

const accommodationCount = computed(() => accommodations.value.length)
const hasAccommodations = computed(() => accommodations.value.length > 0)

const formatPrice = (price) => new Intl.NumberFormat('ko-KR').format(price)
const getStatusLabel = (status, rejectionReason, approvalStatus, isResubmitted) => {
  const normalizedApproval = approvalStatus ? String(approvalStatus).toLowerCase() : ''
  if (status === 'rejected' || normalizedApproval === 'rejected') return '반려'
  if (status === 'reinspection' || isResubmitted) return '재검토중'
  if (status === 'pending' || normalizedApproval === 'pending') {
    return rejectionReason ? '재검토중' : '검수중'
  }
  if (status === 'active') return '운영중'
  if (status === 'inactive') return '운영중지'
  return '상태 확인'
}

const handleRegisterCancel = () => (viewMode.value = 'list')

const handleRegisterSubmit = (formData) => {
  accommodations.value.unshift({
    id: formData.id ?? Date.now(),
    status: formData.status ?? 'active',
    ...formData
  })
  viewMode.value = 'list'
}

const handleDelete = async (id) => {
  if (confirm('정말 이 숙소를 삭제하시겠습니까?')) {
    const response = await deleteHostAccommodation(id)
    if (response.ok) {
      accommodations.value = accommodations.value.filter((item) => item.id !== id)
    } else {
      alert('예약이 있는 객실입니다. 예약 내역을 확인해주세요.')
    }
  }
}

const normalizeStatus = (status) => {
  if (!status) return 'inactive'
  const value = String(status).toLowerCase()
  if (value === 'approved' || value === 'active' || value === 'operating') return 'active'
  if (value === 'reinspection') return 'reinspection'
  if (value === 'pending' || value === 'inspection' || value === 'review' || value === 'reviewing') return 'pending'
  if (value === 'rejected' || value === 'reject' || value === 'denied') return 'rejected'
  if (value === 'inactive' || value === 'stopped' || value === 'stop') return 'inactive'
  return value
}

const resubmitWindowMs = 24 * 60 * 60 * 1000
const getResubmitMap = () => {
  try {
    const raw = sessionStorage.getItem('hostResubmitMap')
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

const isResubmittedRecently = (id) => {
  if (!id) return false
  const map = getResubmitMap()
  const timestamp = map[String(id)]
  if (!timestamp) return false
  return Date.now() - Number(timestamp) <= resubmitWindowMs
}

const normalizeAccommodation = (item) =>{
  const id = item.accommodationsId ?? item.accommodationId ?? item.id
  const name = item.accommodationsName ?? item.name ?? ''
  const approvalStatus = item.approvalStatus ?? item.reviewStatus ?? null
  const isResubmitted = item.isResubmitted === 1 || item.isResubmitted === true
  
  // accommodationStatus: 0 = 운영중지, 1 = 운영중  
  const accommodationStatus = item.accommodationStatus ?? item.accommodation_status
  const rejectionReason = item.rejectionReason ?? item.rejectReason ?? item.approvalReason ?? item.reason ?? ''
  
  // 상태 결정 로직
  let status
  const normalizedApproval = approvalStatus ? String(approvalStatus).toLowerCase() : ''
  
  // 1. 반려 상태 확인
  if (normalizedApproval === 'rejected') {
    // 재제출 확인
    if (isResubmittedRecently(id)) {
      status = 'pending'
    } else {
      status = 'rejected'
    }
  }
  // 2. 검수중 상태 확인  
  else if (normalizedApproval === 'pending') {
    status = rejectionReason ? 'reinspection' : 'pending'
  }
  // 3. 승인됨 - accommodationStatus로 운영 여부 결정
  else if (normalizedApproval === 'approved') {
    status = (accommodationStatus === 1) ? 'active' : 'inactive'
  }
  // 4. 기타 상태
  else {
    status = normalizeStatus(item.status ?? item.accommodationStatus ?? 'inactive')
  }
  
  const location = [item.city, item.district, item.township, item.address]
    .filter(Boolean)
    .join(' ')
  const maxGuests = item.maxGuests ?? item.max_guests ?? item.maxGuestCount ?? 0
  const roomCount = item.roomCount ?? item.roomsCount ?? item.totalRooms ?? 0
  const price = item.pricePerNight ?? item.price ?? item.roomPrice ?? 0
  const images = item.images ?? item.imageUrls ?? item.imageUrl ?? []
  return {
    id,
    name,
    status,
    location,
    maxGuests,
    roomCount,
    price,
    images: Array.isArray(images) ? images : [images].filter(Boolean),
    rejectionReason,
    approvalStatus,
    isResubmitted,
    accommodationStatus
  }
}

const loadAccommodations = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchHostAccommodations()
  if (response.ok) {
    const payload = response.data
    console.log('API Response:', payload)
    const list = Array.isArray(payload)
      ? payload
      : payload?.items ?? payload?.content ?? payload?.data ?? []
    console.log('Raw list:', list)
    accommodations.value = list.map(normalizeAccommodation)
    console.log('Normalized:', accommodations.value)
  } else {
    loadError.value = '숙소 목록을 불러오지 못했습니다.'
  }
  isLoading.value = false
}

onMounted(loadAccommodations)
</script>

<template>
  <div class="accommodation-container">
    <HostAccommodationRegister
        v-if="viewMode === 'register'"
        @cancel="handleRegisterCancel"
        @submit="handleRegisterSubmit"
    />

    <div v-else class="list-view-wrapper">
      <div class="host-view-header">
        <div>
          <h2 class="host-title">숙소 관리</h2>
          <p class="host-subtitle">총 {{ accommodationCount }}개의 숙소</p>
        </div>
      </div>

      <button class="register-btn" @click="$router.push('/host/accommodation/register')">
        <span class="plus-icon">+</span>
        새 숙소 등록
      </button>

      <div v-if="hasAccommodations" class="accommodation-list">
        <article
            v-for="accommodation in accommodations"
            :key="accommodation.id"
            class="accommodation-card"
        >
          <div class="card-image clickable" @click="$router.push(`/room/${accommodation.id}`)">
            <img :src="getFullImageUrl(accommodation.images[0])" :alt="accommodation.name"/>
          </div>

          <div class="card-info">
            <div class="info-header">
              <h3 class="accommodation-name clickable" @click="$router.push(`/room/${accommodation.id}`)">{{ accommodation.name }}</h3>
              <span
                  class="status-badge"
                  :class="{
                    active: accommodation.status === 'active',
                    pending: accommodation.status === 'pending',
                    reinspection: accommodation.status === 'reinspection' || accommodation.isResubmitted,
                    rejected: accommodation.status === 'rejected' || String(accommodation.approvalStatus ?? '').toLowerCase() === 'rejected',
                    inactive: accommodation.status === 'inactive'
                  }"
              >
                {{ getStatusLabel(accommodation.status, accommodation.rejectionReason, accommodation.approvalStatus, accommodation.isResubmitted) }}
              </span>
            </div>

            <p
              v-if="accommodation.status === 'rejected' || String(accommodation.approvalStatus ?? '').toLowerCase() === 'rejected'"
              class="status-reason"
            >
              {{ accommodation.rejectionReason ? `반려 사유: ${accommodation.rejectionReason}` : '반려 사유를 확인할 수 없습니다.' }}
            </p>

            <div class="info-details">
              <span class="detail-item"><span class="detail-icon">📍</span>{{ accommodation.location }}</span>
              <span class="detail-item"><span class="detail-icon">👥</span>최대 {{ accommodation.maxGuests }}명</span>
              <span class="detail-item"><span class="detail-icon">🛏️</span>{{ accommodation.roomCount }}개 객실</span>
            </div>

            <div class="price-actions">
              <div class="price-info">
                <span class="price">₩{{ formatPrice(accommodation.price) }}</span>
                <span class="price-unit">/박</span>
              </div>
              <div class="action-buttons">
                <button class="action-btn edit-btn"
                        @click="$router.push(`/host/accommodation/edit/${accommodation.id}`)">수정
                </button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-else-if="!isLoading && !loadError" class="empty-state">
        <div class="empty-icon">🏠</div>
        <h2>등록된 숙소가 없습니다</h2>
        <p>새 숙소를 등록하여 게스트를 맞이해보세요!</p>
      </div>

      <p v-else-if="isLoading" class="empty-state">숙소 목록을 불러오는 중입니다.</p>
      <p v-else-if="loadError" class="empty-state">{{ loadError }}</p>
    </div>
  </div>
</template>

<style scoped>
/* ✅ 토큰이 있으면 쓰고, 없으면 fallback로 안전하게 */
.accommodation-container {
  padding-bottom: 2rem;
}

.view-header {
  margin-bottom: 1.25rem;
}

.view-header h2 {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--host-title, #0b3b32);
  margin: 0.15rem 0 0.2rem;
  letter-spacing: -0.01em;
}

.subtitle {
  color: var(--text-sub, #6b7280);
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}

.register-btn {
  width: 100%;
  padding: 0.95rem 1rem;
  background: var(--primary, #BFE7DF);
  color: #004d40;
  font-size: 1rem;
  font-weight: 900;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
}

.register-btn:hover {
  background: var(--primary-hover, #A0D1C8);
}

.plus-icon {
  font-size: 1.2rem;
  font-weight: 900;
}

.accommodation-list {
  display: grid;
  gap: 1rem;
}

.accommodation-card {
  background: var(--bg-white, #fff);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border, #e5e7eb);
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
  display: grid;
  grid-template-columns: 1fr;
}

.card-image {
  height: 210px;
  width: 100%;
  overflow: hidden;
}

.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-info {
  padding: 1.15rem 1.15rem 1.2rem;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  justify-content: space-between;
  margin-bottom: 0.6rem;
}

.accommodation-name {
  font-size: 1.12rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
  margin: 0;
}

.clickable {
  cursor: pointer;
}

.clickable:hover {
  opacity: 0.85;
}

.accommodation-name.clickable:hover {
  color: var(--host-accent, #0f766e);
  text-decoration: underline;
}

.status-badge {
  padding: 0.28rem 0.65rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 900;
  border: 1px solid var(--border, #e5e7eb);
  white-space: nowrap;
}

.status-badge.active {
  background: #e0f2f1;
  color: var(--host-accent, #0f766e);
  border-color: #c0e6df;
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
  border-color: #fcd34d;
}

.status-badge.reinspection {
  background: #fde68a;
  color: #92400e;
  border-color: #fbbf24;
}

.status-badge.rejected {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fecaca;
}

.status-badge.inactive {
  background: #f1f5f9;
  color: #475569;
}

.status-reason {
  margin: 0.5rem 0 0.2rem;
  font-size: 0.85rem;
  color: #b91c1c;
  font-weight: 700;
}

.info-details {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem 1rem;
  margin-bottom: 1.1rem;
  color: #374151;
  font-size: 0.92rem;
  font-weight: 700;
}

.detail-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.price-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.price {
  font-size: 1.35rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.price-unit {
  font-size: 0.9rem;
  color: var(--text-sub, #6b7280);
  font-weight: 800;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  border: 1px solid var(--border, #e5e7eb);
  background: white;
  cursor: pointer;
  font-weight: 900;
}

.edit-btn {
  color: var(--host-accent, #0f766e);
}

.edit-btn:hover {
  border-color: #c0e6df;
  background: #f0fcf9;
}

.delete-btn {
  color: #ef4444;
}

.delete-btn:hover {
  border-color: #fee2e2;
  background: #fff5f5;
}

@media (min-width: 768px) {
  .accommodation-card {
    grid-template-columns: 260px 1fr;
  }

  .card-image {
    height: 100%;
  }

  .register-btn {
    width: auto;
  }
}

.empty-state {
  text-align: center;
  padding: 3.25rem 1.5rem;
  background: var(--bg-white, #fff);
  border-radius: 16px;
  border: 1px solid var(--border, #e5e7eb);
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
}

.empty-icon {
  font-size: 3.5rem;
  margin-bottom: 0.8rem;
}

.empty-state h2 {
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
  margin: 0 0 0.35rem;
}

.empty-state p {
  color: var(--text-sub, #6b7280);
  font-size: 0.95rem;
  margin: 0;
}
</style>
