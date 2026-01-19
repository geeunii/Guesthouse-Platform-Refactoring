<template>
  <div class="ai-agent-container">
    <!-- Sidebar: Chat Room List -->
    <div class="sidebar" :class="{ 'sidebar-hidden': !isSidebarOpen }">
      <div class="sidebar-header">
        <h2>채팅 목록</h2>
        <button @click="createNewRoom" class="new-chat-btn">+ 새 채팅</button>
      </div>
      <div class="room-list">
        <div 
          v-for="room in chatRooms" 
          :key="room.id" 
          class="room-item" 
          :class="{ active: currentRoomId === room.id }"
          @click="selectRoom(room.id)"
        >
          <div class="room-title">{{ room.title || '새로운 대화' }}</div>
          <div class="room-date">{{ formatDate(room.updatedAt) }}</div>
          <button @click.stop="deleteRoom(room.id)" class="delete-btn">×</button>
        </div>
      </div>
    </div>

    <!-- Chat Area -->
    <div class="chat-area" @click="closeSidebarOnMobile">
      <div class="chat-header">
        <button class="back-btn" @click="goBack">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <button class="toggle-sidebar-btn" @click.stop="toggleSidebar">☰</button>
        <h3>지금이곳 AI 🏠✨</h3>
      </div>

      <div class="messages-container" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <p>AI에게 제주도 숙소를 물어보세요!</p>
          <div class="suggestion-chips">
            <button @click="sendMessage('애월에서 파티할 수 있는 게하 추천해줘')">🎉 애월 파티 게하</button>
            <button @click="sendMessage('조용한 힐링 숙소 찾아줘')">🌿 조용한 힐링 숙소</button>
            <button @click="sendMessage('바다가 보이는 2인실 있어?')">🌊 오션뷰 2인실</button>
          </div>
        </div>

        <div v-for="msg in messages" :key="msg.id" class="message-wrapper" :class="msg.role">
          <div class="message-bubble">
            <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
            
            <!-- Recommended Accommodations -->
            <div v-if="msg.recommendedAccommodations && msg.recommendedAccommodations.length > 0" class="recommendations">
              <div v-for="acc in msg.recommendedAccommodations" :key="acc.id" class="acc-card" @click="goToDetail(acc.id)">
                <div class="acc-image" :style="{ backgroundImage: acc.thumbnailUrl ? `url(${acc.thumbnailUrl})` : 'none' }">
                  <span v-if="!acc.thumbnailUrl">No Image</span>
                </div>
                <div class="acc-info">
                  <h4>{{ acc.name }}</h4>
                  <p class="acc-location">{{ acc.city }} {{ acc.district }}</p>
                  <div class="acc-meta">
                    <span class="rating">⭐ {{ acc.rating ? acc.rating.toFixed(1) : 'NEW' }}</span>
                    <span class="price">{{ acc.minPrice ? acc.minPrice.toLocaleString() + '원~' : '가격정보없음' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
        
        <div v-if="isLoading" class="message-wrapper model loading">
          <div class="message-bubble">Thinking...</div>
        </div>
      </div>

      <div class="input-area">
        <input 
          v-model="inputMessage" 
          @keyup.enter="sendMessage(inputMessage)" 
          placeholder="메시지를 입력하세요..." 
          :disabled="isLoading"
        />
        <button @click="sendMessage(inputMessage)" :disabled="!inputMessage.trim() || isLoading">전송</button>
      </div>
    </div>

    <!-- 모달 -->
    <div v-if="isModalOpen" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-icon">⚠️</div>
        <p class="modal-message">{{ modalMessage }}</p>
        <button class="modal-btn" @click="closeModal">확인</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import { createAgentRoom, getAgentRooms, getAgentMessages, sendAgentMessage, deleteAgentRoom } from '@/api/aiAgentApi'
import { isAuthenticated } from '@/api/authClient'

// Configure marked for safe rendering
marked.setOptions({
  breaks: true,
  gfm: true
})

const renderMarkdown = (text) => {
  if (!text) return ''
  return marked.parse(text)
}

const router = useRouter()
const chatRooms = ref([])
const currentRoomId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const isLoading = ref(false)
const isSidebarOpen = ref(true)
const messagesContainer = ref(null)

// 모달 상태
const isModalOpen = ref(false)
const modalMessage = ref('')

const showModal = (message) => {
  modalMessage.value = message
  isModalOpen.value = true
}

const closeModal = () => {
  isModalOpen.value = false
  modalMessage.value = ''
}

// Formatters
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getHours()}:${date.getMinutes().toString().padStart(2, '0')}`
}

// Actions
const loadRooms = async () => {
  if (!isAuthenticated()) {
    alert('로그인이 필요합니다.')
    router.push('/login')
    return
  }
  const response = await getAgentRooms()
  if (response.ok) {
    chatRooms.value = response.data
    // If no rooms, create one automatically or show empty state
    if (chatRooms.value.length === 0) {
      await createNewRoom()
    } else if (!currentRoomId.value) {
        // Select the most recent room
        selectRoom(chatRooms.value[0].id)
    }
  }
}

const createNewRoom = async () => {
  if (isLoading.value) return // 중복 호출 방지
  
  isLoading.value = true
  try {
    const response = await createAgentRoom()
    if (response.ok && response.data?.roomId) {
      await loadRooms()
      selectRoom(response.data.roomId)
    } else {
      console.error('새 채팅방 생성 실패:', response)
      showModal('새 채팅방을 만들지 못했습니다. 다시 시도해주세요.')
    }
  } catch (error) {
    console.error('새 채팅방 생성 오류:', error)
    showModal('오류가 발생했습니다. 다시 시도해주세요.')
  } finally {
    isLoading.value = false
  }
}

const selectRoom = async (roomId) => {
  currentRoomId.value = roomId
  const response = await getAgentMessages(roomId)
  if (response.ok) {
    messages.value = response.data
    scrollToBottom()
  }
  // Mobile: close sidebar on selection
  if (window.innerWidth < 768) {
    isSidebarOpen.value = false
  }
}

const deleteRoom = async (roomId) => {
  if (!confirm('대화를 삭제하시겠습니까?')) return
  const response = await deleteAgentRoom(roomId)
  if (response.ok) {
    if (currentRoomId.value === roomId) {
      currentRoomId.value = null
      messages.value = []
    }
    await loadRooms()
  }
}

const sendMessage = async (text) => {
  if (!text || !text.trim() || isLoading.value) return
  
  const userMsg = text
  inputMessage.value = ''
  
  // Optimistic update
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMsg,
    createdAt: new Date().toISOString()
  })
  scrollToBottom()
  
  isLoading.value = true
  const response = await sendAgentMessage(currentRoomId.value, userMsg)
  isLoading.value = false
  
  if (response.ok) {
    // Replace with actual response (or just append AI response)
    // Here we'll just reload messages to be safe and get consistent ID/data
    // Or we can append the response data directly
    const aiMsg = {
        id: Date.now() + 1,
        role: 'model',
        content: response.data.reply,
        recommendedAccommodations: response.data.recommendedAccommodations,
        createdAt: new Date().toISOString()
    }
    messages.value.push(aiMsg)
    scrollToBottom()
    
    // Update room list (last message changed)
    loadRooms()
  } else {
    alert('메시지 전송 실패')
    // Remove optimistic message?
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      // Find the last message element and scroll it into view at the top
      const messageElements = messagesContainer.value.querySelectorAll('.message-wrapper')
      if (messageElements.length > 0) {
        const lastMessage = messageElements[messageElements.length - 1]
        lastMessage.scrollIntoView({ behavior: 'smooth', block: 'start' })
      } else {
        // Fallback to scroll bottom if no messages
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    }
  })
}

const toggleSidebar = () => {
  isSidebarOpen.value = !isSidebarOpen.value
}

const closeSidebarOnMobile = () => {
  if (window.innerWidth < 768 && isSidebarOpen.value) {
    isSidebarOpen.value = false
  }
}

const goToDetail = (accId) => {
    window.open(`/room/${accId}`, '_blank')
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadRooms()
})
</script>

<style scoped>
.ai-agent-container {
  display: flex;
  position: relative; /* For absolute positioned sidebar in mobile */
  height: calc(100vh - 160px); /* Adjust for navbar + footer + margin */
  background-color: #f5f7fa;
  font-family: 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
  margin: 20px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e0e0;
}

/* Sidebar */
.sidebar {
  width: 280px;
  background-color: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  z-index: 10;
  border-top-left-radius: 16px;
  border-bottom-left-radius: 16px;
}

.sidebar-hidden {
  display: none;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h2 {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
}

.new-chat-btn {
  background-color: #6DC3BB;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  transition: background-color 0.2s;
}

.new-chat-btn:hover {
  background-color: #5AB5AC;
}

.room-list {
  flex: 1;
  overflow-y: auto;
}

.room-item {
  padding: 15px 20px;
  cursor: pointer;
  border-bottom: 1px solid #f9f9f9;
  position: relative;
  transition: background-color 0.2s;
}

.room-item:hover {
  background-color: #f0f0f0;
}

.room-item.active {
  background-color: #e8f7f5;
  border-left: 4px solid #6DC3BB;
}

.room-title {
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.room-date {
  font-size: 12px;
  color: #888;
}

.delete-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #ccc;
  font-size: 18px;
  cursor: pointer;
  display: none;
}

.room-item:hover .delete-btn {
  display: block;
}

.delete-btn:hover {
  color: #ff4444;
}

/* Chat Area */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
  background-color: white;
}

.back-btn {
  display: none; /* 데스크탑에서는 숨김 */
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  margin-right: 8px;
  color: #333;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background-color: #f0f0f0;
}

.toggle-sidebar-btn {
  background: none;
  border: none;
  font-size: 20px;
  margin-right: 15px;
  cursor: pointer;
}

.chat-header h3 {
  margin: 0;
  font-size: 18px;
}

.messages-container {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f8f9fa;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.empty-state {
  text-align: center;
  margin-top: 50px;
  color: #888;
}

.suggestion-chips {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.suggestion-chips button {
  background-color: white;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-chips button:hover {
  border-color: #6DC3BB;
  color: #6DC3BB;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 80%;
}

.message-wrapper.user {
  align-self: flex-end;
  align-items: flex-end;
}

.message-wrapper.model {
  align-self: flex-start;
  align-items: flex-start;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
  white-space: pre-wrap;
  line-height: 1.5;
}

.user .message-bubble {
  background: linear-gradient(135deg, #6DC3BB 0%, #5AB5AC 100%);
  color: white;
  border-bottom-right-radius: 2px;
}

.model .message-bubble {
  background-color: white;
  color: #333;
  border-bottom-left-radius: 2px;
  border: 1px solid #eee;
}

/* Markdown content styles */
.message-content {
  font-family: var(--font-main, 'TMoneyDungunbaram', 'NanumSquareRound', -apple-system, BlinkMacSystemFont, sans-serif);
}

.message-content :deep(h1),
.message-content :deep(h2),
.message-content :deep(h3) {
  margin: 12px 0 8px 0;
  font-weight: 700;
  line-height: 1.3;
}

.message-content :deep(h3) {
  font-size: 1.05em;
}

.message-content :deep(strong) {
  font-weight: 700;
  color: #1f2937;
}

.message-content :deep(ul),
.message-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-content :deep(li) {
  margin: 4px 0;
}

.message-content :deep(p) {
  margin: 8px 0;
}

.message-content :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 12px 0;
}

.message-time {
  font-size: 11px;
  color: #aaa;
  margin-top: 4px;
}

/* Recommendations */
.recommendations {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 15px;
}

.acc-card {
  display: flex;
  background-color: white;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
  width: 100%;
}

.acc-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.acc-image {
  width: 80px;
  height: 80px;
  background-size: cover;
  background-position: center;
  background-color: #eee;
  flex-shrink: 0;
}

.acc-info {
  padding: 10px;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.acc-info h4 {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: bold;
}

.acc-location {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.acc-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.rating {
  color: #FFB400;
  font-weight: bold;
}

.price {
  font-weight: bold;
  color: #6DC3BB;
}

.loading .message-bubble {
  background-color: #f0f0f0;
  color: #888;
  font-style: italic;
}

/* Input Area */
.input-area {
  padding: 20px;
  background-color: white;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
}

.input-area input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 24px;
  outline: none;
  font-size: 14px;
}

.input-area input:focus {
  border-color: #6DC3BB;
  box-shadow: 0 0 0 3px rgba(109, 195, 187, 0.2);
}

.input-area button {
  background-color: #6DC3BB;
  color: white;
  border: none;
  padding: 0 20px;
  border-radius: 24px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}

.input-area button:hover:not(:disabled) {
  background-color: #5AB5AC;
}

.input-area button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* Responsive */
@media (max-width: 768px) {
  .ai-agent-container {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    margin: 0;
    height: 100vh;
    height: 100dvh; /* Dynamic viewport height for mobile browsers */
    border-radius: 0;
    z-index: 9999;
    border: none;
    box-shadow: none;
  }

  .sidebar {
    position: absolute;
    width: 240px;
    height: 100%;
    max-height: 100%;
    transform: translateX(-100%);
    border-radius: 0;
    box-shadow: 4px 0 15px rgba(0, 0, 0, 0.15);
    overflow: hidden;
    background-color: white;
  }
  
  .sidebar:not(.sidebar-hidden) {
    transform: translateX(0);
  }

  .chat-area {
    border-radius: 0;
  }

  .chat-header {
    padding: 15px 16px;
    padding-top: env(safe-area-inset-top, 15px);
  }

  .back-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  /* 사이드바 토글 버튼은 계속 표시 */

  .messages-container {
    padding: 16px;
  }

  .input-area {
    padding: 16px;
    padding-bottom: max(16px, env(safe-area-inset-bottom));
  }

  .message-wrapper {
    max-width: 90%;
  }

  /* 모바일에서 삭제 버튼 항상 표시 */
  .delete-btn {
    display: block !important;
    color: #999;
  }

  .room-title {
    max-width: 160px;
  }

  .suggestion-chips {
    flex-direction: column;
    align-items: center;
  }

  .suggestion-chips button {
    width: 100%;
    max-width: 280px;
  }
}

/* 모달 스타일 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  text-align: center;
  max-width: 320px;
  width: 90%;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  animation: modalSlideIn 0.3s ease-out;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.modal-message {
  font-size: 1rem;
  color: #333;
  line-height: 1.5;
  margin-bottom: 1.5rem;
}

.modal-btn {
  background: #6DC3BB;
  color: white;
  border: none;
  padding: 0.8rem 2rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.modal-btn:hover {
  background: #5AB5AC;
}
</style>
