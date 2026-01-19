import { hostRequest } from './adminClient'

export async function requestAccommodationAiSuggestion(payload) {
  return hostRequest('/ai/accommodations/naming', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}
