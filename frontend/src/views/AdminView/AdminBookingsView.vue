<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { exportCSV, exportXLSX } from '../../utils/reportExport'
import { fetchAdminBookings, fetchAdminBookingDetail, fetchRefundQuote, refundBooking } from '../../api/adminApi'
import { extractItems, extractPageMeta, toQueryParams } from '../../utils/adminData'
import {
  RESERVATION_STATUS_OPTIONS,
  getReservationStatusLabel,
  getReservationStatusVariant,
  getPaymentStatusLabel,
  getPaymentStatusVariant
} from '../../constants/adminBookingStatus'
import {
  canChangeReservation,
  changeBlockReason,
  canRefundReservation,
  refundBlockReason
} from '../../constants/adminReservationPolicy'

const bookingList = ref([])
const searchQuery = ref('')
const statusFilter = ref('all')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)
const isLoading = ref(false)
const loadError = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailData = ref(null)
const detailId = ref(null)
const refundQuoteLoading = ref(false)
const refundQuoteError = ref('')
const refundQuote = ref(null)
const route = useRoute()
const router = useRouter()

// 환불 모달 상태
const refundModal = ref({
  open: false,
  booking: null,
  amount: 0,
  reason: '',
  memo: '',
  isManual: false, // 수동 입력 여부
  loading: false,
  error: ''
})

const loadBookings = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminBookings({
    status: statusFilter.value,
    sort: statusFilter.value === 'checkin' ? 'checkin' : 'latest',
    page: page.value,
    size: size.value
  })
  if (response.ok && response.data) {
    const payload = response.data
    bookingList.value = extractItems(payload)
    const meta = extractPageMeta(payload)
    page.value = meta.page
    size.value = meta.size
    totalPages.value = meta.totalPages
    totalElements.value = meta.totalElements
  } else {
    loadError.value = '예약 목록을 불러오지 못했습니다.'
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

const normalizeStatusFilter = (value) => {
  if (!value) return 'all'
  if (value === 'pending') return 'requested'
  return value
}

onMounted(() => {
  statusFilter.value = normalizeStatusFilter(route.query.status) ?? 'all'
  searchQuery.value = route.query.keyword ?? ''
  page.value = Number(route.query.page ?? 0)
  loadBookings()
})

const mapStatusFilter = (filter) => {
  if (filter === 'confirmed') return '2'
  if (filter === 'pending' || filter === 'requested') return '1'
  if (filter === 'checkedin') return '3'
  if (filter === 'canceled') return '9'
  return filter
}

const filteredBookings = computed(() => {
  const query = searchQuery.value.trim().toLowerCase()
  return bookingList.value.filter((item) => {
    const matchesQuery = !query ||
      String(item.reservationId ?? '').includes(query) ||
      String(item.accommodationsId ?? '').includes(query) ||
      String(item.userId ?? '').includes(query)
    const matchesStatus = statusFilter.value === 'all' || String(item.reservationStatus) === mapStatusFilter(statusFilter.value)
    return matchesQuery && matchesStatus
  })
})

watch([searchQuery, statusFilter], () => {
  page.value = 0
  syncQuery({ status: statusFilter.value, keyword: searchQuery.value || undefined, page: page.value })
  loadBookings()
})


const downloadReport = (format) => {
  const today = new Date().toISOString().slice(0, 10)
  const sheets = [
    {
      name: '예약 목록',
      columns: [
        { key: 'id', label: '예약번호' },
        { key: 'accommodation', label: '숙소' },
        { key: 'host', label: '호스트' },
        { key: 'guest', label: '게스트' },
        { key: 'checkIn', label: '체크인' },
        { key: 'checkOut', label: '체크아웃' },
        { key: 'people', label: '인원' },
        { key: 'price', label: '금액' },
        { key: 'status', label: '상태' },
        { key: 'paymentStatus', label: '결제' },
        { key: 'channel', label: '채널' },
        { key: 'createdAt', label: '등록일' }
      ],
      rows: filteredBookings.value
    }
  ]

  if (format === 'xlsx') {
    exportXLSX({ filename: `admin-bookings-${today}.xlsx`, sheets })
    return
  }
  exportCSV({ filename: `admin-bookings-${today}.csv`, sheets })
}

const openDetail = async (item) => {
  if (!item?.reservationId) return
  detailId.value = item.reservationId
  detailOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  refundQuoteLoading.value = true
  refundQuoteError.value = ''
  refundQuote.value = null
  const response = await fetchAdminBookingDetail(item.reservationId)
  if (response.ok && response.data) {
    detailData.value = response.data
  } else {
    detailError.value = '예약 상세 정보를 불러오지 못했습니다.'
  }
  detailLoading.value = false
  await loadRefundQuote(item.reservationId)
}

const closeDetail = () => {
  detailOpen.value = false
  detailError.value = ''
  detailData.value = null
  detailId.value = null
  refundQuoteLoading.value = false
  refundQuoteError.value = ''
  refundQuote.value = null
}

const retryDetail = async () => {
  if (!detailId.value) return
  await openDetail({ reservationId: detailId.value })
}

const loadRefundQuote = async (reservationId) => {
  if (!reservationId) return
  refundQuoteLoading.value = true
  refundQuoteError.value = ''
  const response = await fetchRefundQuote(reservationId)
  if (response.ok && response.data) {
    refundQuote.value = response.data
  } else {
    // refundQuoteError.value = response?.data?.message || '환불 계산 불가'
    // API가 없거나 실패해도 프론트 로직으로 계산 가능하도록 처리
    refundQuote.value = null
  }
  refundQuoteLoading.value = false
}

// 환불 정책 계산 유틸리티
const calculateRefundAmount = (paymentAmount, checkInDate) => {
  if (!paymentAmount || !checkInDate) return { amount: 0, rate: 0 }

  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const checkIn = new Date(checkInDate)
  checkIn.setHours(0, 0, 0, 0)

  const diffTime = checkIn.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  let rate = 0
  if (diffDays >= 7) rate = 1.0
  else if (diffDays >= 5) rate = 0.7
  else if (diffDays >= 3) rate = 0.5
  else rate = 0

  return {
    amount: Math.floor(paymentAmount * rate),
    rate: rate * 100
  }
}

const openRefundModal = (booking) => {
  if (!booking) return

  // 결제 완료 상태 확인
  if (booking.paymentStatus !== 1) { // 1: PAID
    alert('결제 완료된 건만 환불 가능합니다.')
    return
  }

  const { amount, rate } = calculateRefundAmount(booking.finalPaymentAmount, booking.checkin)

  refundModal.value = {
    open: true,
    booking: booking,
    amount: amount,
    rate: rate,
    reason: '고객 변심',
    memo: '',
    isManual: false,
    loading: false,
    error: ''
  }
}

const closeRefundModal = () => {
  refundModal.value.open = false
  refundModal.value.booking = null
}

const handleRefundSubmit = async () => {
  const { booking, amount, reason, memo } = refundModal.value
  if (!booking) return

  refundModal.value.loading = true
  refundModal.value.error = ''

  console.log('Refund Request:', {
    reservationId: booking.reservationId,
    refundAmount: amount,
    reason: reason,
    memo: memo
  })

  const response = await refundBooking(booking.reservationId, {
    refundAmount: amount,
    reason: `${reason} / ${memo}`
  })

  if (response.ok) {
    alert('환불 처리가 완료되었습니다.')
    closeRefundModal()
    closeDetail()
    await loadBookings()
  } else {
    refundModal.value.error = response.data?.message || '환불 처리에 실패했습니다.'
  }
  refundModal.value.loading = false
}

const refundRateLabel = computed(() => {
  if (refundQuote.value) {
    const days = Number(refundQuote.value.daysBefore)
    const dayLabel = Number.isFinite(days) ? `D-${days}` : '-'
    return `${refundQuote.value.refundRate}% (${dayLabel})`
  }
  // 프론트 계산 로직 사용
  if (detailData.value) {
    const { rate } = calculateRefundAmount(detailData.value.finalPaymentAmount, detailData.value.checkin)
    return `${rate}% (예상)`
  }
  return '-'
})

const refundAmountLabel = computed(() => {
  if (refundQuote.value) {
    return formatCurrency(refundQuote.value.refundAmount)
  }
  // 프론트 계산 로직 사용
  if (detailData.value) {
    const { amount } = calculateRefundAmount(detailData.value.finalPaymentAmount, detailData.value.checkin)
    return formatCurrency(amount)
  }
  return '-'
})

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '-'
  return `₩${Number(value).toLocaleString()}`
}

const formatDate = (value) => {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

const effectiveReservationLabel = (reservationStatus, paymentStatus) => {
  if (Number(paymentStatus) === 3) return '환불 완료'
  return getReservationStatusLabel(reservationStatus)
}

const effectiveReservationVariant = (reservationStatus, paymentStatus) => {
  if (Number(paymentStatus) === 3) return 'warning'
  return getReservationStatusVariant(reservationStatus)
}

const canChange = (item) => canChangeReservation(item ?? {})
const canRefund = (item) => {
  // 결제 완료(1) 상태일 때만 환불 가능
  return item?.paymentStatus === 1
}
const changeReason = (item) => changeBlockReason(item ?? {})
const refundReason = (item) => {
  if (item?.paymentStatus !== 1) return '결제 완료 건만 환불 가능'
  return ''
}

const getRefundStatusLabel = (status) => {
  const value = Number(status)
  if (!Number.isFinite(value)) return '-'
  if (value === 0) return '요청'
  if (value === 1) return '완료'
  if (value === 2) return '실패'
  return `알 수 없음(${status})`
}
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">예약 관리</h1>
        <p class="admin-subtitle">예약 상태와 체크인 현황을 확인합니다.</p>
      </div>
    </header>

    <div class="admin-filter-bar">
      <div class="admin-filter-group">
        <span class="admin-chip">검색</span>
        <input
          v-model="searchQuery"
          class="admin-input"
          type="search"
          placeholder="예약번호, 숙소 ID, 게스트 ID"
        />
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">필터</span>
        <select v-model="statusFilter" class="admin-select">
          <option v-for="option in RESERVATION_STATUS_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>
      <div class="admin-filter-group">
        <span class="admin-chip">작업</span>
        <button class="admin-btn admin-btn--primary" type="button">체크인 확인</button>
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

    <AdminTableCard title="예약 목록">
      <div class="booking-cards">
        <article v-for="item in filteredBookings" :key="item.reservationId" class="booking-card">
          <div class="booking-card__row booking-card__row--top">
            <span class="booking-card__id">#{{ item.reservationId }}</span>
            <AdminBadge
              :text="effectiveReservationLabel(item.reservationStatus, item.paymentStatus)"
              :variant="effectiveReservationVariant(item.reservationStatus, item.paymentStatus)"
            />
          </div>
          <div class="booking-card__row">
            <span class="booking-card__title">숙소 #{{ item.accommodationsId ?? '-' }}</span>
            <span class="booking-card__sub">게스트 #{{ item.userId ?? '-' }}</span>
          </div>
          <div class="booking-card__row booking-card__row--dates">
            <span>{{ formatDate(item.checkin) }}</span>
            <span>~</span>
            <span>{{ formatDate(item.checkout) }}</span>
          </div>
          <div class="booking-card__row booking-card__row--bottom">
            <span class="booking-card__amount">{{ formatCurrency(item.finalPaymentAmount) }}</span>
            <AdminBadge
              :text="getPaymentStatusLabel(item.paymentStatus)"
              :variant="getPaymentStatusVariant(item.paymentStatus)"
            />
            <button class="admin-btn admin-btn--ghost booking-card__action" type="button" @click="openDetail(item)">상세</button>
          </div>
        </article>
      </div>

      <table class="admin-table--nowrap admin-table--tight admin-table--stretch booking-table">
        <colgroup>
          <col style="width:110px"/>
          <col style="width:120px"/>
          <col style="width:120px"/>
          <col style="width:110px"/>
          <col style="width:110px"/>
          <col style="width:70px"/>
          <col style="width:120px"/>
          <col style="width:110px"/>
          <col style="width:110px"/>
          <col style="width:110px"/>
          <col style="width:190px"/>
        </colgroup>
        <thead>
          <tr>
            <th class="admin-align-center">예약번호</th>
            <th class="admin-align-center">숙소</th>
            <th class="admin-align-center">게스트</th>
            <th class="admin-align-center">체크인</th>
            <th class="admin-align-center">체크아웃</th>
            <th class="admin-align-center">인원</th>
            <th class="admin-align-right">금액</th>
            <th class="admin-align-center">상태</th>
            <th class="admin-align-center">결제</th>
            <th class="admin-align-center">등록일</th>
            <th class="admin-align-center">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredBookings" :key="item.reservationId">
            <td class="admin-strong admin-align-center">#{{ item.reservationId }}</td>
            <td class="admin-ellipsis admin-align-center" :title="item.accommodationsId">{{ item.accommodationsId }}</td>
            <td class="admin-ellipsis admin-align-center" :title="item.userId">{{ item.userId }}</td>
            <td class="admin-align-center">{{ formatDate(item.checkin) }}</td>
            <td class="admin-align-center">{{ formatDate(item.checkout) }}</td>
            <td class="admin-nowrap admin-align-center">{{ item.guestCount ?? '-' }}명</td>
            <td class="admin-align-right">{{ formatCurrency(item.finalPaymentAmount) }}</td>
            <td class="admin-align-center">
              <AdminBadge
                :text="effectiveReservationLabel(item.reservationStatus, item.paymentStatus)"
                :variant="effectiveReservationVariant(item.reservationStatus, item.paymentStatus)"
              />
            </td>
            <td class="admin-align-center">
              <AdminBadge
                :text="getPaymentStatusLabel(item.paymentStatus)"
                :variant="getPaymentStatusVariant(item.paymentStatus)"
              />
            </td>
            <td class="admin-align-center">{{ formatDate(item.createdAt) }}</td>
            <td class="admin-align-center">
              <div class="admin-inline-actions admin-inline-actions--nowrap">
                <button class="admin-btn admin-btn--ghost" type="button" @click="openDetail(item)">상세</button>
                <button
                  class="admin-btn admin-btn--muted"
                  type="button"
                  :disabled="!canChange(item)"
                  :title="changeReason(item)"
                >
                  변경
                </button>
                <button
                  class="admin-btn admin-btn--danger"
                  type="button"
                  :disabled="!canRefund(item)"
                  :title="refundReason(item)"
                  @click="openRefundModal(item)"
                >
                  환불
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadBookings">다시 시도</button>
      </div>
      <div v-else-if="!filteredBookings.length" class="admin-status">조건에 맞는 예약이 없습니다.</div>
      <div class="admin-pagination">
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page <= 0" @click="page = page - 1; loadBookings()">
          이전
        </button>
        <span>{{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="page + 1 >= totalPages" @click="page = page + 1; loadBookings()">
          다음
        </button>
      </div>
    </AdminTableCard>

    <!-- 상세 모달 -->
    <div v-if="detailOpen" class="admin-modal">
      <div class="admin-modal__backdrop" @click="closeDetail" />
      <div class="admin-modal__panel" role="dialog" aria-modal="true">
        <div class="admin-modal__header">
          <div>
            <p class="admin-modal__eyebrow">예약 상세</p>
            <h2 class="admin-modal__title">#{{ detailData?.reservationId ?? '-' }}</h2>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" @click="closeDetail">닫기</button>
        </div>

        <div v-if="detailLoading" class="admin-status">불러오는 중...</div>
        <div v-else-if="detailError" class="admin-status">
          <span>{{ detailError }}</span>
          <button class="admin-btn admin-btn--ghost" type="button" @click="retryDetail">다시 시도</button>
        </div>
        <div v-else class="admin-modal__body">
          <div class="admin-modal__section">
            <h3>예약 정보</h3>
            <div class="admin-detail-grid">
              <div><span>숙소</span><strong>{{ detailData?.accommodationName ?? '-' }} (#{{ detailData?.accommodationsId ?? '-' }})</strong></div>
              <div><span>호스트 ID</span><strong>{{ detailData?.hostUserId ?? '-' }}</strong></div>
              <div><span>게스트</span><strong>#{{ detailData?.userId ?? '-' }} {{ detailData?.guestEmail ?? '' }}</strong></div>
              <div><span>게스트 연락처</span><strong>{{ detailData?.guestPhone ?? '-' }}</strong></div>
              <div><span>체크인</span><strong>{{ formatDate(detailData?.checkin) }}</strong></div>
              <div><span>체크아웃</span><strong>{{ formatDate(detailData?.checkout) }}</strong></div>
              <div><span>인원</span><strong>{{ detailData?.guestCount ?? '-' }}명</strong></div>
              <div><span>금액</span><strong>{{ formatCurrency(detailData?.finalPaymentAmount) }}</strong></div>
              <div><span>예약 상태</span><strong>{{ effectiveReservationLabel(detailData?.reservationStatus, detailData?.paymentStatus) }}</strong></div>
              <div><span>결제 상태</span><strong>{{ getPaymentStatusLabel(detailData?.paymentStatus) }}</strong></div>
              <div><span>결제 수단</span><strong>{{ detailData?.paymentMethod ?? '-' }}</strong></div>
              <div><span>결제 금액</span><strong>{{ formatCurrency(detailData?.approvedAmount ?? detailData?.finalPaymentAmount) }}</strong></div>
              <div><span>결제일시</span><strong>{{ formatDate(detailData?.approvedAt) }}</strong></div>
              <div><span>환불율</span><strong>{{ refundRateLabel }}</strong></div>
              <div><span>예상 환불액</span><strong>{{ refundAmountLabel }}</strong></div>
              <div><span>주문번호</span><strong>{{ detailData?.orderId ?? '-' }}</strong></div>
              <div><span>PG 키</span><strong>{{ detailData?.pgPaymentKey ?? '-' }}</strong></div>
              <div><span>환불 상태</span><strong>{{ getRefundStatusLabel(detailData?.refundStatus) }}</strong></div>
              <div><span>환불 금액</span><strong>{{ formatCurrency(detailData?.refundAmount) }}</strong></div>
              <div><span>환불일시</span><strong>{{ formatDate(detailData?.refundApprovedAt) }}</strong></div>
              <div><span>환불 사유</span><strong>{{ detailData?.refundReason ?? '-' }}</strong></div>
              <div><span>생성일</span><strong>{{ formatDate(detailData?.createdAt) }}</strong></div>
            </div>
          </div>
          <div class="admin-modal__section admin-modal__note">
            <p>변경: {{ changeReason(detailData) }}</p>
            <p>환불: {{ refundReason(detailData) }}</p>
          </div>
        </div>

        <div class="admin-modal__actions" v-if="!detailLoading">
          <button class="admin-btn admin-btn--muted" type="button" :disabled="!canChange(detailData)">변경</button>
          <button
            class="admin-btn admin-btn--danger"
            type="button"
            :disabled="!canRefund(detailData)"
            @click="openRefundModal(detailData)"
          >
            환불
          </button>
        </div>
      </div>
    </div>

    <!-- 환불 모달 -->
    <div v-if="refundModal.open" class="admin-modal">
      <div class="admin-modal__backdrop" @click="closeRefundModal" />
      <div class="admin-modal__panel" role="dialog" aria-modal="true">
        <div class="admin-modal__header">
          <div>
            <p class="admin-modal__eyebrow">환불 처리</p>
            <h2 class="admin-modal__title">환불 정보를 입력해주세요</h2>
          </div>
          <button class="admin-btn admin-btn--ghost" type="button" :disabled="refundModal.loading" @click="closeRefundModal">닫기</button>
        </div>

        <div class="admin-modal__body">
          <div class="admin-detail-grid">
            <div><span>예약번호</span><strong>#{{ refundModal.booking?.reservationId }}</strong></div>
            <div><span>결제금액</span><strong>{{ formatCurrency(refundModal.booking?.finalPaymentAmount) }}</strong></div>
            <div><span>정책 환불율</span><strong>{{ refundModal.rate }}%</strong></div>
          </div>

          <div class="form-group">
            <label class="form-label">환불 금액</label>
            <div class="refund-amount-input">
              <input
                type="number"
                v-model="refundModal.amount"
                class="admin-input"
                :disabled="!refundModal.isManual || refundModal.loading"
              />
              <div class="refund-options">
                <label>
                  <input type="checkbox" v-model="refundModal.isManual"> 수동 입력 (강제 환불)
                </label>
              </div>
            </div>
            <p class="form-hint">정책 계산 금액: {{ formatCurrency(Math.floor(refundModal.booking?.finalPaymentAmount * (refundModal.rate / 100))) }}</p>
          </div>

          <div class="form-group">
            <label class="form-label">환불 사유</label>
            <select v-model="refundModal.reason" class="admin-select" :disabled="refundModal.loading">
              <option value="고객 변심">고객 변심</option>
              <option value="중복 예약">중복 예약</option>
              <option value="업체 사정">업체 사정</option>
              <option value="천재지변">천재지변</option>
              <option value="기타">기타</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">관리자 메모</label>
            <textarea
              v-model="refundModal.memo"
              class="admin-input"
              rows="3"
              placeholder="상세 사유를 입력하세요 (Audit Log 기록용)"
              :disabled="refundModal.loading"
            ></textarea>
          </div>

          <div v-if="refundModal.error" class="admin-status error">{{ refundModal.error }}</div>
        </div>

        <div class="admin-modal__actions">
          <button class="admin-btn admin-btn--ghost" type="button" :disabled="refundModal.loading" @click="closeRefundModal">취소</button>
          <button class="admin-btn admin-btn--danger" type="button" :disabled="refundModal.loading" @click="handleRefundSubmit">
            {{ refundModal.loading ? '처리 중...' : '환불 실행' }}
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

.admin-status {
  display: flex;
  gap: 12px;
  align-items: center;
  color: var(--text-sub, #6b7280);
  font-weight: 700;
  margin-top: 12px;
}

.admin-status.error {
  color: #e11d48;
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
  width: min(720px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: white;
  border-radius: 16px;
  padding: 1.4rem;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
  display: grid;
  gap: 1.1rem;
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
  font-size: 1.4rem;
  font-weight: 900;
  color: #0b3b32;
}

.admin-modal__body {
  display: grid;
  gap: 1rem;
}

.admin-modal__section h3 {
  margin: 0 0 0.6rem;
  font-size: 1rem;
  font-weight: 800;
  color: #0f172a;
}

.admin-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.65rem 1rem;
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

.admin-modal__note {
  color: #64748b;
  font-size: 0.85rem;
  font-weight: 700;
}

.admin-modal__actions {
  display: flex;
  gap: 0.6rem;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.booking-cards {
  display: none;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.booking-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 0.9rem;
  display: grid;
  gap: 0.6rem;
  background: #fff;
}

.booking-card__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.booking-card__row--top {
  font-weight: 800;
  color: #0b3b32;
}

.booking-card__id {
  font-size: 1rem;
}

.booking-card__title {
  font-weight: 800;
  color: #0f172a;
}

.booking-card__sub {
  color: #64748b;
  font-weight: 600;
}

.booking-card__row--dates {
  color: #475569;
  font-weight: 600;
}

.booking-card__row--bottom {
  gap: 0.6rem;
}

.booking-card__amount {
  font-weight: 900;
  color: #0f172a;
}

.booking-card__action {
  white-space: nowrap;
}

.booking-table th,
.booking-table td {
  vertical-align: middle;
}

.booking-table th {
  text-align: center;
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

.admin-nowrap {
  white-space: nowrap;
}

.admin-align-right {
  text-align: right;
}

.admin-align-center {
  text-align: center;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-weight: 700;
  color: #1f2937;
  font-size: 0.9rem;
}

.form-hint {
  font-size: 0.8rem;
  color: #6b7280;
  margin-top: 0.25rem;
}

.refund-amount-input {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.refund-options {
  font-size: 0.9rem;
  color: #4b5563;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .booking-cards {
    display: flex;
  }

  .booking-table {
    display: none;
  }
}
</style>
