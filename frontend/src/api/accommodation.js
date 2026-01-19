import { hostGet } from './adminClient'

const normalizeDateParam = (value) => {
  if (!value) return null
  if (value instanceof Date) {
    if (Number.isNaN(value.getTime())) return null
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  const trimmed = String(value).trim()
  return trimmed ? trimmed : null
}

export async function fetchAccommodationDetail(accommodationId) {
  if (!accommodationId) {
    return { ok: false, status: 400, data: null }
  }
  return hostGet(`/public/detail/${accommodationId}`)
}

export async function fetchAccommodationAvailability(accommodationId, { checkin, checkout, guestCount } = {}) {
  if (!accommodationId) {
    return { ok: false, status: 400, data: null }
  }
  const params = {}
  const normalizedCheckin = normalizeDateParam(checkin)
  const normalizedCheckout = normalizeDateParam(checkout)
  if (normalizedCheckin) {
    params.checkin = normalizedCheckin
  }
  if (normalizedCheckout) {
    params.checkout = normalizedCheckout
  }
  const normalizedGuestCount = Number(guestCount)
  if (Number.isFinite(normalizedGuestCount) && normalizedGuestCount > 0) {
    params.guestCount = normalizedGuestCount
  }
  return hostGet(`/public/detail/${accommodationId}/availability`, params)
}

export async function fetchAiSummary(accommodationId) {
  if (!accommodationId) {
    return { ok: false, status: 400, data: null }
  }
  // adminClient의 hostGet을 사용하고, 컨트롤러 경로에 맞게 /v1을 추가
  return hostGet(`/v1/accommodations/${accommodationId}/ai-summary`)
}
