import { hostGet } from './adminClient'

export async function fetchHostRevenueSummary({ year, month }) {
  const params = { year, month }
  return hostGet('/host/revenue/summary', params)
}

export async function fetchHostRevenueTrend({ year }) {
  const params = { year }
  return hostGet('/host/revenue/trend', params)
}

export async function fetchHostRevenueDetails({ from, to, granularity }) {
  const params = { from, to }
  if (granularity) params.granularity = granularity
  return hostGet('/host/revenue/details', params)
}
