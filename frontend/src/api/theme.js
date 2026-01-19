import { authenticatedRequest } from './authClient'

export async function fetchThemes() {
  return authenticatedRequest('/api/themes', { method: 'GET', skipAuth: true })
}

export async function fetchThemeCategories() {
  return authenticatedRequest('/api/themes/categories', { method: 'GET', skipAuth: true })
}

export async function fetchUserThemes() {
  return authenticatedRequest('/api/themes/me', { method: 'GET' })
}
