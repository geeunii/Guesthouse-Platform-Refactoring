<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { fetchAdminUserDetail } from '../../api/adminApi'
import AdminBadge from './AdminBadge.vue'

const props = defineProps({
  userId: {
    type: [Number, String],
    required: true
  },
  isOpen: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'action'])

const user = ref(null)
const isLoading = ref(false)
const error = ref(null)

// ESC 키로 닫기
const handleKeydown = (e) => {
  if (e.key === 'Escape' && props.isOpen) {
    emit('close')
  }
}

const loadUserDetail = async () => {
  if (!props.userId) return
  isLoading.value = true
  error.value = null
  try {
    const response = await fetchAdminUserDetail(props.userId)
    if (response.ok && response.data) {
      user.value = response.data
    } else {
      error.value = '회원 정보를 불러오지 못했습니다.'
    }
  } catch (e) {
    error.value = '네트워크 오류가 발생했습니다.'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  if (props.isOpen) {
    loadUserDetail()
    window.addEventListener('keydown', handleKeydown)
    // 모달 오픈 시 body 스크롤 방지
    document.body.style.overflow = 'hidden'
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})

// 데이터 포맷팅 헬퍼
const formatValue = (val) => val ?? '-'
const formatDate = (dateStr) => dateStr ? dateStr.replace('T', ' ').slice(0, 16) : '-'

const resolveRoleLabel = (role) => {
  if (!role) return '-'
  if (role === 'HOST' || role === 'ROLE_HOST') return 'HOST'
  return 'GUEST'
}

const resolveAccountStatus = (u) => (u?.suspended ? '정지' : '정상')
const resolveAccountStatusVariant = (u) => (u?.suspended ? 'danger' : 'success')

const resolveHostStatus = (u) => {
  const role = u?.role
  const approved = u?.hostApproved
  if (role !== 'HOST' && role !== 'ROLE_HOST') return null // 호스트 아님
  if (approved == null) return '승인대기'
  return approved ? '승인' : '반려'
}

const resolveHostVariant = (u) => {
  const status = resolveHostStatus(u)
  if (status === '승인') return 'success'
  if (status === '반려') return 'danger'
  if (status === '승인대기') return 'warning'
  return 'neutral'
}

const resolveLoginType = (u) => {
  if (u?.socialProvider && u?.socialProvider !== 'LOCAL') {
    return 'SOCIAL'
  }
  return 'LOCAL'
}

const resolveProvider = (u) => {
  if (u?.socialProvider && u?.socialProvider !== 'LOCAL') {
    return u.socialProvider
  }
  return '-'
}

// 액션 권한 계산
const canApproveHost = computed(() => resolveHostStatus(user.value) === '승인대기')
const canRejectHost = computed(() => resolveHostStatus(user.value) === '승인대기')
const canSuspend = computed(() => !user.value?.suspended)
const canUnsuspend = computed(() => user.value?.suspended)

const handleAction = (actionType) => {
  emit('action', actionType, user.value)
}
</script>

<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">

      <!-- 2-1. Header 영역 -->
      <header class="modal-header">
        <div class="header-info">
          <h2 class="modal-title">회원 상세</h2>
          <div v-if="user" class="header-sub">
            <span class="header-id">ID: {{ user.userId }}</span>
            <span class="header-divider">·</span>
            <span class="header-email">{{ user.email }}</span>
          </div>
        </div>
        <button class="close-btn" @click="$emit('close')" aria-label="닫기">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
      </header>

      <div v-if="isLoading" class="modal-body loading">
        <div class="spinner"></div>
        <p>정보를 불러오는 중...</p>
      </div>

      <div v-else-if="error" class="modal-body error">
        <p>{{ error }}</p>
        <button class="retry-btn" @click="loadUserDetail">다시 시도</button>
      </div>

      <div v-else-if="user" class="modal-body scrollable">
        <div class="info-grid">

          <!-- 2-2. Section A - 기본 정보 -->
          <section class="info-section">
            <h3 class="section-title">기본 정보</h3>
            <dl class="info-list">
              <div class="info-row">
                <dt>회원 ID</dt>
                <dd>#{{ user.userId }}</dd>
              </div>
              <div class="info-row">
                <dt>이메일</dt>
                <dd>{{ formatValue(user.email) }}</dd>
              </div>
              <div class="info-row">
                <dt>닉네임</dt>
                <dd>{{ formatValue(user.nickname) }}</dd>
              </div>
              <div class="info-row">
                <dt>이름</dt>
                <dd>{{ formatValue(user.name) }}</dd>
              </div>
              <div class="info-row">
                <dt>연락처</dt>
                <dd>{{ formatValue(user.phone) }}</dd>
              </div>
              <div class="info-row">
                <dt>가입일</dt>
                <dd>{{ formatDate(user.createdAt) }}</dd>
              </div>
              <div class="info-row">
                <dt>최근 수정</dt>
                <dd>{{ formatDate(user.updatedAt) }}</dd>
              </div>
            </dl>
          </section>

          <!-- 2-3. Section B - 계정 / 상태 정보 -->
          <section class="info-section">
            <h3 class="section-title">계정 및 상태</h3>
            <dl class="info-list">
              <div class="info-row">
                <dt>계정 유형</dt>
                <dd class="font-bold">{{ resolveRoleLabel(user.role) }}</dd>
              </div>
              <div class="info-row">
                <dt>계정 상태</dt>
                <dd>
                  <AdminBadge :text="resolveAccountStatus(user)" :variant="resolveAccountStatusVariant(user)" />
                </dd>
              </div>
              <div class="info-row" v-if="resolveHostStatus(user)">
                <dt>호스트 상태</dt>
                <dd>
                  <AdminBadge :text="resolveHostStatus(user)" :variant="resolveHostVariant(user)" />
                </dd>
              </div>
            </dl>
          </section>

          <!-- 2-4. Section C - 로그인 방식 정보 -->
          <section class="info-section">
            <h3 class="section-title">로그인 정보</h3>
            <dl class="info-list">
              <div class="info-row">
                <dt>로그인 유형</dt>
                <dd>{{ resolveLoginType(user) }}</dd>
              </div>
              <div class="info-row">
                <dt>소셜 제공자</dt>
                <dd>{{ resolveProvider(user) }}</dd>
              </div>
              <div class="info-row">
                <dt>가입 완료 여부</dt>
                <dd>
                  <span v-if="user.socialSignupCompleted === true" class="text-success">완료</span>
                  <span v-else-if="user.socialSignupCompleted === false" class="text-muted">미완료</span>
                  <span v-else>-</span>
                </dd>
              </div>
            </dl>
          </section>

          <!-- 2-5. Section D - 정책 / 운영 정보 -->
          <section class="info-section">
            <h3 class="section-title">정책 및 운영</h3>
            <dl class="info-list">
              <div class="info-row">
                <dt>마케팅 수신 동의</dt>
                <dd>
                  <span :class="user.marketingAgreed ? 'text-success' : 'text-muted'">
                    {{ user.marketingAgreed ? 'Y' : 'N' }}
                  </span>
                </dd>
              </div>
            </dl>
          </section>
        </div>
      </div>

      <!-- 3. 액션 버튼 영역 -->
      <footer v-if="user" class="modal-footer">
        <div class="action-group left">
           <!-- 공통 액션 -->
          <button
            v-if="canSuspend"
            class="btn btn-danger-outline"
            @click="handleAction('suspend')"
          >
            계정 정지
          </button>
          <button
            v-if="canUnsuspend"
            class="btn btn-primary-outline"
            @click="handleAction('unsuspend')"
          >
            정지 해제
          </button>
        </div>

        <div class="action-group right">
          <!-- HOST 전용 액션 -->
          <template v-if="canApproveHost">
            <button class="btn btn-danger" @click="handleAction('rejectHost')">
              반려
            </button>
            <button class="btn btn-primary" @click="handleAction('approveHost')">
              승인
            </button>
          </template>

          <!-- 닫기 버튼 (하단에도 배치하여 편의성 증대) -->
          <button class="btn btn-ghost" @click="$emit('close')">
            닫기
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.65); /* Dim 처리 강화 */
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
  animation: fadeIn 0.2s ease-out;
}

.modal-content {
  background: white;
  width: min(640px, 92vw);
  max-height: 90vh;
  border-radius: 12px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: scaleIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 2-1. Header */
.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  background: #fff;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.modal-title {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
}

.header-sub {
  font-size: 0.875rem;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-id {
  font-weight: 600;
  color: #475569;
}

.header-divider {
  color: #cbd5e1;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  margin-top: -4px;
  margin-right: -4px;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

/* Body */
.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.loading, .error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  gap: 16px;
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

/* Grid Layout */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px 24px; /* 세로 간격 넓게 */
}

.info-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-title {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 8px;
}

.info-list {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.925rem;
}

.info-row dt {
  color: #64748b;
  font-weight: 500;
}

.info-row dd {
  margin: 0;
  color: #0f172a;
  font-weight: 600;
  text-align: right;
}

.font-bold {
  font-weight: 700 !important;
}

.text-success { color: #059669; font-weight: 700; }
.text-muted { color: #94a3b8; }

/* Footer & Actions */
.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-group {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 9px 16px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-primary {
  background: #0f766e;
  color: white;
}
.btn-primary:hover { background: #0d655d; }

.btn-primary-outline {
  background: white;
  border-color: #0f766e;
  color: #0f766e;
}
.btn-primary-outline:hover { background: #f0fdfa; }

.btn-danger {
  background: #e11d48;
  color: white;
}
.btn-danger:hover { background: #be123c; }

.btn-danger-outline {
  background: white;
  border-color: #e11d48;
  color: #e11d48;
}
.btn-danger-outline:hover { background: #fff1f2; }

.btn-ghost {
  background: transparent;
  color: #64748b;
}
.btn-ghost:hover {
  background: #e2e8f0;
  color: #0f172a;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.98); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .modal-footer {
    flex-direction: column-reverse;
    gap: 12px;
  }

  .action-group {
    width: 100%;
    justify-content: stretch;
  }

  .btn {
    flex: 1;
  }
}
</style>
