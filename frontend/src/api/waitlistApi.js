import { authenticatedRequest } from './authClient'

export async function registerWaitlist(data) {
    return authenticatedRequest('/api/waitlist', {
        method: 'POST',
        body: JSON.stringify(data)
    })
}

export async function cancelWaitlist(waitlistId) {
    return authenticatedRequest(`/api/waitlist/${waitlistId}`, {
        method: 'DELETE'
    })
}
