import { adminGet, adminRequest, hostGet } from './adminClient'

export const fetchAdminDashboardSummary = (params = {}) => adminGet('/dashboard/summary', params)
export const fetchAdminDashboardTimeseries = (params = {}) => adminGet('/dashboard/timeseries', params)
export const fetchAdminDashboardIssues = (params = {}) => adminGet('/dashboard/issues', params)
export const fetchAdminWeeklyReport = (params = {}) => adminGet('/dashboard/weekly', params)

export const fetchAdminAccommodations = (params = {}) => adminGet('/accommodations', params)
export const fetchAdminAccommodationDetail = (accommodationId) => adminGet(`/accommodations/${accommodationId}`)

export const approveAccommodation = (accommodationId) =>
  adminRequest(`/accommodations/${accommodationId}/approve`, { method: 'POST' })

export const rejectAccommodation = (accommodationId, reason) =>
  adminRequest(`/accommodations/${accommodationId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason })
  })

export const fetchAdminUsers = (params = {}) => adminGet('/users', params)
export const fetchAdminUserSummary = () => adminGet('/users/summary')

export const approveHost = (userId, reason) =>
  adminRequest(`/users/${userId}/host/approve`, {
    method: 'POST',
    body: reason ? JSON.stringify({ reason }) : undefined
  })
export const rejectHost = (userId, reason) =>
  adminRequest(`/users/${userId}/host/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason })
  })
export const suspendUser = (userId, reason) =>
  adminRequest(`/users/${userId}/suspend`, {
    method: 'POST',
    body: JSON.stringify({ reason })
  })
export const unsuspendUser = (userId, reason) =>
  adminRequest(`/users/${userId}/unsuspend`, {
    method: 'POST',
    body: reason ? JSON.stringify({ reason }) : undefined
  })

export const fetchAdminUserDetail = (userId) => adminGet(`/users/${userId}`)

export const fetchAdminBookings = (params = {}) => adminGet('/bookings', params)

export const fetchAdminBookingDetail = (reservationId) => adminGet(`/bookings/${reservationId}`)

export const refundBooking = (reservationId, payload) =>
  adminRequest(`/bookings/${reservationId}/refund`, {
    method: 'POST',
    body: JSON.stringify(payload ?? {})
  })

export const fetchRefundQuote = (reservationId) => hostGet('/refunds/quote', { reservationId })

export const fetchAdminPayments = (params = {}) => adminGet('/payments', params)
export const fetchAdminPaymentMetrics = (params = {}) => adminGet('/payments/metrics', params)
export const fetchAdminPaymentSummary = (params = {}) => adminGet('/payments/summary', params)

export const fetchAdminPaymentDetail = (paymentId) => adminGet(`/payments/${paymentId}`)

export const refundPayment = (paymentId, payload) =>
  adminRequest(`/payments/${paymentId}/refund`, {
    method: 'POST',
    body: JSON.stringify(payload ?? {})
  })

export const fetchAdminReports = (params = {}) => adminGet('/reports', params)

export const fetchAdminReportDetail = (reportId) => adminGet(`/reports/${reportId}`)
export const resolveAdminReport = (reportId, payload) =>
  adminRequest(`/reports/${reportId}/resolve`, {
    method: 'POST',
    body: JSON.stringify(payload ?? {})
  })

export const fetchAdminLogs = (params = {}) => adminGet('/logs', params)
