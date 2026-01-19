<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchHostBookings, fetchHostBookingCalendar, fetchRefundQuote } from '@/api/hostBooking'
import { fetchHostAccommodations } from '@/api/hostAccommodation'
import { deriveHostGateInfo, buildHostGateNotice } from '@/composables/useHostState'
import { formatCurrency, formatDate, formatDateRange, formatDateTime } from '@/utils/formatters'
import HostGateNotice from '@/components/host/HostGateNotice.vue'
import ReservationCard from '@/components/host/ReservationCard.vue'

const bookings = ref([])
const calendarBookings = ref([])
const isLoading = ref(false)
const loadError = ref('')
const isDesktop = ref(false)
const pageSize = ref(20)
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const cursorNext = ref(null)
const hasNextCursor = ref(false)
const loadMoreLoading = ref(false)
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

const statusColor = {
  요청: 'badge-gray',
  확정: 'badge-green',
  '체크인 완료': 'badge-outline',
  취소: 'badge-red'
}

const paymentBadge = {
  0: '결제 대기',
  1: '결제 완료',
  2: '결제 실패'
}

const refundBadge = {
  0: '환불 요청',
  1: '환불 완료',
  2: '환불 실패'
}

const route = useRoute()
const router = useRouter()
const activeTab = computed(() => {
  return route.query.view === 'calendar' ? 'calendar' : 'list'
})
const selectedDate = ref(null)
const selectedBooking = ref(null)
const showModal = ref(false)
const refundQuoteLoading = ref(false)
const refundQuoteError = ref('')
const refundQuote = ref(null)
const includePast = ref(false)
const isFilterSheetOpen = ref(false)
const toStartOfDay = (value) => {
  const date = value ? new Date(value) : new Date()
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}
const rangeAnchorDate = ref(toStartOfDay(new Date()))
const selectedRange = ref('upcoming')
const customRange = ref({ start: '', end: '' })

const rangeOptions = [
  { value: 'upcoming', label: '예정/진행' },
  { value: 'today', label: '오늘' },
  { value: 'tomorrow', label: '내일' },
  { value: '7days', label: '7일' },
  { value: '30days', label: '30일' },
  { value: 'custom', label: '기간선택' }
]

const currentMonth = ref(new Date())

const statusFilters = [
  { value: 'all', label: '전체', statuses: [1, 2, 3, 9] },
  { value: 'pending', label: '요청', statuses: [1] },
  { value: 'confirmed', label: '확정', statuses: [2] },
  { value: 'checkin', label: '체크인 완료', statuses: [3] },
  { value: 'canceled', label: '취소', statuses: [9] }
]

const sortOptions = [
  { value: 'latest', label: '최신순' },
  { value: 'checkin', label: '체크인 임박순' },
  { value: 'checkout', label: '체크아웃 임박순' }
]

const selectedStatus = ref('all')
const selectedSort = ref('latest')
const bookingTotalCount = computed(() => {
  if (totalElements.value > 0) return totalElements.value
  return filteredBookings.value.length
})

const getLabel = (list, value, fallback = '-') => {
  const found = list.find((item) => item.value === value)
  return found?.label ?? fallback
}

const filterSummary = computed(() => {
  const rangeLabel = selectedRange.value === 'upcoming' ? '오늘~미래' : getLabel(rangeOptions, selectedRange.value)
  const scopeLabel = includePast.value ? '전체 일정' : '예정/진행'
  const statusLabel = getLabel(statusFilters, selectedStatus.value)
  const sortLabel = getLabel(sortOptions, selectedSort.value)
  return `${scopeLabel} · ${rangeLabel} · ${statusLabel} · ${sortLabel}`
})

const daysInMonth = computed(() => new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 0).getDate())
const firstDay = computed(() => new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), 1).getDay())

const toDate = (value) => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const formatSchedule = (booking, includeYear = false) => {
  const checkIn = formatDateTime(booking.checkIn, includeYear)
  const checkOut = formatDateTime(booking.checkOut, includeYear)
  let nights = booking.stayNights ?? 0
  if (!nights) {
    const start = toDate(booking.checkIn)
    const end = toDate(booking.checkOut)
    if (start && end) {
      const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      const endDate = new Date(end.getFullYear(), end.getMonth(), end.getDate())
      const diff = Math.round((endDate - startDate) / (1000 * 60 * 60 * 24))
      nights = Math.max(1, diff)
    }
  }
  return `${checkIn} → ${checkOut} · ${nights}박`
}

const formatScheduleRange = (booking, includeYear = false) => {
  return formatDateRange(booking.checkIn, booking.checkOut, includeYear)
}

const formatBookingMeta = (booking) => {
  let nights = booking.stayNights ?? 0
  if (!nights || nights <= 0) {
    const start = toDate(booking.checkIn)
    const end = toDate(booking.checkOut)
    if (start && end) {
      const startDate = new Date(start.getFullYear(), start.getMonth(), start.getDate())
      const endDate = new Date(end.getFullYear(), end.getMonth(), end.getDate())
      const diff = Math.round((endDate - startDate) / (1000 * 60 * 60 * 24))
      nights = Math.max(1, diff)
    } else {
      nights = 1
    }
  }
  return `${nights}박 · ${booking.guests}명`
}

const isSameDate = (a, b) => {
  if (!a || !b) return false
  return a.getFullYear() === b.getFullYear()
    && a.getMonth() === b.getMonth()
    && a.getDate() === b.getDate()
}

const toDateOnly = (value) => {
  const date = toDate(value)
  if (!date) return null
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

const todayDate = () => {
  return toStartOfDay(new Date())
}

const calendarCells = computed(() => {
  const cells = []
  for (let i = 0; i < firstDay.value; i++) cells.push({ empty: true })
  for (let d = 1; d <= daysInMonth.value; d++) {
    const dateObj = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), d)
    const dateStr = dateObj.toISOString().split('T')[0]
    const count = calendarBookings.value.filter((booking) => {
      if (booking.reservationStatus === 0) return false
      const checkIn = toDate(booking.checkIn)
      const checkOut = toDate(booking.checkOut)
      return checkIn && checkOut && dateObj >= checkIn && dateObj <= checkOut
    }).length
    cells.push({ empty: false, day: d, dateStr, count })
  }
  return cells
})

const selectedDateBookings = computed(() => {
  if (!selectedDate.value) return []
  const target = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), selectedDate.value)
  return calendarBookings.value.filter((booking) => {
    if (booking.reservationStatus === 0) return false
    const checkIn = toDate(booking.checkIn)
    const checkOut = toDate(booking.checkOut)
    return checkIn && checkOut && target >= checkIn && target <= checkOut
  })
})

const selectedDateLabel = computed(() => {
  if (!selectedDate.value) return ''
  const date = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth(), selectedDate.value)
  return formatDate(date, true)
})

const prevMonth = () => {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() - 1, 1)
  selectedDate.value = null
}
const nextMonth = () => {
  currentMonth.value = new Date(currentMonth.value.getFullYear(), currentMonth.value.getMonth() + 1, 1)
  selectedDate.value = null
}

const formatAmount = (n) => formatCurrency(n)

const openModal = (booking) => {
  selectedBooking.value = booking
  showModal.value = true
  loadRefundQuote(booking?.id)
}

const closeModal = () => {
  showModal.value = false
  refundQuoteLoading.value = false
  refundQuoteError.value = ''
  refundQuote.value = null
}

const loadRefundQuote = async (reservationId) => {
  if (!reservationId) return
  refundQuoteLoading.value = true
  refundQuoteError.value = ''
  const response = await fetchRefundQuote(reservationId)
  if (response.ok && response.data) {
    refundQuote.value = response.data
  } else {
    refundQuoteError.value = response?.data?.message || '환불 계산 불가'
  }
  refundQuoteLoading.value = false
}

const refundRateLabel = computed(() => {
  if (refundQuoteLoading.value) return '계산 중...'
  if (refundQuoteError.value) return '계산 불가'
  if (!refundQuote.value) return '-'
  const days = Number(refundQuote.value.daysBefore)
  const dayLabel = Number.isFinite(days) ? `D-${days}` : '-'
  return `${refundQuote.value.refundRate}% (${dayLabel})`
})

const refundAmountLabel = computed(() => {
  if (refundQuoteLoading.value) return '계산 중...'
  if (refundQuoteError.value) return '-'
  if (!refundQuote.value) return '-'
  return formatAmount(refundQuote.value.refundAmount)
})

const resetFilters = () => {
  selectedStatus.value = 'all'
  selectedSort.value = 'latest'
  includePast.value = false
  selectedRange.value = 'upcoming'
  customRange.value = { start: '', end: '' }
  rangeAnchorDate.value = todayDate()
}

const normalizeStatus = (status) => {
  const numeric = Number(status)
  if (numeric === 1) return '요청'
  if (numeric === 2) return '확정'
  if (numeric === 3) return '체크인 완료'
  if (numeric === 9) return '취소'
  const value = String(status ?? '').toLowerCase()
  if (value.includes('확정') || value === 'confirmed') return '확정'
  if (value.includes('요청') || value.includes('대기') || value === 'pending') return '요청'
  if (value.includes('체크인')) return '체크인 완료'
  if (value.includes('취소') || value === 'cancelled') return '취소'
  return '요청'
}

const normalizeNumber = (value) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

const resolveBookingStatus = (reservationStatus, refundStatus) => {
  const isRefunded = refundStatus === 0 || refundStatus === 1 || refundStatus === 2
  const effectiveStatus = isRefunded ? 9 : reservationStatus
  return {
    code: effectiveStatus,
    label: normalizeStatus(effectiveStatus)
  }
}

const getBookingBadges = (booking) => {
  const primaryLabel = booking.status
  const primaryClass = statusColor[booking.status]
  const isCanceled = booking.bookingStatusCode === 9 || booking.status === '취소'
  let secondaryLabel = ''
  let secondaryClass = ''

  if (!isCanceled) {
    secondaryLabel = paymentBadge[booking.paymentStatus] ?? '결제 대기'
    if (booking.paymentStatus === 1) secondaryClass = 'transaction-chip--paid'
    else if (booking.paymentStatus === 2) secondaryClass = 'transaction-chip--failed'
    else secondaryClass = 'transaction-chip--pending'
  } else if (booking.paymentStatus === 1 && booking.refundStatus !== null && booking.refundStatus !== undefined) {
    secondaryLabel = refundBadge[booking.refundStatus]
    secondaryClass = 'transaction-chip--refund'
  } else if (booking.paymentStatus === 0) {
    secondaryLabel = '결제 취소'
    secondaryClass = 'transaction-chip--warn'
  } else if (booking.paymentStatus === 2) {
    secondaryLabel = paymentBadge[booking.paymentStatus] ?? '결제 실패'
    secondaryClass = 'transaction-chip--failed'
  }

  return {
    primaryLabel,
    primaryClass,
    secondaryLabel,
    secondaryClass
  }
}

const normalizeBooking = (item) => {
  const reservationStatus = normalizeNumber(item.reservationStatus ?? item.status)
  const paymentStatus = normalizeNumber(item.paymentStatus ?? item.paymentStatusCode ?? item.payment)
  const refundStatus = normalizeNumber(item.refundStatus ?? item.refund_status)
  const bookingStatus = resolveBookingStatus(reservationStatus, refundStatus)
  return {
    id: item.bookingId ?? item.reservationId ?? item.id,
    reservationStatus,
    bookingStatusCode: bookingStatus.code,
    bookingStatusLabel: bookingStatus.label,
    paymentStatus,
    refundStatus,
    guestName: item.guestName ?? item.reserverName ?? item.name ?? '',
    guestPhone: item.guestPhone ?? item.reserverPhone ?? item.phone ?? '',
    guestEmail: item.guestEmail ?? item.email ?? '',
    property: item.accommodationName ?? item.property ?? '',
    checkIn: item.checkin ?? item.checkIn ?? '',
    checkOut: item.checkout ?? item.checkOut ?? '',
    guests: item.guestCount ?? item.guests ?? 0,
    stayNights: normalizeNumber(item.stayNights ?? item.stayNightsCount),
    amount: item.finalPaymentAmount ?? item.amount ?? item.totalAmount ?? 0,
    status: bookingStatus.label,
    createdAt: item.createdAt ?? item.created_at ?? ''
  }
}

const normalizeSortQuery = (value) => {
  const normalized = String(value ?? '').toLowerCase()
  if (normalized === 'checkinsoon' || normalized === 'checkin') return 'checkin'
  if (normalized === 'checkoutsoon' || normalized === 'checkout') return 'checkout'
  if (normalized === 'latest') return 'latest'
  return null
}

const normalizeStatusQuery = (value) => {
  const normalized = String(value ?? '').toLowerCase()
  if (!normalized) return null
  const matched = statusFilters.find((item) => item.value === normalized)
  if (matched) return matched.value
  if (normalized.includes('confirm')) return 'confirmed'
  if (normalized.includes('checkin')) return 'checkin'
  if (normalized.includes('cancel')) return 'canceled'
  if (normalized.includes('request') || normalized.includes('pending')) return 'pending'
  return null
}

const normalizeIncludePastQuery = (value) => {
  const normalized = String(value ?? '').toLowerCase()
  return normalized === '1' || normalized === 'true' || normalized === 'yes'
}

const normalizeModeQuery = (value) => {
  const normalized = String(value ?? '').toLowerCase()
  if (normalized === 'today') return 'today'
  return null
}

const normalizeRangeQuery = (value) => {
  const normalized = String(value ?? '').toLowerCase()
  if (normalized === 'upcoming' || normalized === 'today' || normalized === 'tomorrow') return normalized
  if (normalized === '7days' || normalized === '30days' || normalized === 'custom') return normalized
  return null
}

const parseDateQuery = (value) => {
  if (!value) return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : toStartOfDay(parsed)
}

const toDateParam = (date) => {
  if (!date) return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const addDays = (date, amount) => {
  const next = new Date(date)
  next.setDate(next.getDate() + amount)
  return toStartOfDay(next)
}

const resolveRangeDates = () => {
  const anchor = rangeAnchorDate.value
  if (selectedRange.value === 'today') {
    return { start: anchor, end: anchor }
  }
  if (selectedRange.value === 'tomorrow') {
    const target = addDays(anchor, 1)
    return { start: target, end: target }
  }
  if (selectedRange.value === '7days') {
    return { start: anchor, end: addDays(anchor, 6) }
  }
  if (selectedRange.value === '30days') {
    return { start: anchor, end: addDays(anchor, 29) }
  }
  if (selectedRange.value === 'custom') {
    const start = parseDateQuery(customRange.value.start)
    const end = parseDateQuery(customRange.value.end)
    return { start, end }
  }
  return { start: null, end: null }
}

const buildBookingParams = () => {
  if (selectedRange.value === 'upcoming') {
    if (!includePast.value) {
      return {
        upcomingOnly: true,
        startDate: toDateParam(rangeAnchorDate.value)
      }
    }
    return {}
  }

  const { start, end } = resolveRangeDates()
  if (!start || !end) {
    return null
  }
  return {
    startDate: toDateParam(start),
    endDate: toDateParam(end)
  }
}

const resolveServerSort = () => {
  if (selectedSort.value === 'latest') {
    if (selectedRange.value !== 'upcoming' || !includePast.value) return 'CHECKIN_ASC'
    return 'CREATED_DESC'
  }
  if (selectedSort.value === 'checkin') return 'CHECKIN_ASC'
  if (selectedSort.value === 'checkout') return 'CHECKIN_ASC'
  return 'CHECKIN_ASC'
}

const extractListPayload = (payload) => {
  if (Array.isArray(payload)) {
    return { items: payload, meta: null }
  }
  const items = payload?.items ?? payload?.content ?? payload?.data ?? []
  const meta = payload?.meta ?? null
  return { items, meta }
}

const resetPaging = () => {
  currentPage.value = 0
  totalPages.value = 0
  totalElements.value = 0
  cursorNext.value = null
  hasNextCursor.value = false
}

const filteredBookings = computed(() => {
  const base = bookings.value.filter((booking) => booking.bookingStatusCode !== 0)
  const filter = statusFilters.find((item) => item.value === selectedStatus.value)
  let filtered = filter ? base.filter((booking) => filter.statuses.includes(booking.bookingStatusCode)) : base

  const today = todayDate().getTime()
  if (selectedRange.value === 'upcoming' && !includePast.value) {
    filtered = filtered.filter((booking) => {
      const checkIn = toDateOnly(booking.checkIn)
      if (!checkIn) return true
      return checkIn.getTime() >= today
    })
  }
  return filtered
})

const calendarLegend = computed(() => '표시된 건수는 해당 날짜에 포함된 예약 수입니다.')

const loadBookings = async () => {
  if (!canUseHostFeatures.value) return
  const params = buildBookingParams()
  if (params === null) {
    bookings.value = []
    loadError.value = ''
    isLoading.value = false
    return
  }
  isLoading.value = true
  loadError.value = ''
  const requestParams = {
    ...params,
    sort: resolveServerSort(),
    rangeMode: 'OVERLAP'
  }
  if (isDesktop.value) {
    requestParams.page = currentPage.value
    requestParams.size = pageSize.value
  } else {
    requestParams.size = pageSize.value
  }

  const response = await fetchHostBookings(requestParams)
  if (response.ok) {
    const { items, meta } = extractListPayload(response.data)
    bookings.value = items.map(normalizeBooking)
    if (meta?.pageInfo) {
      totalPages.value = meta.pageInfo.totalPages ?? 0
      totalElements.value = meta.pageInfo.totalElements ?? 0
    }
    if (meta?.cursorInfo) {
      cursorNext.value = meta.cursorInfo.nextCursor ?? null
      hasNextCursor.value = Boolean(meta.cursorInfo.hasNext)
    } else {
      cursorNext.value = null
      hasNextCursor.value = false
    }
  } else {
    loadError.value = '예약 목록을 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const loadMoreBookings = async () => {
  if (!canUseHostFeatures.value || loadMoreLoading.value || !cursorNext.value) return
  const params = buildBookingParams()
  if (params === null) return
  loadMoreLoading.value = true
  loadError.value = ''
  const response = await fetchHostBookings({
    ...params,
    sort: resolveServerSort(),
    rangeMode: 'OVERLAP',
    size: pageSize.value,
    cursor: cursorNext.value
  })
  if (response.ok) {
    const { items, meta } = extractListPayload(response.data)
    bookings.value = [...bookings.value, ...items.map(normalizeBooking)]
    if (meta?.cursorInfo) {
      cursorNext.value = meta.cursorInfo.nextCursor ?? null
      hasNextCursor.value = Boolean(meta.cursorInfo.hasNext)
    }
  } else {
    loadError.value = '예약 목록을 불러오지 못했습니다.'
  }
  loadMoreLoading.value = false
}

const goToPage = (nextPage) => {
  const maxPage = Math.max(0, totalPages.value - 1)
  const target = Math.min(Math.max(0, nextPage), maxPage)
  if (target === currentPage.value) return
  currentPage.value = target
  loadBookings()
}

const loadCalendar = async (month) => {
  if (!canUseHostFeatures.value) return
  const response = await fetchHostBookingCalendar(month)
  if (response.ok) {
    const { items } = extractListPayload(response.data)
    calendarBookings.value = items.map(normalizeBooking)
  } else {
    loadError.value = '예약 캘린더를 불러오지 못했습니다.'
  }
}

const toMonthParam = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`

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
  if (typeof window !== 'undefined') {
    const media = window.matchMedia('(min-width: 768px)')
    const update = () => {
      isDesktop.value = media.matches
    }
    update()
    media.addEventListener?.('change', update)
    media.addListener?.(update)
  }
  if (selectedRange.value === 'upcoming' && !includePast.value) {
    selectedSort.value = 'checkin'
  }
  await loadHostState()
  if (canUseHostFeatures.value) {
    await loadBookings()
    await loadCalendar(toMonthParam(currentMonth.value))
  }
})

watch(currentMonth, (value) => {
  selectedDate.value = null
  if (canUseHostFeatures.value) {
    loadCalendar(toMonthParam(value))
  }
})

watch(
  () => route.query,
  (query) => {
    if (!query.view) {
      router.replace({ query: { ...query, view: 'list' } })
      return
    }
    const range = normalizeRangeQuery(query.range)
    if (range) {
      selectedRange.value = range
    }
    const sort = normalizeSortQuery(query.sort)
    if (sort) selectedSort.value = sort
    const type = normalizeStatusQuery(query.type)
    if (type) selectedStatus.value = type
    const status = normalizeStatusQuery(query.status)
    if (status) selectedStatus.value = status
    includePast.value = normalizeIncludePastQuery(query.includePast)

    const mode = normalizeModeQuery(query.mode)
    if (mode === 'today' || range === 'today') {
      selectedRange.value = 'today'
      const anchor = parseDateQuery(query.date) ?? todayDate()
      rangeAnchorDate.value = anchor
    } else if (!query.mode && !query.range && selectedRange.value === 'today') {
      selectedRange.value = 'upcoming'
      rangeAnchorDate.value = todayDate()
    } else if (range && ['tomorrow', '7days', '30days'].includes(range)) {
      rangeAnchorDate.value = parseDateQuery(query.date) ?? todayDate()
    }
  },
  { immediate: true }
)

const syncQuery = (next) => {
  const current = route.query
  const normalized = {
    ...current,
    ...next
  }
  const keys = Object.keys(normalized)
  const isSame = keys.every((key) => String(normalized[key] ?? '') === String(current[key] ?? ''))
  if (!isSame) {
    router.replace({ query: normalized })
  }
}

const syncRangeQuery = () => {
  const cleaned = { ...route.query }
  cleaned.range = selectedRange.value
  if (['today', 'tomorrow', '7days', '30days'].includes(selectedRange.value)) {
    cleaned.date = toDateParam(rangeAnchorDate.value)
  } else {
    delete cleaned.date
  }
  delete cleaned.mode
  router.replace({ query: cleaned })
}

watch(selectedStatus, (value) => {
  syncQuery({ status: value, type: value })
  resetPaging()
  loadBookings()
})

watch(selectedSort, (value) => {
  syncQuery({ sort: value })
  resetPaging()
  loadBookings()
})

watch(includePast, (value) => {
  syncQuery({ includePast: value ? 1 : 0 })
  resetPaging()
  if (!value && selectedSort.value === 'latest') {
    selectedSort.value = 'checkin'
  }
  loadBookings()
})

watch(selectedRange, () => {
  if (selectedRange.value !== 'custom') {
    customRange.value = { start: '', end: '' }
  }
  if (selectedRange.value !== 'upcoming' && selectedSort.value === 'latest') {
    selectedSort.value = 'checkin'
  }
  resetPaging()
  syncRangeQuery()
  loadBookings()
})

watch(rangeAnchorDate, () => {
  resetPaging()
  syncRangeQuery()
  loadBookings()
})

watch(
  () => ({ ...customRange.value }),
  () => {
    if (selectedRange.value === 'custom') {
      resetPaging()
      loadBookings()
    }
  }
)

watch(isDesktop, () => {
  resetPaging()
  loadBookings()
})


const setView = (view) => {
  if (view === activeTab.value) return
  router.replace({ query: { ...route.query, view } })
}
</script>

<template>
  <div class="booking-page">
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
        <h2 class="host-title">예약 관리</h2>
        <p class="host-subtitle">총 {{ bookingTotalCount }}건 · {{ sortOptions.find(item => item.value === selectedSort)?.label }}</p>
      </div>

      <div class="tab-switch host-view-header__actions" role="tablist" aria-label="예약 보기 전환">
        <button
          class="tab-btn host-chip"
          :class="{ 'host-chip--active': activeTab === 'list' }"
          role="tab"
          :aria-selected="activeTab === 'list'"
          :aria-pressed="activeTab === 'list'"
          type="button"
          @click="setView('list')"
        >
          목록
        </button>
        <button
          class="tab-btn host-chip"
          :class="{ 'host-chip--active': activeTab === 'calendar' }"
          role="tab"
          :aria-selected="activeTab === 'calendar'"
          :aria-pressed="activeTab === 'calendar'"
          type="button"
          @click="setView('calendar')"
        >
          캘린더
        </button>
      </div>
    </header>

    <section v-if="activeTab === 'list'" class="list-section">
      <div class="filter-summary">
        <span class="filter-summary__text">{{ filterSummary }}</span>
        <button class="ghost-btn filter-open" type="button" @click="isFilterSheetOpen = true">필터</button>
      </div>

      <div class="filters-desktop">
        <div class="range-row">
          <div class="filter-chips">
            <button
              v-for="option in rangeOptions"
              :key="option.value"
              class="filter-chip host-chip"
              :class="{ 'host-chip--active': selectedRange === option.value }"
              type="button"
              @click="selectedRange = option.value"
            >
              {{ option.label }}
            </button>
          </div>
          <div v-if="selectedRange === 'custom'" class="range-custom">
            <label>
              시작일
              <input v-model="customRange.start" type="date" />
            </label>
            <label>
              종료일
              <input v-model="customRange.end" type="date" />
            </label>
          </div>
        </div>

        <div class="filter-row">
          <div class="filter-chips">
            <button
              v-for="filter in statusFilters"
              :key="filter.value"
              class="filter-chip host-chip"
              :class="{ 'host-chip--active': selectedStatus === filter.value }"
              type="button"
              @click="selectedStatus = filter.value"
            >
              {{ filter.label }}
            </button>
          </div>
          <label class="past-toggle">
            <input v-model="includePast" type="checkbox" :disabled="selectedRange !== 'upcoming'" />
            <span>지난 일정 포함</span>
          </label>
          <select v-model="selectedSort" class="sort-select" aria-label="정렬 선택">
            <option v-for="option in sortOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
      </div>

      <div v-if="isFilterSheetOpen" class="filter-sheet" role="dialog" aria-modal="true">
        <button class="sheet-backdrop" type="button" aria-label="필터 닫기" @click="isFilterSheetOpen = false"></button>
        <div class="sheet-panel">
          <div class="sheet-head">
            <h3>필터</h3>
            <button class="ghost-btn sheet-close" type="button" @click="isFilterSheetOpen = false">닫기</button>
          </div>
          <div class="sheet-body">
            <div class="sheet-section">
              <p class="sheet-title">기간</p>
              <div class="filter-chips">
                <button
                  v-for="option in rangeOptions"
                  :key="option.value"
                  class="filter-chip host-chip"
                  :class="{ 'host-chip--active': selectedRange === option.value }"
                  type="button"
                  @click="selectedRange = option.value"
                >
                  {{ option.label }}
                </button>
              </div>
              <div v-if="selectedRange === 'custom'" class="range-custom">
                <label>
                  시작일
                  <input v-model="customRange.start" type="date" />
                </label>
                <label>
                  종료일
                  <input v-model="customRange.end" type="date" />
                </label>
              </div>
            </div>

            <div class="sheet-section">
              <p class="sheet-title">상태</p>
              <div class="filter-chips">
                <button
                  v-for="filter in statusFilters"
                  :key="filter.value"
                  class="filter-chip host-chip"
                  :class="{ 'host-chip--active': selectedStatus === filter.value }"
                  type="button"
                  @click="selectedStatus = filter.value"
                >
                  {{ filter.label }}
                </button>
              </div>
            </div>

            <div class="sheet-section sheet-row">
              <label class="past-toggle">
                <input v-model="includePast" type="checkbox" :disabled="selectedRange !== 'upcoming'" />
                <span>지난 일정 포함</span>
              </label>
              <select v-model="selectedSort" class="sort-select" aria-label="정렬 선택">
                <option v-for="option in sortOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loadError" class="empty-box">
        <p>데이터를 불러오지 못했어요.</p>
        <button class="ghost-btn" @click="loadBookings">다시 시도</button>
      </div>

      <div class="mobile-cards">
        <div v-if="!loadError && !filteredBookings.length && !isLoading" class="empty-box">
          <p>조건에 맞는 예약이 없습니다.</p>
          <button class="ghost-btn" @click="resetFilters">필터 초기화</button>
        </div>
        <ReservationCard
          v-for="(booking, index) in filteredBookings"
          :key="booking.id"
          :booking="booking"
          :primary-label="getBookingBadges(booking).primaryLabel"
          :primary-class="getBookingBadges(booking).primaryClass"
          :secondary-label="getBookingBadges(booking).secondaryLabel"
          :secondary-class="getBookingBadges(booking).secondaryClass"
          :schedule-range="formatScheduleRange(booking)"
          :schedule-meta="formatBookingMeta(booking)"
          :amount-label="formatAmount(booking.amount)"
          :animation-delay="`${Math.min(index, 5) * 70}ms`"
          @detail="openModal"
        />
        <button
          v-if="hasNextCursor && !isDesktop"
          class="ghost-btn load-more"
          type="button"
          :disabled="loadMoreLoading"
          @click="loadMoreBookings"
        >
          {{ loadMoreLoading ? '불러오는 중...' : '더보기' }}
        </button>
      </div>

      <div class="table-wrap">
        <table class="booking-table">
          <colgroup>
            <col style="width: 96px;" />
            <col style="width: 160px;" />
            <col style="width: 260px;" />
            <col style="width: 160px;" />
            <col style="width: 160px;" />
            <col style="width: 84px;" />
            <col style="width: 140px;" />
            <col style="width: 120px;" />
            <col style="width: 112px;" />
          </colgroup>
          <thead>
          <tr>
            <th>예약번호</th><th>게스트</th><th>숙소</th><th>체크인</th><th>체크아웃</th><th class="nowrap-cell">인원</th><th>금액</th><th class="status-col">상태</th><th></th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="booking in filteredBookings" :key="booking.id">
            <td>#{{ booking.id.toString().padStart(4, '0') }}</td>
            <td class="cell-ellipsis" :title="booking.guestName">{{ booking.guestName }}</td>
            <td class="cell-ellipsis" :title="booking.property">{{ booking.property }}</td>
            <td>{{ formatDateTime(booking.checkIn) }}</td>
            <td>{{ formatDateTime(booking.checkOut) }}</td>
            <td class="nowrap-cell cell-right">{{ booking.guests }}명</td>
            <td class="strong cell-right">{{ formatAmount(booking.amount) }}</td>
            <td class="status-col status-cell">
              <div class="status-stack">
                <span class="pill" :class="getBookingBadges(booking).primaryClass">{{ getBookingBadges(booking).primaryLabel }}</span>
                <span
                  v-if="getBookingBadges(booking).secondaryLabel"
                  class="transaction-chip"
                  :class="getBookingBadges(booking).secondaryClass"
                >
                  {{ getBookingBadges(booking).secondaryLabel }}
                </span>
              </div>
            </td>
            <td class="cell-right"><button class="ghost-btn detail-btn" aria-label="예약 상세" @click="openModal(booking)"><span class="detail-btn__text"></span></button></td>
          </tr>
          </tbody>
        </table>
        <div v-if="isDesktop && totalPages > 0" class="table-pagination">
          <button
            class="ghost-btn"
            type="button"
            :disabled="currentPage === 0"
            @click="goToPage(currentPage - 1)"
          >이전</button>
          <span class="muted">페이지 {{ currentPage + 1 }} / {{ totalPages }}</span>
          <button
            class="ghost-btn"
            type="button"
            :disabled="currentPage >= totalPages - 1"
            @click="goToPage(currentPage + 1)"
          >다음</button>
        </div>
      </div>
    </section>

    <section v-else class="calendar-section">
      <div class="calendar">
        <div class="cal-head">
          <h3>{{ currentMonth.getFullYear() }}년 {{ currentMonth.getMonth() + 1 }}월</h3>
          <div class="cal-nav">
            <button class="circle-btn" @click="prevMonth">‹</button>
            <button class="circle-btn" @click="nextMonth">›</button>
          </div>
        </div>

        <p class="calendar-legend">{{ calendarLegend }}</p>

        <div class="weekdays">
          <span v-for="day in ['일','월','화','수','목','금','토']" :key="day">{{ day }}</span>
        </div>

        <div class="grid">
          <div
            v-for="(cell, idx) in calendarCells"
            :key="idx"
            class="cell"
            :class="{
              empty: cell.empty,
              selected: !cell.empty && selectedDate === cell.day,
              'has-booking': !cell.empty && cell.count
            }"
            @click="!cell.empty && (selectedDate = cell.day)"
            @keydown.enter="!cell.empty && (selectedDate = cell.day)"
            @keydown.space.prevent="!cell.empty && (selectedDate = cell.day)"
            :role="cell.empty ? undefined : 'button'"
            :tabindex="cell.empty ? -1 : 0"
          >
            <span v-if="!cell.empty" class="day">{{ cell.day }}</span>
            <span v-if="!cell.empty && cell.count" class="count-chip">
              <span class="count-number">{{ cell.count }}</span>
              <span class="count-unit">건</span>
            </span>
          </div>
        </div>
      </div>

      <div class="date-panel">
        <h4>{{ selectedDate ? `${selectedDateLabel} 예약` : '날짜를 선택하세요' }}</h4>
        <div v-if="loadError" class="empty-box">
          <p>데이터를 불러오지 못했어요.</p>
          <button class="ghost-btn" @click="loadCalendar(toMonthParam(currentMonth))">다시 시도</button>
        </div>
        <div v-else-if="selectedDate && selectedDateBookings.length" class="date-list">
          <ReservationCard
            v-for="(booking, index) in selectedDateBookings"
            :key="booking.id"
            :booking="booking"
            :primary-label="getBookingBadges(booking).primaryLabel"
            :primary-class="getBookingBadges(booking).primaryClass"
            :secondary-label="getBookingBadges(booking).secondaryLabel"
            :secondary-class="getBookingBadges(booking).secondaryClass"
            :schedule-range="formatSchedule(booking)"
            :schedule-meta="formatBookingMeta(booking)"
            :amount-label="formatAmount(booking.amount)"
            :animation-delay="`${Math.min(index, 5) * 60}ms`"
            @detail="openModal"
          />
        </div>

        <div v-else class="empty-box">
          <p>{{ selectedDate ? '선택한 날짜에 예약이 없습니다.' : '날짜를 선택하세요.' }}</p>
          <button v-if="selectedDate" class="ghost-btn" @click="selectedDate = new Date().getDate()">오늘 보기</button>
        </div>
      </div>
    </section>

    <p v-if="isLoading" class="empty-box">예약 데이터를 불러오는 중입니다.</p>

    <div v-if="showModal && selectedBooking" class="modal-backdrop" @click.self="closeModal">
      <div class="modal">
        <header class="modal-head">
          <div>
            <p class="muted">예약 #{{ selectedBooking.id.toString().padStart(4, '0') }}</p>
            <h3>{{ selectedBooking.guestName }}</h3>
          </div>
          <button class="circle-btn" @click="closeModal">×</button>
        </header>

        <div class="modal-body">
          <div class="modal-row"><span>숙소</span><strong>{{ selectedBooking.property }}</strong></div>
          <div class="modal-row"><span>체크인</span><strong>{{ formatDateTime(selectedBooking.checkIn, true) }}</strong></div>
          <div class="modal-row"><span>체크아웃</span><strong>{{ formatDateTime(selectedBooking.checkOut, true) }}</strong></div>
          <div class="modal-row"><span>인원</span><strong>{{ selectedBooking.guests }}명</strong></div>
          <div class="modal-row"><span>연락처</span><strong>{{ selectedBooking.guestPhone }}</strong></div>
          <div class="modal-row"><span>이메일</span><strong>{{ selectedBooking.guestEmail }}</strong></div>
          <div class="modal-row"><span>금액</span><strong>{{ formatAmount(selectedBooking.amount) }}</strong></div>
          <div class="modal-row"><span>환불율</span><strong>{{ refundRateLabel }}</strong></div>
          <div class="modal-row"><span>예상 환불액</span><strong>{{ refundAmountLabel }}</strong></div>
          <div class="modal-row"><span>상태</span><span class="pill" :class="statusColor[selectedBooking.status]">{{ selectedBooking.status }}</span></div>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>

<style scoped>
.booking-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-bottom: 2rem;
}

.view-header {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.75rem;
  margin-bottom: 0.25rem;
}

.view-header h2 {
  margin: 0.15rem 0 0.2rem;
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--brand-accent);
  letter-spacing: -0.01em;
}

.subtitle {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-weight: 600;
}

.range-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.filter-summary {
  display: none;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.65rem 0.75rem;
  background: #ffffff;
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 12px;
  position: sticky;
  top: 0;
  z-index: 5;
  min-height: 52px;
}

.filter-summary__text {
  font-weight: 800;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.filter-open {
  width: auto;
  min-width: 72px;
  padding: 0.45rem 0.7rem;
  white-space: nowrap;
}

.filters-desktop {
  display: block;
}

.range-custom {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  align-items: center;
  font-weight: 700;
  color: #475569;
}

.range-custom input {
  border: 1px solid var(--brand-border);
  border-radius: 10px;
  padding: 0.4rem 0.6rem;
  min-height: 40px;
  font-weight: 700;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.75rem;
}

.filter-chips {
  display: flex;
  gap: 0.5rem;
  overflow-x: auto;
}

.filter-chip {
  font-weight: 800;
  border-radius: 999px;
  padding: 0.4rem 0.85rem;
  font-size: 0.9rem;
  min-height: 44px;
  white-space: nowrap;
}

.sort-select {
  border: 1px solid var(--brand-border);
  border-radius: 12px;
  padding: 0.5rem 0.8rem;
  font-weight: 700;
  min-height: 44px;
  background: #ffffff;
  color: #111827;
}

.past-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 800;
  color: #334155;
  background: #fff;
  border: 1px solid var(--brand-border);
  border-radius: 999px;
  padding: 0.4rem 0.8rem;
  min-height: 44px;
  cursor: pointer;
  white-space: nowrap;
}

.past-toggle input {
  accent-color: var(--brand-primary-strong, #0f766e);
}

.past-toggle input:disabled {
  cursor: not-allowed;
}

.past-toggle input:disabled + span {
  opacity: 0.6;
}

.tab-switch {
  display: inline-flex;
  width: 100%;
  background: var(--surface);
  border: 1px solid var(--brand-border);
  border-radius: 999px;
  padding: 0.2rem;
  white-space: nowrap;
}

.tab-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 0.6rem 0.9rem;
  border-radius: 999px;
  font-weight: 800;
  color: #334155;
  min-height: 44px;
}

.tab-btn.host-chip--active {
  background: var(--brand-primary, #BFE7DF);
  color: #0f172a;
  border: 1px solid var(--brand-primary-strong, #0f766e);
}

.tab-btn.host-chip--active:hover,
.tab-btn.host-chip--active:focus-visible {
  background: var(--brand-primary-strong, #0f766e);
  color: #0f172a;
}

.mobile-cards {
  display: grid;
  gap: 0.75rem;
}

.mobile-card {
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 14px;
  padding: 1rem;
  background: var(--bg-white, #fff);
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-height: 44px;
}

.muted {
  color: #9ca3af;
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
}

.mobile-card h3 {
  margin: 0.1rem 0 0;
  font-size: 1.12rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.property {
  margin: 0.45rem 0 0.15rem;
  color: #374151;
  font-weight: 900;
}

.payment-chip {
  display: inline-flex;
  align-items: center;
  margin: 0.35rem 0 0.2rem;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
  font-size: 0.75rem;
  font-weight: 800;
  width: fit-content;
}

.card-tags {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
  min-height: 26px;
}

.refund-chip {
  display: inline-flex;
  align-items: center;
  margin: 0.35rem 0 0.2rem;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 0.75rem;
  font-weight: 800;
  width: fit-content;
}

.period {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-size: 0.95rem;
  font-weight: 600;
}

.period-meta {
  margin: 0.2rem 0 0;
  color: var(--text-sub, #6b7280);
  font-size: 0.9rem;
  font-weight: 700;
}

.amount {
  margin: 0.45rem 0;
  font-weight: 900;
  color: var(--text-main, #0f172a);
  font-size: 1.05rem;
}

.ghost-btn {
  width: 100%;
  border: 1px solid var(--brand-border);
  background: transparent;
  color: var(--brand-accent);
  border-radius: 10px;
  padding: 0.6rem;
  font-weight: 900;
  min-height: 44px;
  cursor: pointer;
}

.load-more {
  margin-top: 0.75rem;
}

.nowrap-cell {
  white-space: nowrap;
  word-break: keep-all;
}

.detail-btn {
  min-width: 72px;
  width: auto;
  white-space: nowrap;
  padding: 0.55rem 0.75rem;
}

.detail-btn__text {
  display: inline-block;
}

.detail-btn__text::after {
  content: '예약 상세';
}

@media (max-width: 767px) {
  .detail-btn__text::after {
    content: '상세';
  }
}

.fade-item {
  animation: fadeUp 240ms ease both;
}

.table-wrap {
  display: none;
  background: var(--bg-white, #fff);
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 14px;
  overflow-x: auto;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--border, #e5e7eb);
}

.table-pagination .ghost-btn {
  width: auto;
  min-height: 36px;
  padding: 0.35rem 0.8rem;
  font-size: 0.85rem;
}

.booking-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 820px;
  table-layout: fixed;
}

.booking-table th,
.booking-table td {
  padding: 0.85rem 1rem;
  text-align: left;
}

.booking-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 900;
  font-size: 0.95rem;
}

.booking-table tbody tr { border-top: 1px solid var(--border, #e5e7eb); }
.booking-table td { color: #111827; }
.strong { font-weight: 900; }

.cell-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-right {
  text-align: right;
}

.booking-table th.status-col,
.booking-table td.status-col {
  text-align: center;
  vertical-align: middle;
  padding-left: 0;
  padding-right: 0;
}

.status-stack {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
  margin: 0 auto;
}

.transaction-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  font-size: 0.75rem;
  font-weight: 800;
  white-space: nowrap;
}

.transaction-chip--paid {
  border-color: #bbf7d0;
  background: #ecfdf3;
  color: #166534;
}

.transaction-chip--pending {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #475569;
}

.transaction-chip--failed {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.transaction-chip--warn {
  border-color: #fde68a;
  background: #fffbeb;
  color: #b45309;
}

.transaction-chip--refund {
  border-color: #dbeafe;
  background: #eff6ff;
  color: #1d4ed8;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 900;
  border: 1px solid var(--border, #e5e7eb);
  white-space: nowrap;
}

.badge-green {
  background: var(--brand-primary);
  color: var(--brand-accent);
  border-color: var(--brand-primary-strong);
}

.badge-gray {
  background: #f1f5f9;
  color: #475569;
}

.badge-outline {
  background: white;
  color: #111827;
}

.badge-red {
  background: #fef2f2;
  color: #b91c1c;
}

.calendar-section {
  display: grid;
  gap: 1rem;
}

.calendar,
.date-panel {
  background: var(--bg-white, #fff);
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 14px;
  padding: 1rem;
  box-shadow: var(--shadow-md, 0 4px 14px rgba(0, 0, 0, 0.04));
}

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.cal-head h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.cal-nav { display: flex; gap: 0.35rem; }

.circle-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid var(--border, #e5e7eb);
  background: white;
  cursor: pointer;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  color: var(--text-sub, #6b7280);
  font-weight: 900;
  margin-bottom: 0.35rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 0.35rem;
}

.cell {
  min-height: 68px;
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 10px;
  padding: 0.4rem 0.4rem 1.2rem;
  position: relative;
  background: #f9fafb;
  cursor: pointer;
}

.cell.empty { background: transparent; border: none; cursor: default; }

.cell.selected {
  border-color: var(--brand-600);
  background: var(--brand-primary);
}

.cell.has-booking {
  border: 2px solid var(--brand-primary-strong, #0f766e);
  box-shadow: inset 0 0 0 1px var(--brand-primary-strong, #0f766e);
  background: var(--brand-200);
}

.cell.selected.has-booking {
  background: var(--brand-primary);
}

.cell.has-booking:hover,
.cell.has-booking:focus-visible {
  border-color: var(--brand-primary-strong, #0f766e);
  background: var(--brand-primary);
}

.cell:focus-visible {
  outline: 2px solid var(--brand-primary-strong, #0f766e);
  outline-offset: 2px;
}

.day { font-weight: 900; color: #111827; }

.calendar-legend {
  margin: 0.3rem 0 0.8rem;
  font-size: 0.85rem;
  color: var(--text-sub, #6b7280);
}

.count-chip {
  position: absolute;
  right: 0.35rem;
  bottom: 0.3rem;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
  line-height: 1;
  border: 1px solid var(--brand-primary-strong, #0f766e);
  color: var(--brand-accent);
  background: #fff;
  border-radius: 999px;
  padding: 0.1rem 0.3rem;
  font-size: 0.68rem;
  min-width: 30px;
  justify-content: center;
  font-weight: 800;
}

.count-number {
  font-variant-numeric: tabular-nums;
}

@media (max-width: 420px) {
  .count-unit {
    display: none;
  }

  .count-chip {
    min-width: 22px;
    padding: 0.08rem 0.25rem;
  }
}

.date-panel h4 {
  margin: 0 0 0.75rem;
  font-size: 1rem;
  font-weight: 900;
  color: var(--text-main, #0f172a);
}

.date-list {
  display: grid;
  gap: 0.65rem;
}

.date-card {
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 12px;
  padding: 0.85rem;
  background: #f9fafb;
  cursor: pointer;
}

.date-card:hover { border-color: var(--brand-primary-strong); }

.date-card h5 {
  margin: 0.1rem 0 0;
  font-size: 1rem;
  font-weight: 900;
}

.empty-box {
  text-align: center;
  color: var(--text-sub, #6b7280);
  padding: 1rem 0;
  font-weight: 700;
  display: grid;
  gap: 0.5rem;
  justify-items: center;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  z-index: 50;
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 1.25rem;
  width: min(560px, 100%);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-head h3 { margin: 0.2rem 0 0; font-weight: 900; }

.modal-body {
  margin-top: 1rem;
  display: grid;
  gap: 0.65rem;
}

.modal-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.modal-row span:first-child {
  color: var(--text-sub, #6b7280);
  font-weight: 700;
}

.filter-sheet {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  align-items: end;
}

.sheet-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  border: none;
}

.sheet-panel {
  position: relative;
  background: #ffffff;
  border-radius: 18px 18px 0 0;
  padding: 1rem;
  box-shadow: 0 -12px 32px rgba(0, 0, 0, 0.18);
  display: grid;
  gap: 1rem;
  max-height: 85vh;
  overflow: auto;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.sheet-head h3 {
  margin: 0;
  font-weight: 900;
  font-size: 1.1rem;
}

.sheet-close {
  width: auto;
  padding: 0.45rem 0.75rem;
}

.sheet-body {
  display: grid;
  gap: 1rem;
}

.sheet-section {
  display: grid;
  gap: 0.6rem;
}

.sheet-title {
  margin: 0;
  font-weight: 800;
  color: #334155;
}

.sheet-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

@media (min-width: 768px) {
  .view-header {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
  .tab-switch { width: auto; }
  .mobile-cards { display: none; }
  .table-wrap { display: block; }
  .filter-summary { display: none; }
  .filters-desktop { display: block; }
}

@media (max-width: 767px) {
  .filters-desktop {
    display: none;
  }
  .filter-summary {
    display: flex;
  }
  .filter-sheet .filter-chips {
    flex-wrap: wrap;
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

@media (prefers-reduced-motion: reduce) {
  .fade-item {
    animation: none !important;
  }
}

@media (min-width: 1024px) {
  .calendar-section {
    grid-template-columns: 2fr 1fr;
    align-items: start;
  }
}
</style>
