import io from 'socket.io-client';  // 修改导入语句
import { useAuthStore } from '../store/useAuthStore';

class WebSocketClient {
    constructor() {
        this.socket = null;
        this.messageHandlers = new Set();
        this.connectionStatus = 'disconnected'; // 新增：连接状态跟踪
    }

    connect() {
        const authUser = useAuthStore.getState().authUser;
        console.log('WebSocket connect - Current auth user:', authUser);

        if (!authUser?.token) {
            console.error('No authenticated user found');
            return;
        }

        try {
            console.log('Connecting to socket with user:', authUser.id);
            
            this.socket = io('http://localhost:19098', {
                transports: ['websocket'],
                withCredentials: true,
                reconnection: true,
                reconnectionAttempts: 5,
                reconnectionDelay: 1000,
                timeout: 20000,
                auth: {
                    token: authUser.token
                },
                query: {
                    token: authUser.token
                },
                extraHeaders: {
                    'Authorization': `Bearer ${authUser.token}`
                }
            });

            this.socket.on('connect', () => {
                console.log('Socket.IO connected successfully');
                this.connectionStatus = 'connected';
                
                // 发送认证消息
                this.socket.emit('authenticate', { token: authUser.token }, (response) => {
                    if (response.success) {
                        console.log('Socket authenticated successfully');
                    } else {
                        console.error('Socket authentication failed:', response.error);
                    }
                });
            });

            this.socket.on('connect_error', (error) => {
                console.error('Socket.IO connection error:', error);
                this.connectionStatus = 'error';
            });

            this.socket.on('disconnect', (reason) => {
                console.log('Socket.IO disconnected:', reason);
                this.connectionStatus = 'disconnected';
                
                // 如果是服务器主动断开，尝试重连
                if (reason === 'io server disconnect') {
                    setTimeout(() => {
                        this.connect();
                    }, 1000);
                }
            });

            this.socket.on('error', (error) => {
                console.error('Socket.IO error:', error);
                this.connectionStatus = 'error';
            });

            this.socket.on('message', (message) => {
                console.log('Received message:', message);
                // 确保消息有正确的格式
                const formattedMessage = {
                    ...message,
                    _id: message.messageId || message._id,
                    createdAt: message.createdAt || new Date(message.timestamp).toISOString(),
                    timestamp: message.timestamp || Date.now()
                };
                this.messageHandlers.forEach(handler => handler(formattedMessage));
            });

            this.socket.on('onlineUsers', (users) => {
                console.log('Online users updated:', users);
                useAuthStore.getState().setOnlineUsers(users);
            });

        } catch (error) {
            console.error('Failed to initialize Socket.IO:', error);
            this.connectionStatus = 'error';
        }
    }

    joinRoom(roomId) {
        if (!this.socket || this.connectionStatus !== 'connected') {
            console.error('Socket is not connected');
            return Promise.reject(new Error('Socket is not connected'));
        }

        return new Promise((resolve, reject) => {
            this.socket.emit('join', { roomId }, (response) => {
                if (response.success) {
                    console.log('Joined room:', roomId);
                    resolve(response);
                } else {
                    console.error('Failed to join room:', response.error);
                    reject(new Error(response.error || 'Failed to join room'));
                }
            });
        });
    }

    leaveRoom(roomId) {
        if (!this.socket || this.connectionStatus !== 'connected') {
            console.error('Socket is not connected');
            return Promise.reject(new Error('Socket is not connected'));
        }

        return new Promise((resolve, reject) => {
            this.socket.emit('leave', { roomId }, (response) => {
                if (response.success) {
                    console.log('Left room:', roomId);
                    resolve(response);
                } else {
                    console.error('Failed to leave room:', response.error);
                    reject(new Error(response.error || 'Failed to leave room'));
                }
            });
        });
    }

    sendMessage(message) {
        if (!this.socket || this.connectionStatus !== 'connected') {
            console.error('Socket is not connected');
            return Promise.reject(new Error('Socket is not connected'));
        }

        return new Promise((resolve, reject) => {
            const messageWithTimestamp = {
                ...message,
                timestamp: Date.now()
            };
            
            this.socket.emit('message', messageWithTimestamp, (response) => {
                if (response.success) {
                    console.log('Message sent successfully:', messageWithTimestamp);
                    resolve({
                        ...response,
                        message: {
                            ...messageWithTimestamp,
                            _id: response.messageId,
                            messageId: response.messageId,
                            createdAt: new Date().toISOString()
                        }
                    });
                } else {
                    console.error('Failed to send message:', response.error);
                    reject(new Error(response.error || 'Failed to send message'));
                }
            });
        });
    }

    addMessageHandler(handler) {
        this.messageHandlers.add(handler);
    }

    removeMessageHandler(handler) {
        this.messageHandlers.delete(handler);
    }

    disconnect() {
        if (this.socket) {
            this.socket.disconnect();
            this.socket = null;
            this.connectionStatus = 'disconnected';
        }
    }

    getConnectionStatus() {
        return this.connectionStatus;
    }
}

const websocketClient = new WebSocketClient();
export default websocketClient;