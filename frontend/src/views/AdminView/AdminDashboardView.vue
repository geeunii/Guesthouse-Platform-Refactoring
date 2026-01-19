<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import AdminBarChart from '../../components/admin/AdminBarChart.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminDashboardSummary, fetchAdminDashboardTimeseries } from '../../api/adminApi'
import { buildDateRange, fillSeriesByDate, formatKRW, toISODate } from '../../utils/admin/chartSeries'

const router = useRouter()
const stats = ref([])
const summary = ref(null)
const pendingListings = ref([])
const openReportListings = ref([])
const revenuePoints = ref([])
const reservationPoints = ref([])
const revenueRange = ref({ from: '', to: '' })
const trendLoading = ref(false)
const trendError = ref('')
const reservationError = ref('')
const alerts = ref([])
const activePeriod = ref('30')
const isLoading = ref(false)
const loadError = ref('')
const layoutStorageKey = 'adminDashboardLayout'
const kpiStorageKey = 'adminDashboardKpiExpanded'
const defaultLayout = {
  alerts: true,
  trends: true,
  kpi: true,
  issues: true,
  pendingTable: true
}
const loadLayout = () => {
  try {
    const raw = localStorage.getItem(layoutStorageKey)
    if (!raw) return { ...defaultLayout }
    const parsed = JSON.parse(raw)
    return { ...defaultLayout, ...(parsed || {}) }
  } catch (error) {
    return { ...defaultLayout }
  }
}
const loadKpiExpanded = () => {
  try {
    return localStorage.getItem(kpiStorageKey) === 'true'
  } catch (error) {
    return false
  }
}
const layout = ref(loadLayout())
const isKpiExpanded = ref(loadKpiExpanded())

const buildAlerts = (openReports, pendingAcc) => {
  const reportAlerts = openReports.map((item) => ({
    id: `report-${item.reportId}`,
    title: `리뷰 신고 #${item.reportId}`,
    meta: item.title ?? '신고 사유 확인 필요',
    time: item.createdAt?.slice?.(0, 10) ?? '-',
    tone: 'warning',
    target: `/admin/reports?highlight=${item.reportId}`
  }))
  const accAlerts = pendingAcc.map((item) => ({
    id: `acc-${item.accommodationsId}`,
    title: `숙소 심사 대기 #${item.accommodationsId}`,
    meta: item.name ?? '승인 대기 숙소',
    time: item.createdAt?.slice?.(0, 10) ?? '-',
    tone: 'accent',
    target: `/admin/accommodations?highlight=${item.accommodationsId}`
  }))
  return [...reportAlerts, ...accAlerts].slice(0, 6)
}

const resolveRange = (days) => {
  const count = Number(days) || 30
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - (count - 1))
  return { from: toISODate(start), to: toISODate(end) }
}

const periodLabelMap = {
  7: '최근 7일',
  30: '최근 30일',
  90: '최근 3개월'
}

const activePeriodLabel = computed(() => periodLabelMap[Number(activePeriod.value)] || '최근 30일')

const formatCurrency = (value) => formatKRW(value)
const formatRate = (value) => `${(Number(value ?? 0) * 100).toFixed(1)}%`
const formatNumber = (value) => Number(value ?? 0).toLocaleString()

const formatPercentValue = (value) => {
  const numeric = Number(value ?? 0)
  if (!Number.isFinite(numeric) || numeric < 1) return '0'
  const rounded = Math.round(numeric * 10) / 10
  return Number.isInteger(rounded) ? String(Math.round(rounded)) : rounded.toFixed(1)
}

const loadDashboard = async () => {
  isLoading.value = true
  loadError.value = ''
  trendLoading.value = true
  trendError.value = ''
  reservationError.value = ''
  const range = resolveRange(activePeriod.value)
  revenueRange.value = range
  const [summaryResponse, trendResponse, reservationResponse] = await Promise.all([
    fetchAdminDashboardSummary(range),
    fetchAdminDashboardTimeseries({ metric: 'platform_fee', ...range }),
    fetchAdminDashboardTimeseries({ metric: 'reservations', ...range })
  ])
  if (summaryResponse.ok && summaryResponse.data) {
    const data = summaryResponse.data
    summary.value = data
    const platformFeeRate = data.platformFeeRate ?? 0
    stats.value = [
      { label: '승인 대기 숙소', value: `${data.pendingAccommodations ?? 0}건`, badge: '심사 대기', sub: '', tone: 'warning', target: '/admin/accommodations?status=PENDING' },
      { label: '미처리 신고', value: `${data.openReports ?? 0}건`, badge: '처리 필요', sub: '', tone: 'warning', target: '/admin/reports?status=WAIT' },
      { label: '예약 생성', value: `${data.reservationCount ?? 0}건`, badge: '선택 기간', sub: '', tone: 'success', target: '/admin/bookings?sort=latest' },
      { label: '결제 성공', value: formatCurrency(data.paymentSuccessAmount), badge: '선택 기간', sub: '', tone: 'accent', target: '/admin/payments?status=success' },
      { label: '플랫폼 수익(수수료)', value: formatCurrency(data.platformFeeAmount), badge: '선택 기간', sub: `수수료율 ${formatRate(platformFeeRate)}`, tone: 'primary', target: '/admin/payments?status=success' },
      { label: '결제 실패', value: `${data.paymentFailureCount ?? 0}건`, badge: '실패/취소', sub: '', tone: 'neutral', target: '/admin/payments?status=failed' },
      { label: '환불 요청', value: `${data.refundRequestCount ?? 0}건`, badge: '요청', sub: '', tone: 'neutral', target: '/admin/payments?type=refund' },
      { label: '환불 완료', value: `${data.refundCompletedCount ?? 0}건`, badge: '완료', sub: '', tone: 'neutral', target: '/admin/payments?type=refund' }
    ]
    pendingListings.value = data.pendingAccommodationsList ?? []
    openReportListings.value = data.openReportsList ?? []
    alerts.value = buildAlerts(openReportListings.value, pendingListings.value)
    if (trendResponse.ok && trendResponse.data) {
      const points = trendResponse.data.points ?? []
      revenuePoints.value = points
    } else {
      trendError.value = '수익 추이를 불러오지 못했습니다.'
      revenuePoints.value = []
    }
    if (reservationResponse.ok && reservationResponse.data) {
      reservationPoints.value = reservationResponse.data.points ?? []
    } else {
      reservationError.value = '예약 추이를 불러오지 못했습니다.'
      reservationPoints.value = []
    }
  } else {
    loadError.value = '대시보드 데이터를 불러오지 못했습니다.'
  }
  trendLoading.value = false
  isLoading.value = false
}

const pendingStatusVariant = (status) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'neutral'
}

const formatDateOnly = (value) => (value ? value.slice(0, 10) : '-')

const alertCards = computed(() => {
  if (!summary.value) return []
  const data = summary.value
  return [
    {
      id: 'pending',
      title: '승인 대기',
      badge: '심사 대기',
      variant: 'warning',
      value: `${formatNumber(data.pendingAccommodations)}건`,
      target: '/admin/accommodations?status=PENDING'
    },
    {
      id: 'reports',
      title: '미처리 신고',
      badge: '처리 필요',
      variant: 'danger',
      value: `${formatNumber(data.openReports)}건`,
      target: '/admin/reports?status=WAIT'
    },
    {
      id: 'payments',
      title: '결제 실패/취소',
      badge: '확인 필요',
      variant: 'neutral',
      value: `${formatNumber(data.paymentFailureCount)}건`,
      target: '/admin/payments?status=failed'
    },
    {
      id: 'refunds',
      title: '환불 요청',
      badge: '요청',
      variant: 'accent',
      value: `${formatNumber(data.refundRequestCount)}건`,
      target: '/admin/payments?type=refund'
    }
  ]
})

const primaryKpiLabels = [
  '예약 생성',
  '결제 성공',
  '플랫폼 수익(수수료)',
  '환불 완료'
]

const primaryKpis = computed(() =>
  primaryKpiLabels
    .map((label) => stats.value.find((item) => item.label === label))
    .filter(Boolean)
)

const extraKpis = computed(() =>
  stats.value.filter((item) => !primaryKpiLabels.includes(item.label))
)

const resolvePriority = (item) => {
  if (item.id === 'overdue') return { label: '긴급', tone: 'danger', rank: 4 }
  if (item.id === 'reports') return { label: '높음', tone: 'danger', rank: 3 }
  if (['pending', 'refunds', 'payments', 'pending-over-7'].includes(item.id)) {
    return { label: '보통', tone: 'warning', rank: 2 }
  }
  return { label: '낮음', tone: 'neutral', rank: 1 }
}

const issueList = computed(() => {
  if (!summary.value) return []
  const data = summary.value
  const candidates = [
    {
      id: 'pending',
      title: '승인 대기 숙소',
      count: data.pendingAccommodations ?? 0,
      status: '심사 대기',
      statusTone: 'warning',
      target: '/admin/accommodations?status=PENDING'
    },
    {
      id: 'reports',
      title: '미처리 신고',
      count: data.openReports ?? 0,
      status: '미처리',
      statusTone: 'danger',
      target: '/admin/reports?status=WAIT'
    },
    {
      id: 'payments',
      title: '결제 실패/취소',
      count: data.paymentFailureCount ?? 0,
      status: '실패',
      statusTone: 'neutral',
      target: '/admin/payments?status=failed'
    },
    {
      id: 'refunds',
      title: '환불 요청',
      count: data.refundRequestCount ?? 0,
      status: '요청',
      statusTone: 'accent',
      target: '/admin/payments?type=refund'
    },
    {
      id: 'overdue',
      title: '48시간+ 미처리 신고',
      count: data.overdueOpenReports48h ?? 0,
      status: '지연',
      statusTone: 'danger',
      target: '/admin/reports?status=WAIT'
    },
    {
      id: 'pending-over-7',
      title: '7일+ 승인 대기',
      count: data.weeklyPendingOver7Days ?? 0,
      status: '지연',
      statusTone: 'warning',
      target: '/admin/accommodations?status=PENDING'
    }
  ]
  return candidates
    .filter((item) => item.count > 0)
    .map((item) => ({ ...item, priority: resolvePriority(item) }))
    .sort((a, b) => {
      if (b.priority.rank !== a.priority.rank) return b.priority.rank - a.priority.rank
      return b.count - a.count
    })
    .slice(0, 5)
})

const weeklySummaryLine = computed(() => {
  if (!summary.value) return ''
  const data = summary.value
  return `환불 요청 ${formatNumber(data.weeklyRefundRequestCount)}건 · 결제 실패율 ${formatPercentValue(data.weeklyPaymentFailureRate)}% · 48시간+ 미처리 신고 ${formatNumber(data.overdueOpenReports48h)}건`
})

const pendingPreviewRows = computed(() => pendingListings.value.slice(0, 5))

const revenueLabels = computed(() => buildDateRange(revenueRange.value.from, revenueRange.value.to))
const revenueValues = computed(() => {
  if (!revenueLabels.value.length) return []
  return fillSeriesByDate(
    revenueLabels.value,
    revenuePoints.value,
    (point) => String(point.date ?? '').slice(0, 10),
    (point) => point.value
  )
})
const revenueMaxXTicks = computed(() => {
  const total = revenueLabels.value.length
  return total <= 8 ? total : 8
})
const hasRevenueData = computed(() => revenueValues.value.some((value) => Number(value ?? 0) !== 0))
const reservationLabels = revenueLabels
const reservationValues = computed(() => {
  if (!reservationLabels.value.length) return []
  return fillSeriesByDate(
    reservationLabels.value,
    reservationPoints.value,
    (point) => String(point.date ?? '').slice(0, 10),
    (point) => point.value
  )
})
const hasReservationData = computed(() => reservationValues.value.some((value) => Number(value ?? 0) !== 0))

const goTo = (target) => {
  if (target) router.push(target)
}

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '요약 지표',
      columns: [
        { key: 'label', label: '지표' },
        { key: 'value', label: '값' },
        { key: 'sub', label: '설명' }
      ],
      rows: stats.value
    },
    {
      name: '승인 대기 숙소',
      columns: [
        { key: 'id', label: 'ID' },
        { key: 'name', label: '숙소명' },
        { key: 'host', label: '호스트' },
        { key: 'location', label: '위치' },
        { key: 'type', label: '유형' },
        { key: 'rooms', label: '객실' },
        { key: 'nightlyPrice', label: '가격(1박)' },
        { key: 'contact', label: '연락처' },
        { key: 'submittedAt', label: '신청일' },
        { key: 'status', label: '상태' }
      ],
      rows: pendingListings.value
    },
    {
      name: '운영 알림',
      columns: [
        { key: 'title', label: '알림' },
        { key: 'meta', label: '메타' },
        { key: 'time', label: '시간' }
      ],
      rows: alerts.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-dashboard-report-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-dashboard-report-${today}.csv`, sheets })
}

onMounted(loadDashboard)

watch(activePeriod, loadDashboard)
watch(layout, (value) => {
  localStorage.setItem(layoutStorageKey, JSON.stringify(value))
}, { deep: true })
watch(isKpiExpanded, (value) => {
  localStorage.setItem(kpiStorageKey, value ? 'true' : 'false')
})
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">관리자 대시보드</h1>
        <p class="admin-subtitle">주요 지표와 거래 현황을 한 눈에 확인하세요.</p>
      </div>
    </header>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">운영 기간</span>
        <select v-model="activePeriod" class="admin-select">
          <option value="7">최근 7일</option>
          <option value="30">최근 30일</option>
          <option value="90">최근 3개월</option>
        </select>
        <span class="admin-period-label">선택 기간: {{ activePeriodLabel }}</span>
      </div>
      <div class="admin-filter-group admin-filter-group--actions">
        <span class="admin-chip">빠른 작업</span>
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
        <details class="admin-dropdown admin-layout-control">
          <summary class="admin-btn admin-btn--ghost">대시보드 구성 ⚙️</summary>
          <div class="admin-dropdown__menu admin-layout-control__menu">
            <label class="admin-check">
              <input v-model="layout.alerts" type="checkbox" />
              <span>알림 카드</span>
            </label>
            <label class="admin-check">
              <input v-model="layout.trends" type="checkbox" />
              <span>트렌드 차트</span>
            </label>
            <label class="admin-check">
              <input v-model="layout.kpi" type="checkbox" />
              <span>KPI</span>
            </label>
            <label class="admin-check">
              <input v-model="layout.issues" type="checkbox" />
              <span>운영 이슈 리스트</span>
            </label>
            <label class="admin-check">
              <input v-model="layout.pendingTable" type="checkbox" />
              <span>승인 대기 테이블</span>
            </label>
          </div>
        </details>
      </div>
    </div>

    <section v-if="layout.alerts" class="admin-card admin-alerts-section">
      <div class="admin-card__head">
        <div>
          <p class="admin-card__eyebrow">운영 알림</p>
          <h3 class="admin-card__title">즉시 확인해야 할 리스크</h3>
        </div>
        <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/dashboard/issues')">이슈 센터</button>
      </div>
      <div class="admin-alert-grid">
        <div v-if="isLoading" class="admin-status">불러오는 중...</div>
        <div v-else-if="loadError" class="admin-status">
          <span>{{ loadError }}</span>
          <button class="admin-btn admin-btn--ghost" type="button" @click="loadDashboard">다시 시도</button>
        </div>
        <template v-else>
          <article
            v-for="card in alertCards"
            :key="card.id"
            class="admin-alert-card"
          >
            <div class="admin-alert-card__head">
              <p class="admin-alert-card__title">{{ card.title }}</p>
              <AdminBadge :text="card.badge" :variant="card.variant" />
            </div>
            <div class="admin-alert-card__value">{{ card.value }}</div>
            <button class="admin-btn admin-btn--ghost admin-alert-card__link" type="button" @click="goTo(card.target)">
              바로가기
            </button>
          </article>
        </template>
      </div>
    </section>

    <section v-if="layout.trends" class="admin-card admin-trend-section">
      <div class="admin-card__head">
        <div>
          <p class="admin-card__eyebrow">트렌드</p>
          <h3 class="admin-card__title">플랫폼 수익과 예약 추이</h3>
          <p class="admin-card__caption">기간: {{ activePeriodLabel }}</p>
        </div>
      </div>
      <div class="admin-trend-grid">
        <div class="admin-trend-panel">
          <div class="admin-trend-panel__head">
            <h4>플랫폼 수익(수수료) 추이</h4>
            <span>일별 수익 흐름</span>
          </div>
          <div class="admin-chart-area">
            <div v-if="isLoading || trendLoading" class="admin-status">불러오는 중...</div>
            <div v-else-if="loadError || trendError" class="admin-status">
              <span>{{ loadError || trendError }}</span>
              <button class="admin-btn admin-btn--ghost" type="button" @click="loadDashboard">다시 시도</button>
            </div>
            <div v-else-if="!hasRevenueData" class="admin-status">표시할 데이터가 없습니다.</div>
            <AdminBarChart
              v-else
              :labels="revenueLabels"
              :values="revenueValues"
              :height="220"
              :max-x-ticks="revenueMaxXTicks"
            />
          </div>
        </div>
        <div class="admin-trend-panel">
          <div class="admin-trend-panel__head">
            <h4>예약 생성 추이</h4>
            <span>예약 수 변화를 확인하세요</span>
          </div>
          <div class="admin-chart-area">
            <div v-if="isLoading || trendLoading" class="admin-status">불러오는 중...</div>
            <div v-else-if="loadError || reservationError" class="admin-status">
              <span>{{ loadError || reservationError }}</span>
              <button class="admin-btn admin-btn--ghost" type="button" @click="loadDashboard">다시 시도</button>
            </div>
            <div v-else-if="!hasReservationData" class="admin-status">표시할 데이터가 없습니다.</div>
            <AdminBarChart
              v-else
              :labels="reservationLabels"
              :values="reservationValues"
              :height="220"
              format="count"
              unit-label="건"
              tooltip-label="예약 생성"
              :max-x-ticks="revenueMaxXTicks"
            />
          </div>
        </div>
      </div>
    </section>

    <section v-if="layout.kpi" class="admin-card admin-kpi-section">
      <div class="admin-card__head">
        <div>
          <p class="admin-card__eyebrow">KPI 요약</p>
          <h3 class="admin-card__title">핵심 지표만 먼저 확인하세요</h3>
        </div>
        <button
          v-if="extraKpis.length"
          class="admin-btn admin-btn--ghost admin-kpi-toggle"
          type="button"
          @click="isKpiExpanded = !isKpiExpanded"
        >
          {{ isKpiExpanded ? 'KPI 접기 ▴' : 'KPI 더보기 ▾' }}
        </button>
      </div>
      <div class="admin-grid admin-grid--kpi">
        <AdminStatCard
          v-for="card in primaryKpis"
          :key="card.label"
          :label="card.label"
          :value="card.value"
          :sub="card.sub"
          :badge="card.badge"
          :tone="card.tone"
          :clickable="Boolean(card.target)"
          @click="goTo(card.target)"
        />
        <AdminStatCard
          v-for="card in extraKpis"
          v-if="isKpiExpanded"
          :key="card.label"
          :label="card.label"
          :value="card.value"
          :sub="card.sub"
          :badge="card.badge"
          :tone="card.tone"
          :clickable="Boolean(card.target)"
          @click="goTo(card.target)"
        />
        <div v-if="isLoading" class="admin-status">불러오는 중...</div>
        <div v-else-if="loadError" class="admin-status">
          <span>{{ loadError }}</span>
          <button class="admin-btn admin-btn--ghost" type="button" @click="loadDashboard">다시 시도</button>
        </div>
      </div>
    </section>

    <div v-if="layout.issues" class="admin-grid admin-grid--2 admin-grid--issues">
      <div class="admin-card">
        <div class="admin-card__head">
          <div>
            <p class="admin-card__eyebrow">운영 이슈 센터</p>
            <h3 class="admin-card__title">바로 조치할 항목 Top 5</h3>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/dashboard/issues')">전체 보기</button>
        </div>
        <div class="admin-issue-list">
          <div class="admin-issue-list__head">
            <span>항목</span>
            <span>우선순위</span>
            <span>상태</span>
            <span class="is-right">건수</span>
            <span class="is-right">바로가기</span>
          </div>
          <div v-if="isLoading" class="admin-status">불러오는 중...</div>
          <div v-else-if="loadError" class="admin-status">
            <span>{{ loadError }}</span>
            <button class="admin-btn admin-btn--ghost" type="button" @click="loadDashboard">다시 시도</button>
          </div>
          <div v-else-if="!issueList.length" class="admin-status">현재 바로 조치할 항목이 없습니다.</div>
          <template v-else>
            <div
              v-for="item in issueList"
              :key="item.id"
              class="admin-issue-list__row"
            >
              <span class="admin-issue-list__title">{{ item.title }}</span>
              <AdminBadge :text="item.priority.label" :variant="item.priority.tone" />
              <AdminBadge :text="item.status" :variant="item.statusTone" />
              <span class="admin-issue-list__count is-right">{{ formatNumber(item.count) }}건</span>
              <button class="admin-btn admin-btn--ghost admin-issue-list__link" type="button" @click="goTo(item.target)">
                보기
              </button>
            </div>
          </template>
        </div>
      </div>

      <div class="admin-card admin-summary-card">
        <div class="admin-card__head">
          <div>
            <p class="admin-card__eyebrow">운영 요약</p>
            <h3 class="admin-card__title">이번 주 주요 리포트</h3>
            <p class="admin-card__caption">기간: 이번 주(월~오늘)</p>
          </div>
        </div>
        <div class="admin-summary-list">
          <div class="admin-summary-item">
            <div>
              <p class="admin-summary-title">주간 리포트</p>
              <p class="admin-summary-desc">{{ weeklySummaryLine || '요약 데이터를 불러오는 중입니다.' }}</p>
            </div>
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/dashboard/weekly')">리포트 보기</button>
          </div>
          <div class="admin-summary-item">
            <div>
              <p class="admin-summary-title">운영 이슈 센터</p>
              <p class="admin-summary-desc">상위 이슈를 빠르게 처리할 수 있습니다.</p>
            </div>
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/dashboard/issues')">바로가기</button>
          </div>
        </div>
      </div>
    </div>

    <AdminTableCard v-if="layout.pendingTable" title="승인 대기 숙소 (최근 5개)" class="admin-table-card--wide">
      <template #actions>
        <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/accommodations?status=PENDING')">
          전체 보기
        </button>
      </template>
      <table class="admin-table--nowrap admin-table--tight">
        <thead>
          <tr>
            <th>ID</th>
            <th>숙소명</th>
            <th>신청일</th>
            <th>상태</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in pendingPreviewRows" :key="item.accommodationsId">
            <td class="admin-strong">#{{ item.accommodationsId }}</td>
            <td class="admin-strong">{{ item.name }}</td>
            <td>{{ formatDateOnly(item.createdAt) }}</td>
            <td>
              <AdminBadge :text="item.approvalStatus" :variant="pendingStatusVariant(item.approvalStatus)" />
            </td>
            <td>
              <button class="admin-btn admin-btn--ghost" type="button" @click="goTo(`/admin/accommodations?highlight=${item.accommodationsId}`)">
                상세 보기
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!pendingPreviewRows.length" class="admin-status">승인 대기 숙소가 없습니다.</div>
    </AdminTableCard>
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

.admin-grid--2 {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  align-items: start;
}

.admin-grid--issues {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  align-items: stretch;
}

.admin-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  padding: 16px;
}

.admin-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.admin-card__footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.admin-card__eyebrow {
  margin: 0;
  color: #0f766e;
  font-weight: 800;
  font-size: 0.9rem;
}

.admin-card__title {
  margin: 4px 0 0;
  font-size: 1.15rem;
  color: #0b3b32;
  font-weight: 900;
}

.admin-chart-area {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  min-height: 220px;
  margin-top: 12px;
}

.admin-hint {
  margin: 0;
  color: #6b7280;
  font-size: 0.9rem;
}

.admin-strong {
  font-weight: 800;
  color: #0b3b32;
}

.admin-table--nowrap th,
.admin-table--nowrap td {
  white-space: nowrap;
}

.admin-table--roomy th,
.admin-table--roomy td {
  padding: 14px 12px;
}

.admin-table-card--wide :deep(table) {
  min-width: 680px;
}

.admin-table-card--wide {
  grid-column: 1 / -1;
}

@media (max-width: 1024px) {
  .admin-grid--issues {
    grid-template-columns: 1fr;
  }
}

.admin-period-label {
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 700;
  margin-left: 8px;
}

.admin-card__caption {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 700;
}

.admin-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-filter-group--actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-layout-control__menu {
  min-width: 180px;
  display: grid;
  gap: 8px;
}

.admin-check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 700;
  color: #1f2937;
}

.admin-check input {
  width: 16px;
  height: 16px;
}

.admin-alerts-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-alert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.admin-alert-card {
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-alert-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.admin-alert-card__title {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 800;
  color: #0b3b32;
}

.admin-alert-card__value {
  font-size: 1.4rem;
  font-weight: 900;
  color: #111827;
}

.admin-alert-card__link {
  align-self: flex-start;
}

.admin-trend-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-trend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}

.admin-trend-panel {
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  padding: 14px;
}

.admin-trend-panel__head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-trend-panel__head h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
  color: #0b3b32;
}

.admin-trend-panel__head span {
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 600;
}

.admin-kpi-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-grid--kpi {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.admin-kpi-toggle {
  white-space: nowrap;
}

.admin-issue-list {
  display: grid;
  gap: 10px;
}

.admin-issue-list__head,
.admin-issue-list__row {
  display: grid;
  grid-template-columns: 1.4fr 0.7fr 0.7fr 0.6fr 0.6fr;
  align-items: center;
  gap: 8px;
}

.admin-issue-list__head {
  font-size: 0.8rem;
  font-weight: 800;
  color: #6b7280;
}

.admin-issue-list__row {
  border-bottom: 1px solid #eef1f4;
  padding: 8px 0;
  font-size: 0.9rem;
}

.admin-issue-list__title {
  font-weight: 700;
  color: #0b3b32;
}

.admin-issue-list__count {
  font-weight: 800;
  color: #111827;
}

.admin-issue-list__link {
  justify-self: end;
}

.admin-summary-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.admin-summary-list {
  display: grid;
  gap: 12px;
}

.admin-summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
}

.admin-summary-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 800;
  color: #0b3b32;
}

.admin-summary-desc {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 600;
}

.is-right {
  justify-self: end;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .admin-issue-list__head,
  .admin-issue-list__row {
    grid-template-columns: 1.3fr 0.7fr 0.7fr 0.6fr;
  }

  .admin-issue-list__head span:last-child,
  .admin-issue-list__row button {
    display: none;
  }

  .admin-summary-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
