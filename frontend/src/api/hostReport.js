import { hostGet, hostRequest } from './adminClient'

const cleanParams = (params = {}) => {
  const cleaned = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (!trimmed) return
      cleaned[key] = trimmed
      return
    }
    cleaned[key] = value
  })
  return cleaned
}

export async function fetchHostReviewReportSummary(params = {}) {
  return hostGet('/host/reports/reviews/summary', cleanParams(params))
}

export async function fetchHostReviewReportTrend(params = {}) {
  return hostGet('/host/reports/reviews/trend', cleanParams(params))
}

export async function fetchHostThemeReport(params = {}) {
  return hostGet('/host/reports/themes/popular', cleanParams(params))
}

export async function fetchHostDemandForecast(params = {}) {
  return hostGet('/host/reports/forecast/demand', cleanParams(params))
}

export async function fetchHostReviewAiSummary(payload = {}) {
  return hostRequest('/host/reports/reviews/ai-summary', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function fetchHostAiInsight(payload = {}) {
  return hostRequest('/host/reports/ai-insight', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function fetchHostAiInsightEligibility(params = {}) {
  return hostGet('/host/reports/ai-insight/eligibility', cleanParams(params))
}
