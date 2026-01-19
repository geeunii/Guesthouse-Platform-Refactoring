<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchHostReviews, fetchHostReviewSummary, upsertHostReviewReply, reportHostReview } from '@/api/hostReview'
import { fetchHostAccommodations } from '@/api/hostAccommodation'
import { deriveHostGateInfo, buildHostGateNotice } from '@/composables/useHostState'
import { formatDate } from '@/utils/formatters'
import HostGateNotice from '@/components/host/HostGateNotice.vue'

const reviews = ref([])
const isLoading = ref(false)
const loadError = ref('')
const router = useRouter()
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

const summary = ref({ avgRating: 0, reviewCount: 0 })
const summaryError = ref('')
const summaryLoading = ref(false)
const accommodations = ref([])
const accommodationLoading = ref(false)
const accommodationError = ref('')
const selectedAccommodationId = ref('all')
const selectedSort = ref('latest')

const sortOptions = [
  { value: 'latest', label: '최신순' },
  { value: 'oldest', label: '오래된순' }
]

const activeReplyId = ref(null)
const replyDraft = ref('')
const replyError = ref('')
const replySaving = ref(false)

const reportTarget = ref(null)
const reportReason = ref('')
const reportError = ref('')
const reportSaving = ref(false)

const averageRating = computed(() => Number(summary.value.avgRating ?? 0).toFixed(1))
const reviewCount = computed(() => summary.value.reviewCount ?? 0)

const normalizeReview = (item) => {
  const userName = item.authorName ?? item.userName ?? item.reviewerName ?? item.name ?? ''
  return {
    id: item.reviewId ?? item.id,
    userName,
    userInitial: userName ? userName.slice(0, 1) : 'U',
    accommodationName: item.accommodationName ?? item.property ?? '',
    rating: item.rating ?? 0,
    date: formatDate(item.createdAt ?? item.date, true),
    visitDate: item.visitDate ?? '',
    content: item.content ?? item.reviewContent ?? '',
    isCrawled: Boolean(item.isCrawled),
    replyContent: item.replyContent ?? '',
    replyUpdatedAt: item.replyUpdatedAt ?? null
  }
}

const loadReviews = async () => {
  if (!canUseHostFeatures.value) return
  isLoading.value = true
  loadError.value = ''
  const params = {
    page: 0,
    size: 20,
    sort: selectedSort.value
  }
  if (selectedAccommodationId.value !== 'all') {
    params.accommodationId = selectedAccommodationId.value
  }
  const response = await fetchHostReviews(params)
  if (response.ok) {
    const payload = response.data
    const list = Array.isArray(payload)
      ? payload
      : payload?.items ?? payload?.content ?? payload?.data ?? []
    reviews.value = list.map(normalizeReview)
  } else {
    loadError.value = '리뷰를 불러오지 못했습니다.'
  }
  isLoading.value = false
}

const openReply = (review) => {
  activeReplyId.value = review.id
  replyDraft.value = review.replyContent || ''
  replyError.value = ''
}

const closeReply = () => {
  activeReplyId.value = null
  replyDraft.value = ''
  replyError.value = ''
}

const submitReply = async (review) => {
  const trimmed = replyDraft.value.trim()
  if (!trimmed) {
    replyError.value = '답글 내용을 입력해주세요.'
    return
  }
  replySaving.value = true
  replyError.value = ''
  const response = await upsertHostReviewReply(review.id, { content: trimmed })
  if (response.ok) {
    review.replyContent = response.data?.content ?? trimmed
    review.replyUpdatedAt = response.data?.updatedAt ?? new Date().toISOString()
    closeReply()
  } else {
    replyError.value = response.data?.message || '답글 저장에 실패했습니다.'
  }
  replySaving.value = false
}

const openReport = (review) => {
  reportTarget.value = review
  reportReason.value = ''
  reportError.value = ''
}

const closeReport = () => {
  reportTarget.value = null
  reportReason.value = ''
  reportError.value = ''
}

const submitReport = async () => {
  if (!reportTarget.value) return
  const trimmed = reportReason.value.trim()
  if (!trimmed) {
    reportError.value = '신고 사유를 입력해주세요.'
    return
  }
  reportSaving.value = true
  reportError.value = ''
  const response = await reportHostReview(reportTarget.value.id, { reason: trimmed })
  if (response.ok) {
    closeReport()
    window.alert('신고가 접수되었습니다.')
  } else {
    reportError.value = response.data?.message || '신고 처리에 실패했습니다.'
  }
  reportSaving.value = false
}

const loadSummary = async () => {
  if (!canUseHostFeatures.value) return
  summaryLoading.value = true
  summaryError.value = ''
  const response = await fetchHostReviewSummary()
  if (response.ok) {
    summary.value = response.data ?? { avgRating: 0, reviewCount: 0 }
  } else {
    summaryError.value = '요약 정보를 불러오지 못했습니다.'
  }
  summaryLoading.value = false
}

const loadAccommodations = async () => {
  accommodationLoading.value = true
  accommodationError.value = ''
  const response = await fetchHostAccommodations()
  if (response.ok) {
    const payload = response.data
    accommodations.value = Array.isArray(payload)
      ? payload
      : payload?.items ?? payload?.content ?? payload?.data ?? []
  } else {
    accommodationError.value = '숙소 목록을 불러오지 못했습니다.'
  }
  accommodationLoading.value = false
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
  await loadHostState()
  if (canUseHostFeatures.value) {
    await Promise.all([loadAccommodations(), loadSummary()])
    loadReviews()
  }
})

watch([selectedAccommodationId, selectedSort], () => {
  if (canUseHostFeatures.value) {
    loadReviews()
  }
})
</script>

<template>
  <div class="review-view">
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
        <h2 class="host-title">리뷰 관리</h2>
        <p class="host-subtitle">
          평균 평점 {{ averageRating }} · 총 {{ reviewCount }}개의 리뷰
        </p>
        <p v-if="summaryError" class="muted">요약 정보를 불러오지 못했습니다.</p>
      </div>
      <div class="filters host-view-header__actions">
        <select v-model="selectedAccommodationId" class="filter-select" aria-label="숙소 선택">
          <option value="all">전체 숙소</option>
          <option v-for="acc in accommodations" :key="acc.accommodationsId ?? acc.id" :value="acc.accommodationsId ?? acc.id">
            {{ acc.accommodationsName ?? acc.name ?? acc.accommodationName ?? '숙소' }}
          </option>
        </select>
        <select v-model="selectedSort" class="filter-select" aria-label="정렬 선택">
          <option v-for="option in sortOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
      </div>
    </div>

    <div v-if="accommodationError" class="empty-box">
      <p>숙소 목록을 불러오지 못했어요.</p>
      <button class="ghost-btn" @click="loadAccommodations">다시 시도</button>
    </div>

    <div class="review-list">
      <div v-if="isLoading || summaryLoading || accommodationLoading" class="review-skeleton">
        <div v-for="i in 3" :key="i" class="skeleton-card" />
      </div>
      <div v-else-if="loadError" class="empty-box">
        <p>데이터를 불러오지 못했어요.</p>
        <button class="ghost-btn" @click="loadReviews">다시 시도</button>
      </div>
      <div v-else-if="!reviews.length" class="empty-box">
        <p>조건에 맞는 리뷰가 없습니다.</p>
        <button class="ghost-btn" @click="router.push('/host/booking')">예약 확인하기</button>
      </div>
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <div class="card-header">
          <div class="user-profile">
            <div class="avatar">{{ review.userInitial }}</div>
            <div class="user-info">
              <span class="user-name">{{ review.userName }}</span>
              <span class="accommodation-name">{{ review.accommodationName }}</span>
            </div>
          </div>

          <div class="meta-info">
            <div class="rating">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= review.rating }">★</span>
            </div>
            <span class="date">{{ review.date }}</span>
          </div>
        </div>

        <div class="review-content">
          <p>{{ review.content }}</p>
        </div>

        <div v-if="review.replyContent && activeReplyId !== review.id" class="reply-box">
          <p class="reply-title">호스트 답글</p>
          <p>{{ review.replyContent }}</p>
          <p v-if="review.replyUpdatedAt" class="reply-date">
            {{ formatDate(review.replyUpdatedAt, true) }}
          </p>
        </div>

        <div v-if="activeReplyId === review.id" class="reply-editor">
          <textarea v-model="replyDraft" rows="4" placeholder="답글을 입력해주세요." />
          <p v-if="replyError" class="muted error">{{ replyError }}</p>
          <div class="reply-actions">
            <button class="ghost-btn" type="button" @click="closeReply">취소</button>
            <button class="primary-btn" type="button" :disabled="replySaving" @click="submitReply(review)">
              {{ replySaving ? '저장 중...' : '답글 저장' }}
            </button>
          </div>
        </div>

        <div class="card-footer">
          <span class="review-chip" :class="{ crawled: review.isCrawled }">
            {{ review.isCrawled ? '크롤링 리뷰' : '직접 작성' }}
          </span>
          <span v-if="review.visitDate" class="visit-date">방문일 {{ review.visitDate }}</span>
        </div>

        <div class="card-actions">
          <button class="ghost-btn" type="button" @click="openReply(review)">
            {{ review.replyContent ? '답글 수정' : '답글 달기' }}
          </button>
          <button class="ghost-btn danger" type="button" @click="openReport(review)">신고</button>
        </div>
      </div>
    </div>

    <div v-if="reportTarget" class="modal-overlay" @click.self="closeReport">
      <div class="modal-card">
        <h3>리뷰 신고</h3>
        <p class="muted">신고 사유를 입력해주세요.</p>
        <textarea v-model="reportReason" rows="4" placeholder="예: 욕설/광고/허위 내용" />
        <p v-if="reportError" class="muted error">{{ reportError }}</p>
        <div class="modal-actions">
          <button class="ghost-btn" type="button" @click="closeReport">취소</button>
          <button class="primary-btn" type="button" :disabled="reportSaving" @click="submitReport">
            {{ reportSaving ? '접수 중...' : '신고 접수' }}
          </button>
        </div>
      </div>
    </div>

  </template>
  </div>
</template>

<style scoped>
.review-view {
  padding-bottom: 2rem;
}

/* ✅ 대시보드 톤 헤더 */
.view-header {
  margin-bottom: 1.25rem;
}

.filters {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-top: 0.75rem;
}

.filter-select {
  border: 1px solid var(--brand-border);
  border-radius: 12px;
  padding: 0.55rem 0.85rem;
  font-weight: 800;
  min-height: 44px;
  background: #ffffff;
  color: #111827;
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

/* Review Card (대시보드 카드 톤) */
.review-card {
  background: white;
  border-radius: 16px;
  padding: 1.25rem;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
  margin-bottom: 1rem;
  border: 1px solid var(--brand-border);
}

.reply-box {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 12px;
  padding: 0.85rem;
  color: #0f172a;
}

.reply-title {
  font-weight: 800;
  margin: 0 0 0.35rem;
}

.reply-date {
  margin-top: 0.35rem;
  font-size: 0.85rem;
  color: #64748b;
}

.reply-editor textarea,
.modal-card textarea {
  width: 100%;
  border: 1px solid var(--brand-border);
  border-radius: 12px;
  padding: 0.7rem;
  font-weight: 600;
  min-height: 120px;
  resize: vertical;
}

.reply-actions,
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
  margin-top: 0.6rem;
}

.card-actions {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.ghost-btn.danger {
  border-color: #fecaca;
  color: #b91c1c;
}

.error {
  color: #b91c1c;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  z-index: 50;
}

.modal-card {
  width: min(520px, 92vw);
  background: #fff;
  border-radius: 16px;
  padding: 1.25rem;
  border: 1px solid var(--brand-border);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.2);
}

.empty-box {
  margin-top: 1rem;
  color: #6b7280;
  font-weight: 700;
  text-align: center;
  display: grid;
  gap: 0.5rem;
  justify-items: center;
  padding: 0.5rem 0;
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

.primary-btn {
  background: var(--brand-primary, #BFE7DF);
  color: #0f172a;
  border-radius: 10px;
  padding: 0.55rem 0.9rem;
  font-weight: 800;
  min-height: 44px;
  border: 1px solid var(--brand-primary-strong, #0f766e);
  cursor: pointer;
}

.review-skeleton {
  display: grid;
  gap: 0.75rem;
}

.skeleton-card {
  height: 140px;
  border-radius: 16px;
  background: linear-gradient(90deg, #f1f5f9 0%, #e2e8f0 50%, #f1f5f9 100%);
  background-size: 200% 100%;
  animation: shimmer 1.1s ease infinite;
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
  .skeleton-card {
    animation: none !important;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.9rem;
}

.user-profile {
  display: flex;
  gap: 0.8rem;
  align-items: center;
  min-width: 0;
}

.avatar {
  width: 48px;
  height: 48px;
  background: var(--brand-primary);
  color: var(--brand-accent);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 1.05rem;
  flex: 0 0 auto;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  min-width: 0;
}

.user-name {
  font-weight: 900;
  color: #0f172a;
  font-size: 1rem;
}

.accommodation-name {
  font-size: 0.88rem;
  color: #6b7280;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-info {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
  flex: 0 0 auto;
}

.rating {
  color: #e5e7eb;
  letter-spacing: -2px;
  font-size: 0.95rem;
}

.star.filled {
  color: #FFB300;
}

.date {
  font-size: 0.85rem;
  color: #6b7280;
  font-weight: 700;
}

/* 본문 */
.review-content {
  margin-bottom: 1rem;
  font-size: 0.95rem;
  color: #0f172a;
  line-height: 1.65;
  font-weight: 600;
}

.review-content p {
  margin: 0;
}

/* Reply Section */
.reply-section {
  margin-top: 0.75rem;
}

.reply-form textarea {
  width: 100%;
  padding: 0.9rem 1rem;
  border: 1px solid var(--brand-border);
  border-radius: 12px;
  font-size: 0.95rem;
  resize: vertical;
  box-sizing: border-box;
  font-family: inherit;
  font-weight: 600;
}

.reply-form textarea:focus {
  outline: none;
  border-color: var(--brand-primary-strong);
  box-shadow: 0 0 0 3px var(--brand-primary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.btn-cancel,
.btn-submit {
  height: 44px;
  padding: 0 1rem;
  border-radius: 12px;
  font-size: 0.92rem;
  font-weight: 900;
  cursor: pointer;
}

.btn-cancel {
  background: white;
  border: 1px solid var(--brand-border);
  color: #475569;
}

.btn-submit {
  border: none;
}

.btn-reply-toggle {
  background: none;
  border: none;
  color: var(--brand-accent);
  font-weight: 900;
  cursor: pointer;
  padding: 0;
  font-size: 0.92rem;
}

.existing-reply {
  background: #f8fafc;
  padding: 0.95rem 1rem;
  border-radius: 12px;
  border: 1px solid #eef2f7;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.host-label {
  font-size: 0.88rem;
  font-weight: 900;
  color: #0f172a;
}

.edit-reply-btn {
  background: none;
  border: none;
  color: #6b7280;
  font-size: 0.85rem;
  cursor: pointer;
  text-decoration: underline;
  font-weight: 800;
}

.reply-text {
  font-size: 0.92rem;
  color: #334155;
  margin: 0;
  line-height: 1.6;
  font-weight: 600;
}

/* Footer */
.card-footer {
  margin-top: 0.9rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.review-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  border: 1px solid #d1d5db;
  background: #f8fafc;
  font-size: 0.8rem;
  font-weight: 900;
  color: #475569;
  white-space: nowrap;
}

.review-chip.crawled {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.visit-date {
  font-size: 0.85rem;
  font-weight: 700;
  color: #6b7280;
}

.btn-report {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 0.88rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  font-weight: 900;
}

.btn-report:disabled {
  color: #9ca3af;
  cursor: default;
}

.icon {
  font-size: 1rem;
}

/* ✅ 모바일 퍼스트 보강: 작은 화면에서 헤더 줄바꿈 */
@media (max-width: 430px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .meta-info {
    align-items: flex-start;
    text-align: left;
  }
}
</style>
