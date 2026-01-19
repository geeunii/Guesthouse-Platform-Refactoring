<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHostAccommodations } from '@/api/hostAccommodation'
import { deriveHostGateInfo, buildHostGateNotice } from '@/composables/useHostState'
import HostGateNotice from '@/components/host/HostGateNotice.vue'
import { getAccessToken, getCurrentUser, getUserInfo, saveUserInfo } from '@/api/authClient'
import {
  fetchHostReviewReportSummary,
  fetchHostReviewReportTrend,
  fetchHostThemeReport,
  fetchHostDemandForecast,
  fetchHostAiInsight,
  fetchHostAiInsightEligibility
} from '@/api/hostReport'
import { formatCurrency, formatDate } from '@/utils/formatters'

const tabs = [
  { id: 'reviews', label: '리뷰 리포트' },
  { id: 'themes', label: '테마 인기' },
  { id: 'forecast', label: '수요 예측' }
]

const activeTab = ref('reviews')
const router = useRouter()
const userInfo = ref(getUserInfo())
const authReady = ref(false)
const isHostUser = computed(() => {
  if (!userInfo.value) return false
  return (
    userInfo.value.role === 'HOST' ||
    userInfo.value.role === 'ROLE_HOST' ||
    userInfo.value.hostApproved === true
  )
})
const accommodations = ref([])
const accommodationLoading = ref(false)
const accommodationError = ref('')

const todayISO = () => new Date().toISOString().slice(0, 10)
const daysAgoISO = (days) => {
  const date = new Date()
  date.setDate(date.getDate() - days)
  return date.toISOString().slice(0, 10)
}

const defaultFrom = daysAgoISO(30)
const defaultTo = todayISO()

const reviewFilters = ref({
  accommodationId: 'all',
  from: defaultFrom,
  to: defaultTo
})
const reviewPreset = ref('30days')
const themePreset = ref('30days')
const reviewSummary = ref(null)
const reviewTrend = ref([])
const reviewLoading = ref(false)
const reviewError = ref('')
const compareSummary = ref(null)
const compareLoading = ref(false)
const compareError = ref('')
const showCompare = ref(false)
const aiInsightState = ref({
  REVIEW: { data: null, loading: false, error: '' },
  THEME: { data: null, loading: false, error: '' },
  DEMAND: { data: null, loading: false, error: '' }
})
const expandedReviews = ref({})
const formatNumber = (value) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) return '0'
  return new Intl.NumberFormat('ko-KR').format(numberValue)
}

const formatKrw = (value) => `${formatNumber(value)}원`

const formatGeneratedAt = (value) => {
  if (!value) return '-'
  const normalized = String(value)
    .replace(/\[[^\]]+]/g, '')
    .replace(/(\.\d{3})\d+/g, '$1')
    .trim()
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return '-'
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const formatRating = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '-'
  return numeric.toFixed(1)
}

const formatIsoDate = (date) => {
  if (!date) return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const parseIsoDate = (iso) => {
  if (!iso) return null
  const parsed = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(parsed.getTime())) return null
  return parsed
}

const getPreviousRange = (from, to) => {
  const start = parseIsoDate(from)
  const end = parseIsoDate(to)
  if (!start || !end) return null
  if (end < start) return null
  const diffDays = Math.round((end - start) / (1000 * 60 * 60 * 24))
  const prevEnd = new Date(start)
  prevEnd.setDate(prevEnd.getDate() - 1)
  const prevStart = new Date(prevEnd)
  prevStart.setDate(prevStart.getDate() - diffDays)
  return {
    from: formatIsoDate(prevStart),
    to: formatIsoDate(prevEnd)
  }
}

const calcLowRatingRatio = (summary) => {
  const total = Number(summary?.reviewCount ?? 0)
  if (!total) return null
  const dist = summary?.ratingDistribution ?? {}
  const low = Number(dist?.[1] ?? 0) + Number(dist?.[2] ?? 0) + Number(dist?.[3] ?? 0)
  return low / total
}

const formatDelta = (current, previous, type) => {
  const curr = Number(current)
  const prev = Number(previous)
  if (!Number.isFinite(curr) || !Number.isFinite(prev)) return null
  if (type === 'count') {
    const diff = curr - prev
    const sign = diff > 0 ? '+' : ''
    const pct = prev > 0 ? `${sign}${((diff / prev) * 100).toFixed(1)}%` : null
    const value = `${sign}${formatNumber(diff)}`
    return pct ? `${value} (${pct})` : value
  }
  if (type === 'rating') {
    const diff = curr - prev
    const sign = diff > 0 ? '+' : ''
    return `${sign}${diff.toFixed(1)}`
  }
  if (type === 'ratio') {
    const diff = (curr - prev) * 100
    const sign = diff > 0 ? '+' : ''
    return `${sign}${diff.toFixed(1)}%p`
  }
  return null
}

const reviewAiInsight = computed(() => aiInsightState.value.REVIEW.data)
const themeAiInsight = computed(() => aiInsightState.value.THEME.data)
const demandAiInsight = computed(() => aiInsightState.value.DEMAND.data)
const reviewAiHasContent = computed(() => {
  if (!reviewAiInsight.value) return false
  return Array.isArray(reviewAiInsight.value?.sections) && reviewAiInsight.value.sections.length > 0
})

const themeFilters = ref({
  accommodationId: 'all',
  from: defaultFrom,
  to: defaultTo,
  metric: 'reservations'
})
const themeLoading = ref(false)
const themeError = ref('')
const themeReport = ref(null)

const forecastFilters = ref({
  accommodationId: 'all',
  target: 'reservations',
  horizonDays: 30,
  historyDays: 180
})
const forecastLoading = ref(false)
const forecastError = ref('')
const forecastReport = ref(null)
const forecastViewMode = ref('table')
const isDesktop = ref(false)

const hostGateInfo = computed(() => deriveHostGateInfo(accommodations.value))
const canUseHostFeatures = computed(() => hostGateInfo.value.gateState === 'APPROVED')
const gateVisible = computed(() => !canUseHostFeatures.value && hostGateInfo.value.hostState !== 'loading')
const gateNotice = computed(() => buildHostGateNotice(hostGateInfo.value))

const reviewRatingEntries = computed(() => {
  const distribution = reviewSummary.value?.ratingDistribution || {}
  const total = (reviewSummary.value?.reviewCount ?? 0) || 0
  return [5, 4, 3, 2, 1].map((rating) => {
    const count = distribution[rating] ?? 0
    const percent = total > 0 ? Math.round((count / total) * 100) : 0
    return { rating, count, percent }
  })
})

const reviewHasData = computed(() => (reviewSummary.value?.reviewCount ?? 0) > 0)
const eligibility = ref({ review: null, theme: null, demand: null })
const eligibilityError = ref('')

const fallbackEligibility = {
  status: 'OK',
  canGenerate: true,
  disabledReason: '',
  warningMessage: '',
  current: 0,
  minRequired: 0,
  recommended: 0,
  unitLabel: ''
}

const getEligibilityInfo = (tab) => {
  const state = getInsightState(tab)
  if (state?.data?.meta) {
    return state.data.meta
  }
  if (tab === 'REVIEW') return eligibility.value.review ?? fallbackEligibility
  if (tab === 'THEME') return eligibility.value.theme ?? fallbackEligibility
  return eligibility.value.demand ?? fallbackEligibility
}

const isWarnStatus = (tab) => getEligibilityInfo(tab).status === 'WARN'
const isBlockedStatus = (tab) => getEligibilityInfo(tab).status === 'BLOCKED'

const canGenerateAi = (tab) => getEligibilityInfo(tab).canGenerate && !getInsightState(tab).loading
const compareRange = computed(() => getPreviousRange(reviewFilters.value.from, reviewFilters.value.to))
const compareLabel = computed(() => {
  if (!compareRange.value) return ''
  return `직전 기간 ${compareRange.value.from} ~ ${compareRange.value.to}`
})
const lowRatingRatio = computed(() => calcLowRatingRatio(reviewSummary.value))
const compareLowRatingRatio = computed(() => calcLowRatingRatio(compareSummary.value))
const compareDeltas = computed(() => ({
  reviewCount: compareSummary.value ? formatDelta(reviewSummary.value?.reviewCount, compareSummary.value?.reviewCount, 'count') : null,
  avgRating: compareSummary.value ? formatDelta(reviewSummary.value?.avgRating, compareSummary.value?.avgRating, 'rating') : null,
  lowRatio: (lowRatingRatio.value == null || compareLowRatingRatio.value == null)
    ? null
    : formatDelta(lowRatingRatio.value, compareLowRatingRatio.value, 'ratio')
}))
const compareItems = computed(() => {
  if (!compareSummary.value) return []
  const items = []
  if (compareDeltas.value.reviewCount) {
    items.push({ label: '리뷰 수', value: compareDeltas.value.reviewCount })
  }
  if (compareDeltas.value.avgRating) {
    items.push({ label: '평균 평점', value: compareDeltas.value.avgRating })
  }
  if (compareDeltas.value.lowRatio) {
    items.push({ label: '저평점 비율(3점 이하)', value: compareDeltas.value.lowRatio })
  }
  return items
})
const compareEnabled = computed(() => compareItems.value.length > 0 || compareLoading.value)
const kpiItems = computed(() => {
  const reviewCount = reviewSummary.value?.reviewCount
  const avgRating = reviewSummary.value?.avgRating
  const lowRatio = lowRatingRatio.value
  const topTag = topTagLabel.value
  return [
    {
      label: '리뷰 수',
      value: Number.isFinite(Number(reviewCount)) ? `${formatNumber(reviewCount)}건` : '-',
      delta: compareSummary.value ? formatDelta(reviewCount, compareSummary.value?.reviewCount, 'count') : null
    },
    {
      label: '평균 평점',
      value: formatRating(avgRating),
      delta: compareSummary.value ? formatDelta(avgRating, compareSummary.value?.avgRating, 'rating') : null
    },
    {
      label: '저평점 비율(3점 이하)',
      tooltip: '선택 기간 리뷰 중 평점 3점 이하 비율(= 3점 이하 리뷰 수 / 전체 리뷰 수)',
      value: lowRatio == null ? '-' : `${(lowRatio * 100).toFixed(1)}%`,
      delta: compareLowRatingRatio.value == null ? null : formatDelta(lowRatio, compareLowRatingRatio.value, 'ratio')
    },
    {
      label: '대표 태그',
      value: topTag && topTag !== '데이터 없음' ? topTag : '-',
      delta: null
    }
  ]
})

const buildAiMetaChips = ({ periodLabel, basisLabel, generatedAt }) => {
  const chips = []
  if (periodLabel) chips.push(periodLabel)
  if (basisLabel) chips.push(basisLabel)
  if (generatedAt) {
    chips.push(`생성 ${formatGeneratedAt(generatedAt)}`)
  } else {
    chips.push('생성 전')
  }
  return chips
}

const reviewAiMetaChips = computed(() => {
  const periodLabel = reviewFilters.value.from && reviewFilters.value.to
    ? `기간 ${reviewFilters.value.from} ~ ${reviewFilters.value.to}`
    : ''
  const count = formatNumber(reviewSummary.value?.reviewCount ?? 0)
  const basisLabel = `리뷰 ${count}건 기준`
  return buildAiMetaChips({
    periodLabel,
    basisLabel,
    generatedAt: reviewAiInsight.value?.generatedAt
  })
})

const themeAiMetaChips = computed(() => {
  const periodLabel = themeFilters.value.from && themeFilters.value.to
    ? `기간 ${themeFilters.value.from} ~ ${themeFilters.value.to}`
    : ''
  const basisLabel = themeFilters.value.metric === 'revenue'
    ? `매출 ${formatNumber(themeTotals.value.revenue)}원 기준`
    : `예약 ${formatNumber(themeTotals.value.reservations)}건 기준`
  return buildAiMetaChips({
    periodLabel,
    basisLabel,
    generatedAt: themeAiInsight.value?.generatedAt
  })
})

const demandAiMetaChips = computed(() => {
  const periodLabel = `기간 향후 ${formatNumber(forecastFilters.value.horizonDays)}일`
  const predictedTotal = forecastReport.value?.forecastSummary?.predictedTotal ?? 0
  const basisLabel = forecastFilters.value.target === 'revenue'
    ? `매출 ${formatNumber(predictedTotal)}원 기준`
    : `예약 ${formatNumber(predictedTotal)}건 기준`
  return buildAiMetaChips({
    periodLabel,
    basisLabel,
    generatedAt: demandAiInsight.value?.generatedAt
  })
})

const aiRiskRecommendations = computed(() => ([
  '저평점(3점 이하) 리뷰 추이 주 1회 점검',
  '청결/소음/응대 관련 키워드 급증 여부 확인',
  '이상 징후 발생 시 대응 프로세스 공유'
]))

const getInsightState = (tab) => aiInsightState.value[tab]
const hasInsight = (tab) => {
  const data = getInsightState(tab)?.data
  return Array.isArray(data?.sections) && data.sections.length > 0
}
const getAiButtonLabel = (tab) => {
  const state = getInsightState(tab)
  if (state?.loading) return '생성 중...'
  return hasInsight(tab) ? 'AI 요약 재생성' : 'AI 요약 생성'
}

const normalizeSections = (insight) => {
  return Array.isArray(insight?.sections) ? insight.sections : []
}

const getSectionItems = (insight, title) => {
  const section = normalizeSections(insight).find((item) => item.title === title)
  return section?.items ?? []
}

const buildActionItems = (insight, sectionTitle) => {
  const actions = getSectionItems(insight, sectionTitle)
  return actions.map((text, index) => {
    const parsed = parseInsightItem(text)
    const priority = index === 0 ? '즉시' : index <= 2 ? '이번주' : '상시'
    const tone = index === 0 ? 'urgent' : index <= 2 ? 'recommended' : 'improve'
    return {
      text,
      priority,
      tone,
      main: parsed.main,
      evidence: parsed.evidence,
      showEvidence: parsed.showEvidence
    }
  })
}

const aiActionsWithPriority = computed(() => (
  buildActionItems(reviewAiInsight.value, '다음 액션')
))

const themeActionsWithPriority = computed(() => (
  buildActionItems(themeAiInsight.value, '다음 액션')
))

const reviewRiskItems = computed(() => getSectionItems(reviewAiInsight.value, '주의·리스크'))
const reviewRisksCompact = computed(() => {
  if (reviewRiskItems.value.length === 0) return true
  return reviewRiskItems.value.every((item) => (
    item.includes('유의미한 리스크 없음') ||
    item.includes('데이터 부족') ||
    item.includes('특이 징후 없음') ||
    item.includes('이슈 징후 낮음')
  ))
})

const reviewRiskEmpty = computed(() => reviewRisksCompact.value)

const splitInsightLine = (line) => {
  if (!line) return { main: '-', evidence: '' }
  const raw = String(line)
  if (raw.includes('||')) {
    const [main, evidence] = raw.split('||')
    return { main: main.trim(), evidence: evidence?.trim() ?? '' }
  }
  const marker = '— 근거:'
  if (raw.includes(marker)) {
    const [main, evidence] = raw.split(marker)
    return { main: main.trim(), evidence: evidence?.trim() ?? '' }
  }
  return { main: raw.trim(), evidence: '' }
}

const normalizeInsightText = (text) => {
  if (!text) return ''
  return String(text)
    .replace(/부정 키워드 미감지/g, '불만 신호 낮음')
    .replace(/유의미한 부정 신호 없음/g, '불만 신호 낮음')
    .replace(/리스크 없음/g, '특이 징후 없음')
    .replace(/유의미한 리스크 없음/g, '특이 징후 없음')
    .replace(/저평점 리뷰 키워드 주간 모니터링/g, '저평점(3점 이하) 리뷰 추이 주 1회 점검')
    .replace(/데이터 부족/g, '표본이 적어 신뢰도 낮음')
    .trim()
}

const getEvidenceLabel = () => '근거'

const shouldShowEvidence = (mainText, evidenceText) => {
  if (!evidenceText) return false
  const evidence = String(evidenceText).trim()
  if (!evidence) return false
  const noiseSignals = ['데이터 부족', '데이터 없음', '리스크 없음', '특이 징후 없음', '이슈 징후 낮음']
  if (noiseSignals.some((signal) => evidence.includes(signal))) return false
  const main = String(mainText).trim()
  if (!main) return true
  if (main === evidence || main.includes(evidence)) return false
  const mainNumbers = (main.match(/\d+(?:\.\d+)?/g) ?? [])
  const evidenceNumbers = (evidence.match(/\d+(?:\.\d+)?/g) ?? [])
  if (evidenceNumbers.length > 0 && evidenceNumbers.every((num) => mainNumbers.includes(num))) {
    return false
  }
  return true
}

const getSectionItemsLimited = (insight, title, limit) => {
  const items = getSectionItems(insight, title)
  if (!Number.isFinite(limit)) return items
  return items.slice(0, limit)
}

const parseInsightItem = (item) => {
  const parsed = splitInsightLine(item)
  return {
    key: item,
    main: normalizeInsightText(parsed.main),
    evidence: normalizeInsightText(parsed.evidence),
    showEvidence: shouldShowEvidence(parsed.main, parsed.evidence)
  }
}

const getSectionItemsParsed = (insight, title, limit) => {
  const items = getSectionItemsLimited(insight, title, limit)
  return items.map(parseInsightItem)
}

const themeInsightLimits = {
  '트렌드 요약': 2,
  '강점': 2,
  '보완할 점': 2,
  '다음 액션': 3,
  '모니터링': 2,
  defaultLimit: 2
}

const demandInsightLimits = {
  '수요 예측 요약': 2,
  '기회 요인': 2,
  '리스크 요인': 2,
  '다음 액션': 3,
  '모니터링': 2,
  defaultLimit: 2
}

const demandMonitoringOpen = ref(false)
const demandMonitoringItems = computed(() => getSectionItems(demandAiInsight.value, '모니터링'))
const demandMonitoringDisplay = computed(() => (
  demandMonitoringOpen.value ? demandMonitoringItems.value : demandMonitoringItems.value.slice(0, 2)
))
const demandActionItems = computed(() => {
  const primary = getSectionItems(demandAiInsight.value, '다음 액션')
  const items = primary.length > 0
    ? primary
    : getSectionItems(demandAiInsight.value, '운영 액션 제안')
  return items.map(parseInsightItem)
})

const themeRows = computed(() => themeReport.value?.rows ?? [])
const themeShowAll = ref(false)
const themeVisibleLimit = computed(() => (isDesktop.value ? 8 : 5))
const themeVisibleRows = computed(() => {
  if (themeShowAll.value) return themeRows.value
  return themeRows.value.slice(0, themeVisibleLimit.value)
})
const themeTotals = computed(() => {
  return themeRows.value.reduce((acc, row) => {
    acc.reservations += Number(row.reservationCount ?? 0)
    acc.revenue += Number(row.revenueSum ?? 0)
    return acc
  }, { reservations: 0, revenue: 0 })
})
const themeTopRow = computed(() => {
  if (themeRows.value.length === 0) return null
  if ((themeTotals.value.reservations ?? 0) === 0) return null
  const key = themeFilters.value.metric === 'revenue' ? 'revenueSum' : 'reservationCount'
  return themeRows.value.reduce((best, row) => {
    const value = Number(row[key] ?? 0)
    if (!best) return row
    return value > Number(best[key] ?? 0) ? row : best
  }, null)
})
const themeKpis = computed(() => ([
  {
    label: '기간 예약수',
    value: `${formatNumber(themeTotals.value.reservations)}건`
  },
  {
    label: '기간 매출',
    value: `${formatNumber(themeTotals.value.revenue)}원`
  },
  {
    label: 'Top 테마',
    value: themeTopRow.value?.themeName ?? '데이터 없음'
  }
]))
const themeViewMode = ref('cards')
const isZeroValue = (value) => Number(value ?? 0) === 0
const forecastDaily = computed(() => forecastReport.value?.forecastDaily ?? [])
const forecastPeak = computed(() => {
  if (!forecastDaily.value.length) return null
  return forecastDaily.value.reduce((best, row) => {
    const value = Number(row.predictedValue ?? 0)
    if (!best) return row
    const bestValue = Number(best.predictedValue ?? 0)
    return value > bestValue ? row : best
  }, null)
})
const topTagLabel = computed(() => {
  const tags = reviewSummary.value?.topTags ?? []
  return tags.length > 0 ? `${tags[0].tagName}` : '데이터 없음'
})
const formatForecastValue = (value) => {
  if (forecastFilters.value.target === 'revenue') {
    return formatKrw(value)
  }
  return `${formatNumber(value)}건`
}

const forecastValueLabel = computed(() => (
  forecastFilters.value.target === 'revenue' ? '예측 매출(원)' : '예측 예약(건)'
))
const forecastRangeLabel = computed(() => (
  forecastFilters.value.target === 'revenue' ? '예측 범위(원)' : '예측 범위(건)'
))
const forecastMetaText = computed(() => {
  const explain = forecastReport.value?.explain
  const mape = forecastReport.value?.diagnostics?.mape
  const parts = []
  if (explain) parts.push(explain)
  if (Number.isFinite(mape)) parts.push(`백테스트 MAPE ${formatNumber(mape)}%`)
  return parts.join(' · ')
})
const formatForecastRange = (row) => {
  if (!row || (!row.low && !row.high)) return '-'
  if (Number(row.low) === 0 && Number(row.high) === 0) return '-'
  const low = formatNumber(row.low ?? 0)
  const high = formatNumber(row.high ?? 0)
  return `${low} ~ ${high}`
}
const formatForecastShortDate = (value) => {
  if (!value) return ''
  const text = String(value)
  const parts = text.split('-')
  if (parts.length !== 3) return text
  return `${parts[1]}-${parts[2]}`
}
const forecastChartSeries = computed(() => {
  return forecastDaily.value.map((row) => ({
    date: row.date,
    label: formatForecastShortDate(row.date),
    predicted: Number(row.predictedValue ?? 0),
    low: Number(row.low ?? 0),
    high: Number(row.high ?? 0)
  }))
})
const forecastChartPaths = computed(() => {
  const points = forecastChartSeries.value
  if (!points.length) return null
  const width = 1000
  const height = 200
  const paddingX = 30
  const paddingY = 20
  const minY = Math.min(...points.map((p) => p.low ?? p.predicted))
  const maxY = Math.max(...points.map((p) => p.high ?? p.predicted))
  const safeMin = Number.isFinite(minY) ? minY : 0
  const safeMax = Number.isFinite(maxY) ? maxY : 0
  const range = safeMax - safeMin || 1
  const stepX = points.length > 1 ? (width - paddingX * 2) / (points.length - 1) : 0

  const mapX = (idx) => paddingX + (stepX * idx)
  const mapY = (value) => {
    const clamped = Math.max(safeMin, Math.min(safeMax, value))
    return height - paddingY - ((clamped - safeMin) / range) * (height - paddingY * 2)
  }

  const linePoints = points.map((point, idx) => `${mapX(idx)},${mapY(point.predicted)}`).join(' ')
  const highPoints = points.map((point, idx) => `${mapX(idx)},${mapY(point.high)}`).join(' ')
  const lowPoints = points.map((point, idx) => `${mapX(idx)},${mapY(point.low)}`).reverse().join(' ')
  const areaPath = `M ${highPoints} L ${lowPoints} Z`

  return {
    width,
    height,
    linePoints,
    areaPath,
    minY: safeMin,
    maxY: safeMax
  }
})

const contextSummaryText = computed(() => {
  const reviewCount = formatNumber(reviewSummary.value?.reviewCount ?? 0)
  const avgRating = reviewSummary.value?.avgRating ?? 0
  const tag = topTagLabel.value && topTagLabel.value !== '데이터 없음' ? ` · 대표태그 ${topTagLabel.value}` : ''
  return `기간 ${reviewFilters.value.from} ~ ${reviewFilters.value.to} · 리뷰 ${reviewCount} · 평점 ${avgRating}${tag}`
})

const getPresetRange = (preset) => {
  if (preset === '7days') {
    return { from: daysAgoISO(7), to: todayISO() }
  }
  if (preset === '30days') {
    return { from: daysAgoISO(30), to: todayISO() }
  }
  const date = new Date()
  const from = new Date(date.getFullYear(), date.getMonth(), 1)
  return { from: from.toISOString().slice(0, 10), to: todayISO() }
}

const applyPreset = (filtersRef, preset) => {
  const range = getPresetRange(preset)
  filtersRef.value = { ...filtersRef.value, ...range }
}

const applyReviewPreset = (preset) => {
  reviewPreset.value = preset
  applyPreset(reviewFilters, preset)
}

const applyThemePreset = (preset) => {
  themePreset.value = preset
  applyPreset(themeFilters, preset)
}

const loadAccommodations = async () => {
  accommodationLoading.value = true
  accommodationError.value = ''
  const response = await fetchHostAccommodations()
  if (response.ok) {
    const payload = response.data
    accommodations.value = Array.isArray(payload) ? payload : payload?.items ?? payload?.content ?? payload?.data ?? []
  } else {
    accommodationError.value = '숙소 목록을 불러오지 못했습니다.'
  }
  accommodationLoading.value = false
}

const loadReviewReport = async () => {
  if (!canUseHostFeatures.value) return
  reviewLoading.value = true
  reviewError.value = ''
  compareSummary.value = null
  compareError.value = ''
  const params = {
    from: reviewFilters.value.from,
    to: reviewFilters.value.to
  }
  if (reviewFilters.value.accommodationId !== 'all') {
    params.accommodationId = reviewFilters.value.accommodationId
  }
  const [summaryRes, trendRes] = await Promise.all([
    fetchHostReviewReportSummary(params),
    fetchHostReviewReportTrend({
      accommodationId: params.accommodationId,
      months: 6
    })
  ])

  if (summaryRes.ok) {
    reviewSummary.value = summaryRes.data
    const prevRange = getPreviousRange(params.from, params.to)
    if (prevRange) {
      compareLoading.value = true
      const compareParams = {
        from: prevRange.from,
        to: prevRange.to
      }
      if (params.accommodationId) {
        compareParams.accommodationId = params.accommodationId
      }
      const compareRes = await fetchHostReviewReportSummary(compareParams)
      if (compareRes.ok) {
        compareSummary.value = compareRes.data
      } else {
        compareError.value = '비교 데이터 없음'
      }
      compareLoading.value = false
    }
  } else {
    reviewError.value = '리뷰 리포트를 불러오지 못했습니다.'
  }

  if (trendRes.ok) {
    reviewTrend.value = Array.isArray(trendRes.data) ? trendRes.data : []
  } else {
    reviewTrend.value = []
  }
  reviewLoading.value = false
}

const toggleReviewContent = (reviewId) => {
  expandedReviews.value[reviewId] = !expandedReviews.value[reviewId]
}


const buildDemandRange = () => {
  const today = new Date()
  const historyDays = Number(forecastFilters.value.historyDays ?? 180)
  const horizonDays = Number(forecastFilters.value.horizonDays ?? 30)
  const from = new Date(today)
  from.setDate(from.getDate() - historyDays)
  const to = new Date(today)
  to.setDate(to.getDate() + horizonDays)
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10)
  }
}

const buildEligibilityParams = (tab) => {
  const params = { tab }
  if (tab === 'REVIEW') {
    params.from = reviewFilters.value.from
    params.to = reviewFilters.value.to
    if (reviewFilters.value.accommodationId !== 'all') {
      params.accommodationId = reviewFilters.value.accommodationId
    }
  } else if (tab === 'THEME') {
    params.from = themeFilters.value.from
    params.to = themeFilters.value.to
    params.metric = themeFilters.value.metric
    if (themeFilters.value.accommodationId !== 'all') {
      params.accommodationId = themeFilters.value.accommodationId
    }
  } else if (tab === 'DEMAND') {
    params.historyDays = forecastFilters.value.historyDays
    if (forecastFilters.value.accommodationId !== 'all') {
      params.accommodationId = forecastFilters.value.accommodationId
    }
  }
  return params
}

const loadEligibilityForTab = async (tab) => {
  const response = await fetchHostAiInsightEligibility(buildEligibilityParams(tab))
  if (response.ok) {
    const payload = response.data ?? {}
    if (payload.review) eligibility.value.review = payload.review
    if (payload.theme) eligibility.value.theme = payload.theme
    if (payload.demand) eligibility.value.demand = payload.demand
    eligibilityError.value = ''
    return
  }
  eligibilityError.value = '자격 정보를 불러오지 못했어요. 생성 시 서버에서 다시 확인합니다.'
}

const loadEligibilityAll = async () => {
  await Promise.all([
    loadEligibilityForTab('REVIEW'),
    loadEligibilityForTab('THEME'),
    loadEligibilityForTab('DEMAND')
  ])
}

const loadAiInsight = async (tab, forceRefresh = false) => {
  const state = getInsightState(tab)
  if (state.loading) return
  if (!getEligibilityInfo(tab).canGenerate) {
    state.error = ''
    state.data = null
    return
  }
  state.loading = true
  state.error = ''
  const payload = { tab, forceRefresh }

  if (tab === 'REVIEW') {
    payload.from = reviewFilters.value.from
    payload.to = reviewFilters.value.to
    if (reviewFilters.value.accommodationId !== 'all') {
      payload.accommodationId = reviewFilters.value.accommodationId
    }
  } else if (tab === 'THEME') {
    payload.from = themeFilters.value.from
    payload.to = themeFilters.value.to
    payload.metric = themeFilters.value.metric
    if (themeFilters.value.accommodationId !== 'all') {
      payload.accommodationId = themeFilters.value.accommodationId
    }
  } else if (tab === 'DEMAND') {
    const range = buildDemandRange()
    payload.from = range.from
    payload.to = range.to
    payload.target = forecastFilters.value.target
    payload.horizonDays = forecastFilters.value.horizonDays
    payload.historyDays = forecastFilters.value.historyDays
    if (forecastFilters.value.accommodationId !== 'all') {
      payload.accommodationId = forecastFilters.value.accommodationId
    }
  }

  const response = await fetchHostAiInsight(payload)
  if (response.ok) {
    state.data = response.data
    if (response.data?.meta) {
      if (tab === 'REVIEW') eligibility.value.review = response.data.meta
      if (tab === 'THEME') eligibility.value.theme = response.data.meta
      if (tab === 'DEMAND') eligibility.value.demand = response.data.meta
    }
  } else {
    state.error = 'AI 인사이트를 불러오지 못했습니다.'
    state.data = null
  }
  state.loading = false
}

const loadThemeReport = async () => {
  if (!canUseHostFeatures.value) return
  themeLoading.value = true
  themeError.value = ''
  const params = {
    from: themeFilters.value.from,
    to: themeFilters.value.to,
    metric: themeFilters.value.metric
  }
  if (themeFilters.value.accommodationId !== 'all') {
    params.accommodationId = themeFilters.value.accommodationId
  }
  const response = await fetchHostThemeReport(params)
  if (response.ok) {
    themeReport.value = response.data
  } else {
    themeError.value = '테마 리포트를 불러오지 못했습니다.'
  }
  themeLoading.value = false
}

const loadForecast = async () => {
  if (!canUseHostFeatures.value) return
  forecastLoading.value = true
  forecastError.value = ''
  const params = {
    target: forecastFilters.value.target,
    horizonDays: forecastFilters.value.horizonDays,
    historyDays: forecastFilters.value.historyDays
  }
  if (forecastFilters.value.accommodationId !== 'all') {
    params.accommodationId = forecastFilters.value.accommodationId
  }
  const response = await fetchHostDemandForecast(params)
  if (response.ok) {
    forecastReport.value = response.data
  } else {
    forecastError.value = '수요 예측을 불러오지 못했습니다.'
  }
  forecastLoading.value = false
}

onMounted(async () => {
  if (!userInfo.value) {
    const response = await getCurrentUser()
    if (!response.ok || !response.data) {
      router.replace('/login')
      return
    }
    saveUserInfo(response.data)
    userInfo.value = response.data
  } else if (!isHostUser.value && getAccessToken()) {
    const response = await getCurrentUser()
    if (response.ok && response.data) {
      saveUserInfo(response.data)
      userInfo.value = response.data
    }
  }
  if (typeof window !== 'undefined') {
    const media = window.matchMedia('(min-width: 768px)')
    const update = () => {
      isDesktop.value = media.matches
      demandMonitoringOpen.value = media.matches
    }
    update()
    media.addEventListener?.('change', update)
    media.addListener?.(update)
  }
  const saved = typeof window !== 'undefined' ? window.localStorage.getItem('hostForecastViewMode') : null
  if (saved === 'table' || saved === 'cards') {
    forecastViewMode.value = saved
  }
  authReady.value = true
  await loadAccommodations()
  loadReviewReport()
  loadThemeReport()
  loadForecast()
  loadEligibilityAll()
})

watch(forecastViewMode, (value) => {
  if (typeof window !== 'undefined') {
    window.localStorage.setItem('hostForecastViewMode', value)
  }
})

watch(reviewFilters, () => {
  if (activeTab.value === 'reviews') {
    loadReviewReport()
    aiInsightState.value.REVIEW.data = null
    aiInsightState.value.REVIEW.error = ''
  }
  loadEligibilityForTab('REVIEW')
}, { deep: true })

watch(themeFilters, () => {
  loadThemeReport()
  aiInsightState.value.THEME.data = null
  aiInsightState.value.THEME.error = ''
  themeShowAll.value = false
  loadEligibilityForTab('THEME')
}, { deep: true })

watch(forecastFilters, () => {
  if (activeTab.value === 'forecast') {
    loadForecast()
  }
  aiInsightState.value.DEMAND.data = null
  aiInsightState.value.DEMAND.error = ''
  loadEligibilityForTab('DEMAND')
}, { deep: true })
</script>

<template>
  <section class="report-view" v-if="authReady">
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
      <header class="host-view-header">
        <div>
          <h2 class="host-title">AI 리포트</h2>
          <p class="host-subtitle">AI가 리뷰/테마/수요를 요약해 운영 포인트를 제공합니다.</p>
        </div>
        <p v-if="accommodationLoading" class="host-subtitle">숙소 불러오는 중...</p>
        <p v-else-if="accommodationError" class="error-text">{{ accommodationError }}</p>
      </header>
      <div class="context-bar">
        <p class="context-text">{{ contextSummaryText }}</p>
        <div class="context-chips">
          <span v-if="topTagLabel !== '데이터 없음'" class="tag-chip">대표 태그: {{ topTagLabel }}</span>
          <span class="tag-chip">기간: {{ reviewFilters.from }} ~ {{ reviewFilters.to }}</span>
        </div>
      </div>

      <div class="report-tabs" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="report-tab"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          {{ tab.label }}
        </button>
      </div>

      <section v-if="activeTab === 'reviews'" class="report-section">
      <div class="filter-card">
        <div class="filter-title">필터</div>
        <div class="filters filters-grid">
          <label>
            숙소
            <select v-model="reviewFilters.accommodationId">
              <option value="all">전체 숙소</option>
              <option v-for="acc in accommodations" :key="acc.accommodationsId" :value="acc.accommodationsId">
                {{ acc.accommodationsName }}
              </option>
            </select>
          </label>
          <div class="filter-group">
            <button type="button" :class="{ active: reviewPreset === '7days' }" @click="applyReviewPreset('7days')">7일</button>
            <button type="button" :class="{ active: reviewPreset === '30days' }" @click="applyReviewPreset('30days')">30일</button>
            <button type="button" :class="{ active: reviewPreset === 'thisMonth' }" @click="applyReviewPreset('thisMonth')">이번달</button>
          </div>
          <label>
            시작일
            <input type="date" v-model="reviewFilters.from" />
          </label>
          <label>
            종료일
            <input type="date" v-model="reviewFilters.to" />
          </label>
        </div>
      </div>

      <div v-if="reviewLoading" class="report-skeleton">
        <div class="kpi-grid">
          <div v-for="n in 4" :key="`kpi-skel-${n}`" class="kpi-card skeleton-card">
            <div class="skeleton-line short"></div>
            <div class="skeleton-line long"></div>
            <div class="skeleton-line mini"></div>
          </div>
        </div>
        <div class="skeleton-tags">
          <span v-for="n in 6" :key="`tag-skel-${n}`" class="skeleton-chip"></span>
        </div>
        <div class="skeleton-panels">
          <div class="skeleton-panel"></div>
          <div class="skeleton-panel"></div>
        </div>
      </div>
      <div v-else-if="reviewError" class="error-box">{{ reviewError }}</div>
      <div v-else>
        <div class="kpi-grid">
          <div v-for="item in kpiItems" :key="item.label" class="kpi-card">
            <p class="kpi-label">
              {{ item.label }}
              <span v-if="item.tooltip" class="kpi-help" :title="item.tooltip">?</span>
            </p>
            <strong class="kpi-value">{{ item.value }}</strong>
            <span v-if="item.delta" class="kpi-delta">Δ {{ item.delta }}</span>
          </div>
        </div>
        <div v-if="compareEnabled" class="compare-toolbar">
          <p v-if="showCompare" class="compare-label">{{ compareLabel }}</p>
          <button type="button" class="ghost-btn" @click="showCompare = !showCompare">
            {{ showCompare ? '비교 숨기기' : '전 기간 비교' }}
          </button>
        </div>
        <div v-if="compareEnabled" class="kpi-compare" :class="{ hidden: !showCompare }">
          <div v-if="compareLoading" class="compare-skeleton">
            <span class="skeleton-line mini"></span>
            <span class="skeleton-line mini"></span>
            <span class="skeleton-line mini"></span>
          </div>
          <div v-else-if="compareItems.length" class="compare-items">
            <span v-for="item in compareItems" :key="item.label">
              {{ item.label }} <strong>{{ item.value }}</strong>
            </span>
          </div>
        </div>

        <div v-if="!reviewHasData" class="empty-box">
          <p>데이터가 부족합니다 (리뷰 {{ formatNumber(reviewSummary?.reviewCount ?? 0) }}건).</p>
          <button type="button" class="ghost-btn" @click="applyReviewPreset('30days')">최근 30일 보기</button>
        </div>

        <div v-else class="grid-two">
          <div class="panel">
            <h3>별점 분포</h3>
            <ul class="rating-bars">
              <li v-for="entry in reviewRatingEntries" :key="entry.rating">
                <span class="rating-label">{{ entry.rating }}점</span>
                <div class="rating-bar">
                  <span class="rating-fill" :style="{ width: entry.percent + '%' }"></span>
                </div>
                <span class="rating-count">{{ formatNumber(entry.count) }}건</span>
                <span class="rating-percent">{{ entry.percent }}%</span>
              </li>
            </ul>
          </div>
          <div class="panel">
            <h3>TOP 태그</h3>
            <p class="muted">자주 언급된 키워드를 모았어요.</p>
            <!-- Tag chips are display-only in this view; no click behavior. -->
            <div class="tag-chip-list">
              <span v-if="(reviewSummary?.topTags ?? []).length === 0" class="muted">태그 데이터가 없습니다.</span>
              <span
                v-for="tag in reviewSummary?.topTags"
                :key="tag.tagName"
                class="tag-chip tag-chip-static"
                :title="`${tag.tagName} ${formatNumber(tag.count)}건`"
              >
                {{ tag.tagName }}
                <span class="chip-count">{{ formatNumber(tag.count) }}</span>
              </span>
            </div>
          </div>
        </div>

        <div class="panel ai-panel">
          <div class="ai-head">
            <div>
              <p class="ai-kicker">AI 리뷰 인사이트</p>
              <h3>AI 요약</h3>
              <p class="muted">선택 기간 리뷰 데이터를 기반으로 핵심 포인트를 정리합니다.</p>
              <p v-if="isWarnStatus('REVIEW')" class="ai-warning">{{ getEligibilityInfo('REVIEW').warningMessage }}</p>
              <p v-if="eligibilityError" class="ai-warning ai-warning--muted">{{ eligibilityError }}</p>
            </div>
            <div class="ai-meta">
              <button
                type="button"
                class="primary-btn ai-btn"
                :disabled="!canGenerateAi('REVIEW') || reviewLoading"
                :aria-busy="getInsightState('REVIEW').loading ? 'true' : 'false'"
                @click="loadAiInsight('REVIEW', true)"
              >
                <span v-if="getInsightState('REVIEW').loading" class="spinner" aria-hidden="true"></span>
                {{ getAiButtonLabel('REVIEW') }}
              </button>
              <div class="ai-meta-chips">
                <span v-for="chip in reviewAiMetaChips" :key="chip" class="ai-chip">{{ chip }}</span>
              </div>
            </div>
          </div>
          <div v-if="isBlockedStatus('REVIEW')" class="empty-box ai-state">
            {{ getEligibilityInfo('REVIEW').disabledReason }}
          </div>
          <div v-else-if="getInsightState('REVIEW').loading" class="ai-skeleton">
            <div class="skeleton-card"></div>
            <div class="skeleton-card"></div>
            <div class="skeleton-card"></div>
          </div>
          <div v-else-if="getInsightState('REVIEW').error" class="error-box ai-state">
            <p>{{ getInsightState('REVIEW').error }}</p>
            <button type="button" class="link-btn" @click="loadAiInsight('REVIEW', true)">다시 시도</button>
          </div>
          <div v-else-if="reviewAiInsight && !reviewAiHasContent" class="empty-box ai-state">AI 요약 대상 리뷰가 없습니다.</div>
          <div v-else-if="reviewAiInsight" class="ai-grid review-grid">
            <div class="ai-block ai-card ai-card--overview">
              <div class="ai-card__head">
                <h4>총평</h4>
                <span v-if="isWarnStatus('REVIEW')" class="ai-badge">참고용</span>
              </div>
              <ul class="ai-list">
                <li v-for="item in getSectionItemsParsed(reviewAiInsight, '총평', 2)" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
            <div class="ai-block ai-card ai-card--positives">
              <div class="ai-card__head">
                <h4>좋았던 점</h4>
                <span v-if="isWarnStatus('REVIEW')" class="ai-badge">참고용</span>
              </div>
              <ul class="ai-list">
                <li v-for="item in getSectionItemsParsed(reviewAiInsight, '좋았던 점', 2)" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
            <div class="ai-block ai-card ai-card--negatives">
              <div class="ai-card__head">
                <h4>개선 포인트</h4>
                <span v-if="isWarnStatus('REVIEW')" class="ai-badge">참고용</span>
              </div>
              <ul class="ai-list">
                <li v-for="item in getSectionItemsParsed(reviewAiInsight, '개선 포인트', 2)" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
            <div class="ai-block ai-card ai-card--actions" :class="{ 'ai-wide': reviewRiskEmpty }">
              <div class="ai-card__head">
                <h4>다음 액션</h4>
                <span v-if="isWarnStatus('REVIEW')" class="ai-badge">참고용</span>
              </div>
              <ul class="action-list">
                <li v-for="item in aiActionsWithPriority" :key="item.text" class="action-item">
                  <span class="action-badge" :class="item.tone">{{ item.priority }}</span>
                  <span class="action-text">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
              <div v-if="reviewRiskEmpty" class="monitoring-callout">
                <h5>특이 징후 없음 · 모니터링 포인트</h5>
                <ul>
                  <li v-for="line in aiRiskRecommendations" :key="line">{{ line }}</li>
                </ul>
              </div>
            </div>
            <div v-if="!reviewRiskEmpty" class="ai-block ai-card ai-card--risks">
              <div class="ai-card__head">
                <h4>주의·리스크</h4>
                <span v-if="isWarnStatus('REVIEW')" class="ai-badge">참고용</span>
              </div>
              <ul class="ai-list">
                <li v-for="item in getSectionItemsParsed(reviewAiInsight, '주의·리스크', 2)" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
          </div>
          <div v-else class="muted ai-state">AI 요약을 생성해보세요.</div>
        </div>

        <div class="panel">
          <h3>최근 리뷰</h3>
          <div v-if="(reviewSummary?.recentReviews ?? []).length === 0" class="muted">리뷰가 없습니다.</div>
          <div v-else class="review-cards">
            <article v-for="review in reviewSummary?.recentReviews" :key="review.reviewId" class="review-card">
              <div class="review-head">
                <div>
                  <strong>{{ review.accommodationName }}</strong>
                  <p class="review-meta">{{ review.authorName }} · {{ review.rating }}점</p>
                </div>
                <span class="review-date">{{ formatDate(review.createdAt, true) }}</span>
              </div>
              <p class="review-content" :class="{ expanded: expandedReviews[review.reviewId] }">
                {{ review.content }}
              </p>
              <button
                v-if="review.content && review.content.length > 120"
                type="button"
                class="link-btn"
                @click="toggleReviewContent(review.reviewId)"
              >
                {{ expandedReviews[review.reviewId] ? '접기' : '더보기' }}
              </button>
            </article>
          </div>
        </div>

        <div class="panel" v-if="reviewTrend.length">
          <h3>리뷰 추이</h3>
          <table class="simple-table table-only">
            <thead>
              <tr><th>월</th><th>리뷰수</th><th>평균 평점</th></tr>
            </thead>
            <tbody>
              <tr v-for="row in reviewTrend" :key="row.period">
                <td>{{ row.period }}</td>
                <td>{{ formatNumber(row.reviewCount) }}</td>
                <td>{{ row.avgRating }}</td>
              </tr>
            </tbody>
          </table>
          <div class="card-list mobile-only">
            <div v-for="row in reviewTrend" :key="row.period" class="report-card">
              <div class="report-card__row">
                <strong>{{ row.period }}</strong>
                <span>{{ formatNumber(row.reviewCount) }}건</span>
              </div>
              <p class="muted">평균 평점 {{ row.avgRating }}</p>
            </div>
          </div>
        </div>
      </div>
      </section>

      <section v-else-if="activeTab === 'themes'" class="report-section">
      <div class="theme-section-wrap">
        <div class="theme-layout">
          <aside class="filter-card theme-filter">
            <div class="filter-title">필터</div>
            <div class="filters filters-grid">
              <label>
                숙소
                <select v-model="themeFilters.accommodationId">
                  <option value="all">전체 숙소</option>
                  <option v-for="acc in accommodations" :key="acc.accommodationsId" :value="acc.accommodationsId">
                    {{ acc.accommodationsName }}
                  </option>
                </select>
              </label>
              <div class="filter-group">
                <button type="button" :class="{ active: themePreset === '7days' }" @click="applyThemePreset('7days')">7일</button>
                <button type="button" :class="{ active: themePreset === '30days' }" @click="applyThemePreset('30days')">30일</button>
                <button type="button" :class="{ active: themePreset === 'thisMonth' }" @click="applyThemePreset('thisMonth')">이번달</button>
              </div>
              <label>
                시작일
                <input type="date" v-model="themeFilters.from" />
              </label>
              <label>
                종료일
                <input type="date" v-model="themeFilters.to" />
              </label>
              <label>
                지표
                <select v-model="themeFilters.metric">
                  <option value="reservations">예약수</option>
                  <option value="revenue">매출</option>
                </select>
              </label>
            </div>
          </aside>

          <div class="theme-content">
            <div class="theme-kpi-grid">
              <div v-for="item in themeKpis" :key="item.label" class="kpi-card">
                <div>
                  <p>{{ item.label }}</p>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </div>

            <div class="panel ai-panel">
              <div class="ai-head">
                <div>
                  <p class="ai-kicker">AI 테마 인사이트</p>
                  <h3>AI 요약</h3>
                  <p class="muted">테마 성과를 기반으로 운영 포인트를 정리합니다.</p>
                  <p v-if="isWarnStatus('THEME')" class="ai-warning">{{ getEligibilityInfo('THEME').warningMessage }}</p>
                  <p v-if="eligibilityError" class="ai-warning ai-warning--muted">{{ eligibilityError }}</p>
                </div>
                <div class="ai-meta">
                  <button
                    type="button"
                    class="primary-btn ai-btn"
                    :disabled="!canGenerateAi('THEME') || themeLoading"
                    :aria-busy="getInsightState('THEME').loading ? 'true' : 'false'"
                    @click="loadAiInsight('THEME', true)"
                  >
                    <span v-if="getInsightState('THEME').loading" class="spinner" aria-hidden="true"></span>
                    {{ getAiButtonLabel('THEME') }}
                  </button>
                  <div class="ai-meta-chips">
                    <span v-for="chip in themeAiMetaChips" :key="chip" class="ai-chip">{{ chip }}</span>
                  </div>
                </div>
              </div>
              <div v-if="isBlockedStatus('THEME')" class="empty-box ai-state">
                {{ getEligibilityInfo('THEME').disabledReason }}
              </div>
              <div v-else-if="getInsightState('THEME').loading" class="ai-skeleton">
                <div class="skeleton-card"></div>
                <div class="skeleton-card"></div>
                <div class="skeleton-card"></div>
              </div>
              <div v-else-if="getInsightState('THEME').error" class="error-box ai-state">
                <p>{{ getInsightState('THEME').error }}</p>
                <button type="button" class="link-btn" @click="loadAiInsight('THEME', true)">다시 시도</button>
              </div>
              <div v-else-if="themeAiInsight" class="ai-grid theme-ai-grid">
                <div class="ai-block ai-card ai-card--summary">
                  <div class="ai-card__head">
                    <h4>트렌드 요약</h4>
                    <span v-if="isWarnStatus('THEME')" class="ai-badge">참고용</span>
                  </div>
                  <ul class="ai-list">
                    <li v-for="item in getSectionItemsParsed(themeAiInsight, '트렌드 요약', themeInsightLimits['트렌드 요약'])" :key="item.key">
                      <span class="ai-main">{{ item.main }}</span>
                      <span v-if="item.showEvidence" class="ai-evidence">
                        {{ getEvidenceLabel() }}: {{ item.evidence }}
                      </span>
                    </li>
                  </ul>
                </div>
                <div class="ai-block ai-card ai-card--positives">
                  <div class="ai-card__head">
                    <h4>강점</h4>
                    <span v-if="isWarnStatus('THEME')" class="ai-badge">참고용</span>
                  </div>
                  <ul class="ai-list">
                    <li v-for="item in getSectionItemsParsed(themeAiInsight, '강점', themeInsightLimits['강점'])" :key="item.key">
                      <span class="ai-main">{{ item.main }}</span>
                      <span v-if="item.showEvidence" class="ai-evidence">
                        {{ getEvidenceLabel() }}: {{ item.evidence }}
                      </span>
                    </li>
                  </ul>
                </div>
                <div class="ai-block ai-card ai-card--negatives">
                  <div class="ai-card__head">
                    <h4>보완할 점</h4>
                    <span v-if="isWarnStatus('THEME')" class="ai-badge">참고용</span>
                  </div>
                  <ul class="ai-list">
                    <li v-for="item in getSectionItemsParsed(themeAiInsight, '보완할 점', themeInsightLimits['보완할 점'])" :key="item.key">
                      <span class="ai-main">{{ item.main }}</span>
                      <span v-if="item.showEvidence" class="ai-evidence">
                        {{ getEvidenceLabel() }}: {{ item.evidence }}
                      </span>
                    </li>
                  </ul>
                </div>
                <div class="ai-block ai-card ai-card--actions">
                  <div class="ai-card__head">
                    <h4>다음 액션</h4>
                    <span v-if="isWarnStatus('THEME')" class="ai-badge">참고용</span>
                  </div>
                  <ul class="action-list">
                    <li v-for="item in themeActionsWithPriority" :key="item.text" class="action-item">
                      <span class="action-badge" :class="item.tone">{{ item.priority }}</span>
                      <span class="action-text">{{ item.main }}</span>
                      <span v-if="item.showEvidence" class="ai-evidence">
                        {{ getEvidenceLabel() }}: {{ item.evidence }}
                      </span>
                    </li>
                  </ul>
                </div>
                <div class="ai-block ai-card ai-card--monitoring">
                  <div class="ai-card__head">
                    <h4>모니터링</h4>
                    <span v-if="isWarnStatus('THEME')" class="ai-badge">참고용</span>
                  </div>
                  <ul class="ai-list">
                    <li v-for="item in getSectionItemsParsed(themeAiInsight, '모니터링', themeInsightLimits['모니터링'])" :key="item.key">
                      <span class="ai-main">{{ item.main }}</span>
                      <span v-if="item.showEvidence" class="ai-evidence">
                        {{ getEvidenceLabel() }}: {{ item.evidence }}
                      </span>
                    </li>
                  </ul>
                </div>
              </div>
              <div v-else class="muted ai-state">
                테마 데이터가 충분하지 않으면 인사이트가 제한될 수 있습니다.
              </div>
            </div>

            <div class="theme-results-card">
              <div class="theme-results-head">
                <div>
                  <h4>테마 인기 결과</h4>
                  <p class="muted">선택 기간 기준으로 테마별 성과를 보여줍니다.</p>
                </div>
                <div class="theme-view-toggle" role="tablist" aria-label="보기 전환">
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="themeViewMode === 'cards'"
                    :class="{ active: themeViewMode === 'cards' }"
                    @click="themeViewMode = 'cards'"
                  >카드</button>
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="themeViewMode === 'table'"
                    :class="{ active: themeViewMode === 'table' }"
                    @click="themeViewMode = 'table'"
                  >테이블</button>
                </div>
              </div>

              <div class="theme-results-body">
                <div v-if="themeLoading" class="loading-box">테마 리포트 로딩 중...</div>
                <div v-else-if="themeError" class="error-box">{{ themeError }}</div>
                <div v-else>
                  <div v-if="themeRows.length === 0" class="empty-box">선택한 기간에 테마 데이터가 없습니다.</div>

                  <div v-else>
                    <div v-if="themeViewMode === 'cards'" class="theme-grid-desktop">
                      <article v-for="row in themeVisibleRows" :key="row.themeId" class="theme-card">
                        <div class="theme-card__head">
                          <strong>{{ row.themeName }}</strong>
                        </div>
                        <div class="theme-card__metrics">
                          <div class="theme-metric">
                            <span>예약</span>
                            <strong>{{ formatNumber(row.reservationCount) }}건</strong>
                          </div>
                          <div class="theme-metric">
                            <span>매출</span>
                            <strong>{{ formatNumber(row.revenueSum) }}원</strong>
                          </div>
                          <div class="theme-metric">
                            <span>숙소</span>
                            <strong>{{ formatNumber(row.accommodationCount) }}개</strong>
                          </div>
                        </div>
                      </article>
                    </div>

                    <div v-else class="theme-table-wrapper">
                      <table class="simple-table theme-table">
                        <thead>
                          <tr>
                            <th>테마</th>
                            <th class="cell-right">예약(건)</th>
                            <th class="cell-right">매출(원)</th>
                            <th class="cell-right">숙소(개)</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="row in themeRows" :key="row.themeId">
                            <td>{{ row.themeName }}</td>
                            <td class="cell-right">
                              <span :class="{ 'zero-value': isZeroValue(row.reservationCount) }">
                                {{ formatNumber(row.reservationCount) }}
                              </span>
                            </td>
                            <td class="cell-right">
                              <span :class="{ 'zero-value': isZeroValue(row.revenueSum) }">
                                {{ formatNumber(row.revenueSum) }}
                              </span>
                            </td>
                            <td class="cell-right">
                              <span :class="{ 'zero-value': isZeroValue(row.accommodationCount) }">
                                {{ formatNumber(row.accommodationCount) }}
                              </span>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <div v-if="themeRows.length" class="theme-grid theme-grid-stack">
                    <article v-for="row in themeVisibleRows" :key="row.themeId" class="theme-card">
                      <div class="theme-card__head">
                        <strong>{{ row.themeName }}</strong>
                      </div>
                      <div class="theme-card__metrics">
                        <div class="theme-metric">
                          <span>예약</span>
                          <strong>{{ formatNumber(row.reservationCount) }}건</strong>
                        </div>
                        <div class="theme-metric">
                          <span>매출</span>
                          <strong>{{ formatNumber(row.revenueSum) }}원</strong>
                        </div>
                        <div class="theme-metric">
                          <span>숙소</span>
                          <strong>{{ formatNumber(row.accommodationCount) }}개</strong>
                        </div>
                      </div>
                    </article>
                  </div>
                  <div v-if="themeViewMode === 'cards' && themeRows.length > themeVisibleLimit" class="theme-more">
                    <button type="button" class="ghost-btn" @click="themeShowAll = !themeShowAll">
                      {{ themeShowAll ? '접기' : `테마 더보기 (${themeRows.length - themeVisibleLimit}개)` }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      </section>

      <section v-else class="report-section">
      <div class="filter-card">
        <div class="filter-title">필터</div>
        <div class="filters filters-grid">
          <label>
            숙소
            <select v-model="forecastFilters.accommodationId">
              <option value="all">전체 숙소</option>
              <option v-for="acc in accommodations" :key="acc.accommodationsId" :value="acc.accommodationsId">
                {{ acc.accommodationsName }}
              </option>
            </select>
          </label>
          <label>
            대상
            <select v-model="forecastFilters.target">
              <option value="reservations">예약수</option>
              <option value="revenue">매출</option>
            </select>
          </label>
          <label>
            예측 기간(일)
            <input type="number" min="7" max="60" v-model.number="forecastFilters.horizonDays" />
          </label>
          <label>
            과거 데이터(일)
            <input type="number" min="30" max="365" v-model.number="forecastFilters.historyDays" />
          </label>
        </div>
      </div>

      <div v-if="forecastLoading" class="loading-box">수요 예측 계산 중...</div>
      <div v-else-if="forecastError" class="error-box">{{ forecastError }}</div>
      <div v-else>
        <div class="kpi-grid">
          <div class="kpi-card">
            <p>최근 7일 평균{{ forecastFilters.target === 'revenue' ? '(원/일)' : '(건/일)' }}</p>
            <strong>{{ formatForecastValue(forecastReport?.baseline?.recentAvg7 ?? 0) }}</strong>
          </div>
          <div class="kpi-card">
            <p>최근 28일 평균{{ forecastFilters.target === 'revenue' ? '(원/일)' : '(건/일)' }}</p>
            <strong>{{ formatForecastValue(forecastReport?.baseline?.recentAvg28 ?? 0) }}</strong>
          </div>
          <div class="kpi-card">
            <p>예측 합계</p>
            <strong>{{ formatForecastValue(forecastReport?.forecastSummary?.predictedTotal ?? 0) }}</strong>
          </div>
          <div class="kpi-card">
            <p>최고 예측일</p>
            <strong>{{ forecastPeak ? `${forecastPeak.date} · ${formatForecastValue(forecastPeak.predictedValue)}` : '-' }}</strong>
          </div>
        </div>
        <p v-if="forecastMetaText" class="forecast-meta">{{ forecastMetaText }}</p>

        <div class="panel ai-panel">
          <div class="ai-head">
            <div>
              <p class="ai-kicker">AI 수요 인사이트</p>
              <h3>AI 요약</h3>
              <p class="muted">예측 결과를 기반으로 운영 포인트를 정리합니다.</p>
              <p v-if="isWarnStatus('DEMAND')" class="ai-warning">{{ getEligibilityInfo('DEMAND').warningMessage }}</p>
              <p v-if="eligibilityError" class="ai-warning ai-warning--muted">{{ eligibilityError }}</p>
            </div>
            <div class="ai-meta">
              <button
                type="button"
                class="primary-btn ai-btn"
                :disabled="!canGenerateAi('DEMAND') || forecastLoading"
                :aria-busy="getInsightState('DEMAND').loading ? 'true' : 'false'"
                @click="loadAiInsight('DEMAND', true)"
              >
                <span v-if="getInsightState('DEMAND').loading" class="spinner" aria-hidden="true"></span>
                {{ getAiButtonLabel('DEMAND') }}
              </button>
              <div class="ai-meta-chips">
                <span v-for="chip in demandAiMetaChips" :key="chip" class="ai-chip">{{ chip }}</span>
              </div>
            </div>
          </div>
          <div v-if="isBlockedStatus('DEMAND')" class="empty-box ai-state">
            {{ getEligibilityInfo('DEMAND').disabledReason }}
          </div>
          <div v-else-if="getInsightState('DEMAND').loading" class="ai-skeleton">
            <div class="skeleton-card"></div>
            <div class="skeleton-card"></div>
            <div class="skeleton-card"></div>
          </div>
          <div v-else-if="getInsightState('DEMAND').error" class="error-box ai-state">
            <p>{{ getInsightState('DEMAND').error }}</p>
            <button type="button" class="link-btn" @click="loadAiInsight('DEMAND', true)">다시 시도</button>
          </div>
          <div v-else-if="demandAiInsight" class="ai-grid demand-ai-grid">
            <div class="ai-block ai-card ai-card--summary">
              <div class="ai-card__head">
                <h4>수요 예측 요약</h4>
                <span v-if="isWarnStatus('DEMAND')" class="ai-badge">참고용</span>
              </div>
              <ul class="ai-list">
                <li v-for="item in getSectionItemsParsed(demandAiInsight, '수요 예측 요약', demandInsightLimits['수요 예측 요약'])" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
            <div class="ai-block ai-card ai-card--analysis">
              <div class="ai-card__head">
                <h4>기회 요인</h4>
                <span v-if="isWarnStatus('DEMAND')" class="ai-badge">참고용</span>
              </div>
              <ul class="ai-list">
                <li v-for="item in getSectionItemsParsed(demandAiInsight, '기회 요인', demandInsightLimits['기회 요인'])" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
            <div class="ai-block ai-card ai-card--actions">
              <div class="ai-card__head">
                <h4>다음 액션</h4>
                <span v-if="isWarnStatus('DEMAND')" class="ai-badge">참고용</span>
              </div>
              <ul class="ai-list">
                <li v-for="item in demandActionItems" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
            <div class="ai-block ai-card ai-card--improve">
              <div class="ai-card__head">
                <h4>리스크 요인</h4>
                <span v-if="isWarnStatus('DEMAND')" class="ai-badge">참고용</span>
              </div>
              <ul class="ai-list">
                <li v-for="item in getSectionItemsParsed(demandAiInsight, '리스크 요인', demandInsightLimits['리스크 요인'])" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
            <div class="ai-block ai-card ai-card--monitoring ai-wide">
              <div class="ai-card__head">
                <h4>모니터링</h4>
                <span v-if="isWarnStatus('DEMAND')" class="ai-badge">참고용</span>
                <button
                  v-if="demandMonitoringItems.length > 2"
                  type="button"
                  class="ghost-btn ai-toggle"
                  @click="demandMonitoringOpen = !demandMonitoringOpen"
                >
                  {{ demandMonitoringOpen ? '접기' : '펼치기' }}
                </button>
              </div>
              <ul class="ai-list">
                <li v-for="item in demandMonitoringDisplay.map(parseInsightItem)" :key="item.key">
                  <span class="ai-main">{{ item.main }}</span>
                  <span v-if="item.showEvidence" class="ai-evidence">
                    {{ getEvidenceLabel() }}: {{ item.evidence }}
                  </span>
                </li>
              </ul>
            </div>
          </div>
          <div v-else class="muted ai-state">
            예측 데이터가 충분하지 않으면 인사이트가 제한될 수 있습니다.
          </div>
        </div>

        <div v-if="forecastDaily.length === 0" class="empty-box">예측 데이터가 없습니다.</div>
        <div v-else>
          <div v-if="isDesktop" class="forecast-toolbar">
            <div class="forecast-view-toggle" role="tablist" aria-label="예측 보기 전환">
              <button
                type="button"
                role="tab"
                :aria-selected="forecastViewMode === 'table'"
                :class="{ active: forecastViewMode === 'table' }"
                @click="forecastViewMode = 'table'"
              >테이블</button>
              <button
                type="button"
                role="tab"
                :aria-selected="forecastViewMode === 'cards'"
                :class="{ active: forecastViewMode === 'cards' }"
                @click="forecastViewMode = 'cards'"
              >카드</button>
            </div>
          </div>

          <div v-if="isDesktop && forecastViewMode === 'table'" class="forecast-table-wrap">
            <div v-if="forecastChartPaths" class="forecast-chart">
              <div class="forecast-chart__legend">
                <span class="legend-item"><span class="legend-dot"></span>예측</span>
                <span class="legend-item"><span class="legend-band"></span>범위</span>
              </div>
              <svg
                class="forecast-chart__canvas"
                viewBox="0 0 1000 200"
                role="img"
                aria-label="수요 예측 차트"
                preserveAspectRatio="none"
              >
                <path class="forecast-chart__band" :d="forecastChartPaths.areaPath" />
                <polyline class="forecast-chart__line" :points="forecastChartPaths.linePoints" />
                <g class="forecast-chart__points">
                  <circle
                    v-for="(row, idx) in forecastChartSeries"
                    :key="row.date"
                    :cx="30 + ((1000 - 60) / Math.max(1, forecastChartSeries.length - 1)) * idx"
                    :cy="180 - ((row.predicted - forecastChartPaths.minY) / (forecastChartPaths.maxY - forecastChartPaths.minY || 1)) * 160"
                    r="3"
                  >
                    <title>{{ row.date }} · {{ formatForecastValue(row.predicted) }} · {{ formatForecastRange(row) }}</title>
                  </circle>
                </g>
              </svg>
              <div class="forecast-chart__axis">
                <span>{{ forecastChartSeries[0]?.label }}</span>
                <span>{{ forecastChartSeries[forecastChartSeries.length - 1]?.label }}</span>
              </div>
            </div>
            <div v-else class="empty-box">예측 데이터가 없습니다.</div>

            <table class="simple-table forecast-table">
              <thead>
                <tr>
                  <th>날짜</th>
                  <th>요일</th>
                  <th class="cell-right">{{ forecastValueLabel }}</th>
                  <th class="cell-right">{{ forecastRangeLabel }}</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="row in forecastDaily"
                  :key="row.date"
                  :class="[{ 'forecast-weekend': row.isWeekend }, { 'forecast-holiday': row.isHoliday }]"
                >
                  <td>{{ row.date }}</td>
                  <td>{{ row.dowLabel }}</td>
                  <td class="cell-right">{{ formatForecastValue(row.predictedValue) }}</td>
                  <td class="cell-right">{{ formatForecastRange(row) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="!isDesktop || forecastViewMode === 'cards'" class="card-list forecast-cards">
            <div v-for="row in forecastDaily" :key="row.date" class="report-card">
              <div class="report-card__row">
                <strong>{{ row.date }}</strong>
                <span>{{ formatForecastValue(row.predictedValue) }}</span>
              </div>
              <p class="muted forecast-card-meta">
                {{ row.dowLabel }}{{ row.isHoliday ? ' · 공휴일' : row.isWeekend ? ' · 주말' : '' }}
              </p>
              <p class="muted forecast-card-range">예측 범위 {{ formatForecastRange(row) }}</p>
            </div>
          </div>
        </div>

      </div>
      </section>
    </template>
  </section>
  <section v-else class="report-view">
    <div class="loading-box">권한을 확인하는 중입니다...</div>
  </section>
</template>

<style scoped>
.report-view {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  padding-bottom: 2rem;
}

.error-text {
  color: var(--danger);
  font-size: 0.85rem;
}

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

.context-bar {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 1rem;
  padding: 0.9rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.context-text {
  margin: 0;
  color: var(--text-muted);
  width: 100%;
}

.context-chips {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.context-chips .tag-chip {
  max-width: 220px;
  white-space: normal;
  overflow-wrap: anywhere;
  line-height: 1.3;
}

.report-tabs {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding-bottom: 0.25rem;
  scrollbar-width: none;
}

.report-tabs::-webkit-scrollbar {
  display: none;
}

.report-tab {
  border: 1px solid var(--brand-border);
  background: var(--bg-white);
  border-radius: 999px;
  padding: 0.4rem 0.9rem;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
}

.report-tab.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.filter-card {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.9rem;
  padding: 1rem;
}

.filter-title {
  font-weight: 700;
  margin-bottom: 0.8rem;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  align-items: flex-end;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.filters label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.filters select,
.filters input {
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--brand-border);
  border-radius: 0.5rem;
  background: var(--bg-white);
  color: var(--text-default);
}

.filter-group {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.filter-group button {
  border: 1px solid var(--brand-border);
  background: var(--bg-white);
  border-radius: 999px;
  padding: 0.35rem 0.75rem;
  font-size: 0.85rem;
  cursor: pointer;
}

.filter-group button.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.kpi-card {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 1rem 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  min-height: 96px;
}

.kpi-card p {
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-muted);
}

.kpi-card strong {
  font-size: 1.5rem;
  letter-spacing: -0.02em;
}

.kpi-label {
  margin: 0;
  font-size: 0.78rem;
  color: var(--text-muted);
}

.kpi-help {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-left: 0.35rem;
  border-radius: 999px;
  border: 1px solid var(--brand-border);
  font-size: 0.7rem;
  color: var(--text-muted);
  cursor: help;
}

.kpi-value {
  font-size: 1.5rem;
  letter-spacing: -0.02em;
}

.kpi-delta {
  font-size: 0.78rem;
  color: #0f766e;
}

.kpi-compare {
  margin-top: 0.75rem;
  padding: 0.6rem 0.9rem;
  border-radius: 0.7rem;
  border: 1px dashed var(--brand-border);
  color: var(--text-muted);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.kpi-compare.hidden {
  display: none;
}

.compare-toolbar {
  margin-top: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.compare-label {
  font-size: 0.78rem;
  margin: 0;
}

.compare-items {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.85rem;
  color: var(--text-default);
}

.compare-empty {
  font-size: 0.85rem;
}

.tag-chip-list {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  padding-bottom: 0.2rem;
  scroll-snap-type: x proximity;
}

.tag-chip-list .tag-chip {
  flex: 0 0 auto;
  scroll-snap-align: start;
  border: 1px solid var(--brand-border);
  background: #f8fafc;
  border-radius: 999px;
  padding: 0.35rem 0.7rem;
  font-size: 0.85rem;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

/* Tag chips are display-only in this view; no click behavior. */
.tag-chip-static {
  cursor: default;
}

.chip-count {
  background: #e2e8f0;
  color: #0f172a;
  border-radius: 999px;
  padding: 0.1rem 0.4rem;
  font-size: 0.72rem;
}

.panel {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 1rem;
  margin-top: 1rem;
}

.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.theme-section-wrap {
  max-width: 1240px;
  margin: 0 auto;
  width: 100%;
}

.theme-layout {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.theme-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.theme-kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.theme-results-card {
  border: 1px solid var(--brand-border);
  border-radius: 1rem;
  background: var(--bg-white);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.theme-results-head {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.theme-results-head h4 {
  margin: 0;
  font-size: 1.05rem;
}

.theme-results-head p {
  margin: 0;
}

.theme-results-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.theme-view-toggle {
  display: none;
  align-items: center;
  gap: 0.2rem;
  padding: 0.2rem;
  border-radius: 999px;
  border: 1px solid var(--brand-border);
  background: #f8fafc;
}

.theme-view-toggle button {
  border: none;
  background: transparent;
  border-radius: 999px;
  padding: 0.3rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  color: var(--text-muted);
}

.theme-view-toggle button.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.theme-grid-desktop {
  display: none;
  gap: 1rem;
}

.theme-grid-stack {
  display: grid;
}

.theme-table-wrapper {
  display: none;
  border: 1px solid var(--brand-border);
  border-radius: 0.9rem;
  overflow: hidden;
  background: var(--bg-white);
}

.theme-table thead th {
  position: sticky;
  top: 0;
  background: #f8fafc;
  z-index: 1;
}

.theme-table tbody tr:hover {
  background: #f8fafc;
}

.zero-value {
  color: var(--text-muted);
}

.theme-card {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.theme-card__head {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  font-weight: 700;
}

.theme-card__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
}

.theme-metric {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.theme-metric strong {
  font-size: 1rem;
  color: var(--text-default);
}

.grid-two {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

.rating-bars {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.rating-bars li {
  display: grid;
  grid-template-columns: 3rem 1fr auto auto;
  gap: 0.5rem;
  align-items: center;
}

.rating-bar {
  height: 0.5rem;
  background: var(--brand-border);
  border-radius: 999px;
  overflow: hidden;
}

.rating-fill {
  display: block;
  height: 100%;
  background: var(--brand-accent);
}

.rating-label,
.rating-count,
.rating-percent {
  font-size: 0.85rem;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.tag-chip {
  background: var(--brand-primary);
  color: var(--brand-accent);
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  font-size: 0.8rem;
}

.review-cards {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.review-card {
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 0.9rem;
  background: #fafafa;
}

.review-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.review-meta {
  color: var(--text-muted);
  font-size: 0.85rem;
  margin: 0.25rem 0 0;
}

.review-content {
  margin: 0.3rem 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.review-content.expanded {
  -webkit-line-clamp: unset;
}

.review-date {
  font-size: 0.8rem;
  color: var(--text-muted);
  white-space: nowrap;
}

.link-btn {
  margin-top: 0.35rem;
  border: none;
  background: none;
  color: var(--brand-accent);
  font-weight: 600;
  cursor: pointer;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 0.8rem;
  font-size: 0.9rem;
}

.simple-table th,
.simple-table td {
  padding: 0.55rem 0.6rem;
  border-bottom: 1px solid var(--brand-border);
  text-align: left;
}

.simple-table tr.forecast-weekend td {
  background: #f8fafc;
}

.simple-table tr.forecast-holiday td {
  background: #fff7ed;
}

.forecast-table thead th {
  position: sticky;
  top: 0;
  background: #f8fafc;
  z-index: 1;
}

.forecast-table tbody tr:not(.forecast-holiday):hover td {
  background: #f8fafc;
}

.forecast-toolbar {
  display: none;
  justify-content: flex-end;
  margin: 0 0 0.6rem;
}

.forecast-view-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.2rem;
  border-radius: 999px;
  border: 1px solid var(--brand-border);
  background: #f8fafc;
}

.forecast-view-toggle button {
  border: none;
  background: transparent;
  border-radius: 999px;
  padding: 0.3rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 700;
  cursor: pointer;
  color: var(--text-muted);
}

.forecast-view-toggle button.active {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.forecast-cards {
  margin-top: 0.6rem;
}

.forecast-table-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.forecast-chart {
  border: 1px solid var(--brand-border);
  border-radius: 0.9rem;
  background: #f8fafc;
  padding: 0.75rem 0.9rem 0.9rem;
}

.forecast-chart__legend {
  display: flex;
  gap: 0.8rem;
  align-items: center;
  font-size: 0.8rem;
  color: var(--text-muted);
  margin-bottom: 0.4rem;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--brand-accent);
  display: inline-block;
}

.legend-band {
  width: 14px;
  height: 8px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.2);
  display: inline-block;
}

.forecast-chart__canvas {
  width: 100%;
  height: 180px;
  display: block;
}

.forecast-chart__band {
  fill: rgba(15, 118, 110, 0.15);
  stroke: none;
}

.forecast-chart__line {
  fill: none;
  stroke: var(--brand-accent);
  stroke-width: 2;
}

.forecast-chart__points circle {
  fill: var(--brand-accent);
}

.forecast-chart__axis {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}
.theme-table tbody tr {
  transition: background 0.2s ease;
}

.theme-table tbody tr:hover {
  background: #f8fafc;
}

.cell-right {
  text-align: right;
}

.loading-box,
.error-box,
.empty-box {
  padding: 1rem;
  border-radius: 0.8rem;
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  margin-top: 1rem;
}

.empty-box p {
  margin: 0 0 0.6rem;
}

.error-box {
  color: var(--danger);
}

.muted {
  color: var(--text-muted);
}

.forecast-meta {
  margin: 0.6rem 0 0;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.forecast-card-meta,
.forecast-card-range {
  margin: 0.35rem 0 0;
  font-size: 0.85rem;
}

.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
  flex-wrap: wrap;
}

.toolbar-note {
  margin: 0;
  color: var(--text-muted);
}

.primary-btn {
  border: none;
  background: var(--brand-accent);
  color: #fff;
  padding: 0.5rem 1rem;
  border-radius: 999px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, background 0.15s ease;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.primary-btn:not(:disabled):hover {
  background: var(--brand-primary-strong);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.2);
  transform: translateY(-1px);
}

.primary-btn:not(:disabled):active {
  transform: translateY(0);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.2);
}

.ai-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  min-height: 40px;
}

.spinner {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  animation: spin 0.8s linear infinite;
}

.ghost-btn {
  border: 1px solid var(--brand-border);
  background: #fff;
  border-radius: 999px;
  padding: 0.35rem 0.8rem;
  font-weight: 600;
  cursor: pointer;
}

.ghost-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-panel h3 {
  margin: 0.2rem 0 0;
}

.ai-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1rem;
}

.ai-meta {
  display: flex;
  flex-direction: column;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: flex-end;
  justify-content: flex-end;
  text-align: right;
}

.ai-meta-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  justify-content: flex-end;
}

.ai-chip {
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  font-size: 0.75rem;
  color: var(--text-muted);
  background: #f8fafc;
  border: 1px solid var(--brand-border);
}

.ai-kicker {
  text-transform: uppercase;
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.08em;
  margin: 0;
}

.ai-warning {
  margin: 0.35rem 0 0;
  font-size: 0.82rem;
  color: #b45309;
}

.ai-warning--muted {
  color: var(--text-muted);
}

.ai-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.2rem 0.5rem;
  border-radius: 999px;
  background: #fef3c7;
  color: #b45309;
  font-size: 0.7rem;
  font-weight: 700;
}

.ai-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 0.85rem;
  align-items: stretch;
}

.ai-wide {
  grid-column: 1 / -1;
}

.ai-block {
  border: 1px solid var(--brand-border);
  border-radius: 0.9rem;
  padding: 0.9rem 1rem;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.theme-ai-grid .ai-block,
.demand-ai-grid .ai-block {
  padding: 0.8rem 0.9rem;
}

.ai-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.ai-card__head h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.ai-card-label {
  font-size: 0.72rem;
  color: var(--text-muted);
}

.ai-toggle {
  font-size: 0.75rem;
  padding: 0.25rem 0.65rem;
}

.ai-list {
  display: grid;
  gap: 0.4rem;
  margin: 0;
  padding-left: 1.1rem;
  line-height: 1.6;
  word-break: keep-all;
  overflow-wrap: anywhere;
}

.ai-main {
  display: block;
}

.ai-evidence {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.78rem;
  color: var(--text-muted);
  line-height: 1.4;
}

.action-list {
  list-style: none;
  padding-left: 0;
  margin: 0.35rem 0 0;
  display: grid;
  gap: 0.5rem;
}

.action-item {
  display: grid;
  grid-template-columns: auto 1fr;
  column-gap: 0.5rem;
  row-gap: 0.15rem;
}

.action-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 700;
  line-height: 1;
  border-radius: 999px;
  padding: 0.2rem 0.55rem;
  flex: 0 0 auto;
}

.action-badge.urgent {
  background: #fee2e2;
  color: #b91c1c;
}

.action-badge.recommended {
  background: #fef3c7;
  color: #b45309;
}

.action-badge.improve {
  background: #e0f2fe;
  color: #0369a1;
}

.action-text {
  line-height: 1.4;
  word-break: keep-all;
}

.action-item .ai-evidence {
  grid-column: 2;
}

.monitoring-callout {
  margin-top: 0.8rem;
  padding: 0.75rem 0.9rem;
  border-radius: 0.7rem;
  background: #f8fafc;
  border: 1px dashed var(--brand-border);
}

.monitoring-callout h5 {
  margin: 0 0 0.5rem;
  font-size: 0.9rem;
}

.monitoring-callout ul {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--text-muted);
}

.risk-callout {
  grid-column: 1 / -1;
  padding: 0.8rem 1rem;
  border-radius: 0.8rem;
  background: #ecfdf3;
  border: 1px solid #86efac;
  color: #166534;
  font-weight: 600;
}

.report-skeleton {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.skeleton-line {
  height: 0.6rem;
  border-radius: 999px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

.skeleton-line.short {
  width: 40%;
}

.skeleton-line.long {
  width: 70%;
  height: 1.2rem;
}

.skeleton-line.mini {
  width: 50%;
  height: 0.5rem;
}

.skeleton-card {
  height: 110px;
  border-radius: 0.8rem;
  border: 1px solid var(--brand-border);
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

.skeleton-tags {
  display: flex;
  gap: 0.5rem;
}

.skeleton-chip {
  width: 90px;
  height: 32px;
  border-radius: 999px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

.skeleton-panels {
  display: grid;
  gap: 1rem;
}

.skeleton-panel {
  height: 160px;
  border-radius: 0.9rem;
  border: 1px solid var(--brand-border);
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

.ai-skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
}

.theme-more {
  display: flex;
  justify-content: center;
  margin-top: 0.75rem;
}


.ai-state {
  margin-top: 0;
}

.compare-skeleton {
  display: flex;
  gap: 0.5rem;
}

.table-only {
  display: table;
}

.mobile-only {
  display: none;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.report-card {
  background: var(--bg-white);
  border: 1px solid var(--brand-border);
  border-radius: 0.8rem;
  padding: 0.8rem;
}

.report-card__row {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  font-weight: 700;
}

@media (max-width: 768px) {
  .report-view select,
  .report-view .filters select {
    font-size: 16px;
    min-height: 44px;
    padding: 0.6rem 0.75rem;
    line-height: 1.2;
  }

  .report-view option {
    font-size: 16px;
  }

  .filters-grid {
    grid-template-columns: 1fr;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .compare-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .rating-bars li {
    grid-template-columns: 2.5rem 1fr auto;
  }

  .rating-percent {
    display: none;
  }

  .table-only {
    display: none;
  }

  .mobile-only {
    display: block;
  }

  .ai-grid {
    grid-template-columns: 1fr;
  }

  .ai-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .ai-meta {
    align-items: flex-start;
    text-align: left;
  }

  .ai-meta-chips {
    justify-content: flex-start;
  }

  .action-badge {
    font-size: 0.72rem;
    padding: 0.16rem 0.5rem;
  }
}

@media (max-width: 480px) {
  .context-bar {
    display: none;
  }

  .context-text {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .context-chips {
    margin-top: 0;
    position: static;
    width: 100%;
  }
}

@media (min-width: 769px) {
  .filters-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-card--overview,
  .ai-card--summary {
    grid-column: 1 / -1;
  }

  .ai-wide {
    grid-column: auto;
  }
}

@media (min-width: 1024px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .report-section {
    max-width: 1240px;
    margin: 0 auto;
    width: 100%;
  }

  .ai-panel {
    padding: 1.1rem 1.25rem;
  }

  .theme-grid {
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }

  .tag-chip-list {
    flex-wrap: wrap;
    overflow: visible;
  }

  .theme-layout {
    flex-direction: row;
    align-items: flex-start;
  }

  .theme-filter {
    flex: 0 0 300px;
    position: sticky;
    top: 96px;
  }

  .theme-content {
    flex: 1;
  }

  .theme-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .theme-view-toggle {
    display: flex;
  }

  .theme-results-head {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .theme-grid-desktop {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .theme-table-wrapper {
    display: block;
  }

  .theme-grid-stack {
    display: none;
  }

  .forecast-toolbar {
    display: flex;
  }
}

@media (min-width: 1280px) {
  .theme-grid-desktop {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .kpi-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (min-width: 1440px) {
  .theme-grid-desktop {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
</style>
