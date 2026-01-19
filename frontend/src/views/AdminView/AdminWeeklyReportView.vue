<script setup>
import { onMounted, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { exportCSV } from '../../utils/reportExport'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBarChart from '../../components/admin/AdminBarChart.vue'
import { fetchAdminWeeklyReport } from '../../api/adminApi'
import { buildDateRange, fillSeriesByDate, formatKRW, toISODate } from '../../utils/admin/chartSeries'

const activeDays = ref(7)
const report = ref(null)
const isLoading = ref(false)
const loadError = ref('')
const router = useRouter()

const formatCurrency = (value) => formatKRW(value)
const formatCount = (value, unit = '건') => `${Number(value ?? 0).toLocaleString()}${unit}`
const formatPeople = (value) => `${Number(value ?? 0).toLocaleString()}명`
const formatUnits = (value) => `${Number(value ?? 0).toLocaleString()}개`
const formatDateOnly = (value) => (value ? String(value).slice(0, 10) : '')

const loadReport = async () => {
  isLoading.value = true
  loadError.value = ''
  const response = await fetchAdminWeeklyReport({ days: activeDays.value })
  if (response.ok && response.data) {
    report.value = response.data
  } else {
    loadError.value = '주간 리포트를 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const stats = computed(() => {
  if (!report.value) return []
  const { from, to } = resolveRange()
  const rangeQuery = from && to ? { from, to } : {}
  return [
    {
      label: '예약 생성',
      value: formatCount(report.value.reservationCount),
      sub: '예약 생성(선택 기간)',
      tone: 'primary',
      to: { path: '/admin/bookings', query: rangeQuery }
    },
    {
      label: '결제 성공',
      value: formatCount(report.value.paymentSuccessCount),
      sub: '결제 성공(완료 기준)',
      tone: 'success',
      to: { path: '/admin/payments', query: { status: 'SUCCESS', ...rangeQuery } }
    },
    {
      label: '취소',
      value: formatCount(report.value.cancelCount),
      sub: '예약 취소(완료 기준)',
      tone: 'neutral',
      to: { path: '/admin/bookings', query: { status: 'canceled', ...rangeQuery } }
    },
    {
      label: '환불',
      value: formatCount(report.value.refundCount),
      sub: `환불 금액 ${formatCurrency(report.value.refundAmount)}`,
      tone: 'warning',
      to: { path: '/admin/payments', query: { status: 'REFUNDED', type: 'REFUND', ...rangeQuery } }
    },
    {
      label: '신규 가입자',
      value: formatPeople(report.value.newUsers),
      sub: '신규 가입(전체 계정)',
      tone: 'accent',
      to: { path: '/admin/users', query: rangeQuery }
    },
    {
      label: '신규 숙소',
      value: formatUnits(report.value.newAccommodations),
      sub: '숙소 등록(완료 기준)',
      tone: 'primary',
      to: { path: '/admin/accommodations', query: rangeQuery }
    },
    {
      label: '승인 대기 숙소',
      value: formatUnits(report.value.pendingAccommodations),
      sub: '승인 대기(심사 기준)',
      tone: 'warning',
      to: { path: '/admin/accommodations', query: { status: 'PENDING', ...rangeQuery } }
    },
    {
      label: '신규 호스트',
      value: formatPeople(report.value.newHosts),
      sub: '호스트 전환(승인 기준)',
      tone: 'success',
      to: { path: '/admin/users', query: { role: 'HOST', ...rangeQuery } }
    }
  ]
})

const resolveRange = () => {
  if (report.value?.from && report.value?.to) {
    return { from: report.value.from, to: report.value.to }
  }
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - (Number(activeDays.value ?? 7) - 1))
  return { from: toISODate(start), to: toISODate(end) }
}

const series = computed(() => {
  if (!report.value) return []
  const rawSeries = report.value.revenueSeries ?? []
  const { from, to } = resolveRange()
  const dates = buildDateRange(from, to)
  const values = fillSeriesByDate(dates, rawSeries, (item) => formatDateOnly(item.date), (item) => item.value)
  return dates.map((date, idx) => ({
    date,
    value: values[idx]
  }))
})

const hasSeriesData = computed(() => {
  return series.value.some((item) => Number(item.value ?? 0) > 0)
})

const chartLabels = computed(() => series.value.map((item) => item.date))
const chartValues = computed(() => series.value.map((item) => Number(item.value ?? 0)))
const chartMaxXTicks = computed(() => (Number(activeDays.value) > 10 ? 8 : 7))

const exportDailyCsv = () => {
  if (!report.value) return
  const rows = series.value.map((item) => ({
    date: item.date,
    revenue: item.value ?? 0
  }))
  exportCSV({
    filename: `weekly-report_${report.value.from}_${report.value.to}.csv`,
    sheets: [
      {
        name: '일별 매출',
        columns: [
          { key: 'date', label: '날짜' },
          { key: 'revenue', label: '매출(원)' }
        ],
        rows
      }
    ]
  })
}

onMounted(loadReport)

watch(activeDays, loadReport)

const handleStatClick = (card) => {
  if (!card?.to) return
  router.push(card.to)
}

</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">주간 리포트</h1>
        <p class="admin-subtitle">선택 기간의 KPI와 매출 추이를 요약합니다.</p>
      </div>
      <div class="admin-inline-actions admin-inline-actions--nowrap">
        <select v-model="activeDays" class="admin-select">
          <option :value="7">최근 7일</option>
          <option :value="30">최근 30일</option>
        </select>
        <button class="admin-btn admin-btn--ghost" type="button" @click="exportDailyCsv">CSV 다운로드</button>
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
        :clickable="Boolean(card.to)"
        @click="handleStatClick(card)"
      />
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadReport">다시 시도</button>
      </div>
    </div>

    <div class="admin-card admin-report-card">
      <div class="admin-card__head">
        <div>
          <p class="admin-card__eyebrow">일별 매출(원)</p>
          <h3 class="admin-card__title">
            {{ report?.from ?? '-' }} ~ {{ report?.to ?? '-' }}
          </h3>
        </div>
        <span v-if="report && !report.statsReady" class="admin-chip admin-chip--muted">통계 데이터 준비 중</span>
      </div>
      <div class="admin-chart-area">
        <div v-if="!hasSeriesData" class="admin-status">표시할 데이터가 없습니다.</div>
        <AdminBarChart
          v-else
          :labels="chartLabels"
          :values="chartValues"
          :height="260"
          format="currency"
          tooltip-label="매출"
          :max-x-ticks="chartMaxXTicks"
        />
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
  min-height: 260px;
}

.admin-chip {
  border-radius: 999px;
  padding: 6px 12px;
  font-weight: 700;
  font-size: 0.8rem;
}

.admin-chip--muted {
  background: #f1f5f9;
  color: #64748b;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
