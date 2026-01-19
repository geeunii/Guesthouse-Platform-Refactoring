<script setup>
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {useRouter} from 'vue-router'
import {fetchHostDashboardSummary, fetchHostTodaySchedule} from '@/api/hostDashboard'
import { fetchHostAccommodations } from '@/api/hostAccommodation'
import { deriveHostGateInfo, normalizeApprovalStatus } from '@/composables/useHostState'
import NavStay from '@/components/nav-icons/NavStay.vue'
import { formatCurrency, formatDate, formatNumber, formatShortTime } from '@/utils/formatters'

const router = useRouter()

const dashboardSummary = ref({
  confirmedRevenue: 0,
  confirmedReservations: 0,
  avgRating: 0,
  operatingAccommodations: 0,
  totalAccommodations: 0
})

const todaySchedule = ref([])
const accommodations = ref([])
const accommodationsLoading = ref(false)
const accommodationsError = ref('')
const hostAccessDenied = ref(false)
const todayLabel = ref('')
const isLoading = ref(false)
const summaryError = ref('')
const scheduleError = ref('')
const prefersReducedMotion = ref(false)

const periodOptions = [
  { value: 'today', label: '오늘' },
  { value: '7days', label: '7일' },
  { value: '30days', label: '30일' },
  { value: 'month', label: '이번달' },
  { value: 'year', label: '올해' }
]

const selectedPeriod = ref('month')

const periodLabel = computed(() => {
  const option = periodOptions.find((item) => item.value === selectedPeriod.value)
  return option ? option.label : '이번달'
})

const periodPrefix = computed(() => {
  switch (selectedPeriod.value) {
    case 'today':
      return '오늘'
    case '7days':
      return '최근 7일'
    case '30days':
      return '최근 30일'
    case 'year':
      return '올해'
    default:
      return '이번 달'
  }
})

const animatedSummary = ref({
  confirmedRevenue: 0,
  confirmedReservations: 0,
  avgRating: 0,
  operatingAccommodations: 0,
  totalAccommodations: 0
})

const scheduleFilters = [
  { value: 'all', label: '전체' },
  { value: 'checkin', label: '체크인' },
  { value: 'checkout', label: '체크아웃' }
]

const selectedScheduleFilter = ref('all')

const scheduleCountLabel = computed(() => {
  if (selectedScheduleFilter.value === 'checkin') return '체크인'
  if (selectedScheduleFilter.value === 'checkout') return '체크아웃'
  return '체크인/아웃'
})

const parseTimeToMinutes = (time) => {
  if (!time) return null
  const [hours, minutes] = String(time).split(':').map((item) => Number(item))
  if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return null
  return (hours * 60) + minutes
}

const todayInsight = computed(() => {
  const checkinCount = todaySchedule.value.filter((item) => item.type === 'CHECKIN').length
  const checkoutCount = todaySchedule.value.filter((item) => item.type === 'CHECKOUT').length
  const total = checkinCount + checkoutCount
  if (!total) return null

  let title = ''
  let description = ''
  let tag = '오늘'
  let typeQuery = null

  if (checkinCount > 0 && checkoutCount > 0) {
    title = `오늘 일정 ${total}건이 있어요`
    description = `체크인 ${checkinCount} · 체크아웃 ${checkoutCount}`
  } else if (checkinCount > 0) {
    title = `오늘 체크인 ${checkinCount}건이 있어요`
    description = '체크아웃 일정은 없어요'
    tag = '체크인'
    typeQuery = 'CHECKIN'
  } else {
    title = `오늘 체크아웃 ${checkoutCount}건이 있어요`
    description = '체크인 일정은 없어요'
    tag = '체크아웃'
    typeQuery = 'CHECKOUT'
  }

  const earliest = todaySchedule.value
    .map((item) => ({ ...item, minutes: parseTimeToMinutes(item.time) }))
    .filter((item) => Number.isFinite(item.minutes))
    .sort((a, b) => a.minutes - b.minutes)[0]

  const earliestHint = earliest
    ? `첫 일정 ${formatTimeShort(earliest.time)} · ${earliest.accommodationName}`
    : null

  return {
    title,
    description,
    tag,
    typeQuery,
    earliestHint
  }
})

const hasKpiData = computed(() => {
  const summary = dashboardSummary.value
  return Boolean(
    summary.confirmedRevenue ||
    summary.confirmedReservations ||
    summary.avgRating ||
    summary.operatingAccommodations ||
    summary.totalAccommodations
  )
})

const formatKpiValue = (value, unit) => {
  if (typeof value === 'number') {
    if (unit === '/5.0') {
      return `${value.toFixed(1)}${unit}`
    }
    if (unit === '₩') {
      return formatCurrency(value)
    }
    return `${formatNumber(value)}${unit ?? ''}`
  }
  return `${value}${unit ?? ''}`
}

const formatTimeShort = (time) => {
  return formatShortTime(time)
}

const kpis = computed(() => ([
  {
    key: 'confirmedRevenue',
    label: `${periodPrefix.value} 확정 매출`,
    value: animatedSummary.value.confirmedRevenue ?? 0,
    unit: '₩',
    tone: 'positive',
    delta: null,
    target: '/host/revenue',
    disabled: pendingOnly.value
  },
  {
    key: 'confirmedReservations',
    label: `${periodPrefix.value} 예약 확정`,
    value: animatedSummary.value.confirmedReservations ?? 0,
    unit: '건',
    tone: 'positive',
    delta: null,
    target: '/host/booking',
    disabled: pendingOnly.value
  },
  {
    key: 'avgRating',
    label: `${periodPrefix.value} 평균 평점`,
    value: animatedSummary.value.avgRating ?? 0,
    unit: '/5.0',
    tone: 'neutral',
    delta: null,
    target: '/host/review',
    disabled: pendingOnly.value
  },
  {
    key: 'operatingAccommodations',
    label: '내 숙소 운영 상태',
    value: animatedSummary.value.operatingAccommodations ?? 0,
    total: animatedSummary.value.totalAccommodations ?? 0,
    unit: '운영중',
    tone: 'warning',
    delta: null,
    target: '/host/accommodation',
    disabled: false
  }
]))

const tasks = computed(() => todaySchedule.value.map((item) => ({
  id: item.reservationId ?? `${item.accommodationName}-${item.time}`,
  type: item.type === 'CHECKOUT' ? 'checkout' : 'checkin',
  time: item.time || '',
  displayTime: formatTimeShort(item.time),
  accommodation: `${item.accommodationName}${item.roomName ? ` ${item.roomName}` : ''}`,
  guest: item.guestName || '',
  phone: item.phone || '',
  email: '',
  memo: item.requestNote || ''
})))

const filteredTasks = computed(() => {
  if (selectedScheduleFilter.value === 'checkin') {
    return tasks.value.filter((task) => task.type === 'checkin')
  }
  if (selectedScheduleFilter.value === 'checkout') {
    return tasks.value.filter((task) => task.type === 'checkout')
  }
  return tasks.value
})

const hasMemo = computed(() => filteredTasks.value.some(t => t.memo))

const emptyMessage = computed(() => {
  return selectedScheduleFilter.value === 'all'
    ? '오늘 예정된 일정이 없습니다.'
    : '선택한 조건의 일정이 없습니다.'
})

const normalizeAccommodation = (item) => {
  const statusSource = item.approvalStatus ?? item.status ?? item.accommodationStatus ?? item.reviewStatus
  const id = item.accommodationsId ?? item.accommodationId ?? item.id
  const normalizedStatus = normalizeApprovalStatus(statusSource)
  return {
    id: id ?? `${item.name ?? 'acc'}-${Math.random()}`,
    name: item.accommodationsName ?? item.name ?? '',
    approvalStatus: normalizedStatus,
    rejectReason: item.rejectReason ?? item.rejectionReason ?? item.approvalReason ?? item.reason ?? ''
  }
}

const hostGateInfo = computed(() => deriveHostGateInfo(accommodations.value))
const counts = computed(() => hostGateInfo.value.counts)
const approvedCount = computed(() => counts.value.approved)
const pendingCount = computed(() => counts.value.pending)
const totalCount = computed(() => counts.value.total)
const hostState = computed(() => hostGateInfo.value.hostState)
const isRecheck = computed(() => hostGateInfo.value.hostState === 'recheck')
const pendingOnly = computed(() => ['pending-only', 'recheck'].includes(hostState.value))

const rejectedReasonText = computed(() => {
  const reason = hostGateInfo.value.rejectedReason
  return reason ? `반려 사유: ${reason}` : '등록 정보를 확인 후 수정해 주세요.'
})

const hostStateComputed = computed(() => {
  if (accommodationsLoading.value) return 'loading'
  if (hostAccessDenied.value) return 'empty'
  if (hostGateInfo.value.hostState === 'rejected' && totalCount.value > 0 && approvedCount.value === 0 && pendingCount.value === 0) {
    return 'rejected'
  }
  return hostGateInfo.value.hostState
})

const goToRegister = () => router.push('/host/accommodation/register')
const goToManage = () => router.push('/host/accommodation')

const goTo = (path) => {
  if (path) router.push(path)
}

const toLocalDateString = (date) => {
  const value = date ?? new Date()
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const goToTodayBookings = () => {
  if (!todayInsight.value) return
  router.push({
    path: '/host/booking',
    query: {
      view: 'list',
      range: 'today',
      type: 'all',
      sort: 'checkin',
      includePast: 0,
      date: toLocalDateString(new Date())
    }
  })
}

const selectedTask = ref(null)
const showTaskModal = ref(false)
const openTask = (task) => {
  selectedTask.value = task
  showTaskModal.value = true
}
const closeTask = () => {
  selectedTask.value = null
  showTaskModal.value = false
}

const kpiGridRef = ref(null)
const activeKpiIndex = ref(0)
let kpiScrollHandler = null

const animateValue = (key, target, options = {}) => {
  const duration = options.duration ?? 420
  const decimals = options.decimals ?? 0
  const start = animatedSummary.value[key] ?? 0
  if (prefersReducedMotion.value) {
    animatedSummary.value[key] = target
    return
  }
  const startTime = performance.now()
  const step = (now) => {
    const progress = Math.min((now - startTime) / duration, 1)
    const value = start + (target - start) * progress
    animatedSummary.value[key] = decimals ? Number(value.toFixed(decimals)) : Math.round(value)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

const runKpiCountUp = () => {
  animateValue('confirmedRevenue', dashboardSummary.value.confirmedRevenue ?? 0)
  animateValue('confirmedReservations', dashboardSummary.value.confirmedReservations ?? 0)
  animateValue('avgRating', dashboardSummary.value.avgRating ?? 0, { decimals: 1 })
  animateValue('operatingAccommodations', dashboardSummary.value.operatingAccommodations ?? 0)
  animateValue('totalAccommodations', dashboardSummary.value.totalAccommodations ?? 0)
}

const setupKpiIndicator = () => {
  const grid = kpiGridRef.value
  if (!grid) return
  const cards = grid.querySelectorAll('.kpi-card')
  const firstCard = cards[0]
  if (!firstCard) return
  const cardWidth = firstCard.getBoundingClientRect().width
  const gap = 12

  kpiScrollHandler = () => {
    const index = Math.round(grid.scrollLeft / (cardWidth + gap))
    activeKpiIndex.value = Math.min(Math.max(index, 0), kpis.value.length - 1)
  }

  grid.addEventListener('scroll', kpiScrollHandler, { passive: true })
}

const scrollToKpi = (index) => {
  const grid = kpiGridRef.value
  if (!grid) return
  const cards = grid.querySelectorAll('.kpi-card')
  const target = cards[index]
  if (!target) return
  grid.scrollTo({ left: target.offsetLeft, behavior: 'smooth' })
}

const buildSummaryParams = () => ({ range: selectedPeriod.value })

const loadDashboard = async () => {
  if (hostAccessDenied.value) return
  isLoading.value = true
  summaryError.value = ''
  scheduleError.value = ''
  const today = new Date()
  todayLabel.value = formatDate(today, true)

  const [summaryRes, scheduleRes] = await Promise.all([
    fetchHostDashboardSummary(buildSummaryParams()),
    fetchHostTodaySchedule({date: today.toISOString().slice(0, 10)})
  ])

  if (summaryRes.status === 403 || scheduleRes.status === 403) {
    hostAccessDenied.value = true
    isLoading.value = false
    return
  }

  if (summaryRes.ok && summaryRes.data) {
    dashboardSummary.value = summaryRes.data
  } else {
    summaryError.value = '데이터를 불러오지 못했어요.'
  }

  if (scheduleRes.ok && Array.isArray(scheduleRes.data)) {
    todaySchedule.value = scheduleRes.data
  } else {
    scheduleError.value = '데이터를 불러오지 못했어요.'
  }

  isLoading.value = false
  runKpiCountUp()
}

const loadAccommodations = async () => {
  accommodationsLoading.value = true
  accommodationsError.value = ''
  const response = await fetchHostAccommodations()
  if (response.ok) {
    const payload = response.data
    const list = Array.isArray(payload)
      ? payload
      : payload?.items ?? payload?.content ?? payload?.data ?? []
    accommodations.value = list.map(normalizeAccommodation)
  } else if (response.status === 403) {
    hostAccessDenied.value = true
    accommodations.value = []
  } else {
    accommodationsError.value = '숙소 상태를 불러오지 못했습니다.'
  }
  accommodationsLoading.value = false
}

onMounted(async () => {
  prefersReducedMotion.value = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false
  hostAccessDenied.value = false
  await loadAccommodations()
  if (!hostAccessDenied.value) {
    loadDashboard()
    setupKpiIndicator()
  }
})

onUnmounted(() => {
  const grid = kpiGridRef.value
  if (grid && kpiScrollHandler) {
    grid.removeEventListener('scroll', kpiScrollHandler)
  }
})

watch(selectedPeriod, () => {
  loadDashboard()
})
</script>

<template>
  <div class="dashboard-home">
    <section v-if="hostStateComputed === 'empty' || hostStateComputed === 'rejected' || hostStateComputed === 'pending-only' || hostStateComputed === 'recheck'" class="host-state">
      <div class="state-card host-card">
        <div class="state-icon">
          <NavStay />
        </div>
        <div class="state-text">
          <h3 v-if="hostStateComputed === 'empty'">숙소를 등록하세요!</h3>
          <h3 v-else-if="hostStateComputed === 'pending-only' || hostStateComputed === 'recheck'">
            {{ isRecheck ? '숙소 재검토중이에요' : '숙소 심사중이에요' }}
          </h3>
          <h3 v-else-if="hostStateComputed === 'rejected'">숙소 심사가 반려되었어요</h3>
          <h3 v-else>숙소 상태를 확인 중이에요</h3>
          <p v-if="hostStateComputed === 'empty'">숙소를 등록하면 예약/매출뿐 아니라 리뷰, 일정, 통계까지 한 곳에서 관리할 수 있어요.</p>
          <p v-else-if="hostStateComputed === 'pending-only' || hostStateComputed === 'recheck'">
            {{ isRecheck ? `이전 반려 사유: ${hostGateInfo.recheckReason}` : '등록하신 숙소를 확인하고 있어요. 평균 영업일 1~2일 내에 결과를 안내합니다.' }}
          </p>
          <p v-else-if="hostStateComputed === 'rejected'">
            {{ rejectedReasonText }}
          </p>
          <p v-else-if="accommodationsError">{{ accommodationsError }}</p>
        </div>
        <div class="state-actions">
          <button
            v-if="hostStateComputed === 'empty'"
            class="state-btn primary"
            type="button"
            @click="goToRegister"
          >
            숙소 등록하기
          </button>
          <template v-else>
            <button class="state-btn" type="button" @click="goToManage">숙소 관리</button>
            <button
              class="state-btn primary"
              type="button"
              @click="goToManage"
            >
              {{ hostStateComputed === 'rejected' ? '수정 후 재제출' : '등록 정보 확인/수정' }}
            </button>
          </template>
        </div>
      </div>
    </section>

    <template v-else>
      <header class="host-view-header">
        <div>
          <h2 class="host-title">대시보드</h2>
          <p class="host-subtitle">{{ periodLabel }} 기준 운영 현황을 빠르게 확인하세요.</p>
        </div>
      </header>

      <section v-if="pendingCount > 0" class="pending-banner host-card" :class="{ 'pending-banner--only': pendingOnly }">
        <div class="pending-banner__text">
          <h3>숙소 심사중이에요 ({{ pendingCount }}개)</h3>
          <p>관리자 승인 후 예약/매출/리뷰가 활성화됩니다. 평균 영업일 1~2일 내에 결과를 안내합니다.</p>
        </div>
        <div class="pending-banner__actions">
          <button class="ghost-btn" type="button" @click="goToManage">숙소 관리</button>
          <button class="ghost-btn pending-cta" type="button" @click="goToRegister">추가 등록</button>
        </div>
      </section>

      <button
        v-if="todayInsight"
        class="insight-card host-card"
        type="button"
        aria-label="오늘 일정 요약"
        @click="goToTodayBookings"
      >
        <div class="insight-main">
          <span class="insight-icon" aria-hidden="true">🔔</span>
          <div class="insight-text">
            <div class="insight-title">{{ todayInsight.title }}</div>
            <div class="insight-desc">{{ todayInsight.description }}</div>
            <div v-if="todayInsight.earliestHint" class="insight-meta">{{ todayInsight.earliestHint }}</div>
          </div>
        </div>
        <div class="insight-side">
          <span class="insight-cta insight-cta-btn">예약 관리 &gt;</span>
        </div>
      </button>

      <section class="period-segment" role="tablist" aria-label="기간 선택">
        <button
          v-for="option in periodOptions"
          :key="option.value"
          class="segment-btn host-chip"
          :class="{ 'host-chip--active': selectedPeriod === option.value }"
          type="button"
          role="tab"
          :aria-selected="selectedPeriod === option.value"
          @click="selectedPeriod = option.value"
        >
          {{ option.label }}
        </button>
      </section>

      <!-- KPI grid -->
      <section class="kpi-grid" ref="kpiGridRef" :class="{ 'fade-section': !isLoading }">
        <div v-if="isLoading" class="kpi-skeleton">
          <div v-for="i in 3" :key="i" class="skeleton-card" />
        </div>
        <div v-else-if="summaryError" class="status-card">
          <p>데이터를 불러오지 못했어요.</p>
          <button class="ghost-btn" type="button" @click="loadDashboard">다시 시도</button>
        </div>
        <div v-else-if="!hasKpiData && !pendingOnly" class="status-card">
          <p>선택한 기간에 확정 매출이 없습니다.</p>
          <button class="ghost-btn" type="button" @click="selectedPeriod = 'month'">기간 변경</button>
        </div>
        <article
            v-for="item in kpis"
            :key="item.label"
            class="kpi-card"
            :class="{ 'fade-item': !isLoading, 'is-disabled': item.disabled }"
            :style="{ animationDelay: `${Math.min(kpis.indexOf(item), 5) * 60}ms` }"
            role="button"
            :tabindex="item.disabled ? -1 : 0"
            :aria-disabled="item.disabled ? 'true' : 'false'"
            @click="!item.disabled && goTo(item.target)"
            @keypress.enter="!item.disabled && goTo(item.target)"
        >
          <div class="kpi-top">
            <p class="kpi-label">{{ item.label }}</p>
          </div>
          <p class="kpi-value">
            <span v-if="item.total !== undefined">
              <span v-if="item.total > 0">운영중 {{ item.value }} / 전체 {{ item.total }}</span>
              <span v-else>운영중 {{ item.value }}</span>
            </span>
            <span v-else>{{ formatKpiValue(item.value, item.unit) }}</span>
          </p>
          <p v-if="item.disabled" class="kpi-sub">승인 후 집계됩니다.</p>
          <div class="kpi-delta" :class="{ hidden: !item.delta }">
            <span v-if="item.delta">{{ item.delta }}</span>
          </div>
        </article>
      </section>

      <div class="kpi-indicator" aria-hidden="true">
        <button
          v-for="(_, index) in kpis"
          :key="index"
          class="kpi-dot"
          :class="{ active: index === activeKpiIndex }"
          type="button"
          @click="scrollToKpi(index)"
        />
      </div>

      <!-- Today tasks -->
      <section class="task-panel" :class="{ 'fade-section': !isLoading }">
        <div class="task-head">
          <div>
            <h3>오늘 일정</h3>
            <p class="task-date">{{ todayLabel }}</p>
          </div>
          <span class="task-chip host-chip">
            <span class="task-chip-label">{{ scheduleCountLabel }}</span>
            <span class="task-chip-count">{{ filteredTasks.length }}건</span>
          </span>
        </div>

        <div class="task-filters">
          <button
            v-for="filter in scheduleFilters"
            :key="filter.value"
            class="filter-chip host-chip"
            :class="{ 'host-chip--active': selectedScheduleFilter === filter.value }"
            type="button"
            @click="selectedScheduleFilter = filter.value"
          >
            {{ filter.label }}
          </button>
        </div>

        <div class="task-list">
          <div v-if="isLoading" class="task-skeleton">
            <div v-for="i in 4" :key="i" class="skeleton-card" />
          </div>
          <div v-else-if="scheduleError" class="status-card">
            <p>데이터를 불러오지 못했어요.</p>
            <button class="ghost-btn" type="button" @click="loadDashboard">다시 시도</button>
          </div>
          <div v-for="(task, index) in filteredTasks" :key="task.id" class="task-card" :class="{ 'fade-item': !isLoading }"
               :style="{ animationDelay: `${Math.min(index, 5) * 70}ms` }"
               role="button" tabindex="0" @click="openTask(task)"
               @keypress.enter="openTask(task)">
            <div class="task-row">
              <span class="pill" :class="task.type === 'checkin' ? 'pill-green' : 'pill-gray'">
                {{ task.type === 'checkin' ? '체크인' : '체크아웃' }}
              </span>
              <span class="time">{{ task.displayTime }}</span>
            </div>
            <p class="accommodation">{{ task.accommodation }}</p>
            <p class="guest">{{ task.guest }} 님</p>
            <div class="task-actions">
              <a
                v-if="task.phone"
                class="call-btn"
                :href="`tel:${task.phone}`"
                @click.stop
                aria-label="게스트 전화"
              >
                <span class="call-icon">☎</span>
                <span>전화</span>
              </a>
              <span class="detail-hint">
                예약 상세
                <span class="chevron">›</span>
              </span>
            </div>
            <p v-if="task.memo" class="memo">📝 {{ task.memo }}</p>
          </div>
        </div>

        <div v-if="!filteredTasks.length && !isLoading && !scheduleError" class="status-card">
          <p>{{ emptyMessage }}</p>
          <button class="ghost-btn" type="button" @click="goToTodayBookings">목록 보기</button>
        </div>
        <p v-else-if="isLoading" class="empty">일정을 불러오는 중입니다.</p>
        <p v-else-if="hasMemo" class="footnote">메모가 있는 일정은 📝 로 표시됩니다.</p>
      </section>
    </template>

    <div v-if="showTaskModal && selectedTask" class="modal-backdrop" @click.self="closeTask">
      <div class="modal">
        <header class="modal-head">
          <div>
            <p class="eyebrow small">오늘 일정</p>
            <h3>{{ selectedTask.accommodation }}</h3>
          </div>
          <button class="close-btn" @click="closeTask">×</button>
        </header>
        <div class="modal-body">
          <div class="modal-row"><span>유형</span><strong>{{ selectedTask.type === 'checkin' ? '체크인' : '체크아웃' }}</strong>
          </div>
          <div class="modal-row"><span>시간</span><strong>{{ selectedTask.time }}</strong></div>
          <div class="modal-row"><span>게스트</span><strong>{{ selectedTask.guest }}</strong></div>
          <div class="modal-row"><span>연락처</span><strong>{{ selectedTask.phone || '미입력' }}</strong></div>
          <div class="modal-row"><span>이메일</span><strong>{{ selectedTask.email || '미입력' }}</strong></div>
          <div class="modal-row"><span>메모</span><strong>{{ selectedTask.memo || '메모 없음' }}</strong></div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-home {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 100vh;
  padding: 0 0 calc(1.5rem + var(--bn-h, 0px) + (var(--bn-pad, 0px) * 2) + env(safe-area-inset-bottom));
}

.view-header h2 {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--brand-accent);
  margin: 0.25rem 0;
}

.eyebrow {
  font-size: 0.85rem;
  color: var(--brand-accent);
  font-weight: 700;
  margin: 0;
}

.subtitle {
  color: #6b7280;
  margin: 0;
}

.host-state {
  display: flex;
  justify-content: center;
}

.state-card {
  width: 100%;
  max-width: 420px;
  padding: 1.2rem 1.1rem;
  display: grid;
  gap: 0.9rem;
  text-align: left;
}

.state-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: var(--brand-primary);
  color: var(--brand-accent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.state-icon :deep(svg),
.state-icon :deep(img) {
  width: 22px;
  height: 22px;
  display: block;
}

.state-text h3 {
  margin: 0 0 0.35rem;
  font-size: 1.15rem;
  font-weight: 900;
  color: var(--brand-accent);
}

.state-text p {
  margin: 0;
  color: #475569;
  font-weight: 700;
  line-height: 1.5;
}

.state-actions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.state-btn {
  border: 1px solid var(--brand-primary-strong, #0f766e);
  background: #ffffff;
  color: var(--brand-accent);
  border-radius: 12px;
  min-height: 44px;
  padding: 0.6rem 0.9rem;
  font-weight: 900;
}

.state-btn.primary {
  background: var(--brand-primary);
  color: #0f172a;
}

.state-btn:focus-visible {
  outline: 2px solid var(--brand-primary-strong, #0f766e);
  outline-offset: 2px;
}

.pending-banner {
  width: 100%;
  border: 1px solid #fde68a;
  background: #fffbeb;
  color: #92400e;
  border-radius: 16px;
  padding: 0.95rem 1.1rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.pending-banner__text h3 {
  margin: 0 0 0.25rem;
  font-size: 1rem;
  font-weight: 900;
}

.pending-banner__text p {
  margin: 0;
  font-weight: 700;
  color: #92400e;
}

.pending-banner__actions {
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.pending-banner__actions .ghost-btn {
  white-space: nowrap;
  word-break: keep-all;
}

.pending-cta {
  background: #fef3c7;
  border-color: #f59e0b;
  color: #92400e;
}

.insight-card {
  width: 100%;
  border: 1px solid var(--brand-primary-strong);
  background: var(--brand-primary);
  color: var(--text-default);
  border-radius: 16px;
  padding: 0.95rem 1.1rem;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.65rem;
  text-align: left;
  min-height: 44px;
}

.insight-card:hover {
  background: var(--brand-primary-strong);
}

.insight-card:focus-visible {
  outline: 2px solid var(--brand-primary-strong);
  outline-offset: 2px;
}

.insight-main {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  width: 100%;
}

.insight-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: var(--surface);
  color: var(--brand-accent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  flex: 0 0 auto;
}

.insight-text {
  display: grid;
  gap: 0.2rem;
}

.insight-title {
  font-weight: 900;
  color: var(--brand-accent);
  font-size: 1rem;
}

.insight-desc {
  font-weight: 700;
  color: var(--text-default);
  font-size: 0.9rem;
}

.insight-meta {
  font-size: 0.85rem;
  color: #475569;
  font-weight: 700;
}

.insight-side {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
  width: 100%;
}

.insight-cta {
  color: var(--brand-accent);
  font-weight: 800;
  font-size: 0.9rem;
}

.insight-cta-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.35rem 0.8rem;
  min-width: 120px;
  min-height: 36px;
  border-radius: 999px;
  border: 1px solid var(--brand-primary-strong);
  background: var(--surface);
  color: var(--brand-accent);
  font-weight: 800;
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

.period-segment {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--brand-bg);
  padding-top: 0.35rem;
  padding-bottom: 0.25rem;
  scroll-snap-type: x mandatory;
}

.fade-section {
  animation: fadeUp 240ms ease both;
}

.fade-item {
  animation: fadeUp 240ms ease both;
}

.segment-btn {
  font-weight: 700;
  border-radius: 999px;
  padding: 0.45rem 0.85rem;
  font-size: 0.9rem;
  white-space: nowrap;
  scroll-snap-align: start;
}

.kpi-grid {
  display: flex;
  gap: 0.75rem;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: 0.25rem;
  position: relative;
}

.kpi-skeleton {
  display: flex;
  gap: 0.75rem;
  width: 100%;
}

.skeleton-card {
  flex: 0 0 78%;
  height: 120px;
  border-radius: 14px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
}

.kpi-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 1.1rem 1.25rem;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  transition: transform 0.12s ease, box-shadow 0.12s ease, border-color 0.12s ease;
  cursor: pointer;
  flex: 0 0 78%;
  scroll-snap-align: start;
}

.kpi-card.is-disabled {
  opacity: 0.6;
  cursor: default;
  box-shadow: none;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.kpi-label {
  font-size: 0.95rem;
  color: #4b5563;
  margin: 0;
}

.kpi-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.kpi-sub {
  margin: 0.35rem 0 0;
  font-size: 0.85rem;
  color: #6b7280;
  font-weight: 700;
}

.kpi-delta {
  margin-top: 0.4rem;
  font-size: 0.85rem;
  color: #6b7280;
  min-height: 1rem;
}

.kpi-delta.hidden {
  visibility: hidden;
}

.kpi-indicator {
  display: flex;
  justify-content: center;
  gap: 0.35rem;
  margin-top: -0.25rem;
}

.kpi-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: none;
  background: #cbd5e1;
  opacity: 0.6;
}

.kpi-dot.active {
  background: var(--brand-primary);
  opacity: 1;
}

.kpi-card:hover {
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
  border-color: var(--brand-primary-strong);
}

.task-panel {
  background: white;
  border: 1px solid var(--brand-border);
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}

.task-head h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #0f172a;
}

.task-date {
  margin: 0.15rem 0 0;
  color: #6b7280;
  font-size: 0.95rem;
}

.task-chip {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
  font-weight: 700;
  padding: 0.4rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  line-height: 1.1;
  text-align: center;
}

.task-chip-label {
  font-size: 0.78rem;
}

.task-chip-count {
  font-size: 0.95rem;
  font-weight: 800;
}

.task-filters {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  overflow-x: auto;
}

.filter-chip {
  font-weight: 700;
  border-radius: 999px;
  padding: 0.3rem 0.75rem;
  font-size: 0.85rem;
  white-space: nowrap;
}

.task-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.75rem;
}

.task-skeleton {
  display: grid;
  gap: 0.75rem;
}

.task-card {
  border: 1px solid var(--brand-border);
  border-radius: 12px;
  padding: 1rem;
  background: #f9fafb;
  cursor: pointer;
  transition: transform 0.12s ease, box-shadow 0.12s ease, border-color 0.12s ease;
}

.task-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.35rem;
}

.pill {
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 700;
}

.pill-green {
  background: var(--brand-primary);
  color: var(--brand-accent);
}

.pill-gray {
  background: #e5e7eb;
  color: #4b5563;
}

.time {
  font-weight: 700;
  color: #111827;
}

.accommodation {
  font-weight: 700;
  color: #111827;
  margin: 0.1rem 0;
}

.guest {
  margin: 0;
  color: #374151;
  font-size: 0.95rem;
}

.task-actions {
  margin-top: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: #64748b;
}

.call-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  border: 1px solid var(--brand-primary-strong);
  background: var(--brand-primary);
  color: var(--brand-accent);
  font-weight: 700;
  text-decoration: none;
}

.call-icon {
  font-size: 0.9rem;
}

.detail-hint {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  font-weight: 700;
  color: #64748b;
}

.chevron {
  font-size: 1rem;
}

.memo {
  margin: 0.35rem 0 0;
  color: #b45309;
  font-size: 0.9rem;
}

.task-card:hover {
  border-color: var(--brand-primary-strong);
  background: var(--brand-primary);
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.07);
}

.empty {
  text-align: center;
  color: #9ca3af;
  margin: 1rem 0 0;
}

.footnote {
  margin: 0.75rem 0 0;
  font-size: 0.85rem;
  color: #6b7280;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  z-index: 80;
}

.modal {
  background: white;
  border-radius: 14px;
  padding: 1.25rem;
  width: min(420px, 100%);
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.2);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.close-btn {
  border: none;
  background: var(--brand-bg);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 1.1rem;
  cursor: pointer;
}

.modal-body {
  margin-top: 1rem;
  display: grid;
  gap: 0.55rem;
}

.modal-row {
  display: flex;
  justify-content: space-between;
  gap: 0.5rem;
}

.modal-row span:first-child {
  color: #6b7280;
}

.eyebrow.small {
  font-size: 0.8rem;
  margin: 0;
}

@media (min-width: 768px) {
  .dashboard-home {
    padding: 1.5rem 1.5rem calc(2rem + var(--bn-h, 0px) + (var(--bn-pad, 0px) * 2) + env(safe-area-inset-bottom));
  }

  .pending-banner__actions {
    flex-direction: row;
  }

  .period-segment {
    position: static;
    background: transparent;
    padding-top: 0;
  }

  .kpi-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    overflow: visible;
  }

  .kpi-card {
    flex: 1;
  }

  .kpi-skeleton,
  .skeleton-card {
    flex: 1;
  }

  .kpi-indicator {
    display: none;
  }

  .task-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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

@media (prefers-reduced-motion: reduce) {
  .fade-section,
  .fade-item,
  .skeleton-card {
    animation: none !important;
  }
}

@media (min-width: 1024px) {
  .host-state {
    justify-content: center;
  }

  .state-card {
    max-width: 600px;
  }

  .state-actions {
    flex-direction: row;
    align-items: center;
  }

  .insight-card {
    display: grid;
    grid-template-columns: 1fr auto;
    align-items: center;
    padding: 16px 20px;
    min-height: 72px;
  }

  .insight-main {
    align-items: flex-start;
    gap: 12px;
  }

  .insight-side {
    width: auto;
    justify-content: flex-end;
    align-items: center;
    gap: 12px;
  }

  .insight-cta-btn {
    min-height: 40px;
    padding: 0.4rem 0.95rem;
  }

  .view-header h2 {
    font-size: 2rem;
  }

  .task-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
