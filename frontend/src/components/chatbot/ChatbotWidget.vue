<script setup>
import { ref, nextTick, onMounted, onUnmounted, computed, watch } from 'vue';
import { createChatRoom, getChatRooms, getRoomMessages, sendChatbotMessage, deleteChatRoom } from '@/api/chatbotClient';
import { getAccessToken, getUserInfo } from '@/api/authClient';
import { useRealtimeChatStore } from '@/stores/realtimeChatStore';

// --- Pinia Store ---
const realtimeChatStore = useRealtimeChatStore();

// --- New state for tabs & real-time chat ---
const activeTab = ref('faq');
const realtimeChatRooms = ref([]);
const currentChatRoom = ref(null); // This will hold the full room object
const messageInput = ref(''); // For the real-time chat input
const currentUser = computed(() => getUserInfo());

const totalUnreadCount = computed(() => {
  return realtimeChatRooms.value.reduce((total, room) => total + (room.unreadCount || 0), 0);
});

// 새 메시지 알림 감시 - 실시간으로 채팅방 목록 업데이트
watch(() => realtimeChatStore.roomNotifications.length, (newLength) => {
    if (newLength > 0) {
        const notifications = [...realtimeChatStore.roomNotifications];
        realtimeChatStore.clearNotifications();

        notifications.forEach(notification => {
            // 해당 채팅방 찾아서 업데이트
            const room = realtimeChatRooms.value.find(r => Number(r.id) === Number(notification.roomId));
            if (room) {
                room.unreadCount = notification.unreadCount;
                room.lastMessage = notification.lastMessage;
                room.lastMessageTime = notification.lastMessageTime;
                console.log('Updated room from notification:', room);
            } else {
                // 채팅방 목록에 없으면 새로고침
                console.log('Room not found in list, reloading...');
                loadRealtimeChatRooms();
            }
        });

        // 최신 메시지 순으로 재정렬
        realtimeChatRooms.value.sort((a, b) => {
            if (!a.lastMessageTime && !b.lastMessageTime) return 0;
            if (!a.lastMessageTime) return 1;
            if (!b.lastMessageTime) return -1;
            return new Date(b.lastMessageTime) - new Date(a.lastMessageTime);
        });
    }
});

// Combined messages computed property
const currentMessages = computed(() => {
    if (activeTab.value === 'chat') {
        // 메시지 시간순 정렬 (오름차순: 과거 -> 최신)
        // Redis ZSet Score가 꼬였을 경우를 대비해 클라이언트에서 재정렬
        return [...realtimeChatStore.messages].sort((a, b) => {
            if (!a.createdAt && !b.createdAt) return 0;
            if (!a.createdAt) return -1;
            if (!b.createdAt) return 1;
            return new Date(a.createdAt) - new Date(b.createdAt);
        });
    }
    return messages.value;
});

// --- Existing state for chatbot ---
const isOpen = ref(false);
const viewMode = ref('list'); // 'list' | 'chat'
const chatRooms = ref([]);
const messages = ref([]); // For chatbot messages
const currentRoomId = ref(null); // For chatbot room id
const isLoading = ref(false);
const chatContainer = ref(null);

// --- Drag functionality state ---
const isDragging = ref(false);
const hasMoved = ref(false);
const dragStartX = ref(0);
const dragStartY = ref(0);
const initialClientX = ref(0);
const initialClientY = ref(0);
const positionX = ref(35);
const positionY = ref(160);
const minX = 20;
const maxX = 200;
const minY = 80;
const maxY = 400;
const DRAG_THRESHOLD = 10;

// --- Modal state ---
const showModal = ref(false);
const modalState = ref({ title: '', message: '', type: 'confirm', onConfirm: null });

const wrapperStyle = computed(() => ({
  right: `${positionX.value}px`,
  bottom: `${positionY.value}px`
}));

// --- New methods for real-time chat integration ---
const enterRealtimeChatRoom = async (room) => {
    currentChatRoom.value = room;
    viewMode.value = 'chat';
    isLoading.value = true;

    try {
        // Ensure WebSocket is connected before subscribing
        if (!realtimeChatStore.connected) {
            console.log("WebSocket not connected. Attempting to connect now...");
            const token = getAccessToken();
            if (!token) throw new Error("Cannot connect: No access token found.");
            await realtimeChatStore.connect(token); // await the promise
        }

        // Now we are sure we are connected, subscribe to the room
        realtimeChatStore.subscribeToRoom(room.id);

        // Mark messages as read
        await fetch(`/api/realtime-chat/rooms/${room.id}/read`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${getAccessToken()}` }
        });
        const localRoom = realtimeChatRooms.value.find(r => r.id === room.id);
        if (localRoom) {
            localRoom.unreadCount = 0;
        }

        // Fetch initial messages
        const res = await fetch(`/api/realtime-chat/rooms/${room.id}/messages`, {
            headers: { 'Authorization': `Bearer ${getAccessToken()}` }
        });
        if (res.ok) {
            const fetchedMessages = await res.json();
            const myId = currentUser.value?.userId || currentUser.value?.id;
            // Initialize readByRecipient property for each message
            realtimeChatStore.messages = fetchedMessages.map(msg => ({
                ...msg,
                // For messages sent by current user, set readByRecipient based on backend's isRead field
                readByRecipient: myId && Number(msg.senderUserId) === Number(myId) ? msg.isRead : undefined
            }));
        } else {
            realtimeChatStore.messages = [];
        }

    } catch (error) {
        console.error("Failed to connect or subscribe:", error);
        openAlert('연결 오류', '채팅 서버에 연결할 수 없습니다. 페이지를 새로고침 해주세요.');
    } finally {
        isLoading.value = false;
        await nextTick();
        scrollToBottom();
    }
};

const sendRealtimeMessage = () => {
    if (!messageInput.value.trim() || !currentChatRoom.value) return;

    realtimeChatStore.sendMessage(
        currentChatRoom.value.id,
        messageInput.value
    );
    messageInput.value = '';
    
    // The message will be added to the store via the websocket subscription
    // We can scroll down optimistically
    setTimeout(() => scrollToBottom(), 100);
};

const getUnreadCount = (room) => room.unreadCount || 0;

const isHost = (room) => {
    if (!currentUser.value || !room) return false;
    return currentUser.value.userId === room.hostUserId;
};

// 기본 게스트 아바타 이미지 (SVG 데이터 URI)
const defaultGuestAvatar = "data:image/svg+xml,%3csvg%20xmlns='http://www.w3.org/2000/svg'%20width='100'%20height='100'%20viewBox='0%200%2024%2024'%3e%3ccircle%20cx='12'%20cy='12'%20r='12'%20fill='%23E0E0E0'/%3e%3cpath%20d='M20%2021v-2a4%204%200%200%200-4-4H8a4%204%200%200%200-4%204v2'%20fill='none'%20stroke='%239E9E9E'%20stroke-width='2'%20stroke-linecap='round'%20stroke-linejoin='round'%3e%3c/path%3e%3ccircle%20cx='12'%20cy='7'%20r='4'%20fill='none'%20stroke='%239E9E9E'%20stroke-width='2'%3e%3c/circle%3e%3c/svg%3e";

// 현재 사용자 기준으로 상대방 정보 가져오기
const getOtherUserName = (room) => {
    if (!currentUser.value || !room) return '상대방';
    if (isHost(room)) {
        return room.guestName || '게스트';
    } else {
        return room.hostName || '호스트';
    }
};

// 상대방 프로필 이미지 가져오기 (호스트면 게스트 아바타, 게스트면 숙소 이미지)
const getOtherUserProfileImage = (room) => {
    if (!room) return '/icon.png';
    if (isHost(room)) {
        // 호스트가 보는 화면: 게스트 프로필 (기본 아바타)
        return room.guestProfileImage || defaultGuestAvatar;
    } else {
        // 게스트가 보는 화면: 숙소 이미지
        return room.accommodationImage || '/icon.png';
    }
};

// 현재 사용자 ID 가져오기 (userId 또는 id)
const getCurrentUserId = () => {
    if (!currentUser.value) return null;
    return currentUser.value.userId || currentUser.value.id;
};

// 현재 사용자가 메시지를 보낸 사람인지 확인 (타입 맞춰서 비교)
const isMyMessage = (msg) => {
    const myId = getCurrentUserId();
    if (!myId) return false;
    // 숫자/문자열 타입 차이를 고려하여 비교
    return Number(msg.senderUserId) === Number(myId);
};

// Watch for incoming messages to scroll down
watch(() => realtimeChatStore.messages, () => {
    if (activeTab.value === 'chat') {
        nextTick(() => {
            scrollToBottom();
        });
    }
}, { deep: true });

// Auto-read messages when they arrive while viewing chat room
watch(() => realtimeChatStore.messages.length, (newLength, oldLength) => {
    if (activeTab.value === 'chat' && currentChatRoom.value && viewMode.value === 'chat') {
        // Check if new message was added
        if (newLength > oldLength) {
            const latestMessage = realtimeChatStore.messages[realtimeChatStore.messages.length - 1];
            // If message is from other user, mark as read automatically (즉시 호출)
            const myId = getCurrentUserId();
            if (latestMessage && myId && Number(latestMessage.senderUserId) !== Number(myId)) {
                console.log('New message from other user detected, marking as read...');
                markCurrentRoomAsRead();
            }
        }
    }
});

const markCurrentRoomAsRead = async () => {
    if (!currentChatRoom.value) return;
    
    try {
        await fetch(`/api/realtime-chat/rooms/${currentChatRoom.value.id}/read`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${getAccessToken()}` }
        });
        console.log('Auto-marked messages as read for room:', currentChatRoom.value.id);
    } catch (error) {
        console.error('Failed to auto-mark messages as read:', error);
    }
};

// --- Methods for tabs ---
const switchTab = (tab) => {
  activeTab.value = tab;
  viewMode.value = 'list';
  if (tab === 'chat') {
    loadRealtimeChatRooms();
  } else {
    loadRooms();
  }
};

const loadRealtimeChatRooms = async () => {
  isLoading.value = true;
  try {
    const res = await fetch('/api/realtime-chat/rooms', {
      headers: { 'Authorization': `Bearer ${getAccessToken()}` }
    });
    if (res.ok) {
        realtimeChatRooms.value = await res.json();
    } else {
        console.error('Failed to load real-time chat rooms');
        realtimeChatRooms.value = [];
    }
  } catch (e) {
    console.error(e);
    realtimeChatRooms.value = [];
  } finally {
    isLoading.value = false;
  }
};

// --- Existing methods (potentially modified) ---
const startDrag = (e) => {
  isDragging.value = true;
  hasMoved.value = false;
  if (isOpen.value) return;
  const clientX = e.touches ? e.touches[0].clientX : e.clientX;
  const clientY = e.touches ? e.touches[0].clientY : e.clientY;
  initialClientX.value = clientX;
  initialClientY.value = clientY;
  dragStartX.value = clientX + positionX.value;
  dragStartY.value = clientY + positionY.value;
};

const onDrag = (e) => {
  if (!isDragging.value || isOpen.value) return;
  const clientX = e.touches ? e.touches[0].clientX : e.clientX;
  const clientY = e.touches ? e.touches[0].clientY : e.clientY;
  const deltaX = Math.abs(clientX - initialClientX.value);
  const deltaY = Math.abs(clientY - initialClientY.value);
  if (deltaX > DRAG_THRESHOLD || deltaY > DRAG_THRESHOLD) {
    hasMoved.value = true;
    e.preventDefault();
    let newX = dragStartX.value - clientX;
    let newY = dragStartY.value - clientY;
    newX = Math.max(minX, Math.min(maxX, newX));
    newY = Math.max(minY, Math.min(maxY, newY));
    positionX.value = newX;
    positionY.value = newY;
  }
};

const endDrag = () => {
  if (isDragging.value && !hasMoved.value) {
    toggleChat();
  }
  isDragging.value = false;
  hasMoved.value = false;
};

onMounted(() => {
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', endDrag);
  document.addEventListener('touchmove', onDrag, { passive: false });
  document.addEventListener('touchend', endDrag);

  // Connect to WebSocket if token exists
  const token = getAccessToken();
  if (token) {
    realtimeChatStore.connect(token);
  }
});

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', endDrag);
  document.removeEventListener('touchmove', onDrag);
  document.removeEventListener('touchend', endDrag);
  
  // Disconnect WebSocket
  realtimeChatStore.disconnect();
});

const closeModal = () => { showModal.value = false; };
const openConfirm = (title, message, onConfirm) => {
  modalState.value = { title, message, type: 'confirm', onConfirm };
  showModal.value = true;
};
const openAlert = (title, message) => {
  modalState.value = { title, message, type: 'alert', onConfirm: null };
  showModal.value = true;
};
const onModalConfirm = () => {
  if (modalState.value.onConfirm) modalState.value.onConfirm();
  closeModal();
};

const menuCategories = [
  { emoji: '📅', label: '예약 문의' },
  { emoji: '💳', label: '결제 문의' },
  { emoji: '💰', label: '환불 문의' },
  { emoji: '👤', label: '계정/로그인' },
  { emoji: '⭐', label: '후기/리뷰' },
  { emoji: '🚨', label: '문제/신고' },
  { emoji: '🏠', label: '호스트 센터' }
];

const formatTime = (dateInput) => {
  if (!dateInput) return '';
  const date = new Date(dateInput);
  const now = new Date();
  const isToday = date.getDate() === now.getDate() && date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();
  if (isToday) {
    const hours = date.getHours();
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const period = hours >= 12 ? '오후' : '오전';
    const displayHours = hours % 12 === 0 ? 12 : hours % 12;
    return `${period} ${displayHours}:${minutes}`;
  } else {
    return `${date.getMonth() + 1}월 ${date.getDate()}일`;
  }
};

const getCurrentTime = () => formatTime(new Date());

const parseNumberedOptions = (text) => {
  if (!text) return [];
  const options = [];
  const regex = /(\d+)[.\)]\s*([^\n]+)/g;
  let match;
  while ((match = regex.exec(text)) !== null) {
    options.push({ number: match[1], label: match[2].trim(), fullText: match[0].trim() });
  }
  return options;
};

const getCleanBody = (text) => {
  if (!text) return '';
  return text.replace(/(\d+)[.\)]\s*([^\n]+)/g, '').trim();
};

const toggleChat = async () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    if (activeTab.value === 'chat') {
      await loadRealtimeChatRooms();
    } else {
      await loadRooms();
    }
  } else {
    // 닫힐 때 채팅방 나가기 처리 (구독 해지)
    if (activeTab.value === 'chat') {
        realtimeChatStore.leaveRoom();
        currentChatRoom.value = null;
        viewMode.value = 'list'; // 다음에 열 때 목록부터 보이게
    }
  }
};

const loadRooms = async () => {
  isLoading.value = true;
  try {
    const res = await getChatRooms();
    chatRooms.value = res.ok && res.data ? res.data : [];
  } catch (e) {
    console.error(e);
  } finally {
    isLoading.value = false;
  }
};

const startNewChat = async () => {
  isLoading.value = true;
  try {
    const res = await createChatRoom();
    if (res.ok && res.data && res.data.roomId) {
      await enterRoom(res.data.roomId);
    }
  } catch(e) {
    console.error(e);
  } finally {
    isLoading.value = false;
  }
};

const deleteRoom = (roomId, event) => {
  event.stopPropagation();
  openConfirm('대화 삭제', '정말 이 대화를 삭제하시겠습니까?', async () => {
    try {
      const res = await deleteChatRoom(roomId);
      if (res.ok) {
        await loadRooms();
        if (currentRoomId.value === roomId) {
          goBackToList();
        }
      } else {
        openAlert('오류', '삭제에 실패했습니다.');
      }
    } catch(e) {
      console.error(e);
      openAlert('오류', '오류가 발생했습니다.');
    }
  });
};

const enterRoom = async (roomId) => {
  currentRoomId.value = roomId;
  messages.value = [];
  viewMode.value = 'chat';
  isLoading.value = true;
  try {
    const res = await getRoomMessages(roomId);
    if (res.ok && res.data && res.data.length > 0) {
      for (const item of res.data) {
        const timeStr = formatTime(item.createdAt);
        if (item.fromBot) {
          const extractedOptions = parseNumberedOptions(item.message);
          const cleanBody = getCleanBody(item.message);
          const isWelcome = item.message.includes('도와드릴까요');
          messages.value.push({ type: 'bot', text: cleanBody, time: timeStr, options: extractedOptions, showMenu: isWelcome, showHome: !isWelcome });
        } else {
          messages.value.push({ type: 'user', text: item.message, time: timeStr });
        }
      }
    } else {
      messages.value.push({ type: 'bot', text: '안녕하세요 지금이곳 FAQ 챗봇 입니다. 무엇을 도와드릴까요?', time: getCurrentTime(), showMenu: true, showHome: false });
    }
  } catch(e) {
    console.error(e);
  } finally {
    isLoading.value = false;
    await nextTick();
    scrollToBottom();
  }
};

const goBackToList = () => {
  // 실시간 채팅방에서 나갈 때 구독 해지
  if (activeTab.value === 'chat') {
    realtimeChatStore.leaveRoom();
  }
  
  viewMode.value = 'list';
  currentRoomId.value = null;
  currentChatRoom.value = null; // Clear real-time chat room as well
  if (activeTab.value === 'chat') {
    loadRealtimeChatRooms();
  } else {
    loadRooms();
  }
};

const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
  }
};

const sendUserMessage = async (message) => {
  if (!message || isLoading.value || !currentRoomId.value) return;
  messages.value.push({ type: 'user', text: message, time: getCurrentTime() });
  isLoading.value = true;
  await nextTick();
  scrollToBottom();
  try {
    const res = await sendChatbotMessage(currentRoomId.value, message);
    if (res.ok && res.data) {
      const text = res.data.reply;
      const options = parseNumberedOptions(text);
      const cleanBody = getCleanBody(text);
      const isWelcome = text.includes('도와드릴까요');
      messages.value.push({ type: 'bot', text: cleanBody, options: options, time: getCurrentTime(), showMenu: isWelcome, showHome: !isWelcome });
    } else {
      messages.value.push({ type: 'bot', text: '죄송합니다. 답변을 불러오지 못했습니다.', time: getCurrentTime() });
    }
  } catch(e) {
    console.error(e);
    messages.value.push({ type: 'bot', text: '오류가 발생했습니다.', time: getCurrentTime() });
  } finally {
    isLoading.value = false;
    await nextTick();
    scrollToBottom();
  }
};

const selectOption = (label) => { sendUserMessage(label); };
const goHome = () => {
  messages.value.push({ type: 'user', text: '처음으로', time: getCurrentTime() });
  setTimeout(() => {
    messages.value.push({ type: 'bot', text: '무엇을 도와드릴까요?', time: getCurrentTime(), showMenu: true, showHome: false });
    scrollToBottom();
  }, 500);
};

</script>

<template>
  <div class="chatbot-wrapper" :style="wrapperStyle">
    <button class="chat-launcher" @mousedown="startDrag" @touchstart="startDrag" :class="{ 'is-open': isOpen, 'is-dragging': hasMoved }">
      <!-- Chat Icon (SVG) - Bubble Style -->
      <svg v-if="!isOpen" width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M20 2H4C2.9 2 2 2.9 2 4V22L6 18H20C21.1 18 22 17.1 22 16V4C22 2.9 21.1 2 20 2ZM20 16H6L4 18V4H20V16Z" fill="#0f766e"/>
        <circle cx="8" cy="10" r="1.5" fill="white"/>
        <circle cx="12" cy="10" r="1.5" fill="white"/>
        <circle cx="16" cy="10" r="1.5" fill="white"/>
      </svg>
      <span v-if="!isOpen && totalUnreadCount > 0" class="notification-badge">{{ totalUnreadCount }}</span>
      <span v-if="isOpen" class="close-icon">✕</span>
    </button>
    <div v-if="isOpen" class="chatbot-window">
      <div class="chatbot-header">
        <button v-if="viewMode === 'chat'" @click="goBackToList" class="back-btn">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M15 18L9 12L15 6" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
        <div v-else class="tab-buttons">
          <button @click="switchTab('faq')" :class="{ active: activeTab === 'faq' }" class="tab-btn">FAQ</button>
          <button @click="switchTab('chat')" :class="{ active: activeTab === 'chat' }" class="tab-btn">채팅방</button>
        </div>
        <span v-if="viewMode === 'chat'" class="header-title">{{ activeTab === 'faq' ? '지금이곳' : currentChatRoom?.accommodationName || '채팅' }}</span>
        <button class="close-btn" @click="toggleChat">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M18 6L6 18M6 6L18 18" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
      </div>
      <div v-if="viewMode === 'list'" class="room-list-container">
        <template v-if="activeTab === 'faq'">
          <div v-if="isLoading && chatRooms.length === 0" class="loading-bubble"><span></span><span></span><span></span></div>
          <div v-else-if="chatRooms.length === 0" class="empty-state"><p>문의 내역이 없습니다.</p><p class="sub-text">새 문의하기를 눌러 대화를 시작하세요.</p></div>
          <div v-else class="room-list">
            <div v-for="room in chatRooms" :key="room.id" class="room-item" @click="enterRoom(room.id)">
              <div class="room-icon"><img src="/icon.png" alt="Bot" /></div>
              <div class="room-info">
                <div class="room-sender">지금이곳</div>
                <div class="room-msg">{{ room.lastMessage }}</div>
                <div class="room-time">{{ formatTime(room.updatedAt) }}</div>
              </div>
              <button class="room-delete-btn" @click="(e) => deleteRoom(room.id, e)">✕</button>
            </div>
          </div>
          <div class="new-chat-container"><button class="new-chat-btn" @click="startNewChat">새 문의하기 ▼</button></div>
        </template>
        <template v-if="activeTab === 'chat'">
            <div v-if="isLoading && realtimeChatRooms.length === 0" class="loading-bubble"><span></span><span></span><span></span></div>
            <div v-else-if="realtimeChatRooms.length === 0" class="empty-state"><p>진행중인 대화가 없습니다</p><p class="sub-text">숙소 예약 후 호스트와 대화를 시작할 수 있습니다.</p></div>
            <div v-else class="room-list">
                <div v-for="room in realtimeChatRooms" :key="room.id" class="room-item" @click="enterRealtimeChatRoom(room)">
                    <div class="room-icon"><img :src="getOtherUserProfileImage(room)" alt="프로필" /></div>
                    <div class="room-info">
                        <div class="room-sender">{{ room.accommodationName }}</div>
                        <div class="room-subtitle">{{ isHost(room) ? '게스트: ' + (room.guestName || '게스트') : '호스트: ' + (room.hostName || '호스트') }}</div>
                        <div class="room-msg">{{ room.lastMessage || '아직 메시지가 없습니다' }}</div>
                        <div class="room-time">{{ formatTime(room.lastMessageTime) }}</div>
                    </div>
                    <span v-if="getUnreadCount(room) > 0" class="badge">{{ getUnreadCount(room) }}</span>
                </div>
            </div>
        </template>
      </div>
      <div v-else-if="viewMode === 'chat'" class="chat-container">
        <div class="messages-area" ref="chatContainer">
            <!-- Chatbot Messages -->
            <template v-if="activeTab === 'faq'">
                <div v-for="(msg, index) in messages" :key="`bot-${index}`" class="message-row" :class="msg.type">
                    <div v-if="msg.type === 'bot'" class="bot-container"><div class="bot-profile"><img src="/icon.png" alt="Bot" /></div><div class="bot-content"><div class="bot-name">지금이곳</div><div class="message-bubble bot-bubble"><div class="message-text" style="white-space: pre-wrap;">{{ msg.text }}</div></div><div v-if="msg.showMenu" class="menu-grid"><button v-for="(cat, idx) in menuCategories" :key="idx" class="menu-btn" @click="selectOption(cat.label)"><span class="menu-emoji">{{ cat.emoji }}</span><span class="menu-label">{{ cat.label }}</span></button></div><div v-if="msg.options && msg.options.length > 0" class="options-list"><button v-for="(opt, idx) in msg.options" :key="idx" class="option-btn" @click="selectOption(opt.fullText)">{{ opt.fullText }}</button></div><div v-if="msg.showHome" class="home-btn-container"><button class="home-btn" @click="goHome">처음으로</button></div><div class="message-time">{{ msg.time }}</div></div></div>
                    <div v-else class="user-container"><div class="message-bubble user-bubble">{{ msg.text }}</div><div class="message-time">{{ msg.time }}</div></div>
                </div>
            </template>
            <!-- Real-time Chat Messages -->
            <template v-if="activeTab === 'chat'">
                <div v-for="msg in currentMessages" :key="`rt-${msg.id}`" class="message-row" :class="isMyMessage(msg) ? 'user' : 'bot'">
                    <!-- 상대방(게스트 또는 호스트)의 메시지 -->
                    <div v-if="!isMyMessage(msg)" class="bot-container">
                        <div class="bot-profile">
                            <img :src="getOtherUserProfileImage(currentChatRoom)" alt="상대방" />
                        </div>
                        <div class="bot-content">
                            <div class="bot-name">{{ msg.senderName || '상대방' }}</div>
                            <div class="message-bubble bot-bubble"><div class="message-text" style="white-space: pre-wrap;">{{ msg.messageContent }}</div></div>
                            <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
                        </div>
                    </div>
                    <!-- 내가 보낸 메시지 -->
                    <div v-else class="user-container">
                        <div class="message-bubble user-bubble">{{ msg.messageContent }}</div>
                        <div class="message-meta">
                            <span v-if="msg.readByRecipient === false" class="unread-indicator">1</span>
                            <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
                        </div>
                    </div>
                </div>
            </template>
            <div v-if="isLoading" class="loading-bubble"><span>.</span><span>.</span><span>.</span></div>
        </div>
        <div class="message-input-area">
             <template v-if="activeTab === 'chat'">
                <input type="text" v-model="messageInput" @keydown.enter="sendRealtimeMessage" placeholder="메시지를 입력하세요..." />
                <button @click="sendRealtimeMessage" :disabled="!messageInput.trim()">전송</button>
             </template>
        </div>
      </div>
      <div v-if="showModal" class="modal-overlay">
          <div class="modal-content"><div class="modal-title">{{ modalState.title }}</div><div class="modal-desc">{{ modalState.message }}</div><div class="modal-actions"><button v-if="modalState.type === 'confirm'" class="modal-btn cancel" @click="closeModal">취소</button><button class="modal-btn confirm" @click="onModalConfirm">확인</button></div></div>
      </div>
    </div>
  </div>
</template>


<style scoped>
/* General Wrapper */
.chatbot-wrapper{position:fixed;bottom:160px;right:35px;z-index:9999;font-family:'Pretendard',sans-serif}.chat-launcher{width:50px;height:50px;border-radius:50%;background:white;border:1px solid #ddd;box-shadow:0 4px 12px rgba(0,0,0,0.15);cursor:grab;display:flex;align-items:center;justify-content:center;transition:transform .2s,box-shadow .2s;user-select:none;touch-action:none}.chat-launcher:hover{transform:scale(1.05)}.chat-launcher.is-dragging{cursor:grabbing;box-shadow:0 8px 20px rgba(0,0,0,0.25);transform:scale(1.1)}.chat-launcher img{width:24px;height:24px;pointer-events:none}.close-icon{font-size:24px;color:#333}.chatbot-window{position:absolute;bottom:70px;right:0;width:380px;height:500px;max-height:calc(100vh - 180px);background:#fff;border-radius:20px;box-shadow:0 8px 30px rgba(0,0,0,0.12);display:flex;flex-direction:column;overflow:hidden;border:1px solid #eee}
.notification-badge{position:absolute;top:-5px;right:-5px;background-color:red;color:white;border-radius:50%;width:24px;height:24px;font-size:12px;display:flex;align-items:center;justify-content:center;font-weight:bold;border:2px solid white}

/* Header */
.chatbot-header{height:60px;display:flex;align-items:center;justify-content:space-between;padding:0 20px;border-bottom:1px solid #f0f0f0;background:#fff;flex-shrink:0;position:relative}.header-title{font-weight:700;font-size:18px;position:absolute;left:50%;transform:translateX(-50%)}.back-btn,.close-btn{background:none;border:none;cursor:pointer;padding:0;display:flex;align-items:center;z-index:2}.close-btn{display:none}

/* Tab Styles */
.tab-buttons{display:flex;gap:12px;position:absolute;left:50%;transform:translateX(-50%)}.tab-btn{background:none;border:none;padding:8px 16px;font-size:16px;font-weight:600;color:#999;cursor:pointer;position:relative;transition:color .2s}.tab-btn.active{color:#333}.tab-btn.active::after{content:'';position:absolute;bottom:-13px;left:0;right:0;height:3px;background:#0f766e;border-radius:3px 3px 0 0}

/* Room List */
.room-list-container{flex:1;display:flex;flex-direction:column;background:#fff;overflow-y:auto}.room-list{padding:10px 0}.room-item{display:flex;padding:16px 20px;cursor:pointer;transition:background .2s;border-bottom:1px solid #f9f9f9;align-items:center}.room-item:hover{background:#f9f9f9}.room-item:hover .room-delete-btn{opacity:1}.room-icon img{width:48px;height:48px;border-radius:50%;margin-right:16px;border:1px solid #eee}.room-info{flex:1;overflow:hidden;display:flex;flex-direction:column;justify-content:center}.room-sender{font-weight:700;font-size:15px;margin-bottom:4px}.room-msg{font-size:14px;color:#666;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-bottom:4px}.room-time{font-size:12px;color:#999}.empty-state{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;color:#999;padding:20px}.empty-state .sub-text{font-size:14px;color:#aaa;margin-top:4px}.new-chat-container{padding:20px;text-align:center;background:linear-gradient(to top,#fff 80%,transparent);position:sticky;bottom:0;left:0;right:0}.new-chat-btn{background:#0f766e;color:white;border:none;padding:12px 24px;border-radius:30px;font-size:15px;font-weight:600;cursor:pointer;box-shadow:0 4px 10px rgba(15,118,110,.3);transition:background .2s}.new-chat-btn:hover{background:#0d6e66}.room-delete-btn{opacity:0;background:none;border:none;color:#999;font-size:16px;cursor:pointer;padding:0 8px;transition:opacity .2s,color .2s}.room-delete-btn:hover{color:#ff4444}

/* Real-time chat specific styles */
.badge{background:#ff4444;color:white;font-size:12px;font-weight:700;padding:2px 8px;border-radius:12px;min-width:20px;text-align:center}.room-subtitle{font-size:12px;color:#999;margin-bottom:4px}

/* Chat View */
.chat-container{flex:1;display:flex;flex-direction:column;overflow:hidden;background:#fff}.messages-area{flex:1;overflow-y:auto;padding:20px}.message-row.user{display:flex;justify-content:flex-end}.message-row.bot{display:flex;justify-content:flex-start}.bot-container{display:flex;gap:12px;margin-bottom:20px}.bot-profile img{width:40px;height:40px;border-radius:50%;border:1px solid #eee}.bot-content{flex:1;max-width:85%}.bot-name{font-size:13px;font-weight:700;margin-bottom:6px;color:#333}.message-bubble{padding:12px 16px;border-radius:16px;font-size:15px;line-height:1.5;word-break:keep-all;white-space:pre-wrap}.bot-bubble{background:#f2f2f2;border-top-left-radius:4px;color:#333}.user-container{display:flex;align-items:flex-end;gap:8px;margin-bottom:20px;justify-content:flex-end;width:100%}.user-bubble{background:#333;color:white;border-bottom-right-radius:4px;max-width:calc(100% - 70px);order:1}.message-meta{display:flex;flex-direction:column;align-items:flex-end;justify-content:flex-end;flex-shrink:0;order:0}.message-time{font-size:11px;color:#bbb}.unread-indicator{font-size:12px;font-weight:700;color:#0f766e;margin-bottom:4px}

/* Input Area */
.message-input-area{padding:10px 20px;border-top:1px solid #f0f0f0;background:#fff;display:flex;gap:10px;align-items:center}.message-input-area input{flex:1;border:1px solid #ddd;border-radius:20px;padding:10px 16px;font-size:15px;outline:none;transition:border-color .2s}.message-input-area input:focus{border-color:#0f766e}.message-input-area button{background:#0f766e;color:white;border:none;border-radius:50%;width:40px;height:40px;display:flex;align-items:center;justify-content:center;cursor:pointer;font-size:14px;font-weight:600;transition:background .2s}.message-input-area button:disabled{background:#ccc;cursor:not-allowed}.message-input-area button:hover:not(:disabled){background:#0d6e66}

/* Menu/Options */
.menu-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:8px;margin-top:10px}.menu-btn{background:white;border:1px solid #eee;border-radius:12px;padding:12px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;cursor:pointer;transition:all .2s}.menu-btn:hover{border-color:#333;transform:translateY(-2px)}.menu-emoji{font-size:20px}.menu-label{font-size:13px;font-weight:500}.options-list{display:flex;flex-direction:column;gap:8px;margin-top:10px}.option-btn{background:white;border:1px solid #ddd;border-radius:20px;padding:10px 16px;text-align:left;font-size:14px;cursor:pointer;color:#333;transition:all .2s}.option-btn:hover{background:#f9f9f9;border-color:#999}.home-btn-container{margin-top:10px}.home-btn{background:white;border:1px solid #ddd;border-radius:20px;padding:10px 16px;text-align:center;font-size:14px;cursor:pointer;color:#333;width:100%;transition:all .2s}.home-btn:hover{background:#f9f9f9;border-color:#999}

/* Modal */
.modal-overlay{position:absolute;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,.4);display:flex;align-items:center;justify-content:center;z-index:100;border-radius:20px}.modal-content{background:white;padding:24px;border-radius:16px;width:80%;text-align:center;box-shadow:0 4px 20px rgba(0,0,0,.15);animation:modalPop .2s ease-out}@keyframes modalPop{from{transform:scale(.9);opacity:0}to{transform:scale(1);opacity:1}}.modal-title{font-weight:700;font-size:16px;margin-bottom:8px;color:#333}.modal-desc{font-size:14px;color:#666;margin-bottom:20px;white-space:pre-wrap}.modal-actions{display:flex;gap:10px}.modal-btn{flex:1;padding:12px 0;border-radius:12px;border:none;font-size:14px;font-weight:600;cursor:pointer;transition:opacity .2s}.modal-btn.cancel{background:#f5f5f5;color:#666}.modal-btn.confirm{background:#333;color:white}.modal-btn:hover{opacity:.9}

/* Mobile */
@media (max-width:480px){.chatbot-wrapper{bottom:16px;right:16px}.chat-launcher{width:56px;height:56px}.chat-launcher img{width:28px;height:28px}.close-btn{display:flex}.chatbot-window{position:fixed;top:0;left:0;width:100%;height:100%;bottom:0;right:0;border-radius:0;max-height:none}}

/* Loading Bubble */
.loading-bubble{display:flex;justify-content:center;align-items:center;gap:6px;padding:20px;margin:0 auto}.loading-bubble span{width:6px;height:6px;background:#333;border-radius:50%;display:inline-block;animation:bounce 1.4s infinite ease-in-out both}.loading-bubble span:nth-child(1){animation-delay:-.32s}.loading-bubble span:nth-child(2){animation-delay:-.16s}.loading-bubble span:nth-child(3){color:transparent}@keyframes bounce{0%,80%,100%{transform:scale(0);opacity:.5}40%{transform:scale(1);opacity:1}}
</style>
