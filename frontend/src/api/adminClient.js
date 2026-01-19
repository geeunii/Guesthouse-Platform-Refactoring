// Lightweight admin API client scaffold using the admin base URL.
// The base URL is configurable so user/admin servers can be split later.
import { getAccessToken } from './authClient'

const adminBaseURL = (import.meta.env.VITE_ADMIN_API_BASE_URL || '/api/admin').replace(/\/$/, '')

let adminAuthToken = ''

export function setAdminAuthToken(token) {
  adminAuthToken = token || ''
}

export async function adminRequest(path, options = {}) {
  const url = `${adminBaseURL}${path.startsWith('/') ? path : `/${path}`}`
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  const token = adminAuthToken || getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  // In V1 we keep this simple; consumers can handle non-OK states.
  const data = await response.json().catch(() => null)
  return { ok: response.ok, status: response.status, data }
}

const cleanParams = (params = {}) => {
  const cleaned = {}
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (!trimmed) return
      if (trimmed.toLowerCase() === 'all') return
      if (trimmed.toLowerCase() === 'undefined') return
      if (trimmed.toLowerCase() === 'null') return
      cleaned[key] = trimmed
      return
    }
    cleaned[key] = value
  })
  return cleaned
}

// Convenience GET wrapper for future use (currently unused, ready for swap-in)
export async function adminGet(path, params = {}) {
  const query = new URLSearchParams(cleanParams(params)).toString()
  const fullPath = query ? `${path}?${query}` : path
  return adminRequest(fullPath, { method: 'GET' })
}

// Host API client (shares same fetch wrapper style)
const hostBaseURL = (import.meta.env.VITE_HOST_API_BASE_URL || '/api').replace(/\/$/, '')

let hostAuthToken = ''

export function setHostAuthToken(token) {
  hostAuthToken = token || ''
}

export async function hostRequest(path, options = {}) {
  const url = `${hostBaseURL}${path.startsWith('/') ? path : `/${path}`}`
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  }

  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url, {
    ...options,
    headers
  })

  const data = await response.json().catch(() => null)
  return { ok: response.ok, status: response.status, data }
}

export async function hostGet(path, params = {}) {
  const query = new URLSearchParams(params).toString()
  const fullPath = query ? `${path}?${query}` : path
  return hostRequest(fullPath, { method: 'GET' })
}

export async function hostPost(path, body = {}) {
  return hostRequest(path, {
    method: 'POST',
    body: JSON.stringify(body)
  })
}

