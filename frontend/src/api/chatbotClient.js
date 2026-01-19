import { authenticatedRequest } from './authClient'

export async function createChatRoom() {
    return authenticatedRequest('/api/chatbot/room', { method: 'POST' })
}

export async function getChatRooms() {
    return authenticatedRequest('/api/chatbot/rooms', { method: 'GET' })
}

export async function getRoomMessages(roomId) {
    return authenticatedRequest(`/api/chatbot/room/${roomId}/messages`, { method: 'GET' })
}

export async function sendChatbotMessage(roomId, message) {
    return authenticatedRequest('/api/chatbot/send', {
        method: 'POST',
        body: JSON.stringify({ roomId, message })
    })
}

export async function deleteChatRoom(roomId) {
    return authenticatedRequest(`/api/chatbot/room/${roomId}`, { method: 'DELETE' })
}
