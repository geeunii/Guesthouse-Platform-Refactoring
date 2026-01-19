export const PAYMENT_STATUS_LABELS = {
  0: '요청',
  1: '완료',
  2: '실패',
  3: '환불'
}

export const PAYMENT_STATUS_VARIANTS = {
  0: 'neutral',
  1: 'success',
  2: 'danger',
  3: 'warning'
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
