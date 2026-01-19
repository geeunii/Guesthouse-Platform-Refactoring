<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { fetchAdminReportDetail } from '../../api/adminApi'
import AdminBadge from './AdminBadge.vue'

const props = defineProps({
  reportId: {
    type: [Number, String],
    required: true
  },
  isOpen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'action'])

const report = ref(null)
const isLoading = ref(false)
const error = ref(null)
const showContent = ref(false) // 블라인드 콘텐츠 보기 여부

// ESC 키로 닫기
const handleKeydown = (e) => {
  if (e.key === 'Escape' && props.isOpen) {
    emit('close')
  }
}

const loadDetail = async () => {
  if (!props.reportId) return
  isLoading.value = true
  error.value = null
  try {
    const response = await fetchAdminReportDetail(props.reportId)
    if (response.ok && response.data) {
      report.value = response.data
    } else {
      error.value = '신고 상세 정보를 불러오지 못했습니다.'
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

const formatDate = (dateStr) => dateStr ? dateStr.replace('T', ' ').slice(0, 16) : '-'

const resolveStatusVariant = (status) => {
  if (status === 'PROCESSED' || status === 'BLIND') return 'success'
  if (status === 'IGNORED') return 'neutral'
  return 'warning'
}

const resolveStatusLabel = (status) => {
  if (status === 'PROCESSED' || status === 'BLIND') return '처리완료'
  if (status === 'IGNORED') return '반려됨'
  return '대기중'
}

const isPending = computed(() => report.value?.status === 'PENDING' || report.value?.status === 'WAIT')

const handleAction = (actionType) => {
  emit('action', actionType, report.value)
}

const toggleContent = () => {
  showContent.value = !showContent.value
}
</script>

<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">

      <header class="modal-header">
        <div class="header-main">
          <h2 class="modal-title">신고 상세 #{{ reportId }}</h2>
          <AdminBadge
            v-if="report"
            :text="resolveStatusLabel(report.status)"
            :variant="resolveStatusVariant(report.status)"
          />
        </div>
        <button class="close-btn" @click="$emit('close')">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
      </header>

      <div v-if="isLoading" class="modal-body loading">
        <div class="spinner"></div>
        <p>정보를 불러오는 중...</p>
      </div>

      <div v-else-if="error" class="modal-body error">
        <p>{{ error }}</p>
        <button class="retry-btn" @click="loadDetail">다시 시도</button>
      </div>

      <div v-else-if="report" class="modal-body split-layout">

        <!-- 좌측: 신고 정보 -->
        <div class="left-panel">
          <section class="detail-section">
            <h3 class="section-title">신고 개요</h3>
            <dl class="info-list">
              <div class="info-row">
                <dt>신고 사유</dt>
                <dd class="reason-text">{{ report.reason }}</dd>
              </div>
              <div class="info-row">
                <dt>접수 일시</dt>
                <dd>{{ formatDate(report.createdAt) }}</dd>
              </div>
              <div class="info-row">
                <dt>대상 유형</dt>
                <dd><span class="type-badge">{{ report.targetType }}</span></dd>
              </div>
            </dl>
          </section>

          <section class="detail-section">
            <h3 class="section-title">신고자 정보</h3>
            <dl class="info-list">
              <div class="info-row">
                <dt>닉네임</dt>
                <dd>{{ report.reporter?.nickname }}</dd>
              </div>
              <div class="info-row">
                <dt>이메일</dt>
                <dd>{{ report.reporter?.email }}</dd>
              </div>
              <div class="info-row">
                <dt>ID</dt>
                <dd>#{{ report.reporter?.id }}</dd>
              </div>
            </dl>
          </section>

          <section class="detail-section">
            <h3 class="section-title">상세 내용</h3>
            <p class="description-text">{{ report.description || '-' }}</p>
          </section>
        </div>

        <!-- 우측: 증거 자료 (Target Data) -->
        <div class="right-panel">
          <h3 class="section-title">신고 대상 콘텐츠 (증거 자료)</h3>

          <!-- REVIEW Target -->
          <div v-if="report.targetType === 'REVIEW'" class="evidence-box">
            <div class="evidence-header">
              <span class="evidence-label">리뷰 ID: #{{ report.targetId }}</span>
              <span class="evidence-author">작성자: {{ report.targetData?.reviewAuthorName || '알 수 없음' }}</span>
            </div>

            <div class="evidence-content" :class="{ 'blurred': !showContent }">
              <div class="review-rating">
                <span class="star">★</span> {{ report.targetData?.reviewRating?.toFixed(1) }}
              </div>
              <p class="review-text">{{ report.targetData?.reviewContent }}</p>

              <div v-if="report.targetData?.reviewImages?.length" class="review-images">
                <img
                  v-for="(img, idx) in report.targetData.reviewImages"
                  :key="idx"
                  :src="img"
                  alt="리뷰 이미지"
                />
              </div>
            </div>

            <div v-if="!showContent" class="blind-overlay" @click="toggleContent">
              <span class="blind-msg">⚠️ 콘텐츠 확인하기 (클릭)</span>
            </div>
          </div>

          <!-- USER Target (Placeholder) -->
          <div v-else-if="report.targetType === 'USER'" class="evidence-box">
            <p>사용자 프로필 정보가 여기에 표시됩니다.</p>
          </div>

          <!-- ACCOMMODATION Target (Placeholder) -->
          <div v-else-if="report.targetType === 'ACCOMMODATION'" class="evidence-box">
            <p>숙소 정보가 여기에 표시됩니다.</p>
          </div>
        </div>
      </div>

      <footer v-if="report" class="modal-footer">
        <div class="footer-left">
          <button class="btn btn-ghost" @click="$emit('close')">닫기</button>
        </div>
        <div class="footer-right">
          <template v-if="isPending">
            <button class="btn btn-neutral" @click="handleAction('ignore')">신고 반려</button>

            <button
              v-if="report.targetType === 'REVIEW'"
              class="btn btn-danger"
              @click="handleAction('blind')"
            >
              블라인드 처리
            </button>

            <button
              v-if="report.targetType === 'USER' || report.targetType === 'ACCOMMODATION'"
              class="btn btn-danger"
              @click="handleAction('ban')"
            >
              제재 처리
            </button>
          </template>
          <span v-else class="status-text">이미 처리된 신고입니다.</span>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: white;
  width: 90%;
  max-width: 1000px;
  max-height: 85vh;
  border-radius: 12px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: scaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 50;
}

/* Header */
.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}
.close-btn:hover { background: #f1f5f9; color: #0f172a; }

/* Body */
.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  background: #f8fafc;
}

.split-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  height: 100%;
}

.left-panel {
  padding: 24px;
  border-right: 1px solid #e2e8f0;
  background: #fff;
  overflow-y: auto;
}

.right-panel {
  padding: 24px;
  background: #f1f5f9;
  overflow-y: auto;
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
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 0.9rem;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-list {
  display: grid;
  gap: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.95rem;
}

.info-row dt { color: #64748b; font-weight: 500; }
.info-row dd { color: #0f172a; font-weight: 600; text-align: right; }

.reason-text { color: #e11d48 !important; font-weight: 700 !important; }
.type-badge {
  background: #e2e8f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: 700;
}

.description-text {
  font-size: 0.95rem;
  color: #334155;
  line-height: 1.6;
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

/* Evidence Box */
.evidence-box {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  position: relative;
}

.evidence-header {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 600;
}

.evidence-content {
  padding: 20px;
  transition: filter 0.3s;
}

.evidence-content.blurred {
  filter: blur(8px);
  user-select: none;
}

.blind-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  cursor: pointer;
  z-index: 10;
}

.blind-msg {
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 10px 20px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 0.95rem;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.review-rating {
  color: #f59e0b;
  font-weight: 800;
  margin-bottom: 8px;
}

.review-text {
  font-size: 1rem;
  line-height: 1.6;
  color: #1e293b;
  margin-bottom: 16px;
  white-space: pre-line;
}

.review-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
}

.review-images img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

/* Footer */
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  bottom: 0;
  z-index: 10;
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

.btn-neutral { background: #f1f5f9; color: #475569; }
.btn-neutral:hover { background: #e2e8f0; }

.btn-danger { background: #e11d48; color: white; }
.btn-danger:hover { background: #be123c; }

.btn-ghost { background: transparent; color: #64748b; }
.btn-ghost:hover { background: #f1f5f9; color: #0f172a; }

.status-text { color: #64748b; font-weight: 600; }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { opacity: 0; transform: scale(0.98); } to { opacity: 1; transform: scale(1); } }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .split-layout { grid-template-columns: 1fr; }
  .left-panel { border-right: none; border-bottom: 1px solid #e2e8f0; max-height: 300px; }
  .modal-content { width: 95%; height: 90vh; }
}
</style>
