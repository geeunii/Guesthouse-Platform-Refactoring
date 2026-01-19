import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getUserInfo } from '@/api/authClient'

export const useRealtimeChatStore = defineStore('realtimeChat', {
    state: () => ({
        stompClient: null,
        connected: false,
        currentRoomId: null,
        messages: [],
        subscription: null,
        notificationSubscription: null, // 사용자별 알림 구독
        reconnectAttempts: 0,
        token: null,
        // 실시간 알림으로 받은 채팅방 업데이트 정보
        roomNotifications: [],
    }),

    actions: {
        connect(token) {
            return new Promise((resolve, reject) => {
                if (this.connected && this.stompClient && this.stompClient.active) {
                    console.log('WebSocket is already connected.');
                    return resolve();
                }

                if (!token) {
                    const errorMessage = 'Authentication token not found, WebSocket connection aborted.';
                    console.error(errorMessage);
                    alert('로그인이 필요합니다.');
                    return reject(new Error(errorMessage));
                }
                this.token = token;

                const wsUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin;
                const socket = new SockJS(`${wsUrl}/ws-chat`);

                this.stompClient = new Client({
                    webSocketFactory: () => socket,
                    connectHeaders: { Authorization: `Bearer ${this.token}` },
                    heartbeatIncoming: 10000, // 서버로부터 10초마다 heartbeat 수신
                    heartbeatOutgoing: 10000, // 서버로 10초마다 heartbeat 전송
                    reconnectDelay: 5000, // 자동 재연결 딜레이
                    debug: (str) => {
                        console.log('[STOMP Debug]', str);
                    },
                    onConnect: (frame) => {
                        console.log('STOMP Connected! Frame:', frame);
                        this.connected = true;
                        this.reconnectAttempts = 0;
                        console.log('WebSocket connected successfully. Connected state:', this.connected);

                        // 사용자별 알림 채널 구독
                        const currentUser = getUserInfo();
                        const userId = currentUser?.userId || currentUser?.id;
                        if (userId) {
                            this.subscribeToNotifications(userId);
                        }

                        // 연결 안정화를 위해 약간의 딜레이 후 구독
                        if (this.currentRoomId) {
                            setTimeout(() => {
                                this.subscribeToRoom(this.currentRoomId);
                            }, 100);
                        }

                        resolve();
                    },
                    onStompError: (frame) => {
                        console.error('STOMP error occurred');
                        console.error('Error headers:', frame.headers);
                        console.error('Error body:', frame.body);
                        console.error('Error message:', frame.headers['message']);
                        this.connected = false;
                        // STOMP 클라이언트의 자동 재연결 기능에 맡김 (reconnectDelay 설정됨)
                        // 수동 재연결 시도 제거하여 이중 재연결 방지
                    },
                    onWebSocketError: (error) => {
                        console.error('WebSocket error:', error);
                        this.connected = false;
                        reject(error);
                    },
                    onWebSocketClose: (event) => {
                        console.log('WebSocket closed:', event);
                        this.connected = false;
                    },
                    onDisconnect: () => {
                        this.connected = false;
                        console.log('WebSocket disconnected.');
                    }
                });

                console.log('Activating STOMP client...');
                this.stompClient.activate();
            });
        },

        subscribeToRoom(roomId) {
            console.log(`Attempting to subscribe to room ${roomId}. Connected: ${this.connected}, Client active: ${this.stompClient?.active}`);

            if (!this.connected || !this.stompClient || !this.stompClient.active) {
                console.error('Cannot subscribe, not connected. Connected:', this.connected, 'Client:', !!this.stompClient, 'Active:', this.stompClient?.active);
                return;
            }

            this.currentRoomId = roomId;
            this.messages = [];

            if (this.subscription) {
                console.log('Unsubscribing from previous room');
                this.subscription.unsubscribe();
            }

            try {
                this.subscription = this.stompClient.subscribe(
                    `/topic/chatroom/${roomId}`,
                    (message) => {
                        console.log('Received message:', message);
                        const data = JSON.parse(message.body);
                        const currentUser = getUserInfo();

                        // 현재 사용자 ID 가져오기 (userId 또는 id)
                        const myId = currentUser?.userId || currentUser?.id;

                        // 읽음 알림 처리 (type이 MESSAGES_READ인 경우)
                        if (data.type === 'MESSAGES_READ') {
                            console.log('Received read receipt:', data);
                            // 읽은 사람이 나 자신이 아닌 경우에만 처리 (상대방이 읽었을 때)
                            if (myId && Number(data.readerId) !== Number(myId)) {
                                // 내가 보낸 메시지들을 읽음 처리 - 배열을 새로 생성하여 반응성 트리거
                                this.messages = this.messages.map(msg => {
                                    if (Number(msg.senderUserId) === Number(myId)) {
                                        return { ...msg, readByRecipient: true };
                                    }
                                    return msg;
                                });
                                console.log('Updated messages with read status');
                            }
                            return;
                        }

                        // 일반 메시지 처리
                        // Initialize readByRecipient for new messages (내가 보낸 메시지는 false, 상대방 메시지는 undefined)
                        data.readByRecipient = myId && Number(data.senderUserId) === Number(myId) ? false : undefined;
                        this.messages.push(data);
                    },
                    (error) => {
                        console.error('Subscription error:', error);
                    }
                );
                console.log(`Successfully subscribed to room: ${roomId}`);
            } catch (error) {
                console.error('Error during subscription:', error);
            }
        },

        // 사용자별 알림 채널 구독
        subscribeToNotifications(userId) {
            if (!this.connected || !this.stompClient || !this.stompClient.active) {
                console.error('Cannot subscribe to notifications, not connected.');
                return;
            }

            if (this.notificationSubscription) {
                console.log('Unsubscribing from previous notification channel');
                this.notificationSubscription.unsubscribe();
            }

            try {
                this.notificationSubscription = this.stompClient.subscribe(
                    `/topic/user/${userId}/notifications`,
                    (message) => {
                        console.log('Received notification:', message);
                        const data = JSON.parse(message.body);

                        if (data.type === 'NEW_MESSAGE') {
                            console.log('New message notification:', data);
                            // 채팅방 목록 업데이트를 위한 알림 저장
                            this.roomNotifications.push(data);
                        }
                    }
                );
                console.log(`Successfully subscribed to notifications for user: ${userId}`);
            } catch (error) {
                console.error('Error subscribing to notifications:', error);
            }
        },

        // 알림 목록 초기화
        clearNotifications() {
            this.roomNotifications = [];
        },

        sendMessage(roomId, content) {
            console.log('Attempting to send message. Connected:', this.connected, 'Client active:', this.stompClient?.active);

            if (!this.connected || !this.stompClient || !this.stompClient.active) {
                console.error('Cannot send message, not connected. Connected:', this.connected, 'Client:', !!this.stompClient, 'Active:', this.stompClient?.active);
                alert('채팅 서버에 연결되어 있지 않습니다. 페이지를 새로고침하거나 다시 로그인해주세요.');
                return;
            }

            console.log('Publishing message to:', `/app/chat/${roomId}/send`);
            this.stompClient.publish({
                destination: `/app/chat/${roomId}/send`,
                body: JSON.stringify({ content })
            });
            console.log('Message published successfully');
        },

        leaveRoom() {
            if (this.subscription) {
                this.subscription.unsubscribe();
                this.subscription = null;
            }
            this.currentRoomId = null;
            this.messages = [];
            console.log('Left chat room');
        },

        disconnect() {
            if (this.stompClient) {
                this.stompClient.deactivate();
            }
            this.connected = false;
            this.currentRoomId = null;
            this.subscription = null;
            this.notificationSubscription = null;
            this.messages = [];
            this.roomNotifications = [];
            this.token = null;
            this.reconnectAttempts = 0;
        }
    }
})
