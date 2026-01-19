<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { fetchAdminLogs } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'
import { ADMIN_ROUTES } from '../../router/adminPaths'
import { exportCSV, exportXLSX } from '../../utils/reportExport'

const logs = ref([])
const actionFilter = ref('all')
const targetTypeFilter = ref('all')
const targetIdExact = ref('')
const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const route = useRoute()
const router = useRouter()
const isSyncingDefaults = ref(false)
const selectedLog = ref(null)

const formatLogDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const normalizeTargetId = (value) => {
  if (!value) return ''
  const trimmed = String(value).trim()
  if (!trimmed) return ''
  return /^\d+$/.test(trimmed) ? trimmed : ''
}

const toDateInput = (date) => {
  if (!date) return ''
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const initDefaultRange = () => {
  const today = new Date()
  const from = new Date()
  from.setDate(today.getDate() - 6)
  startDate.value = toDateInput(from)
  endDate.value = toDateInput(today)
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

const loadLogs = async () => {
  isLoading.value = true
  loadError.value = ''
  const normalizedTargetId = normalizeTargetId(targetIdExact.value)
  const response = await fetchAdminLogs({
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    actionType: actionFilter.value === 'all' ? undefined : actionFilter.value,
    targetType: targetTypeFilter.value === 'all' ? undefined : targetTypeFilter.value,
    targetIdExact: normalizedTargetId || undefined,
    keyword: keyword.value || undefined,
    page: page.value,
    size: size.value
  })
  if (response.ok && response.data) {
    const payload = response.data
    logs.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
  } else {
    loadError.value = '감사 로그를 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const applyPreset = (days) => {
  const today = new Date()
  const start = new Date()
  start.setDate(today.getDate() - (days - 1))
  startDate.value = toDateInput(start)
  endDate.value = toDateInput(today)
  page.value = 0
  syncQuery({
    startDate: startDate.value,
    endDate: endDate.value,
    page: page.value
  })
}

const applyActionFilter = () => {
  page.value = 0
  syncQuery({
    actionType: actionFilter.value === 'all' ? undefined : actionFilter.value,
    page: page.value
  })
}

const applyTargetFilter = () => {
  page.value = 0
  syncQuery({
    targetType: targetTypeFilter.value === 'all' ? undefined : targetTypeFilter.value,
    page: page.value
  })
}

const applySearch = () => {
  page.value = 0
  syncQuery({
    targetIdExact: normalizeTargetId(targetIdExact.value) || undefined,
    keyword: keyword.value || undefined,
    page: page.value
  })
}

const applyDateFilter = () => {
  page.value = 0
  syncQuery({
    startDate: startDate.value || undefined,
    endDate: endDate.value || undefined,
    page: page.value
  })
}

const goToPage = (nextPage) => {
  page.value = nextPage
  syncQuery({
    page: page.value
  })
}

const formatReason = (value) => {
  if (!value || String(value).trim() === '') return '-'
  return String(value)
}

const actionLabelMap = {
  APPROVE: '승인',
  REJECT: '반려',
  REFUND: '환불',
  BAN: '정지',
  UNBAN: '해제',
  RESOLVE: '처리'
}

const actionVariantMap = {
  APPROVE: 'success',
  REJECT: 'danger',
  REFUND: 'accent',
  BAN: 'warning',
  UNBAN: 'neutral',
  RESOLVE: 'primary'
}

const targetLabelMap = {
  ACC: '숙소',
  USER: '회원',
  PAY: '결제',
  REVIEW: '리뷰'
}

const resolveTargetLabel = (value) => targetLabelMap[value] ?? value ?? '-'

const resolveActionLabel = (value) => actionLabelMap[value] ?? value ?? '-'

const resolveActionVariant = (value) => actionVariantMap[value] ?? 'neutral'

const resolveAdminLabel = (item) => {
  const username = item?.adminUsername
  const id = item?.adminId
  if (username && id) return `${username} (#${id})`
  if (username) return username
  if (id) return `#${id}`
  return '-'
}

const resolveTargetLink = (item) => {
  const id = item?.targetId
  if (!id) return null
  if (item?.targetType === 'ACC') return `${ADMIN_ROUTES.ACCOMMODATIONS}?highlight=${id}`
  if (item?.targetType === 'USER') return `${ADMIN_ROUTES.USERS}?highlight=${id}`
  if (item?.targetType === 'PAY') return `${ADMIN_ROUTES.PAYMENTS}?highlight=${id}`
  if (item?.targetType === 'REVIEW') return `${ADMIN_ROUTES.REPORTS}?highlight=${id}`
  return null
}

const goToTarget = (item) => {
  const target = resolveTargetLink(item)
  if (target) router.push(target)
}

const openDetail = (item) => {
  selectedLog.value = item
}

const closeDetail = () => {
  selectedLog.value = null
}

const syncStateFromQuery = (query) => {
  actionFilter.value = query.actionType ?? 'all'
  targetTypeFilter.value = query.targetType ?? 'all'
  targetIdExact.value = query.targetIdExact ?? ''
  keyword.value = query.keyword ?? ''
  page.value = Number(query.page ?? 0)
  size.value = Number(query.size ?? 20)
  if (query.startDate) {
    startDate.value = query.startDate
  } else {
    initDefaultRange()
  }
  if (query.endDate) {
    endDate.value = query.endDate
  } else {
    endDate.value = endDate.value || toDateInput(new Date())
  }
}

onMounted(() => {
  initDefaultRange()
  if (!route.query.startDate || !route.query.endDate) {
    isSyncingDefaults.value = true
    syncQuery({
      startDate: startDate.value,
      endDate: endDate.value,
      page: page.value
    })
  }
})

watch(
  () => route.query,
  (query) => {
    if (isSyncingDefaults.value && (!query.startDate || !query.endDate)) {
      return
    }
    isSyncingDefaults.value = false
    syncStateFromQuery(query)
    loadLogs()
  },
  { immediate: true }
)
const formatMetadata = (value) => {
  if (!value) return '-'
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return JSON.stringify(parsed, null, 2)
  } catch (error) {
    return String(value)
  }
}

const exportAuditLogs = (format) => {
  if (!logs.value.length) return
  const from = startDate.value || 'from'
  const to = endDate.value || 'to'
  const filename = `admin-audit-logs_${from}_${to}.${format}`
  const rows = logs.value.map((item) => ({
    time: formatLogDateTime(item.createdAt),
    action: resolveActionLabel(item.actionType),
    targetType: resolveTargetLabel(item.targetType),
    targetId: item.targetId ?? '-',
    reason: formatReason(item.reason),
    admin: resolveAdminLabel(item),
    requestIp: item.requestIp ?? '-',
    userAgent: item.userAgent ?? '-',
    metadata: formatMetadata(item.metadataJson)
  }))
  const sheet = {
    name: '감사 로그',
    columns: [
      { key: 'time', label: '시간(YYYY-MM-DD HH:mm)' },
      { key: 'action', label: '액션' },
      { key: 'targetType', label: '대상 타입' },
      { key: 'targetId', label: '대상ID' },
      { key: 'reason', label: '사유' },
      { key: 'admin', label: '관리자' },
      { key: 'requestIp', label: '요청 IP' },
      { key: 'userAgent', label: 'User-Agent' },
      { key: 'metadata', label: '변경 메타데이터(JSON)' }
    ],
    rows
  }
  if (format === 'xlsx') {
    exportXLSX({ filename, sheets: [sheet] })
    return
  }
  exportCSV({ filename, sheets: [sheet] })
}

</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">감사 로그</h1>
        <p class="admin-subtitle">관리자 주요 액션 기록을 조회합니다.</p>
      </div>
      <div class="admin-summary-chip">
        총 {{ totalElements.toLocaleString() }}건
      </div>
    </header>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">기간</span>
        <input v-model="startDate" class="admin-input" type="date" @change="applyDateFilter" />
        <span class="admin-divider">~</span>
        <input v-model="endDate" class="admin-input" type="date" @change="applyDateFilter" />
        <div class="admin-preset-group">
          <button class="admin-btn admin-btn--ghost" type="button" @click="applyPreset(7)">최근 7일(오늘 포함)</button>
          <button class="admin-btn admin-btn--ghost" type="button" @click="applyPreset(30)">최근 30일(오늘 포함)</button>
          <button class="admin-btn admin-btn--ghost" type="button" @click="applyPreset(90)">최근 90일(오늘 포함)</button>
        </div>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">액션 필터</span>
        <select v-model="actionFilter" class="admin-select" @change="applyActionFilter">
          <option value="all">전체</option>
          <option value="APPROVE">승인</option>
          <option value="REJECT">반려</option>
          <option value="REFUND">환불</option>
          <option value="BAN">정지</option>
          <option value="UNBAN">해제</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">대상 타입</span>
        <select v-model="targetTypeFilter" class="admin-select" @change="applyTargetFilter">
          <option value="all">전체</option>
          <option value="ACC">숙소</option>
          <option value="USER">회원</option>
          <option value="PAY">결제</option>
          <option value="REVIEW">리뷰/신고</option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="targetIdExact"
          class="admin-input"
          type="search"
          placeholder="대상 ID로 검색"
          @keydown.enter.prevent="applySearch"
        />
        <input
          v-model="keyword"
          class="admin-input"
          type="search"
          placeholder="사유/대상 타입/관리자ID 등"
          @keydown.enter.prevent="applySearch"
        />
        <button class="admin-btn admin-btn--ghost" type="button" @click="applySearch">검색</button>
        <button class="admin-btn admin-btn--ghost" type="button" @click="exportAuditLogs('csv')">CSV</button>
        <button class="admin-btn admin-btn--ghost" type="button" @click="exportAuditLogs('xlsx')">XLSX</button>
      </div>
    </div>

    <AdminTableCard title="감사 로그" class="admin-audit-card">
      <table class="admin-audit-table admin-table--nowrap">
        <colgroup>
          <col style="width: 170px" />
          <col style="width: 90px" />
          <col style="width: 90px" />
          <col style="width: 110px" />
          <col class="col-reason" />
          <col style="width: 170px" />
          <col style="width: 80px" />
        </colgroup>
      <thead>
        <tr>
          <th>시간</th>
          <th class="is-center">액션</th>
          <th class="is-center">대상</th>
          <th class="is-center">대상ID</th>
          <th class="col-reason">사유</th>
          <th class="is-center">관리자</th>
          <th class="is-center">상세</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in logs" :key="item.logId">
          <td>{{ formatLogDateTime(item.createdAt) }}</td>
            <td class="is-center">
              <span class="admin-action-badge" :class="`admin-action-badge--${resolveActionVariant(item.actionType)}`">
                {{ resolveActionLabel(item.actionType) }}
              </span>
            </td>
            <td class="is-center">{{ resolveTargetLabel(item.targetType) }}</td>
            <td class="is-center">
              <button
                class="admin-link-button"
                type="button"
                :disabled="!resolveTargetLink(item)"
                @click="goToTarget(item)"
              >
                {{ item.targetId }}
              </button>
            </td>
            <td class="admin-audit-reason col-reason">
              <span
                class="reason-text"
                :title="formatReason(item.reason)"
              >
                <span class="reason-text__content">
                  {{ formatReason(item.reason) }}
                </span>
              </span>
            </td>
            <td class="is-center">
              <div class="admin-admin-cell">
                <span class="admin-admin-cell__name">{{ item.adminUsername ?? '-' }}</span>
                <span class="admin-admin-cell__id">#{{ item.adminId ?? '-' }}</span>
              </div>
            </td>
            <td class="is-center">
              <button class="admin-btn admin-btn--ghost" type="button" @click="openDetail(item)">상세</button>
            </td>
          </tr>
          <tr v-if="!logs.length && !isLoading && !loadError">
            <td class="admin-audit-empty" colspan="7">조건에 맞는 로그가 없습니다.</td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadLogs">다시 시도</button>
      </div>
      <div class="admin-pagination" v-if="totalElements > 0">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="goToPage(page - 1)">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="goToPage(page + 1)">
          다음
        </button>
      </div>
    </AdminTableCard>

    <div v-if="selectedLog" class="admin-modal" @click.self="closeDetail">
      <div class="admin-modal__content">
        <div class="admin-modal__head">
          <h3>감사 로그 상세</h3>
          <button class="admin-btn admin-btn--ghost" type="button" @click="closeDetail">닫기</button>
        </div>
        <div class="admin-modal__body">
          <div class="admin-modal__row">
            <span>시간</span>
            <strong>{{ formatLogDateTime(selectedLog.createdAt) }}</strong>
          </div>
          <div class="admin-modal__row">
            <span>액션</span>
            <strong>{{ resolveActionLabel(selectedLog.actionType) }}</strong>
          </div>
          <div class="admin-modal__row">
            <span>대상</span>
            <strong>{{ resolveTargetLabel(selectedLog.targetType) }}</strong>
          </div>
          <div class="admin-modal__row">
            <span>대상ID</span>
            <strong>{{ selectedLog.targetId ?? '-' }}</strong>
          </div>
          <div class="admin-modal__row admin-modal__row--detail">
            <span>사유</span>
            <p>{{ formatReason(selectedLog.reason) }}</p>
          </div>
          <div v-if="selectedLog.metadataJson" class="admin-modal__row admin-modal__row--detail">
            <span>변경 내용</span>
            <pre>{{ formatMetadata(selectedLog.metadataJson) }}</pre>
          </div>
          <div class="admin-modal__row">
            <span>요청 IP</span>
            <strong>{{ selectedLog.requestIp ?? '-' }}</strong>
          </div>
          <div class="admin-modal__row admin-modal__row--detail">
            <span>User-Agent</span>
            <p>{{ selectedLog.userAgent ?? '-' }}</p>
          </div>
          <div class="admin-modal__row">
            <span>관리자ID</span>
            <strong>{{ resolveAdminLabel(selectedLog) }}</strong>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-summary-chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: #f0fdfa;
  color: #0f766e;
  font-weight: 700;
  font-size: 0.85rem;
  border: 1px solid rgba(15, 118, 110, 0.2);
}

.admin-divider {
  color: #94a3b8;
  font-size: 0.85rem;
}

.admin-preset-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.admin-audit-card :deep(.admin-table-wrap) {
  width: 100%;
  max-width: none;
  display: block;
}

.admin-audit-card {
  width: 100%;
}

.admin-audit-table {
  width: 100%;
  min-width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}

.admin-audit-table col.col-reason {
  width: auto;
}

.admin-audit-table th,
.admin-audit-table td {
  padding: 12px 12px;
}

.admin-audit-table th.is-center,
.admin-audit-table td.is-center {
  text-align: center;
}

.admin-audit-table th.col-reason,
.admin-audit-table td.col-reason {
  text-align: center;
  vertical-align: middle;
}

.admin-audit-table tbody tr:nth-child(even) {
  background: #f8fafc;
}

.admin-audit-table tbody tr:hover {
  background: #eef8f6;
}

.admin-audit-reason {
  white-space: normal;
  line-height: 1.45;
}

.reason-text {
  display: inline-block;
  max-width: 100%;
  text-align: left;
}

.reason-text__content {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 800;
  border: 1px solid transparent;
  min-width: 64px;
}

.admin-action-badge--success {
  background: #ecfdf3;
  color: #047857;
  border-color: #a7f3d0;
}

.admin-action-badge--danger {
  background: #fef2f2;
  color: #b91c1c;
  border-color: #fecaca;
}

.admin-action-badge--accent {
  background: #eef2ff;
  color: #1d4ed8;
  border-color: #c7d2fe;
}

.admin-action-badge--warning {
  background: #fffbeb;
  color: #b45309;
  border-color: #fde68a;
}

.admin-action-badge--neutral {
  background: #f1f5f9;
  color: #475569;
  border-color: #e2e8f0;
}

.admin-action-badge--primary {
  background: #f0fdfa;
  color: #0f766e;
  border-color: rgba(15, 118, 110, 0.2);
}

.admin-link-button {
  background: none;
  border: none;
  padding: 0;
  color: #0f766e;
  font-weight: 700;
  cursor: pointer;
}

.admin-link-button:disabled {
  color: #9ca3af;
  cursor: default;
}

.admin-audit-empty {
  text-align: center;
  padding: 28px 12px;
  color: #64748b;
  font-weight: 700;
}

.admin-admin-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: 0.82rem;
  line-height: 1.2;
}

.admin-admin-cell__name {
  font-weight: 700;
  color: #0f766e;
}

.admin-admin-cell__id {
  color: #6b7280;
  font-weight: 600;
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
  width: min(560px, 92vw);
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
  gap: 10px;
}

.admin-modal__row {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px;
  align-items: start;
  font-size: 0.9rem;
  color: #1f2937;
}

.admin-modal__row span {
  color: #6b7280;
  font-weight: 700;
}

.admin-modal__row--detail p {
  margin: 0;
  color: #111827;
  line-height: 1.4;
}

.admin-modal__row--detail pre {
  margin: 0;
  padding: 10px;
  background: #f8fafc;
  border-radius: 10px;
  font-size: 0.85rem;
  overflow: auto;
}

@media (max-width: 900px) {
  .admin-filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
