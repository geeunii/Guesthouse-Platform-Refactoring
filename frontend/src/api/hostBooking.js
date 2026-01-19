import { hostGet } from './adminClient'

export async function fetchHostBookings(params = {}) {
  return hostGet('/host/booking', params)
}

export async function fetchHostBookingDetail(bookingId) {
  return hostGet(`/host/booking/${bookingId}`)
}

export async function fetchHostBookingCalendar(month) {
  const params = { month, class: 'calendar' }
  return hostGet('/host/booking', params)
}

export async function fetchRefundQuote(reservationId) {
  return hostGet('/refunds/quote', { reservationId })
}
