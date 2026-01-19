<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import AdminReportDetailModal from '../../components/admin/AdminReportDetailModal.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminReports, resolveAdminReport } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'

const stats = ref([])
const reportList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const typeFilter = ref('all')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const detailOpen = ref(false)
const detailId = ref(null)
const actionLoading = ref(false)
const route = useRoute()
const router = useRouter()

// 매핑 상수
const REPORT_TYPE_MAP = {
  REVIEW: { label: '리뷰', color: 'blue', icon: '📝' },
  USER: { label: '사용자', color: 'green', icon: '👤' },
  ACCOMMODATION: { label: '숙소', color: 'purple', icon: '🏠' },
}

const REPORT_STATUS_MAP = {
  WAIT: { label: '대기중', color: 'yellow' },
  PENDING: { label: '대기중', color: 'yellow' },
  PROCESSED: { label: '처리완료', color: 'green' },
  BLIND: { label: '블라인드', color: 'green' },
  IGNORED: { label: '반려됨', color: 'gray' },
}

const loadReports = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminReports({
    status: statusFilter.value === 'all' ? undefined : statusFilter.value,
    type: typeFilter.value === 'all' ? undefined : typeFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value,
    size: size.value,
    sort: 'latest'
  })
  if (response.ok && response.data) {
    const payload = response.data
    reportList.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements

    const pending = reportList.value.filter((r) => r.status === 'WAIT' || r.status === 'PENDING').length
    const processed = reportList.value.filter((r) => r.status === 'PROCESSED' || r.status === 'BLIND').length
    const ignored = reportList.value.filter((r) => r.status === 'IGNORED').length

    stats.value = [
      { label: '대기중', value: `${pending}건`, sub: '현재 페이지 기준', tone: 'warning' },
      { label: '처리완료', value: `${processed}건`, sub: '현재 페이지 기준', tone: 'success' },
      { label: '반려됨', value: `${ignored}건`, sub: '현재 페이지 기준', tone: 'neutral' }
    ]
  } else {
    loadError.value = '신고 목록을 불러오지 못했습니다.'
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
  statusFilter.value = route.query.status ?? 'all'
  typeFilter.value = route.query.type ?? 'all'
  searchQuery.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  loadReports()
})

const resolveTypeInfo = (type) => {
  return REPORT_TYPE_MAP[type] || { label: type, color: 'gray', icon: '❓' }
}

const resolveStatusInfo = (status) => {
  return REPORT_STATUS_MAP[status] || { label: status, color: 'neutral' }
}

const filteredReports = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return reportList.value.filter((item) => {
    const matchesQuery = !query ||
      String(item.reportId ?? '').includes(query) ||
      (item.reason ?? '').toLowerCase().includes(query)
    const matchesStatus = statusFilter.value === 'all' || item.status === statusFilter.value
    const matchesType = typeFilter.value === 'all' || item.targetType === typeFilter.value
    return matchesQuery && matchesStatus && matchesType
  })
})

watch([searchQuery, statusFilter, typeFilter], () => {
  page.value = 0
  syncQuery({
    status: statusFilter.value,
    type: typeFilter.value,
    keyword: searchQuery.value || undefined,
    page: page.value
  })
  loadReports()
})

const openDetail = (item) => {
  if (!item?.reportId) return
  detailId.value = item.reportId
  detailOpen.value = true
}

const closeDetail = () => {
  detailOpen.value = false
  detailId.value = null
}

const handleDetailAction = async (actionType, report) => {
  if (!report?.reportId) return

  let action = ''
  let memo = ''

  if (actionType === 'ignore') {
    if (!confirm('신고를 반려하시겠습니까?')) return
    action = 'IGNORED'
    memo = '관리자에 의해 반려됨'
  } else if (actionType === 'blind') {
    if (!confirm('해당 콘텐츠를 블라인드 처리하시겠습니까?')) return
    action = 'BLIND'
    memo = '부적절한 콘텐츠로 블라인드 처리'
  } else if (actionType === 'ban') {
    if (!confirm('해당 사용자를 정지하시겠습니까?')) return
    action = 'PROCESSED'
    memo = '신고 누적으로 인한 계정 정지'
  } else {
    return
  }

  actionLoading.value = true
  const response = await resolveAdminReport(report.reportId, { action, memo })
  actionLoading.value = false

  if (response.ok) {
    alert('처리가 완료되었습니다.')
    closeDetail()
    loadReports()
  } else {
    alert('처리 중 오류가 발생했습니다.')
  }
}

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '신고 목록',
      columns: [
        { key: 'reportId', label: '신고ID' },
        { key: 'targetType', label: '유형' },
        { key: 'reason', label: '사유' },
        { key: 'createdAt', label: '신고 일시' },
        { key: 'status', label: '상태' }
      ],
      rows: filteredReports.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-reports-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-reports-${today}.csv`, sheets })
}

const formatDate = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">신고 관리</h1>
        <p class="admin-subtitle">신고 접수 현황과 처리 단계를 추적합니다.</p>
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
    </div>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="searchQuery"
          class="admin-input"
          type="search"
          placeholder="신고ID, 사유"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">필터</span>
        <select v-model="typeFilter" class="admin-select">
          <option value="all">전체 유형</option>
          <option value="REVIEW">리뷰</option>
          <option value="USER">회원</option>
          <option value="ACCOMMODATION">숙소</option>
        </select>
        <select v-model="statusFilter" class="admin-select">
          <option value="all">전체 상태</option>
          <option value="WAIT">대기중</option>
          <option value="PROCESSED">처리완료</option>
          <option value="IGNORED">반려됨</option>
        </select>
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

    <AdminTableCard title="신고 목록">
      <table class="admin-table--nowrap admin-table--tight admin-table--stretch">
        <colgroup>
          <col style="width:100px"/>
          <col style="width:120px"/>
          <col style="width:200px"/>
          <col style="width:160px"/>
          <col style="width:120px"/>
          <col style="width:100px"/>
        </colgroup>
        <thead>
          <tr>
            <th class="admin-align-center">신고ID</th>
            <th class="admin-align-center">유형</th>
            <th class="admin-align-left">신고 사유</th>
            <th class="admin-align-center">신고 일시</th>
            <th class="admin-align-center">상태</th>
            <th class="admin-align-center">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredReports" :key="item.reportId">
            <td class="admin-strong admin-align-center">#{{ item.reportId }}</td>
            <td class="admin-align-center">
              <span
                class="type-badge"
                :class="`type-badge--${resolveTypeInfo(item.targetType).color}`"
              >
                {{ resolveTypeInfo(item.targetType).icon }} {{ resolveTypeInfo(item.targetType).label }}
              </span>
            </td>
            <td class="admin-align-left">
              <div class="reason-cell">
                <span class="admin-strong admin-ellipsis" :title="item.reason">{{ item.reason }}</span>
                <span class="reporter-info" v-if="item.reporterId">(by #{{ item.reporterId }})</span>
              </div>
            </td>
            <td class="admin-align-center">{{ formatDate(item.createdAt) }}</td>
            <td class="admin-align-center">
              <AdminBadge
                :text="resolveStatusInfo(item.status).label"
                :variant="resolveStatusInfo(item.status).color === 'yellow' ? 'warning' : resolveStatusInfo(item.status).color === 'green' ? 'success' : 'neutral'"
              />
            </td>
            <td class="admin-align-center">
              <button class="admin-btn admin-btn--ghost" type="button" @click="openDetail(item)">상세</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadReports">다시 시도</button>
      </div>
      <div v-else-if="!filteredReports.length" class="admin-status">데이터가 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadReports()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadReports()">
          다음
        </button>
      </div>
    </AdminTableCard>

    <AdminReportDetailModal
      v-if="detailOpen"
      :report-id="detailId"
      :is-open="detailOpen"
      @close="closeDetail"
      @action="handleDetailAction"
    />
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
  margin-top: 12px;
  justify-content: center;
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

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
}

.admin-table--stretch {
  width: 100%;
  table-layout: fixed;
}

.admin-align-center { text-align: center; }
.admin-align-left { text-align: left; }
.admin-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-badge {
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.type-badge--blue { background: #e0f2fe; color: #0369a1; }
.type-badge--green { background: #dcfce7; color: #15803d; }
.type-badge--purple { background: #f3e8ff; color: #7e22ce; }
.type-badge--gray { background: #f3f4f6; color: #4b5563; }

.reason-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.reporter-info {
  font-size: 0.8rem;
  color: #9ca3af;
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

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
