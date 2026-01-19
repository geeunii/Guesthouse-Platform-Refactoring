<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AdminStatCard from '../../components/admin/AdminStatCard.vue'
import AdminBadge from '../../components/admin/AdminBadge.vue'
import AdminTableCard from '../../components/admin/AdminTableCard.vue'
import { fetchAdminDashboardIssues } from '../../api/adminApi'

const router = useRouter()
const stats = ref([])
const issues = ref({
  pendingAccommodations: 0,
  openReports: 0,
  refundCount: 0,
  paymentFailureCount: 0,
  pendingAccommodationsList: [],
  openReportsList: []
})
const isLoading = ref(false)
const loadError = ref('')
const includeZeroIssues = ref(false)
const lastUpdatedAt = ref('')
const reportDetail = ref(null)

const toDateString = (date) => date.toISOString().slice(0, 10)
const padNumber = (value) => String(value).padStart(2, '0')
const formatTimestamp = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())} ${padNumber(date.getHours())}:${padNumber(date.getMinutes())}`
}

const isOver48Hours = (value) => {
  if (!value) return false
  const created = new Date(value)
  if (Number.isNaN(created.getTime())) return false
  return Date.now() - created.getTime() >= 48 * 60 * 60 * 1000
}

const countOverdue = (items) => (items ?? []).filter((item) => isOver48Hours(item.createdAt)).length

const loadIssues = async () => {
  isLoading.value = true
  loadError.value = ''
  const today = new Date()
  const params = { from: toDateString(today), to: toDateString(today) }
  const response = await fetchAdminDashboardIssues(params)
  if (response.ok && response.data) {
    issues.value = response.data
    const pendingCount = response.data.pendingAccommodations ?? 0
    const reportCount = response.data.openReports ?? 0
    const paymentFailureCount = response.data.paymentFailureCount ?? 0
    const refundCount = response.data.refundCount ?? 0
    const pendingOverdue = countOverdue(response.data.pendingAccommodationsList)
    const reportOverdue = countOverdue(response.data.openReportsList)
    stats.value = [
      {
        label: '승인 대기 숙소',
        count: pendingCount,
        value: `${pendingCount}건`,
        sub: '심사 대기',
        badge: pendingOverdue ? `48시간+ ${pendingOverdue}건` : '',
        tone: 'warning'
      },
      {
        label: '미처리 신고',
        count: reportCount,
        value: `${reportCount}건`,
        sub: '처리 필요',
        badge: reportOverdue ? `48시간+ ${reportOverdue}건` : '',
        tone: 'warning'
      },
      {
        label: '결제 실패',
        count: paymentFailureCount,
        value: `${paymentFailureCount}건`,
        sub: '실패/취소',
        tone: 'neutral'
      },
      {
        label: '환불 요청',
        count: refundCount,
        value: `${refundCount}건`,
        sub: '요청/완료',
        tone: 'accent'
      }
    ]
    lastUpdatedAt.value = new Date().toISOString()
  } else {
    loadError.value = '이슈 센터 데이터를 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const statusVariant = (status) => {
  if (status === 'PENDING') return 'warning'
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'neutral'
}

const reportVariant = (status) => {
  if (status === 'WAIT') return 'warning'
  if (status === 'CHECKED') return 'success'
  return 'neutral'
}

const resolveReportTarget = (item) => {
  const type = String(item?.type ?? '').toUpperCase()
  if (type.includes('REVIEW')) return '리뷰'
  if (type.includes('ACCOM')) return '숙소'
  if (type.includes('USER')) return '회원'
  if (type.includes('RESERV')) return '예약'
  return '기타'
}

const resolveReportReason = (item) => item?.title ?? '-'

const resolveReportDetail = (item) => item?.content ?? item?.description ?? item?.detail ?? item?.message ?? ''

const formatElapsed = (value) => {
  if (!value) return '-'
  const created = new Date(value)
  if (Number.isNaN(created.getTime())) return '-'
  const diffMs = Date.now() - created.getTime()
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  if (diffHours < 24) return `${diffHours}h`
  return `${Math.floor(diffHours / 24)}일`
}

const goTo = (target) => {
  if (target) router.push(target)
}

const openReportDetail = (item) => {
  reportDetail.value = item
}

const closeReportDetail = () => {
  reportDetail.value = null
}

const visibleStats = computed(() => {
  if (includeZeroIssues.value) return stats.value
  return stats.value.filter((card) => (card.count ?? 0) > 0)
})

const pendingCount = computed(() => issues.value.pendingAccommodations ?? 0)
const reportCount = computed(() => issues.value.openReports ?? 0)
const paymentFailureCount = computed(() => issues.value.paymentFailureCount ?? 0)
const refundCount = computed(() => issues.value.refundCount ?? 0)

const shouldShowSection = (count) => includeZeroIssues.value || count > 0
const pendingEmpty = computed(() => pendingCount.value === 0)
const reportEmpty = computed(() => reportCount.value === 0)
const paymentEmpty = computed(() => paymentFailureCount.value === 0)
const refundEmpty = computed(() => refundCount.value === 0)

onMounted(loadIssues)
</script>

<template>
  <section class="admin-page">
    <header class="admin-page__header">
      <div>
        <h1 class="admin-title">운영 이슈 센터</h1>
        <p class="admin-subtitle">오늘 확인해야 할 운영 이슈를 빠르게 점검합니다.</p>
      </div>
      <div class="admin-header-actions">
        <label class="admin-check">
          <input v-model="includeZeroIssues" type="checkbox" />
          <span>이상 없음 포함</span>
        </label>
        <div class="admin-update-meta">
          마지막 갱신: {{ formatTimestamp(lastUpdatedAt) }}
        </div>
        <button class="admin-btn admin-btn--ghost" type="button" :disabled="isLoading" @click="loadIssues">
          {{ isLoading ? '새로고침 중...' : '새로고침' }}
        </button>
      </div>
    </header>

    <div class="admin-grid">
      <AdminStatCard
        v-for="card in visibleStats"
        :key="card.label"
        :label="card.label"
        :value="card.value"
        :sub="card.sub"
        :badge="card.badge"
        :tone="card.tone"
      />
      <div v-if="isLoading" class="admin-status">불러오는 중...</div>
      <div v-else-if="loadError" class="admin-status">
        <span>{{ loadError }}</span>
        <button class="admin-btn admin-btn--ghost" type="button" @click="loadIssues">다시 시도</button>
      </div>
    </div>

    <div class="admin-grid admin-grid--2">
      <template v-if="shouldShowSection(pendingCount)">
        <details v-if="pendingEmpty" class="admin-issue-accordion">
          <summary class="admin-issue-accordion__summary">
            <span>숙소 승인 대기</span>
            <AdminBadge text="이상 없음" variant="success" />
            <span class="admin-issue-accordion__meta">기간 내 신규 없음</span>
          </summary>
          <div class="admin-issue-accordion__body">
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/accommodations?status=PENDING')">
              숙소 관리로 이동
            </button>
          </div>
        </details>
        <AdminTableCard v-else title="숙소 승인 대기">
          <template #actions>
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/accommodations?status=PENDING')">
              바로가기
            </button>
          </template>
          <table class="admin-table--tight admin-issue-table admin-issue-table--pending">
            <colgroup>
              <col style="width: 90px" />
              <col style="width: 1fr" />
              <col style="width: 120px" />
              <col style="width: 80px" />
              <col style="width: 100px" />
            </colgroup>
            <thead>
              <tr>
                <th>ID</th>
                <th>숙소명</th>
                <th>신청일</th>
                <th>경과</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in issues.pendingAccommodationsList" :key="item.accommodationsId">
                <td class="admin-strong">#{{ item.accommodationsId }}</td>
                <td class="admin-strong">{{ item.name }}</td>
                <td class="admin-issue-cell--center admin-issue-cell--date">
                  {{ item.createdAt?.slice?.(0, 10) ?? '-' }}
                </td>
                <td class="admin-issue-cell--center">{{ formatElapsed(item.createdAt) }}</td>
                <td class="admin-issue-cell--center">
                  <AdminBadge :text="item.approvalStatus" :variant="statusVariant(item.approvalStatus)" />
                </td>
              </tr>
            </tbody>
          </table>
        </AdminTableCard>
      </template>

      <template v-if="shouldShowSection(reportCount)">
        <details v-if="reportEmpty" class="admin-issue-accordion">
          <summary class="admin-issue-accordion__summary">
            <span>미처리 신고</span>
            <AdminBadge text="이상 없음" variant="success" />
            <span class="admin-issue-accordion__meta">기간 내 신규 없음</span>
          </summary>
          <div class="admin-issue-accordion__body">
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/reports?status=WAIT')">
              신고 관리로 이동
            </button>
          </div>
        </details>
        <AdminTableCard v-else title="미처리 신고">
          <template #actions>
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/reports?status=WAIT')">
              바로가기
            </button>
          </template>
          <table class="admin-table--tight admin-issue-table admin-issue-table--reports">
            <colgroup>
              <col style="width: 90px" />
              <col style="width: 110px" />
              <col style="width: 120px" />
              <col style="width: 80px" />
              <col style="width: 90px" />
              <col style="width: 90px" />
            </colgroup>
            <thead>
              <tr>
                <th>신고ID</th>
                <th>대상</th>
                <th>등록일</th>
                <th>경과</th>
                <th>상태</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in issues.openReportsList" :key="item.reportId">
                <td class="admin-strong">{{ item.reportId ? `#${item.reportId}` : '-' }}</td>
                <td class="admin-issue-cell--center" :title="resolveReportTarget(item)">{{ resolveReportTarget(item) }}</td>
                <td class="admin-issue-cell--center admin-issue-cell--date">
                  {{ item.createdAt?.slice?.(0, 10) ?? '-' }}
                </td>
                <td class="admin-issue-cell--center">{{ formatElapsed(item.createdAt) }}</td>
                <td class="admin-issue-cell--center">
                  <AdminBadge :text="item.status" :variant="reportVariant(item.status)" />
                </td>
                <td class="admin-issue-cell--center">
                  <button class="admin-btn admin-btn--ghost" type="button" @click="openReportDetail(item)">
                    보기/처리
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </AdminTableCard>
      </template>
    </div>

    <div class="admin-grid admin-grid--2">
      <template v-if="shouldShowSection(paymentFailureCount)">
        <details v-if="paymentEmpty" class="admin-issue-accordion">
          <summary class="admin-issue-accordion__summary">
            <span>결제 실패/취소</span>
            <AdminBadge text="이상 없음" variant="success" />
            <span class="admin-issue-accordion__meta">기간 내 신규 없음</span>
          </summary>
          <div class="admin-issue-accordion__body">
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/payments?status=FAILED')">
              결제 관리로 이동
            </button>
          </div>
        </details>
        <div v-else class="admin-card">
          <div class="admin-card__head">
            <div>
              <p class="admin-card__eyebrow">결제 실패/취소</p>
              <h3 class="admin-card__title">{{ issues.paymentFailureCount ?? 0 }}건</h3>
            </div>
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/payments?status=FAILED')">
              바로가기
            </button>
          </div>
          <div class="admin-card__meta">
            <p class="admin-card__desc">
              최근 결제 실패/취소 건을 빠르게 확인하세요.
            </p>
          </div>
        </div>
      </template>

      <template v-if="shouldShowSection(refundCount)">
        <details v-if="refundEmpty" class="admin-issue-accordion">
          <summary class="admin-issue-accordion__summary">
            <span>환불 요청/완료</span>
            <AdminBadge text="이상 없음" variant="success" />
            <span class="admin-issue-accordion__meta">기간 내 신규 없음</span>
          </summary>
          <div class="admin-issue-accordion__body">
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/payments?status=REFUNDED')">
              환불 내역으로 이동
            </button>
          </div>
        </details>
        <div v-else class="admin-card">
          <div class="admin-card__head">
            <div>
              <p class="admin-card__eyebrow">환불 요청/완료</p>
              <h3 class="admin-card__title">{{ issues.refundCount ?? 0 }}건</h3>
            </div>
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/payments?status=REFUNDED')">
              바로가기
            </button>
          </div>
          <div class="admin-card__meta">
            <p class="admin-card__desc">
              환불 진행 내역을 확인할 수 있습니다.
            </p>
          </div>
        </div>
      </template>
    </div>
    <div v-if="reportDetail" class="admin-modal" @click.self="closeReportDetail">
      <div class="admin-modal__content">
        <div class="admin-modal__head">
          <h3>신고 상세</h3>
          <div class="admin-modal__actions">
            <button class="admin-btn admin-btn--ghost" type="button" @click="goTo('/admin/reports?status=WAIT')">
              신고 관리로 이동
            </button>
            <button class="admin-btn admin-btn--ghost" type="button" @click="closeReportDetail">닫기</button>
          </div>
        </div>
        <div class="admin-modal__body">
          <div class="admin-modal__row">
            <span>신고ID</span>
            <strong>{{ reportDetail.reportId ? `#${reportDetail.reportId}` : '-' }}</strong>
          </div>
          <div class="admin-modal__row">
            <span>대상</span>
            <strong>{{ resolveReportTarget(reportDetail) }}</strong>
          </div>
          <div class="admin-modal__row">
            <span>사유</span>
            <strong>{{ resolveReportReason(reportDetail) }}</strong>
          </div>
          <div class="admin-modal__row admin-modal__row--detail">
            <span>내용</span>
            <p>{{ resolveReportDetail(reportDetail) || '-' }}</p>
          </div>
          <div class="admin-modal__row">
            <span>등록일</span>
            <strong>{{ reportDetail.createdAt?.slice?.(0, 10) ?? '-' }}</strong>
          </div>
          <div class="admin-modal__row">
            <span>경과</span>
            <strong>{{ formatElapsed(reportDetail.createdAt) }}</strong>
          </div>
          <div class="admin-modal__row">
            <span>상태</span>
            <AdminBadge :text="reportDetail.status" :variant="reportVariant(reportDetail.status)" />
          </div>
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

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.admin-grid--2 {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  align-items: start;
}

.admin-issue-table {
  width: 100%;
  table-layout: fixed;
  border-collapse: separate;
}

.admin-issue-table th,
.admin-issue-table td {
  padding: 8px 10px;
  line-height: 1.4;
}

.admin-issue-table th {
  overflow: visible;
  text-overflow: initial;
  white-space: nowrap;
}

.admin-issue-table td {
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-issue-cell--center {
  text-align: center;
}

.admin-issue-cell--date {
  white-space: nowrap;
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
  margin-bottom: 8px;
}

.admin-card__eyebrow {
  margin: 0;
  color: #111827;
  font-weight: 800;
  font-size: 0.9rem;
}

.admin-card__title {
  margin: 4px 0 0;
  font-size: 1.2rem;
  color: #111827;
  font-weight: 900;
}

.admin-card__desc {
  margin: 0;
  color: var(--text-sub, #6b7280);
  font-weight: 600;
}

.admin-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.admin-table-card__title) {
  color: #111827;
}

.admin-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.admin-check {
  display: inline-flex;
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

.admin-update-meta {
  font-size: 0.85rem;
  font-weight: 700;
  color: #6b7280;
}

.admin-issue-accordion {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-white);
  box-shadow: var(--shadow-md);
  padding: 12px 14px;
}

.admin-issue-accordion__summary {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  list-style: none;
  font-weight: 800;
  color: #0b3b32;
}

.admin-issue-accordion__summary::-webkit-details-marker {
  display: none;
}

.admin-issue-accordion__meta {
  color: #6b7280;
  font-weight: 700;
  font-size: 0.85rem;
}

.admin-issue-accordion__body {
  padding: 12px 0 4px;
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
  width: min(520px, 92vw);
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

.admin-modal__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
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

:deep(.admin-table-card table) {
  min-width: 0;
}

@media (max-width: 768px) {
  .admin-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .admin-issue-table {
    font-size: 0.85rem;
  }

  .admin-modal__row {
    grid-template-columns: 70px 1fr;
  }
}
</style>
