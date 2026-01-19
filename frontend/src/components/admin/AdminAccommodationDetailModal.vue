<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { fetchAdminAccommodationDetail } from '../../api/adminApi'
import AdminBadge from './AdminBadge.vue'

const props = defineProps({
  accommodationId: {
    type: [Number, String],
    required: true
  },
  isOpen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'action'])

const accommodation = ref(null)
const isLoading = ref(false)
const error = ref(null)
const activeTab = ref('content') // content, rooms, stats

// Lightbox 상태
const lightboxOpen = ref(false)
const lightboxImage = ref('')
const lightboxIndex = ref(0)

// ESC 키로 닫기
const handleKeydown = (e) => {
  if (e.key === 'Escape') {
    if (lightboxOpen.value) {
      closeLightbox()
    } else if (props.isOpen) {
      emit('close')
    }
  }
  // Lightbox 화살표 이동
  if (lightboxOpen.value) {
    if (e.key === 'ArrowLeft') prevImage()
    if (e.key === 'ArrowRight') nextImage()
  }
}

const loadDetail = async () => {
  if (!props.accommodationId) return
  isLoading.value = true
  error.value = null
  try {
    const response = await fetchAdminAccommodationDetail(props.accommodationId)
    if (response.ok && response.data) {
      accommodation.value = response.data
    } else {
      error.value = '숙소 정보를 불러오지 못했습니다.'
    }
  } catch (e) {
    error.value = '네트워크 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  if (props.isOpen) {
    loadDetail()
    window.addEventListener('keydown', handleKeydown)
    document.body.style.overflow = 'hidden'
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})

const formatPrice = (price) => price ? `${price.toLocaleString()}원` : '-'
const formatDate = (dateStr) => dateStr ? dateStr.replace('T', ' ').slice(0, 16) : '-'

const resolveStatusVariant = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const resolveStatusLabel = (status) => {
  if (status === 'APPROVED') return '승인됨'
  if (status === 'REJECTED') return '반려됨'
  return '승인대기'
}

const canApprove = computed(() => accommodation.value?.info?.status === 'PENDING')
const canReject = computed(() => accommodation.value?.info?.status === 'PENDING')
const isApproved = computed(() => accommodation.value?.info?.status === 'APPROVED')
const isRejected = computed(() => accommodation.value?.info?.status === 'REJECTED')

const handleAction = (actionType) => {
  // accommodation 전체 객체를 넘겨서 처리 (ID는 info.id에 있음)
  const target = {
    ...accommodation.value,
    accommodationsId: accommodation.value.info.id,
    name: accommodation.value.info.name
  }
  emit('action', actionType, target)
}

// Lightbox 기능
const openLightbox = (imgUrl, index) => {
  lightboxImage.value = imgUrl
  lightboxIndex.value = index
  lightboxOpen.value = true
}

const closeLightbox = () => {
  lightboxOpen.value = false
  lightboxImage.value = ''
}

const prevImage = () => {
  const images = accommodation.value?.content?.images || []
  if (images.length <= 1) return
  lightboxIndex.value = (lightboxIndex.value - 1 + images.length) % images.length
  lightboxImage.value = images[lightboxIndex.value]
}

const nextImage = () => {
  const images = accommodation.value?.content?.images || []
  if (images.length <= 1) return
  lightboxIndex.value = (lightboxIndex.value + 1) % images.length
  lightboxImage.value = images[lightboxIndex.value]
}
</script>

<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">

      <!-- A. Header (Sticky) -->
      <header class="modal-header">
        <div class="header-main">
          <div class="header-top">
            <h2 class="modal-title">{{ accommodation?.info?.name || '숙소 상세' }}</h2>
            <AdminBadge
              v-if="accommodation"
              :text="resolveStatusLabel(accommodation.info.status)"
              :variant="resolveStatusVariant(accommodation.info.status)"
            />
          </div>
          <div v-if="accommodation" class="header-sub">
            <span class="category-badge">{{ accommodation.info.type }}</span>
            <span class="divider">·</span>
            <span class="host-info">ID: #{{ accommodation.info.id }}</span>
          </div>
        </div>
        <button class="close-btn" @click="$emit('close')">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
      </header>

      <!-- Tabs (Sticky) -->
      <div class="modal-tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'content' }"
          @click="activeTab = 'content'"
        >
          숙소 정보
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'rooms' }"
          @click="activeTab = 'rooms'"
        >
          객실 목록 ({{ accommodation?.rooms?.length || 0 }})
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'stats' }"
          @click="activeTab = 'stats'"
        >
          운영 지표
        </button>
      </div>

      <div v-if="isLoading" class="modal-body loading">
        <div class="spinner"></div>
        <p>정보를 불러오는 중...</p>
      </div>

      <div v-else-if="error" class="modal-body error">
        <p>{{ error }}</p>
        <button class="retry-btn" @click="loadDetail">다시 시도</button>
      </div>

      <div v-else-if="accommodation" class="modal-body scrollable">

        <!-- B. Section 1 - 숙소 상세 콘텐츠 -->
        <div v-show="activeTab === 'content'" class="tab-content">
          <!-- 이미지 갤러리 -->
          <section class="detail-section">
            <h3 class="section-title">이미지 ({{ accommodation.content.images?.length || 0 }})</h3>
            <div v-if="accommodation.content.images?.length" class="image-grid">
              <div
                v-for="(img, idx) in accommodation.content.images"
                :key="idx"
                class="image-item"
                @click="openLightbox(img, idx)"
              >
                <img :src="img" alt="숙소 이미지" loading="lazy" />
                <div class="zoom-overlay">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line><line x1="11" y1="8" x2="11" y2="14"></line><line x1="8" y1="11" x2="14" y2="11"></line></svg>
                </div>
              </div>
            </div>
            <p v-else class="empty-text">등록된 이미지가 없습니다.</p>
          </section>

          <div class="info-grid-2col">
            <!-- 숙소 기본 정보 -->
            <section class="detail-section">
              <h3 class="section-title">숙소 정보</h3>
              <dl class="info-list">
                <div class="info-row">
                  <dt>주소</dt>
                  <dd>{{ accommodation.info.address }} {{ accommodation.info.addressDetail }}</dd>
                </div>
                <div class="info-row">
                  <dt>대표 번호</dt>
                  <dd>{{ accommodation.info.contact || '-' }}</dd>
                </div>
                <div class="info-row">
                  <dt>체크인/아웃</dt>
                  <dd>{{ accommodation.info.checkIn }} / {{ accommodation.info.checkOut }}</dd>
                </div>
                <div class="info-row">
                  <dt>사업자번호</dt>
                  <dd>{{ accommodation.info.bizNumber }}</dd>
                </div>
              </dl>
            </section>

            <!-- 호스트 정보 (관리자용) -->
            <section class="detail-section host-section">
              <h3 class="section-title">호스트 정보 (관리자용)</h3>
              <dl class="info-list">
                <div class="info-row">
                  <dt>이름</dt>
                  <dd>{{ accommodation.host.name }}</dd>
                </div>
                <div class="info-row">
                  <dt>연락처</dt>
                  <dd class="highlight-text">{{ accommodation.host.phone }}</dd>
                </div>
                <div class="info-row">
                  <dt>이메일</dt>
                  <dd>{{ accommodation.host.email }}</dd>
                </div>
                <div class="info-row">
                  <dt>회원 ID</dt>
                  <dd>#{{ accommodation.host.hostId }}</dd>
                </div>
              </dl>
            </section>
          </div>

          <!-- 소개글 -->
          <section class="detail-section">
            <h3 class="section-title">소개글</h3>
            <div class="description-box">
              <p class="short-desc">{{ accommodation.content.shortDescription }}</p>
              <hr class="desc-divider" />
              <p class="full-desc">{{ accommodation.content.description }}</p>
            </div>
          </section>

          <!-- 편의시설 -->
          <section class="detail-section">
            <h3 class="section-title">편의시설</h3>
            <div class="amenity-tags">
              <span v-for="amenity in accommodation.content.amenities" :key="amenity" class="amenity-tag">
                {{ amenity }}
              </span>
              <span v-if="!accommodation.content.amenities?.length" class="empty-text">등록된 편의시설이 없습니다.</span>
            </div>
          </section>
        </div>

        <!-- C. Section 2 - 객실 목록 -->
        <div v-show="activeTab === 'rooms'" class="tab-content">
          <div v-if="accommodation.rooms?.length" class="room-list">
            <div v-for="room in accommodation.rooms" :key="room.roomId" class="room-card">
              <div class="room-image">
                <img :src="room.mainImageUrl || '/placeholder-room.jpg'" alt="객실 이미지" />
              </div>
              <div class="room-info">
                <div class="room-header">
                  <h4 class="room-name">{{ room.roomName }}</h4>
                  <span class="room-price">{{ formatPrice(room.price) }} / 1박</span>
                </div>
                <div class="room-specs">
                  <span>기준 {{ room.minGuests }}인 (최대 {{ room.maxGuests }}인)</span>
                  <span class="divider">|</span>
                  <span>{{ room.roomType || '일반' }}</span>
                  <span class="divider">|</span>
                  <span>침대 {{ room.bedCount }}개</span>
                  <span class="divider">|</span>
                  <span>욕실 {{ room.bathroomCount }}개</span>
                </div>
                <p class="room-desc">{{ room.roomDescription }}</p>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <p>등록된 객실이 없습니다.</p>
          </div>
        </div>

        <!-- D. Section 3 - 운영 지표 -->
        <div v-show="activeTab === 'stats'" class="tab-content">
          <div class="stats-grid">
            <div class="stat-card">
              <span class="stat-label">평점</span>
              <span class="stat-value">{{ accommodation.stats.rating?.toFixed(1) || '0.0' }}</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">리뷰 수</span>
              <span class="stat-value">{{ accommodation.stats.reviewCount || 0 }}개</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">최저가</span>
              <span class="stat-value">{{ formatPrice(accommodation.stats.minPrice) }}</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">등록일</span>
              <span class="stat-value text-sm">{{ formatDate(accommodation.info.createdAt) }}</span>
            </div>
          </div>

          <div v-if="isRejected" class="rejection-info">
            <h4 class="rejection-title">반려 사유</h4>
            <p class="rejection-reason">{{ accommodation.info.rejectionReason }}</p>
          </div>
        </div>

      </div>

      <!-- Footer - 액션 버튼 (Sticky) -->
      <footer v-if="accommodation" class="modal-footer">
        <div class="footer-left">
          <button class="btn btn-ghost" @click="$emit('close')">닫기</button>
        </div>
        <div class="footer-right">
          <template v-if="canApprove">
            <button class="btn btn-danger-outline" @click="handleAction('reject')">반려</button>
            <button class="btn btn-primary" @click="handleAction('approve')">승인</button>
          </template>
          <template v-if="isApproved">
            <button class="btn btn-danger" @click="handleAction('suspend')">판매 중지</button>
          </template>
          <template v-if="isRejected">
            <span class="status-text danger">이미 반려된 숙소입니다.</span>
          </template>
        </div>
      </footer>
    </div>

    <!-- Lightbox Modal -->
    <div v-if="lightboxOpen" class="lightbox-overlay" @click.self="closeLightbox">
      <button class="lightbox-close" @click="closeLightbox">&times;</button>
      <button class="lightbox-nav prev" @click.stop="prevImage">&lt;</button>
      <div class="lightbox-content">
        <img :src="lightboxImage" alt="확대 이미지" />
        <div class="lightbox-counter">{{ lightboxIndex + 1 }} / {{ accommodation.content.images.length }}</div>
      </div>
      <button class="lightbox-nav next" @click.stop="nextImage">&gt;</button>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.75); /* Dim 처리 강화 */
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: white;
  width: min(900px, 95vw);
  max-height: 85vh; /* 화면 높이의 85% 제한 */
  border-radius: 12px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: scaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative; /* Sticky positioning context */
}

/* Header */
.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #fff;
  flex-shrink: 0; /* 크기 고정 */
}

.header-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
}

.header-sub {
  font-size: 0.9rem;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-badge {
  background: #f1f5f9;
  color: #475569;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 600;
}

.divider { color: #cbd5e1; }

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}
.close-btn:hover { background: #f1f5f9; color: #0f172a; }

/* Tabs */
.modal-tabs {
  display: flex;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 24px;
  background: #fff;
  flex-shrink: 0; /* 크기 고정 */
}

.tab-btn {
  padding: 12px 16px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: #64748b;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover { color: #0f172a; }
.tab-btn.active {
  color: #0f766e;
  border-bottom-color: #0f766e;
}

/* Body (Scrollable) */
.modal-body {
  flex: 1;
  overflow-y: auto; /* 내부 스크롤 */
  padding: 24px;
  background: #f8fafc;
}

.loading, .error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #64748b;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e2e8f0;
  border-top-color: #0f766e;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* Content Sections */
.detail-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.section-title {
  margin: 0 0 16px;
  font-size: 1rem;
  font-weight: 700;
  color: #334155;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

/* Image Grid */
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
}

.image-item {
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  position: relative;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.image-item:hover img {
  transform: scale(1.05);
}

.zoom-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: white;
}

.image-item:hover .zoom-overlay {
  opacity: 1;
}

/* Info Grid (2 Columns) */
.info-grid-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.host-section {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

/* Info List */
.info-list {
  display: grid;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.95rem;
}

.info-row dt { color: #64748b; font-weight: 500; }
.info-row dd { color: #0f172a; font-weight: 600; text-align: right; }

.highlight-text { color: #0f766e; font-weight: 700; }

/* Description */
.description-box {
  font-size: 0.95rem;
  color: #334155;
  line-height: 1.6;
}

.short-desc { font-weight: 600; color: #0f172a; }
.desc-divider { border: 0; border-top: 1px dashed #e2e8f0; margin: 12px 0; }
.full-desc { white-space: pre-line; }

/* Amenities */
.amenity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.amenity-tag {
  background: #f1f5f9;
  color: #475569;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
}

/* Room List */
.room-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.room-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  border: 1px solid #e2e8f0;
}

.room-image {
  width: 160px;
  height: 120px;
  flex-shrink: 0;
}

.room-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.room-info {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.room-name {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #0f172a;
}

.room-price {
  font-weight: 700;
  color: #0f766e;
}

.room-specs {
  font-size: 0.85rem;
  color: #64748b;
  margin-bottom: 8px;
}

.room-desc {
  margin: 0;
  font-size: 0.9rem;
  color: #475569;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.stat-label { font-size: 0.9rem; color: #64748b; font-weight: 600; }
.stat-value { font-size: 1.5rem; font-weight: 800; color: #0f172a; }
.text-sm { font-size: 1rem; }

.rejection-info {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 16px;
}

.rejection-title { margin: 0 0 8px; color: #991b1b; font-size: 0.95rem; }
.rejection-reason { margin: 0; color: #b91c1c; font-size: 0.9rem; }

/* Footer */
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0; /* 크기 고정 */
}

.footer-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn {
  padding: 10px 16px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  border: 1px solid transparent;
}

.btn-primary { background: #0f766e; color: white; }
.btn-primary:hover { background: #0d655d; }

.btn-danger { background: #e11d48; color: white; }
.btn-danger:hover { background: #be123c; }

.btn-danger-outline { background: white; border-color: #e11d48; color: #e11d48; }
.btn-danger-outline:hover { background: #fff1f2; }

.btn-ghost { background: transparent; color: #64748b; }
.btn-ghost:hover { background: #f1f5f9; color: #0f172a; }

.status-text.danger { color: #e11d48; font-weight: 600; font-size: 0.9rem; }
.empty-text { color: #94a3b8; font-size: 0.9rem; font-style: italic; }
.empty-state { text-align: center; padding: 40px; color: #94a3b8; }

/* Lightbox */
.lightbox-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.9);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lightbox-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.lightbox-content img {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
}

.lightbox-close {
  position: absolute;
  top: 20px;
  right: 20px;
  background: none;
  border: none;
  color: white;
  font-size: 2rem;
  cursor: pointer;
  z-index: 210;
}

.lightbox-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: white;
  font-size: 2rem;
  padding: 20px;
  cursor: pointer;
  z-index: 210;
}

.lightbox-nav:hover {
  background: rgba(255, 255, 255, 0.2);
}

.lightbox-nav.prev { left: 20px; }
.lightbox-nav.next { right: 20px; }

.lightbox-counter {
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  color: white;
  font-size: 0.9rem;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { opacity: 0; transform: scale(0.98); } to { opacity: 1; transform: scale(1); } }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .info-grid-2col { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: 1fr; }
  .room-card { flex-direction: column; }
  .room-image { width: 100%; height: 180px; }
}
</style>
