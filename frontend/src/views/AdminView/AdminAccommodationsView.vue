<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import AdminAccommodationDetailModal from '../../components/admin/AdminAccommodationDetailModal.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminAccommodations, approveAccommodation, rejectAccommodation } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'

const accommodationList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')

// 상세 모달 상태
const detailModal = ref({
  isOpen: false,
  accommodationId: null
})

// 승인/반려 모달 상태
const approveOpen = ref(false)
const rejectOpen = ref(false)
const actionLoading = ref(false)
const actionError = ref('')
const actionTarget = ref(null)
const rejectReason = ref('')
const route = useRoute()
const router = useRouter()

// Backend ApprovalStatus enum: PENDING / APPROVED / REJECTED
const APPROVAL_STATUSES = ['PENDING', 'APPROVED', 'REJECTED']

const normalizeStatus = (value) => {
  if (!value) return 'all'
  const normalized = String(value).toUpperCase().trim()
  return APPROVAL_STATUSES.includes(normalized) ? normalized : 'all'
}

const loadAccommodations = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminAccommodations({
    status: statusFilter.value === 'all' ? undefined : statusFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value,
    size: size.value,
    sort: 'latest'
  })
  if (response.ok && response.data) {
    const payload = response.data
    accommodationList.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
  } else {
    loadError.value = '숙소 목록을 불러오지 못했습니다.'
  }
  isLoading.value = false
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
  statusFilter.value = normalizeStatus(route.query.status)
  searchQuery.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  loadAccommodations()
})

const statusVariant = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'PENDING') return 'warning'
  if (status === 'REJECTED') return 'danger'
  return 'neutral'
}

const filteredAccommodations = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return accommodationList.value.filter((item) => {
    const matchesQuery = !query ||
      (item.name ?? '').toLowerCase().includes(query) ||
      String(item.hostUserId ?? '').includes(query) ||
      `${item.city ?? ''} ${item.district ?? ''}`.toLowerCase().includes(query) ||
      String(item.accommodationsId ?? '').includes(query)
    const matchesStatus = statusFilter.value === 'all' || item.approvalStatus === statusFilter.value
    return matchesQuery && matchesStatus
  })
})

watch([searchQuery, statusFilter], () => {
  page.value = 0
  syncQuery({ status: statusFilter.value, keyword: searchQuery.value || undefined, page: page.value })
  loadAccommodations()
})

const applyPendingFilter = () => {
  statusFilter.value = 'PENDING'
  page.value = 0
  syncQuery({ status: statusFilter.value, page: page.value })
  loadAccommodations()
}

// 상세 모달 열기
const openDetail = (item) => {
  if (!item?.accommodationsId) return
  detailModal.value = {
    isOpen: true,
    accommodationId: item.accommodationsId
  }
}

const closeDetail = () => {
  detailModal.value = {
    isOpen: false,
    accommodationId: null
  }
}

// 상세 모달에서 액션 발생 시 처리
const handleDetailAction = (actionType, accommodation) => {
  closeDetail()
  // 모달 닫힘 애니메이션 후 액션 모달 열기
  setTimeout(() => {
    if (actionType === 'approve') openApprove(accommodation)
    if (actionType === 'reject') openReject(accommodation)
    if (actionType === 'suspend') {
      // 판매 중지는 반려와 동일한 로직 사용 (사유 입력 필요 시) 또는 별도 처리
      // 여기서는 반려 로직을 재사용하되 타이틀만 다르게 처리할 수도 있음
      // 일단 반려 모달을 띄우되, 내부적으로 구분 가능
      openReject(accommodation)
    }
  }, 100)
}

const openApprove = (item) => {
  // item 구조가 목록/상세에 따라 다를 수 있으므로 ID 추출
  const id = item.accommodationsId || item.accommodationId
  if (!id) return

  actionTarget.value = { ...item, accommodationsId: id }
  actionError.value = ''
  actionLoading.value = false
  approveOpen.value = true
}

const openReject = (item) => {
  const id = item.accommodationsId || item.accommodationId
  if (!id) return

  actionTarget.value = { ...item, accommodationsId: id }
  rejectReason.value = ''
  actionError.value = ''
  actionLoading.value = false
  rejectOpen.value = true
}

const closeApprove = () => {
  approveOpen.value = false
  actionTarget.value = null
  actionError.value = ''
  actionLoading.value = false
}

const closeReject = () => {
  rejectOpen.value = false
  actionTarget.value = null
  actionError.value = ''
  actionLoading.value = false
  rejectReason.value = ''
}

const confirmApprove = async () => {
  if (!actionTarget.value?.accommodationsId) return
  actionLoading.value = true
  actionError.value = ''
  const response = await approveAccommodation(actionTarget.value.accommodationsId)
  if (!response.ok) {
    actionError.value = response?.data?.message || '승인 처리에 실패했습니다.'
    actionLoading.value = false
    return
  }
  closeApprove()
  await loadAccommodations()
}

const confirmReject = async () => {
  if (!actionTarget.value?.accommodationsId) return
  const reason = rejectReason.value.trim()
  if (!reason) {
    actionError.value = '반려 사유를 입력해주세요.'
    return
  }
  actionLoading.value = true
  actionError.value = ''
  const response = await rejectAccommodation(actionTarget.value.accommodationsId, reason)
  if (!response.ok) {
    actionError.value = response?.data?.message || '반려 처리에 실패했습니다.'
    actionLoading.value = false
    return
  }
  closeReject()
  await loadAccommodations()
}

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '숙소 목록',
      columns: [
        { key: 'accommodationsId', label: 'ID' },
        { key: 'name', label: '숙소명' },
        { key: 'hostUserId', label: '호스트' },
        { key: 'city', label: '도시' },
        { key: 'district', label: '지역' },
        { key: 'category', label: '유형' },
        { key: 'createdAt', label: '등록일' },
        { key: 'avgRating', label: '평점' },
        { key: 'reviewCount', label: '리뷰 수' },
        { key: 'reservationCount', label: '예약 수' },
        { key: 'occupancyRate', label: '가동률' },
        { key: 'cancellationRate', label: '취소율' },
        { key: 'totalRevenue', label: '총 매출' },
        { key: 'approvalStatus', label: '상태' }
      ],
      rows: filteredAccommodations.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-accommodations-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-accommodations-${today}.csv`, sheets })
}

const formatCurrency = (value, fallbackZero = false) => {
  if (value === null || value === undefined) return fallbackZero ? '₩0' : '-'
  return `₩${Number(value).toLocaleString()}`
}

const formatCount = (value, fallbackZero = false) => {
  if (value === null || value === undefined) return fallbackZero ? '0' : '-'
  return Number(value).toLocaleString()
}

const formatPercent = (value, fallbackZero = false) => {
  if (value === null || value === undefined) return fallbackZero ? '0.0%' : '-'
  return `${Number(value).toFixed(1)}%`
}

const formatRating = (value) => {
  if (value === null || value === undefined) return '-'
  return Number(value).toFixed(1)
}

const formatDate = (value) => {
  if (!value) return '-'
  return String(value).slice(0, 10)
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">숙소 관리</h1>
        <p class="admin-subtitle">등록 숙소 상태와 매출을 모니터링합니다.</p>
      </div>
    </header>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="searchQuery"
          class="admin-input"
          type="search"
          placeholder="숙소명, 호스트, 지역, ID"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">상태</span>
        <select v-model="statusFilter" class="admin-select">
          <option value="all">전체 상태</option>
          <option value="PENDING">승인 대기</option>
          <option value="APPROVED">승인</option>
          <option value="REJECTED">반려</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">작업</span>
        <button class="admin-btn admin-btn--primary" type="button" @click="applyPendingFilter">대기 숙소 보기</button>
      </div>
      <div class="admin-filter-group">
        <details class="admin-dropdown">
          <summary class="admin-btn admin-btn--ghost">다운로드</summary>
          <div class="admin-dropdown__menu">
            <button class="admin-btn admin-btn--ghost admin-dropdown__item" type="button" @click="downloadReport('csv')">CSV</button>
            <button class="admin-btn admin-btn--primary admin-dropdown__item" type="button" @click="downloadReport('xlsx')">XLSX</button>
          </div>
        </details>
      </div>
    </div>

    <AdminTableCard title="숙소 목록">
      <table class="admin-table--nowrap admin-table--tight">
        <thead>
          <tr>
            <th>ID</th>
            <th>숙소명</th>
            <th>호스트</th>
            <th>위치</th>
            <th>평점</th>
            <th>리뷰</th>
            <th>예약</th>
            <th>가동률</th>
            <th>취소율</th>
            <th>총 매출</th>
            <th>등록일</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredAccommodations" :key="item.accommodationsId">
            <td class="admin-strong">#{{ item.accommodationsId }}</td>
            <td class="admin-strong">{{ item.name }}</td>
            <td>{{ item.hostUserId }}</td>
            <td>{{ item.city }} {{ item.district }}</td>
            <td>{{ formatRating(item.avgRating) }}</td>
            <td>{{ formatCount(item.reviewCount) }}</td>
            <td>{{ formatCount(item.reservationCount, true) }}</td>
            <td>{{ formatPercent(item.occupancyRate, true) }}</td>
            <td>{{ formatPercent(item.cancellationRate, true) }}</td>
            <td>{{ formatCurrency(item.totalRevenue, true) }}</td>
            <td>{{ formatDate(item.createdAt) }}</td>
            <td>
              <AdminBadge :text="item.approvalStatus" :variant="statusVariant(item.approvalStatus)" />
            </td>
            <td>
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button" @click="openDetail(item)">상세</button>
                <button class="admin-btn admin-btn--primary" type="button" @click="openApprove(item)">승인</button>
                <button class="admin-btn admin-btn--muted" type="button" @click="openReject(item)">반려</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadAccommodations">다시 시도</button>
      </div>
      <div v-else-if="!filteredAccommodations.length" class="admin-status">조건에 맞는 숙소가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadAccommodations()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadAccommodations()">
          다음
        </button>
      </div>
    </AdminTableCard>

    <!-- 상세 모달 -->
    <AdminAccommodationDetailModal
      v-if="detailModal.isOpen"
      :accommodation-id="detailModal.accommodationId"
      :is-open="detailModal.isOpen"
      @close="closeDetail"
      @action="handleDetailAction"
    />

    <!-- 승인 모달 -->
    <div v-if="approveOpen" class="admin-modal">
      <div class="admin-modal__backdrop" @click="closeApprove" />
      <div class="admin-modal__panel" role="dialog" aria-modal="true">
        <div class="admin-modal__header">
          <div>
            <p class="admin-modal__eyebrow">숙소 승인</p>
            <h2 class="admin-modal__title">숙소를 승인할까요?</h2>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" :disabled="actionLoading" @click="closeApprove">닫기</button>
        </div>
        <div class="admin-detail-grid">
          <div><span>숙소 ID</span><strong>#{{ actionTarget?.accommodationsId ?? '-' }}</strong></div>
          <div><span>숙소명</span><strong>{{ actionTarget?.name || actionTarget?.accommodationName || '-' }}</strong></div>
        </div>
        <div v-if="actionError" class="admin-status">{{ actionError }}</div>
        <div class="admin-modal__actions">
          <button class="admin-btn admin-btn--ghost" type="button" :disabled="actionLoading" @click="closeApprove">취소</button>
          <button class="admin-btn admin-btn--primary" type="button" :disabled="actionLoading" @click="confirmApprove">
            {{ actionLoading ? '처리 중...' : '승인' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 반려 모달 -->
    <div v-if="rejectOpen" class="admin-modal">
      <div class="admin-modal__backdrop" @click="closeReject" />
      <div class="admin-modal__panel" role="dialog" aria-modal="true">
        <div class="admin-modal__header">
          <div>
            <p class="admin-modal__eyebrow">숙소 반려</p>
            <h2 class="admin-modal__title">반려 사유를 입력해주세요</h2>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" :disabled="actionLoading" @click="closeReject">닫기</button>
        </div>
        <div class="admin-detail-grid">
          <div><span>숙소 ID</span><strong>#{{ actionTarget?.accommodationsId ?? '-' }}</strong></div>
          <div><span>숙소명</span><strong>{{ actionTarget?.name || actionTarget?.accommodationName || '-' }}</strong></div>
        </div>
        <textarea
          v-model="rejectReason"
          class="admin-input"
          rows="3"
          placeholder="예) 사진이 부족합니다. 시설 설명을 보완해주세요."
          :disabled="actionLoading"
        />
        <div v-if="actionError" class="admin-status">{{ actionError }}</div>
        <div class="admin-modal__actions">
          <button class="admin-btn admin-btn--ghost" type="button" :disabled="actionLoading" @click="closeReject">취소</button>
          <button class="admin-btn admin-btn--muted" type="button" :disabled="actionLoading" @click="confirmReject">
            {{ actionLoading ? '처리 중...' : '반려' }}
          </button>
        </div>
      </div>
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

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
}

.admin-status {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
  margin-top: 12px;
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

.admin-modal {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: grid;
  place-items: center;
  padding: 1.5rem;
}

.admin-modal__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
}

.admin-modal__panel {
  position: relative;
  width: min(600px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
  display: grid;
  gap: 1.25rem;
}

.admin-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.admin-modal__eyebrow {
  margin: 0 0 0.25rem;
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 700;
}

.admin-modal__title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 900;
  color: #0b3b32;
}

.admin-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem 1.25rem;
}

.admin-detail-grid span {
  display: block;
  font-size: 0.82rem;
  color: #64748b;
  font-weight: 700;
}

.admin-detail-grid strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.95rem;
  color: #0f172a;
}

.admin-modal__actions {
  display: flex;
  gap: 0.6rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .admin-modal__panel {
    padding: 1.2rem;
  }
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
