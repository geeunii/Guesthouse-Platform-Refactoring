export const RESERVATION_STATUS_LABELS = {
  0: '임시',
  1: '요청',
  2: '확정',
  3: '체크인 완료',
  9: '취소'
}

export const RESERVATION_STATUS_VARIANTS = {
  0: 'neutral',
  1: 'warning',
  2: 'success',
  3: 'success',
  9: 'danger'
}

export const PAYMENT_STATUS_LABELS = {
  0: '미결제',
  1: '결제 완료',
  2: '결제 실패',
  3: '환불 완료'
}

export const PAYMENT_STATUS_VARIANTS = {
  0: 'neutral',
  1: 'success',
  2: 'danger',
  3: 'warning'
}

export const RESERVATION_STATUS_OPTIONS = [
  { value: 'all', label: '전체 상태' },
  { value: 'requested', label: '요청' },
  { value: 'confirmed', label: '확정' },
  { value: 'checkedin', label: '체크인 완료' },
  { value: 'canceled', label: '취소' }
]

export const getReservationStatusLabel = (code) => {
  const key = Number(code)
  if (!Number.isFinite(key)) return '알 수 없음'
  return RESERVATION_STATUS_LABELS[key] ?? `알 수 없음(${code})`
}

export const getReservationStatusVariant = (code) => {
  const key = Number(code)
  return RESERVATION_STATUS_VARIANTS[key] ?? 'neutral'
}

export const getPaymentStatusLabel = (code) => {
  const key = Number(code)
  if (!Number.isFinite(key)) return '알 수 없음'
  return PAYMENT_STATUS_LABELS[key] ?? `알 수 없음(${code})`
}

export const getPaymentStatusVariant = (code) => {
  const key = Number(code)
  return PAYMENT_STATUS_VARIANTS[key] ?? 'neutral'
}
