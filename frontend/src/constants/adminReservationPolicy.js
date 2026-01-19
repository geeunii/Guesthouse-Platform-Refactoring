export const RESERVATION_STATUS = {
  TEMP: 0,
  REQUESTED: 1,
  CONFIRMED: 2,
  CHECKED_IN: 3,
  CANCELED: 9
}

export const PAYMENT_STATUS = {
  NOT_PAID: 0,
  PAID: 1,
  FAILED: 2,
  REFUNDED: 3
}

export const RESERVATION_STATUS_TABLE = {
  reservationStatus: {
    0: '임시',
    1: '요청',
    2: '확정',
    3: '체크인 완료',
    9: '취소'
  },
  paymentStatus: {
    0: '미결제',
    1: '결제 완료',
    2: '결제 실패',
    3: '환불 완료'
  },
  rules: {
    refundAllowed: '예약확정 + 결제완료 + 체크인 전',
    changeAllowed: '요청/임시 + 미결제 + 체크인 전'
  }
}

const toDate = (value) => {
  if (!value) return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export const canChangeReservation = ({ reservationStatus, paymentStatus, checkin }) => {
  const status = Number(reservationStatus)
  const payment = Number(paymentStatus)
  if (payment === PAYMENT_STATUS.REFUNDED) return false
  if (payment === PAYMENT_STATUS.PAID) return false
  if (status === RESERVATION_STATUS.CANCELED || status === RESERVATION_STATUS.CHECKED_IN) return false
  const checkinDate = toDate(checkin)
  if (checkinDate && checkinDate <= new Date()) return false
  return status === RESERVATION_STATUS.TEMP || status === RESERVATION_STATUS.REQUESTED
}

export const changeBlockReason = ({ reservationStatus, paymentStatus, checkin }) => {
  const status = Number(reservationStatus)
  const payment = Number(paymentStatus)
  if (!Number.isFinite(status)) return '예약 상태 확인이 필요합니다.'
  if (payment === PAYMENT_STATUS.REFUNDED) return '환불 완료된 예약은 변경할 수 없습니다.'
  if (payment === PAYMENT_STATUS.PAID) return '결제 완료 후에는 변경할 수 없습니다.'
  if (status === RESERVATION_STATUS.CANCELED) return '취소된 예약은 변경할 수 없습니다.'
  if (status === RESERVATION_STATUS.CHECKED_IN) return '체크인 완료 후에는 변경할 수 없습니다.'
  const checkinDate = toDate(checkin)
  if (checkinDate && checkinDate <= new Date()) return '체크인 이후에는 변경할 수 없습니다.'
  return '예약 변경 가능'
}

export const canRefundReservation = ({ reservationStatus, paymentStatus, checkin }) => {
  const status = Number(reservationStatus)
  const payment = Number(paymentStatus)
  if (payment !== PAYMENT_STATUS.PAID) return false
  if (status === RESERVATION_STATUS.CANCELED || status === RESERVATION_STATUS.CHECKED_IN) return false
  const checkinDate = toDate(checkin)
  if (checkinDate && checkinDate <= new Date()) return false
  return status === RESERVATION_STATUS.CONFIRMED
}

export const refundBlockReason = ({ reservationStatus, paymentStatus, checkin }) => {
  const status = Number(reservationStatus)
  const payment = Number(paymentStatus)
  if (!Number.isFinite(status)) return '예약 상태 확인이 필요합니다.'
  if (payment === PAYMENT_STATUS.REFUNDED) return '이미 환불 완료되었습니다.'
  if (payment !== PAYMENT_STATUS.PAID) return '결제 완료 상태에서만 환불 가능합니다.'
  if (status === RESERVATION_STATUS.CHECKED_IN) return '체크인 완료 이후 환불은 불가합니다.'
  if (status === RESERVATION_STATUS.CANCELED) return '이미 취소된 예약입니다.'
  const checkinDate = toDate(checkin)
  if (checkinDate && checkinDate <= new Date()) return '체크인 이후 환불은 불가합니다.'
  return '환불 가능'
}
