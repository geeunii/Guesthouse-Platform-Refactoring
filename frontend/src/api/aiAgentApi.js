import { authenticatedRequest } from './authClient'

export async function createAgentRoom() {
    return authenticatedRequest('/api/ai-agent/rooms', { method: 'POST' })
}

export async function getAgentRooms() {
    return authenticatedRequest('/api/ai-agent/rooms', { method: 'GET' })
}

export async function getAgentMessages(roomId) {
    return authenticatedRequest(`/api/ai-agent/rooms/${roomId}/messages`, { method: 'GET' })
}

export async function sendAgentMessage(roomId, message) {
    return authenticatedRequest(`/api/ai-agent/rooms/${roomId}/chat`, {
        method: 'POST',
        body: JSON.stringify({ message })
    })
}

export async function deleteAgentRoom(roomId) {
    return authenticatedRequest(`/api/ai-agent/rooms/${roomId}`, { method: 'DELETE' })
}
