<script setup>
import {ref, computed, nextTick, onMounted, watch} from 'vue'
import { useRoute } from 'vue-router'
import {exportCSV, exportXLSX} from '../../utils/reportExport'
import {fetchHostRevenueDetails, fetchHostRevenueSummary, fetchHostRevenueTrend} from '@/api/hostRevenue'
import { fetchHostAccommodations } from '@/api/hostAccommodation'
import { deriveHostGateInfo, buildHostGateNotice } from '@/composables/useHostState'
import {formatCurrency, formatDate} from '@/utils/formatters'
import HostGateNotice from '@/components/host/HostGateNotice.vue'

const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = now.getMonth() + 1
const STORAGE_KEY = 'hostRevenueReportFilter'
const route = useRoute()
const selectedYear = ref(currentYear)
const selectedMonth = ref(String(currentMonth))
const years = computed(() => Array.from({length: 6}, (_, idx) => currentYear - idx))
const months = ['all', ...Array.from({length: 12}, (_, idx) => String(idx + 1))]

const revenueSummary = ref({
  year: selectedYear.value,
  month: 1,
  totalRevenue: 0,
  expectedNextMonthRevenue: 0,
  platformFeeRate: 0.04,
  platformFeeAmount: 0,
  reservationCount: 0
})
const revenueTrend = ref([])
const revenueDetails = ref([])
const isLoading = ref(false)
const loadError = ref('')
const prefersReducedMotion = ref(false)
const animateCharts = ref(false)
const includeZero = ref(false)
const hostGateInfo = ref({
  hostState: 'loading',
  counts: { total: 0, approved: 0, pending: 0, rejected: 0, unknown: 0 },
  rejectedReason: '',
  recheckReason: ''
})
const hostState = computed(() => hostGateInfo.value.hostState)
const canUseHostFeatures = computed(() => hostGateInfo.value.gateState === 'APPROVED')
const gateVisible = computed(() => !canUseHostFeatures.value && hostState.value !== 'loading')
const gateNotice = computed(() => buildHostGateNotice(hostGateInfo.value))
const filtersReady = ref(false)

const currentYearSummary = computed(() => revenueSummary.value)
const currentYearTrend = computed(() => revenueTrend.value)
const currentYearDetails = computed(() => revenueDetails.value)

const isDailyView = computed(() => selectedMonth.value !== 'all')

const selectedPeriodLabel = computed(() => {
  if (selectedMonth.value === 'all') return `${selectedYear.value}년 재무 현황`
  return `${selectedYear.value}년 ${Number(selectedMonth.value)}월 재무 현황`
})

const summaryLabelPrefix = computed(() => {
  if (selectedMonth.value === 'all') return `${selectedYear.value}년`
  return `${Number(selectedMonth.value)}월`
})

const emptyMessage = computed(() => {
  if (selectedMonth.value === 'all') return '선택한 연도에 확정 매출이 없습니다.'
  return '선택한 기간에 확정 매출이 없습니다.'
})

const chartSeries = computed(() => {
  if (isDailyView.value) {
    return currentYearDetails.value.map((item) => ({
      label: Number(item.period.split('-')[2]),
      revenue: item.revenue
    }))
  }

  const byMonth = new Map(currentYearTrend.value.map((item) => [item.month, item.revenue]))
  return Array.from({length: 12}, (_, idx) => {
    const month = idx + 1
    return {label: month, revenue: byMonth.get(month) ?? 0}
  })
})

const maxRevenue = computed(() => {
  const values = chartSeries.value.map(d => d.revenue)
  return values.length ? Math.max(...values) : 0
})

const formatPercent = (value, withSign = true) => {
  const amount = Number(value ?? 0)
  if (!Number.isFinite(amount)) return '0.0%'
  const label = amount.toFixed(1)
  if (!withSign) return `${label}%`
  if (amount > 0) return `+${label}%`
  return `${label}%`
}

const monthLabel = (year, month) => {
  const padded = String(month).padStart(2, '0')
  return `${year}.${padded}`
}

const getBarHeight = (revenue) => {
  if (!maxRevenue.value) return '0%'
  const percentage = (revenue / maxRevenue.value) * 100
  return `${percentage}%`
}

const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const rows = currentYearTrend.value.map((item) => ({
    year: selectedYear.value,
    month: `${item.month}월`,
    revenue: item.revenue
  }))
  const sheets = [
    {
      name: '매출 리포트',
      columns: [
        {key: 'year', label: '연도'},
        {key: 'month', label: '월'},
        {key: 'revenue', label: '매출액'}
      ],
      rows
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({filename: `host-revenue-${today}.xlsx`, sheets})
    return
  }
  exportCSV({filename: `host-revenue-${today}.csv`, sheets})
}

const normalizeMonth = (value) => {
  if (value === undefined || value === null) return null
  const raw = String(value).trim().toLowerCase()
  if (!raw) return null
  if (raw === 'all') return 'all'
  const num = Number(raw)
  if (!Number.isFinite(num) || num < 1 || num > 12) return null
  return String(num)
}

const normalizeYear = (value, fallback) => {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

const normalizeBool = (value) => {
  return value === true || value === 'true' || value === '1'
}

const initFilters = () => {
  const queryYear = route.query?.year
  const queryMonth = route.query?.month
  const queryIncludeZero = route.query?.includeZero

  let year = currentYear
  let month = String(currentMonth)
  let includeZeroValue = false

  if (queryYear !== undefined || queryMonth !== undefined || queryIncludeZero !== undefined) {
    year = normalizeYear(queryYear, year)
    const normalizedMonth = normalizeMonth(queryMonth)
    if (normalizedMonth) month = normalizedMonth
    includeZeroValue = normalizeBool(queryIncludeZero)
  } else if (typeof window !== 'undefined') {
    const saved = window.sessionStorage.getItem(STORAGE_KEY)
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        year = normalizeYear(parsed?.year, year)
        const normalizedMonth = normalizeMonth(parsed?.month)
        if (normalizedMonth) month = normalizedMonth
        includeZeroValue = normalizeBool(parsed?.includeZero)
      } catch (error) {
        window.sessionStorage.removeItem(STORAGE_KEY)
      }
    }
  }

  selectedYear.value = year
  selectedMonth.value = month
  includeZero.value = includeZeroValue
}

const loadRevenue = async (year, month) => {
  if (!canUseHostFeatures.value) return
  isLoading.value = true
  loadError.value = ''
  const monthValue = month === 'all' ? null : Number(month)
  const currentMonthValue = now.getMonth() + 1
  const summaryMonth = monthValue ?? currentMonthValue
  const detailFrom = monthValue ? `${year}-${String(monthValue).padStart(2, '0')}-01` : `${year}-01-01`
  const detailTo = monthValue
      ? `${year}-${String(monthValue).padStart(2, '0')}-${String(new Date(year, monthValue, 0).getDate()).padStart(2, '0')}`
      : `${year}-12-31`
  const detailGranularity = monthValue ? 'day' : 'month'

  const [summaryRes, trendRes, detailsRes] = await Promise.all([
    fetchHostRevenueSummary({year, month: summaryMonth}),
    fetchHostRevenueTrend({year}),
    fetchHostRevenueDetails({from: detailFrom, to: detailTo, granularity: detailGranularity})
  ])

  const summaryData = summaryRes.ok && summaryRes.data ? summaryRes.data : null
  if (monthValue === null) {
    const totalRevenue = trendRes.ok && Array.isArray(trendRes.data)
      ? trendRes.data.reduce((sum, item) => sum + Number(item.revenue ?? 0), 0)
      : 0
    const platformFeeRate = summaryData?.platformFeeRate ?? revenueSummary.value?.platformFeeRate ?? 0.04
    const platformFeeAmount = Math.round(totalRevenue * platformFeeRate)
    revenueSummary.value = {
      year,
      month: 0,
      totalRevenue,
      expectedNextMonthRevenue: summaryData?.expectedNextMonthRevenue ?? 0,
      platformFeeRate,
      platformFeeAmount,
      reservationCount: summaryData?.reservationCount ?? 0
    }
  } else if (summaryData) {
    revenueSummary.value = summaryData
  } else {
    loadError.value = '데이터를 불러오지 못했어요.'
  }

  if (trendRes.ok && Array.isArray(trendRes.data)) {
    revenueTrend.value = trendRes.data
  } else {
    loadError.value = '데이터를 불러오지 못했어요.'
  }

  if (detailsRes.ok && Array.isArray(detailsRes.data)) {
    revenueDetails.value = detailsRes.data
  } else if (trendRes.ok && Array.isArray(trendRes.data)) {
    revenueDetails.value = trendRes.data.map((item) => ({
      period: `${year}-${String(item.month).padStart(2, '0')}`,
      revenue: item.revenue,
      occupancyRate: item.occupancyRate
    }))
  }

  isLoading.value = false
  if (!prefersReducedMotion.value) {
    animateCharts.value = false
    nextTick(() => {
      requestAnimationFrame(() => {
        animateCharts.value = true
      })
    })
  }
}

const loadHostState = async () => {
  const response = await fetchHostAccommodations()
  if (response.ok) {
    const payload = response.data
    const list = Array.isArray(payload)
      ? payload
      : payload?.items ?? payload?.content ?? payload?.data ?? []
    hostGateInfo.value = deriveHostGateInfo(list)
  } else {
    hostGateInfo.value = {
      hostState: 'empty',
      counts: { total: 0, approved: 0, pending: 0, rejected: 0, unknown: 0 },
      rejectedReason: '',
      recheckReason: ''
    }
  }
}

onMounted(async () => {
  initFilters()
  filtersReady.value = true
  await loadHostState()
  if (canUseHostFeatures.value) {
    loadRevenue(selectedYear.value, selectedMonth.value)
  }
})

watch([selectedYear, selectedMonth], ([year, month]) => {
  if (!canUseHostFeatures.value || !filtersReady.value) return
  loadRevenue(year, month)
})

watch([selectedYear, selectedMonth, includeZero], () => {
  if (!filtersReady.value || typeof window === 'undefined') return
  const payload = {
    year: selectedYear.value,
    month: selectedMonth.value,
    includeZero: includeZero.value
  }
  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
})

const netRevenue = computed(() => {
  return (currentYearSummary.value?.totalRevenue ?? 0) - (currentYearSummary.value?.platformFeeAmount ?? 0)
})

const animatedSummary = ref({
  totalRevenue: 0,
  platformFeeAmount: 0,
  netRevenue: 0
})

const animateValue = (key, target, duration = 420) => {
  const start = animatedSummary.value[key] ?? 0
  if (prefersReducedMotion.value) {
    animatedSummary.value[key] = target
    return
  }
  const startTime = performance.now()
  const step = (now) => {
    const progress = Math.min((now - startTime) / duration, 1)
    const value = start + (target - start) * progress
    animatedSummary.value[key] = Math.round(value)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

watch(
    () => [currentYearSummary.value, netRevenue.value],
    () => {
      animateValue('totalRevenue', currentYearSummary.value?.totalRevenue ?? 0)
      animateValue('platformFeeAmount', currentYearSummary.value?.platformFeeAmount ?? 0)
      animateValue('netRevenue', netRevenue.value ?? 0)
    },
    {immediate: true}
)

onMounted(() => {
  prefersReducedMotion.value = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
})

const summaryCards = computed(() => ([
  {
    label: `${summaryLabelPrefix.value} 확정 매출`,
    value: animatedSummary.value.totalRevenue ?? 0,
    tone: 'green',
    note: null
  },
  {
    label: `${summaryLabelPrefix.value} 플랫폼 수수료`,
    value: animatedSummary.value.platformFeeAmount ?? 0,
    tone: 'orange',
    note: null
  },
  {
    label: `${summaryLabelPrefix.value} 순매출`,
    value: animatedSummary.value.netRevenue ?? 0,
    tone: 'blue',
    note: null
  }
]))

const hasRevenueData = computed(() => {
  return chartSeries.value.some((item) => item.revenue > 0)
})

const monthlyItems = computed(() => {
  const year = Number(selectedYear.value)
  const byMonth = new Map(currentYearTrend.value.map((item) => [Number(item.month), Number(item.revenue ?? 0)]))

  const months = Array.from({length: 12}, (_, idx) => {
    const month = idx + 1
    const amount = byMonth.get(month) ?? 0
    const prev = idx > 0 ? (byMonth.get(month - 1) ?? 0) : 0
    return {year, month, amount, prev}
  })

  const yearTotal = months.reduce((sum, item) => sum + item.amount, 0)
  const platformRate = Number(currentYearSummary.value?.platformFeeRate ?? 0)

  const items = months.map((item) => {
    const momValue = item.prev > 0
        ? ((item.amount - item.prev) / item.prev) * 100
        : null

    const momText = momValue !== null
        ? `전월 대비 ${formatPercent(momValue, true)}`
        : '전월 데이터 없음'

    const shareValue = yearTotal > 0 ? (item.amount / yearTotal) * 100 : 0
    const shareValueText = formatPercent(shareValue, false)

    const feeAmount = item.amount * platformRate

    return {
      ...item,
      yearTotal,
      label: monthLabel(year, item.month),

      // ✅ 카드용(라벨 포함) - 모바일에서 사용
      momText,
      shareText: `연간 비중 ${shareValueText}`,

      // ✅ PC 테이블용(값만) - 헤더와 중복 라벨 제거
      momTableText: momValue !== null ? formatPercent(momValue, true) : '-',
      shareTableText: shareValueText,

      feeAmount,
      netAmount: item.amount - feeAmount
    }
  })

  const filtered = includeZero.value ? items : items.filter((item) => item.amount > 0)
  return filtered.slice().reverse()
})

const dailyItemsAll = computed(() => {
  if (selectedMonth.value === 'all') return []
  const year = Number(selectedYear.value)
  const month = Number(selectedMonth.value)
  const daysInMonth = new Date(year, month, 0).getDate()
  const amountByDay = new Map()
  currentYearDetails.value.forEach((item) => {
    if (!item?.period) return
    const parts = String(item.period).split('-')
    const day = Number(parts[2])
    if (!Number.isFinite(day)) return
    amountByDay.set(day, Number(item.revenue ?? 0))
  })
  const items = Array.from({length: daysInMonth}, (_, idx) => {
    const day = idx + 1
    const amount = amountByDay.get(day) ?? 0
    const prevAmount = day > 1 ? (amountByDay.get(day - 1) ?? 0) : 0
    const dodText = prevAmount > 0
        ? `전일 대비 ${formatPercent(((amount - prevAmount) / prevAmount) * 100, true)}`
        : '전일 데이터 없음'
    const dodTextMobile = prevAmount > 0 ? dodText : '-'
    const date = new Date(year, month - 1, day)
    return {
      day,
      amount,
      date,
      label: formatDate(date),
      dodText,
      dodTextMobile,
      anchorId: `day-${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    }
  })
  return items
})

const dailyItems = computed(() => {
  const items = includeZero.value ? dailyItemsAll.value : dailyItemsAll.value.filter((item) => item.amount > 0)
  return items.slice().reverse()
})

const dailyStats = computed(() => {
  const items = dailyItemsAll.value
  if (!items.length) {
    return {monthTotal: 0, peakLabel: '-', avgDaily: 0, peakAmount: 0}
  }
  const monthTotal = items.reduce((sum, item) => sum + item.amount, 0)
  const peakAmount = items.reduce((max, item) => Math.max(max, item.amount), 0)
  const peakItem = [...items].reverse().find((item) => item.amount === peakAmount && peakAmount > 0)
  const daysWithRevenue = items.filter((item) => item.amount > 0)
  const avgDaily = daysWithRevenue.length
      ? Math.round(daysWithRevenue.reduce((sum, item) => sum + item.amount, 0) / daysWithRevenue.length)
      : 0
  return {
    monthTotal,
    peakLabel: peakItem ? peakItem.label : '-',
    avgDaily,
    peakAmount
  }
})

const weeklyItems = computed(() => {
  if (selectedMonth.value === 'all') return []
  const year = Number(selectedYear.value)
  const month = Number(selectedMonth.value)
  const daysInMonth = new Date(year, month, 0).getDate()
  const firstDate = new Date(year, month - 1, 1)
  const lastDate = new Date(year, month - 1, daysInMonth)
  const weekStartDay = 1
  const weekMap = new Map()
  dailyItemsAll.value.forEach((item) => {
    const date = new Date(item.date)
    const day = date.getDay()
    const diff = (day - weekStartDay + 7) % 7
    const start = new Date(date)
    start.setDate(date.getDate() - diff)
    const key = start.toISOString().slice(0, 10)
    const entry = weekMap.get(key) ?? {start, amount: 0, firstPositiveDay: null}
    entry.amount += item.amount
    if (item.amount > 0 && entry.firstPositiveDay === null) {
      entry.firstPositiveDay = item.day
    }
    weekMap.set(key, entry)
  })
  const weeks = Array.from(weekMap.values()).map((entry) => {
    const rangeStart = new Date(Math.max(entry.start.getTime(), firstDate.getTime()))
    const rangeEnd = new Date(entry.start)
    rangeEnd.setDate(rangeEnd.getDate() + 6)
    const clippedEnd = new Date(Math.min(rangeEnd.getTime(), lastDate.getTime()))
    const label = `${String(rangeStart.getMonth() + 1).padStart(2, '0')}/${String(rangeStart.getDate()).padStart(2, '0')}~${String(clippedEnd.getMonth() + 1).padStart(2, '0')}/${String(clippedEnd.getDate()).padStart(2, '0')}`
    const anchorDay = includeZero.value
        ? rangeStart.getDate()
        : (entry.firstPositiveDay ?? rangeStart.getDate())
    const anchorId = `day-${year}-${String(month).padStart(2, '0')}-${String(anchorDay).padStart(2, '0')}`
    return {...entry, rangeStart, rangeEnd: clippedEnd, label, anchorId}
  })
  const filtered = includeZero.value ? weeks : weeks.filter((week) => week.amount > 0)
  return filtered.sort((a, b) => b.rangeStart - a.rangeStart)
})

const isEmptyDetails = computed(() => {
  if (selectedMonth.value === 'all') {
    return monthlyItems.value.length === 0
  }
  return dailyItems.value.length === 0
})

const scrollToAnchor = (anchorId) => {
  if (!anchorId) return
  nextTick(() => {
    const target = document.getElementById(anchorId)
    target?.scrollIntoView({behavior: 'smooth', block: 'start'})
  })
}
</script>

<template>
  <div class="revenue-view">
    <HostGateNotice
      v-if="gateVisible"
      :title="gateNotice.title"
      :description="gateNotice.description"
      :reason="gateNotice.reason"
      :primary-text="gateNotice.primaryText"
      :primary-to="gateNotice.primaryTo"
      :secondary-text="gateNotice.secondaryText"
      :secondary-to="gateNotice.secondaryTo"
    />

    <template v-else>
    <div class="host-view-header">
      <div>
        <h2 class="host-title">매출 리포트</h2>
        <p class="host-subtitle">{{ selectedPeriodLabel }}</p>
      </div>
    </div>

    <div class="header-actions">
      <select v-model="selectedYear" class="year-select">
        <option v-for="year in years" :key="year" :value="year">{{ year }}년</option>
      </select>
      <select v-model="selectedMonth" class="year-select">
        <option value="all">전체</option>
        <option v-for="month in months.slice(1)" :key="month" :value="month">{{ Number(month) }}월</option>
      </select>
      <details class="admin-dropdown">
        <summary class="admin-btn admin-btn--ghost">다운로드</summary>
        <div class="admin-dropdown__menu">
          <button class="admin-btn admin-btn--ghost admin-dropdown__item" type="button" @click="downloadReport('csv')">
            CSV
          </button>
          <button class="admin-btn host-btn--primary admin-dropdown__item" type="button"
                  @click="downloadReport('xlsx')">
            XLSX
          </button>
        </div>
      </details>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards" :class="{ 'fade-section': !isLoading }">
      <div v-if="isLoading" class="summary-skeleton">
        <div v-for="i in 3" :key="i" class="skeleton-card"/>
      </div>
      <div v-else-if="loadError" class="status-card">
        <p>데이터를 불러오지 못했어요.</p>
        <button class="ghost-btn" type="button" @click="loadRevenue(selectedYear, selectedMonth)">다시 시도</button>
      </div>
      <div v-for="(card, index) in summaryCards" :key="card.label" class="summary-card"
           :class="{ 'fade-item': !isLoading }"
           :style="{ animationDelay: `${Math.min(index, 5) * 70}ms` }">
        <div class="card-icon" :class="`${card.tone}-bg`">💲</div>
        <p class="card-label">{{ card.label }}</p>
        <h3 class="card-value">{{ formatCurrency(card.value) }}</h3>
        <p v-if="card.note" class="card-sub">{{ card.note }}</p>
      </div>
    </div>

    <!-- Monthly Revenue Chart -->
    <div class="chart-section" :class="{ 'fade-section': !isLoading }">
      <h3>{{ isDailyView ? `${selectedMonth}월 일별 매출` : '월별 매출 추이' }}</h3>
      <div v-if="isLoading" class="chart-skeleton"/>
      <div v-else-if="!hasRevenueData" class="status-card">
        <p>{{ emptyMessage }}</p>
        <button class="ghost-btn" type="button" @click="selectedMonth = 'all'">기간 변경</button>
      </div>
      <div v-else class="bar-chart" :class="[isDailyView ? 'daily' : 'monthly', { animate: animateCharts }]">
        <div
            v-for="data in chartSeries"
            :key="data.label"
            class="bar-column"
        >
          <div class="bar-container">
            <div class="bar" :style="{ height: getBarHeight(data.revenue) }">
              <span class="tooltip">{{ formatCurrency(data.revenue) }}</span>
            </div>
          </div>
          <span class="month-label">{{ data.label }}</span>
        </div>
      </div>
    </div>

    <!-- Detailed Stats List -->
    <div class="stats-list" :class="{ 'fade-section': !isLoading }">
      <div class="stats-header">
        <h3>{{ isDailyView ? '일별 상세 내역' : '월별 상세 내역' }}</h3>
        <label class="zero-toggle">
          <input v-model="includeZero" type="checkbox"/>
          <span>0원 포함</span>
        </label>
      </div>

      <div v-if="isDailyView" class="daily-summary-bar">
        <div class="summary-item">
          <span class="summary-label">이번 달 확정 매출</span>
          <span class="summary-value">{{ formatCurrency(dailyStats.monthTotal) }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">최고 매출일</span>
          <span class="summary-value">{{ dailyStats.peakLabel }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">평균 일매출(매출 발생일 기준)</span>
          <span class="summary-value">{{ formatCurrency(dailyStats.avgDaily) }}</span>
        </div>
      </div>

      <div v-if="isDailyView && !isLoading && !loadError && weeklyItems.length" class="weekly-summary">
        <div class="weekly-header">
          <h4>주간 합계</h4>
        </div>
        <div class="week-cards">
          <button
              v-for="week in weeklyItems"
              :key="week.label"
              class="week-card"
              type="button"
              @click="scrollToAnchor(week.anchorId)"
          >
            <span class="week-range">{{ week.label }}</span>
            <span class="week-amount">{{ formatCurrency(week.amount) }}</span>
          </button>
        </div>
      </div>

      <div v-if="isLoading" class="list-skeleton">
        <div v-for="i in 5" :key="i" class="skeleton-row"/>
      </div>
      <div v-else-if="loadError" class="status-card">
        <p>데이터를 불러오지 못했어요.</p>
        <button class="ghost-btn" type="button" @click="loadRevenue(selectedYear, selectedMonth)">다시 시도</button>
      </div>
      <div v-else-if="isEmptyDetails" class="status-card">
        <p>{{ emptyMessage }}</p>
        <button class="ghost-btn" type="button" @click="includeZero = true">0원 포함 보기</button>
      </div>
      <div v-else class="details-content">
        <div class="mobile-only">
          <div v-if="!isDailyView" class="detail-cards">
            <div v-for="item in monthlyItems" :key="item.label" class="detail-card">
              <div class="card-row">
                <span class="card-title">{{ item.label }}</span>
                <span class="card-amount">{{ formatCurrency(item.amount) }}</span>
              </div>
              <div class="card-row sub">
                <span>{{ item.momText }}</span>
                <span class="card-sub-right">{{ item.shareText }}</span>
              </div>
              <details class="card-accordion">
                <summary>상세</summary>
                <div class="accordion-row">
                  <span>플랫폼 수수료</span>
                  <span class="card-amount">{{ formatCurrency(item.feeAmount) }}</span>
                </div>
                <div class="accordion-row">
                  <span>순매출</span>
                  <span class="card-amount">{{ formatCurrency(item.netAmount) }}</span>
                </div>
              </details>
            </div>
          </div>

          <div v-else class="detail-cards">
            <p class="mobile-note">전일 데이터 없음은 '-'로 표시됩니다.</p>
            <div
                v-for="item in dailyItems"
                :id="item.anchorId"
                :key="item.anchorId"
                class="detail-card"
                :class="{ peak: item.amount === dailyStats.peakAmount && item.amount > 0 }"
            >
              <div class="card-row">
                <div class="card-title-group">
                  <span class="card-title">{{ item.label }}</span>
                  <span v-if="item.amount === dailyStats.peakAmount && item.amount > 0" class="peak-badge">최고</span>
                </div>
                <span class="card-amount">{{ formatCurrency(item.amount) }}</span>
              </div>
              <div class="card-row sub">
                <span>{{ item.dodTextMobile }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="desktop-only">
          <table v-if="!isDailyView" class="detail-table">
            <colgroup>
              <col class="col-period" style="width:120px"/>
              <col class="col-amount" style="width:220px"/>
              <col class="col-mom" style="width:200px"/>
              <col class="col-share" style="width:160px"/>
            </colgroup>

            <thead>
            <tr>
              <th>기간</th>
              <th class="align-right">매출액</th>
              <th class="align-right">전월 대비</th>
              <th class="align-right">연간 비중</th>
            </tr>
            </thead>

            <tbody>
            <tr v-for="item in monthlyItems" :key="item.label">
              <td>{{ item.label }}</td>
              <td class="align-right">{{ formatCurrency(item.amount) }}</td>
              <td class="align-right">{{ item.momTableText }}</td>
              <td class="align-right">{{ item.shareTableText }}</td>
            </tr>
            </tbody>
          </table>


          <table v-else class="detail-table">
            <colgroup>
              <col style="width:120px"/>
              <col style="width:220px"/>
              <col style="width:200px"/>
            </colgroup>
            <thead>
            <tr>
              <th>날짜</th>
              <th class="align-right">매출액</th>
              <th class="align-right">전일 대비</th>
            </tr>
            </thead>
            <tbody>
            <tr
                v-for="item in dailyItems"
                :key="item.anchorId"
                :class="{ peak: item.amount === dailyStats.peakAmount && item.amount > 0 }"
            >
              <td>{{ item.label }}</td>
              <td class="align-right">{{ formatCurrency(item.amount) }}</td>
              <td class="align-right">{{ item.dodText }}</td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>

<style scoped>
.revenue-view {
  padding-bottom: calc(2rem + var(--bn-h, 0px) + (var(--bn-pad, 0px) * 2) + env(safe-area-inset-bottom));
}

/* ✅ 대시보드 톤: 헤더 선명/굵게 + 모바일 퍼스트 스택 */
.view-header {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.view-header h2 {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--brand-accent);
  margin: 0.15rem 0 0.2rem;
  letter-spacing: -0.01em;
}

.subtitle {
  color: #6b7280;
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0;
}

.year-select {
  padding: 0.65rem 0.9rem;
  border: 1px solid var(--brand-border);
  border-radius: 12px;
  font-size: 0.95rem;
  font-weight: 800;
  color: #0f172a;
  outline: none;
  background: white;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 6;
  background: var(--brand-bg);
  padding: 0.6rem 0;
}

.status-card {
  width: 100%;
  padding: 1rem;
  border-radius: 12px;
  border: 1px dashed var(--brand-border);
  background: var(--brand-bg);
  text-align: center;
  display: grid;
  gap: 0.5rem;
}

.status-card p {
  margin: 0;
  color: #6b7280;
  font-weight: 700;
}

.ghost-btn {
  border: 1px solid var(--brand-border);
  background: white;
  color: var(--brand-accent);
  border-radius: 10px;
  padding: 0.55rem 0.9rem;
  font-weight: 800;
  min-height: 44px;
  cursor: pointer;
}

.fade-section {
  animation: fadeUp 240ms ease both;
}

.fade-item {
  animation: fadeUp 240ms ease both;
}

/* Summary Cards (모바일: 세로, 태블릿+: 3열) */
.summary-cards {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: 0.25rem;
  position: relative;
}

.summary-skeleton {
  display: flex;
  gap: 0.75rem;
  width: 100%;
}

.skeleton-card {
  flex: 0 0 78%;
  height: 140px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
}

.summary-card {
  background: white;
  border-radius: 16px;
  padding: 1.25rem 1.25rem 1.2rem;
  position: relative;
  border: 1px solid var(--brand-border);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  flex: 0 0 78%;
  scroll-snap-align: start;
}

.card-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  margin-bottom: 0.9rem;
}

.green-bg {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.blue-bg {
  background: #E3F2FD;
  color: #1565C0;
}

.orange-bg {
  background: #FFF3E0;
  color: #B45309;
}

.trend-icon {
  position: absolute;
  top: 1.15rem;
  right: 1.15rem;
  font-weight: 900;
}

.trend-icon.up {
  color: var(--brand-accent);
}

.trend-icon.down {
  color: #b45309;
}

.card-label {
  font-size: 0.92rem;
  color: #6b7280;
  font-weight: 700;
  margin: 0 0 0.45rem;
}

.card-value {
  font-size: 1.55rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 0.4rem;
}

.card-trend {
  font-size: 0.86rem;
  font-weight: 800;
  margin: 0;
}

.card-trend.positive {
  color: var(--brand-accent);
}

.card-sub {
  font-size: 0.86rem;
  font-weight: 700;
  color: #1565C0;
  margin: 0;
}

/* Chart Section */
.chart-section {
  background: white;
  padding: 1.25rem;
  border-radius: 16px;
  margin-bottom: 1.5rem;
  border: 1px solid var(--brand-border);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
}

.chart-section h3 {
  font-size: 1.1rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 1rem;
}

/* ✅ 모바일에서 12개월 막대가 답답해서: 가로 스크롤 허용(템플릿 변경 없이 CSS만) */
.bar-chart {
  align-items: flex-end;
  height: 260px;
  padding-top: 2rem; /* tooltip 공간 */
}

.bar-chart.monthly {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 0.35rem;
}

.bar-chart.daily {
  display: flex;
  overflow-x: auto;
  gap: 0.5rem;
}

.bar-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.bar-chart.daily .bar-column {
  flex: 0 0 36px;
}

.bar-container {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  position: relative;
}

.bar {
  width: 100%;
  background: var(--brand-primary);
  border-radius: 6px 6px 0 0;
  transition: height 0.3s ease, background 0.2s;
  position: relative;
  cursor: pointer;
  transform-origin: bottom;
}

.chart-skeleton {
  height: 260px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
}

.bar-chart.animate .bar {
  animation: barGrow 480ms ease;
}

.bar:hover {
  background: var(--brand-primary-strong);
}

.tooltip {
  position: absolute;
  top: -26px;
  left: 50%;
  transform: translateX(-50%);
  background: #111827;
  color: white;
  font-size: 0.72rem;
  padding: 4px 8px;
  border-radius: 6px;
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}

.bar:hover .tooltip {
  opacity: 1;
}

.month-label {
  font-size: 0.82rem;
  color: #6b7280;
  font-weight: 700;
  margin-top: 0.45rem;
}

/* Stats List */
.stats-list {
  background: white;
  padding: 1.25rem;
  border-radius: 16px;
  border: 1px solid var(--brand-border);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
}

.stats-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.stats-header h3 {
  font-size: 1.1rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0;
}

.zero-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-weight: 800;
  color: #0f172a;
  font-size: 0.9rem;
}

.zero-toggle input {
  width: 18px;
  height: 18px;
  accent-color: var(--brand-primary-strong);
}

.mobile-note {
  margin: 0 0 0.75rem;
  font-size: 0.82rem;
  font-weight: 700;
  color: #94a3b8;
}

.daily-summary-bar {
  display: grid;
  gap: 0.6rem;
  padding: 0.85rem 1rem;
  border-radius: 12px;
  border: 1px solid var(--brand-border);
  background: var(--brand-bg);
  margin-bottom: 1rem;
  position: sticky;
  top: 0;
  z-index: 3;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-weight: 800;
  font-size: 0.92rem;
}

.summary-label {
  color: #64748b;
}

.summary-value {
  color: #0f172a;
  text-align: right;
}

.weekly-summary {
  margin-bottom: 1rem;
}

.weekly-header h4 {
  font-size: 1rem;
  font-weight: 900;
  color: #0f172a;
  margin: 0 0 0.6rem;
}

.week-cards {
  display: grid;
  gap: 0.6rem;
}

.week-card {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  width: 100%;
  padding: 0.85rem 1rem;
  border-radius: 12px;
  border: 1px solid var(--brand-border);
  background: white;
  font-weight: 800;
  cursor: pointer;
  text-align: left;
}

.week-range {
  color: #0f172a;
}

.week-amount {
  color: #0f172a;
  text-align: right;
}

.details-content {
  display: grid;
  gap: 1rem;
}

.detail-cards {
  display: grid;
  gap: 0.75rem;
}

.detail-card {
  border-radius: 14px;
  border: 1px solid var(--brand-border);
  padding: 0.9rem 1rem;
  background: white;
  display: grid;
  gap: 0.55rem;
}

.detail-card.peak {
  border-color: var(--brand-primary-strong);
  box-shadow: 0 0 0 2px var(--brand-primary-strong);
}

.card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.card-row.sub {
  font-size: 0.88rem;
  color: #64748b;
  font-weight: 700;
}

.card-title-group {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.card-title {
  font-weight: 900;
  color: #0f172a;
}

.card-amount {
  font-weight: 900;
  color: #0f172a;
  text-align: right;
}

.card-sub-right {
  text-align: right;
}

.peak-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  background: var(--brand-primary-strong);
  color: var(--brand-on-primary);
  font-size: 0.72rem;
  font-weight: 800;
}

.card-accordion {
  border-top: 1px dashed #e2e8f0;
  padding-top: 0.6rem;
  font-weight: 800;
  color: #0f172a;
}

.card-accordion summary {
  cursor: pointer;
  font-size: 0.88rem;
  color: #475569;
  margin-bottom: 0.4rem;
}

.accordion-row {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.9rem;
  padding: 0.2rem 0;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.95rem;
}

.detail-table th,
.detail-table td {
  padding: 0.85rem 0.6rem;
  border-bottom: 1px solid #eef2f7;
}

.detail-table th {
  text-align: left;
  color: #475569;
  font-weight: 900;
  background: #f8fafc;
}

.detail-table tr.peak td {
  background: var(--brand-primary);
}

.align-right {
  font-variant-numeric: tabular-nums;
}

.mobile-only {
  display: block;
}

.desktop-only {
  display: none;
}

.list-skeleton {
  display: grid;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.skeleton-row {
  height: 18px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
}

@media (min-width: 768px) {
  .daily-summary-bar {
    position: static;
  }

  .weekly-summary {
    margin-bottom: 1.25rem;
  }

  .week-cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .mobile-only {
    display: none;
  }

  .desktop-only {
    display: block;
  }
}

/* ✅ PC에서만 컬럼폭 고정 + 흔들림 제거 */
@media (min-width: 768px) {
  .detail-table {
    table-layout: fixed;
  }

  .detail-table th.align-right,
  .detail-table td.align-right {
    text-align: right !important;
  }

  .detail-table .col-period {
    width: 120px;
  }

  .detail-table .col-amount {
    width: 220px;
  }

  .detail-table .col-mom {
    width: 200px;
  }

  .detail-table .col-share {
    width: 160px;
  }

  .detail-table th,
  .detail-table td {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

/* 태블릿+에서는 헤더 가로, 카드 3열, 차트 스크롤 해제 */
@media (min-width: 768px) {
  .view-header {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .header-actions {
    position: static;
    background: transparent;
    padding: 0;
  }

  .year-select {
    width: auto;
  }

  .summary-cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    display: grid;
    overflow: visible;
  }

  .summary-card {
    flex: 1;
  }

  .summary-skeleton,
  .skeleton-card {
    flex: 1;
  }

  .bar-chart {
    overflow-x: visible;
    justify-content: space-between;
    gap: 0;
    height: 320px;
  }

  .bar-column {
    flex: 1;
  }

  .bar {
    width: 60%;
  }
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes shimmer {
  from {
    background-position: 200% 0;
  }
  to {
    background-position: -200% 0;
  }
}

@keyframes barGrow {
  from {
    transform: scaleY(0);
  }
  to {
    transform: scaleY(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .fade-section,
  .fade-item,
  .bar-chart.animate .bar,
  .skeleton-card,
  .skeleton-row {
    animation: none !important;
  }
}
</style>
