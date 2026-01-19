import { hostGet, hostRequest } from './adminClient'

export async function fetchHostReviews(params = {}) {
  return hostGet('/host/reviews', params)
}

export async function fetchHostReviewSummary() {
  return hostGet('/host/reviews/summary')
}

export async function upsertHostReviewReply(reviewId, payload) {
  return hostRequest(`/host/reviews/${reviewId}/reply`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function reportHostReview(reviewId, payload) {
  return hostRequest(`/host/reviews/${reviewId}/report`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}
