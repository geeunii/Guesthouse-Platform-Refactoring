import { hostGet, hostRequest } from './adminClient'

export async function fetchHostAccommodations(params = {}) {
  return hostGet('/host/accommodation', params)
}

export async function fetchHostAccommodation(accommodationId) {
  return hostGet(`/host/accommodation/${accommodationId}`)
}

export async function createHostAccommodation(payload) {
  return hostRequest('/host/accommodation/register', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function updateHostAccommodation(accommodationId, payload) {
  return hostRequest(`/accommodation/edit/${accommodationId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export async function deleteHostAccommodation(accommodationId) {
  return hostRequest(`/host/accommodation/${accommodationId}`, {
    method: 'DELETE'
  })
}
