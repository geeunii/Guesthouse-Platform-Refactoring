<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import AdminUserDetailModal from '../../components/admin/AdminUserDetailModal.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminUsers, fetchAdminUserSummary, approveHost, rejectHost, suspendUser, unsuspendUser } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'

const stats = ref([])
const users = ref([])
const searchQuery = ref('')
const typeFilter = ref('all')
const accountStatusFilter = ref('all')
const hostStatusFilter = ref('all')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const route = useRoute()
const router = useRouter()
const summary = ref(null)
const toast = ref(null)

// 상세 모달 상태
const detailModal = ref({
  isOpen: false,
  userId: null
})

// 액션 모달 상태 (승인, 반려, 정지 등)
const actionModal = ref({
  open: false,
  title: '',
  description: '',
  confirmLabel: '',
  reasonLabel: '',
  reasonPlaceholder: '',
  reasonRequired: false,
  actionType: '',
  user: null,
  reason: ''
})
const isSubmitting = ref(false)
const reasonError = ref('')

const loadUsers = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminUsers({
    role: typeFilter.value === 'all' ? undefined : typeFilter.value,
    accountStatus: accountStatusFilter.value === 'all' ? undefined : accountStatusFilter.value,
    hostStatus: hostStatusFilter.value === 'all' ? undefined : hostStatusFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value,
    size: size.value,
    sort: 'latest'
  })
  if (response.ok && response.data) {
    const payload = response.data
    users.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
    if (!summary.value) {
      await loadSummary()
    }
  } else {
    loadError.value = '회원 목록을 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const loadSummary = async () => {
  const response = await fetchAdminUserSummary()
  if (response.ok && response.data) {
    summary.value = response.data
    stats.value = [
      { label: '전체 사용자', value: `${response.data.totalUsers ?? 0}명`, sub: '전체 계정 기준', tone: 'primary' },
      { label: '게스트', value: `${response.data.totalGuests ?? 0}명`, sub: '전체 기준', tone: 'success' },
      { label: '호스트', value: `${response.data.totalHosts ?? 0}명`, sub: '전체 기준', tone: 'accent' },
      { label: '호스트 승인대기', value: `${response.data.pendingHostApprovals ?? 0}명`, sub: '승인 대기', tone: 'warning' },
      { label: '정지', value: `${response.data.suspendedUsers ?? 0}명`, sub: '계정 정지', tone: 'neutral' }
    ]
  }
}

const syncQuery = (next) => {
  const params = { ...route.query, ...next }
  const normalized = toQueryParams(params)
  const current = toQueryParams(route.query)
  const isSame = Object.keys({ ...normalized, ...current })
    .every((key) => String(normalized[key] ?? '') === String(current[key] ?? ''))
  if (!isSame) {
    router.replace({ query: normalized })
  }
}

onMounted(() => {
  typeFilter.value = route.query.role ?? 'all'
  accountStatusFilter.value = route.query.accountStatus ?? 'all'
  hostStatusFilter.value = route.query.hostStatus ?? 'all'
  searchQuery.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  loadSummary()
  loadUsers()
})

watch([searchQuery, typeFilter, accountStatusFilter, hostStatusFilter], () => {
  page.value = 0
  syncQuery({
    role: typeFilter.value,
    accountStatus: accountStatusFilter.value,
    hostStatus: hostStatusFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value
  })
  loadUsers()
})

watch(
  () => actionModal.value.reason,
  () => {
    if (reasonError.value) {
      reasonError.value = ''
    }
  }
)

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const rows = users.value.map((user) => ({
    id: user.userId ?? '-',
    email: user.email ?? '-',
    phone: user.phone ?? '-',
    role: resolveRoleLabel(user.role),
    joinedAt: user.createdAt?.slice?.(0, 10) ?? '-',
    accountStatus: resolveAccountStatus(user),
    hostStatus: resolveHostStatus(user)
  }))
  const sheets = [
    {
      name: '회원 목록',
      columns: [
        { key: 'id', label: 'ID' },
        { key: 'email', label: '이메일' },
        { key: 'phone', label: '연락처' },
        { key: 'role', label: '유형' },
        { key: 'joinedAt', label: '가입일' },
        { key: 'accountStatus', label: '계정 상태' },
        { key: 'hostStatus', label: '호스트 상태' }
      ],
      rows
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-users-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-users-${today}.csv`, sheets })
}

const resolveRoleLabel = (role) => {
  if (!role) return '-'
  if (role === 'HOST' || role === 'ROLE_HOST') return '호스트'
  return '게스트'
}

const resolveAccountStatus = (user) => (user?.suspended ? '정지' : '정상')

const resolveAccountStatusVariant = (user) => (user?.suspended ? 'danger' : 'success')

const resolveHostStatus = (user) => {
  const role = user?.role
  const approved = user?.hostApproved
  if (role !== 'HOST' && role !== 'ROLE_HOST') return '해당없음'
  if (approved == null) return '승인대기'
  return approved ? '승인' : '반려'
}

const resolveHostVariant = (user) => {
  const status = resolveHostStatus(user)
  if (status === '승인') return 'success'
  if (status === '반려') return 'danger'
  if (status === '승인대기') return 'warning'
  return 'neutral'
}

const canApproveHost = (user) => resolveHostStatus(user) === '승인대기'
const canRejectHost = (user) => resolveHostStatus(user) === '승인대기'
const canSuspend = (user) => !user?.suspended
const canUnsuspend = (user) => user?.suspended

const showToast = (message, tone = 'success') => {
  toast.value = { message, tone }
  setTimeout(() => {
    toast.value = null
  }, 2200)
}

const openDetailModal = (user) => {
  detailModal.value = {
    isOpen: true,
    userId: user.userId
  }
}

const closeDetailModal = () => {
  detailModal.value = {
    isOpen: false,
    userId: null
  }
}

const handleDetailAction = (actionType, user) => {
  closeDetailModal()
  // 약간의 지연을 주어 모달 전환이 자연스럽게 보이도록 함
  setTimeout(() => {
    openActionModal(actionType, user)
  }, 100)
}

const openActionModal = (actionType, user) => {
  const config = {
    approveHost: {
      title: '호스트 승인을 진행할까요?',
      confirmLabel: '승인',
      description: '승인 후 호스트 기능이 활성화됩니다.',
      reasonLabel: '메모(선택)',
      reasonPlaceholder: '예) 서류 확인 완료',
      reasonRequired: false
    },
    rejectHost: {
      title: '호스트 신청을 반려할까요?',
      confirmLabel: '반려',
      description: '반려 사유는 호스트에게 안내될 수 있습니다.',
      reasonLabel: '반려 사유(필수)',
      reasonPlaceholder: '예) 사업자 등록증 확인 불가 / 연락처 불명확',
      reasonRequired: true
    },
    suspend: {
      title: '회원을 정지할까요?',
      confirmLabel: '정지',
      description: '정지 시 로그인 및 주요 기능 이용이 제한됩니다.',
      reasonLabel: '정지 사유(필수)',
      reasonPlaceholder: '예) 부정 이용 의심(다중 계정/허위 예약) / 신고 누적',
      reasonRequired: true
    },
    unsuspend: {
      title: '정지를 해제할까요?',
      confirmLabel: '해제',
      description: '해제 후 정상적으로 서비스를 이용할 수 있습니다.',
      reasonLabel: '메모(선택)',
      reasonPlaceholder: '예) 소명 확인 완료 / 오탐 해제',
      reasonRequired: false
    }
  }[actionType]
  if (!config) return
  actionModal.value = {
    open: true,
    title: config.title,
    description: config.description,
    confirmLabel: config.confirmLabel,
    reasonLabel: config.reasonLabel ?? '',
    reasonPlaceholder: config.reasonPlaceholder ?? '',
    reasonRequired: config.reasonRequired ?? false,
    actionType,
    user,
    reason: ''
  }
  reasonError.value = ''
}

const closeActionModal = () => {
  actionModal.value = {
    open: false,
    title: '',
    description: '',
    confirmLabel: '',
    reasonLabel: '',
    reasonPlaceholder: '',
    reasonRequired: false,
    actionType: '',
    user: null,
    reason: ''
  }
  reasonError.value = ''
}

const handleAction = async () => {
  const { actionType, user, reason } = actionModal.value
  if (!user) return
  const trimmedReason = reason.trim()
  const requiresReason = actionType === 'rejectHost' || actionType === 'suspend'
  if (requiresReason && trimmedReason.length < 5) {
    reasonError.value = actionType === 'rejectHost'
      ? '반려 사유를 5자 이상 입력해 주세요.'
      : '정지 사유를 5자 이상 입력해 주세요.'
    return
  }
  if (!requiresReason && trimmedReason.length > 0 && trimmedReason.length < 5) {
    reasonError.value = '메모는 5자 이상 입력해 주세요.'
    return
  }
  if (trimmedReason.length > 200) {
    reasonError.value = '사유는 200자 이내로 입력해 주세요.'
    return
  }
  reasonError.value = ''

  isSubmitting.value = true
  let response = null
  const reasonPayload = trimmedReason.length > 0 ? trimmedReason : null
  if (actionType === 'approveHost') response = await approveHost(user.userId, reasonPayload)
  if (actionType === 'rejectHost') response = await rejectHost(user.userId, trimmedReason)
  if (actionType === 'suspend') response = await suspendUser(user.userId, trimmedReason)
  if (actionType === 'unsuspend') response = await unsuspendUser(user.userId, reasonPayload)
  isSubmitting.value = false
  if (response?.ok) {
    const successMessage = {
      approveHost: '호스트가 승인되었습니다.',
      rejectHost: '호스트 신청이 반려되었습니다.',
      suspend: '회원이 정지되었습니다.',
      unsuspend: '정지가 해제되었습니다.'
    }[actionType]
    showToast(successMessage || '처리가 완료되었습니다.')
    closeActionModal()
    await loadSummary()
    await loadUsers()
  } else {
    showToast('처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.', 'danger')
  }
}

const changePage = (nextPage) => {
  page.value = nextPage
  syncQuery({
    role: typeFilter.value,
    accountStatus: accountStatusFilter.value,
    hostStatus: hostStatusFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value
  })
  loadUsers()
}

</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">회원 관리</h1>
        <p class="admin-subtitle">게스트/호스트 현황과 상태를 확인합니다.</p>
      </div>
    </header>

    <div class="admin-grid">
      <AdminStatCard
        v-for="card in stats"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :sub="card.sub"
        :tone="card.tone"
      />
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadUsers">다시 시도</button>
      </div>
    </div>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="searchQuery"
          class="admin-input"
          type="search"
          placeholder="이메일, 연락처"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">필터</span>
        <select v-model="typeFilter" class="admin-select">
          <option value="all">전체 유형</option>
          <option value="USER">게스트</option>
          <option value="HOST">호스트</option>
        </select>
        <select v-model="accountStatusFilter" class="admin-select">
          <option value="all">계정 전체</option>
          <option value="ACTIVE">정상</option>
          <option value="SUSPENDED">정지</option>
        </select>
        <select v-model="hostStatusFilter" class="admin-select">
          <option value="all">호스트 전체</option>
          <option value="PENDING">승인대기</option>
          <option value="APPROVED">승인</option>
          <option value="REJECTED">반려</option>
          <option value="NONE">해당없음</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <details class="admin-dropdown">
          <summary class="admin-btn admin-btn--ghost">다운로드</summary>
          <div class="admin-dropdown__menu">
            <button class="admin-btn admin-btn--ghost admin-dropdown__item" type="button" @click="downloadReport('csv')">
              CSV
            </button>
            <button class="admin-btn admin-btn--primary admin-dropdown__item" type="button" @click="downloadReport('xlsx')">
              XLSX
            </button>
          </div>
        </details>
      </div>
    </div>

    <AdminTableCard title="회원 목록">
      <div class="admin-table-meta">총 {{ totalElements.toLocaleString() }}명 (필터 적용)</div>
      <table class="admin-table--nowrap admin-table--tight admin-table--stretch">
        <colgroup>
          <col style="width:90px"/>
          <col style="width:260px"/>
          <col style="width:150px"/>
          <col style="width:90px"/>
          <col style="width:120px"/>
          <col style="width:120px"/>
          <col style="width:120px"/>
          <col style="width:110px"/>
        </colgroup>
        <thead>
          <tr>
            <th class="admin-align-center">ID</th>
            <th class="admin-align-left">이메일</th>
            <th class="admin-align-center">연락처</th>
            <th class="admin-align-center">유형</th>
            <th class="admin-align-center">가입일</th>
            <th class="admin-align-center">계정 상태</th>
            <th class="admin-align-center">호스트 상태</th>
            <th class="admin-align-center">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.userId">
            <td class="admin-strong admin-align-center">#{{ user.userId }}</td>
            <td class="admin-strong admin-ellipsis admin-align-left" :title="user.email">{{ user.email }}</td>
            <td class="admin-align-center">{{ user.phone ?? '-' }}</td>
            <td class="admin-align-center">{{ resolveRoleLabel(user.role) }}</td>
            <td class="admin-align-center">{{ user.createdAt?.slice?.(0, 10) ?? '-' }}</td>
            <td class="admin-align-center">
              <AdminBadge :text="resolveAccountStatus(user)" :variant="resolveAccountStatusVariant(user)" />
            </td>
            <td class="admin-align-center">
              <AdminBadge :text="resolveHostStatus(user)" :variant="resolveHostVariant(user)" />
            </td>
            <td class="admin-align-center">
              <details class="admin-dropdown admin-dropdown--table">
                <summary class="admin-btn admin-btn--ghost">관리</summary>
                <div class="admin-dropdown__menu">
                  <button class="admin-btn admin-btn--ghost admin-dropdown__item" type="button" @click="openDetailModal(user)">
                    상세 보기
                  </button>
                  <button
                    v-if="canApproveHost(user)"
                    class="admin-btn admin-btn--ghost admin-dropdown__item"
                    type="button"
                    @click="openActionModal('approveHost', user)"
                  >
                    호스트 승인
                  </button>
                  <button
                    v-if="canRejectHost(user)"
                    class="admin-btn admin-btn--ghost admin-dropdown__item"
                    type="button"
                    @click="openActionModal('rejectHost', user)"
                  >
                    호스트 반려
                  </button>
                  <button
                    v-if="canSuspend(user)"
                    class="admin-btn admin-btn--ghost admin-dropdown__item"
                    type="button"
                    @click="openActionModal('suspend', user)"
                  >
                    계정 정지
                  </button>
                  <button
                    v-if="canUnsuspend(user)"
                    class="admin-btn admin-btn--ghost admin-dropdown__item"
                    type="button"
                    @click="openActionModal('unsuspend', user)"
                  >
                    정지 해제
                  </button>
                </div>
              </details>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!isLoading && !loadError && !users.length" class="admin-status">데이터가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="changePage(page - 1)">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="changePage(page + 1)">
          다음
        </button>
      </div>
    </AdminTableCard>

    <!-- 상세 모달 -->
    <AdminUserDetailModal
      v-if="detailModal.isOpen"
      :user-id="detailModal.userId"
      :is-open="detailModal.isOpen"
      @close="closeDetailModal"
      @action="handleDetailAction"
    />

    <!-- 액션 모달 -->
    <div v-if="actionModal.open" class="admin-modal" @click.self="closeActionModal">
      <div class="admin-modal__content">
        <div class="admin-modal__head">
          <h3>{{ actionModal.title }}</h3>
          <button class="admin-btn admin-btn--ghost" type="button" @click="closeActionModal">닫기</button>
        </div>
        <div class="admin-modal__body">
          <p v-if="actionModal.description" class="admin-modal__desc">{{ actionModal.description }}</p>
          <div v-if="actionModal.user" class="admin-modal__summary">
            <div>
              <span>대상</span>
              <strong>#{{ actionModal.user.userId }} · {{ actionModal.user.email }}</strong>
            </div>
            <div>
              <span>유형</span>
              <strong>{{ resolveRoleLabel(actionModal.user.role) }}</strong>
            </div>
            <div>
              <span>계정 상태</span>
              <strong>{{ resolveAccountStatus(actionModal.user) }}</strong>
            </div>
            <div>
              <span>호스트 상태</span>
              <strong>{{ resolveHostStatus(actionModal.user) }}</strong>
            </div>
          </div>
          <div v-if="actionModal.reasonLabel" class="admin-modal__field">
            <label class="admin-modal__label">{{ actionModal.reasonLabel }}</label>
            <textarea
              v-model="actionModal.reason"
              class="admin-input admin-textarea"
              rows="3"
              :placeholder="actionModal.reasonPlaceholder"
            />
            <p v-if="reasonError" class="admin-modal__error">{{ reasonError }}</p>
          </div>
          <div class="admin-modal__actions">
            <button class="admin-btn admin-btn--ghost" type="button" @click="closeActionModal">취소</button>
            <button class="admin-btn admin-btn--primary" type="button" :disabled="isSubmitting" @click="handleAction">
              {{ isSubmitting ? '처리 중...' : actionModal.confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="toast" class="admin-toast" :class="`admin-toast--${toast.tone}`">
      {{ toast.message }}
    </div>
  </section>
</template>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.admin-page__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.admin-title {
  margin: 0;
  font-size: 2rem;
  font-weight: 900;
  color: #0b3b32;
}

.admin-subtitle {
  margin: 6px 0 0;
  color: var(--text-sub);
  font-weight: 600;
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.admin-status {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
}

.admin-pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
}

.admin-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.admin-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: #e5f3ef;
  color: #0f766e;
  display: grid;
  place-items: center;
  font-weight: 900;
}

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
}

.admin-btn-ghost {
  background: transparent;
  border: 1px solid #d1d5db;
  color: #0f766e;
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 800;
}

.admin-btn-ghost:hover {
  border-color: #0f766e;
}

.admin-table--stretch {
  width: 100%;
  table-layout: fixed;
}

.admin-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-align-center {
  text-align: center;
}

.admin-align-left {
  text-align: left;
}

.admin-table-meta {
  margin-bottom: 10px;
  color: #64748b;
  font-weight: 700;
  font-size: 0.9rem;
}

.admin-dropdown--table {
  display: inline-block;
}

.admin-modal {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 60px 16px;
  z-index: 50;
}

.admin-modal__content {
  background: #ffffff;
  border-radius: 16px;
  width: min(520px, 92vw);
  padding: 16px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
}

.admin-modal__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.admin-modal__head h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 900;
  color: #0b3b32;
}

.admin-modal__body {
  display: grid;
  gap: 12px;
}

.admin-modal__desc {
  margin: 0;
  color: #6b7280;
  font-weight: 600;
}

.admin-modal__field {
  display: grid;
  gap: 6px;
}

.admin-modal__label {
  font-weight: 700;
  color: #1f2937;
  font-size: 0.9rem;
}

.admin-modal__error {
  margin: 0;
  color: #b91c1c;
  font-weight: 700;
  font-size: 0.85rem;
}

.admin-modal__summary {
  display: grid;
  gap: 6px;
  font-size: 0.9rem;
  color: #1f2937;
}

.admin-modal__summary span {
  color: #6b7280;
  font-weight: 700;
  margin-right: 6px;
}

.admin-textarea {
  resize: vertical;
}

.admin-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.admin-toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  padding: 10px 16px;
  border-radius: 12px;
  font-weight: 800;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.18);
  z-index: 60;
}

.admin-toast--success {
  background: #ecfdf3;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.admin-toast--warning {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
}

.admin-toast--danger {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.admin-actions-right {
  justify-content: flex-end;
}

.admin-action-disabled {
  opacity: 0.6;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
