import { useAuthStore } from '../store/useAuthStore';

class WebSocketClient {
    constructor() {
        this.messageHandlers = new Set();
    }

    // 添加connect方法
    connect() {
        const store = useAuthStore.getState();
        if (!store.socketConnected) {
            store.connectSocket();
        }
        return store.socketConnected;
    }

    // 添加disconnect方法
    disconnect() {
        const store = useAuthStore.getState();
        if (store.socketConnected) {
            store.disconnectSocket();
        }
    }

    // 获取连接状态
    getConnectionStatus() {
        return this.isConnected() ? 'connected' : 'disconnected';
    }

    // 获取全局 socket 实例
    getSocket() {
        const store = useAuthStore.getState();
        return store.globalSocket;
    }

    // 检查连接状态
    isConnected() {
        const store = useAuthStore.getState();
        return store.socketConnected;
    }

    joinRoom(roomId) {
        const socket = this.getSocket();
        if (!socket || !this.isConnected()) {
            console.error('Socket is not connected');
            return Promise.reject(new Error('Socket is not connected'));
        }

        return new Promise((resolve, reject) => {
            socket.emit('join', { roomId }, (response) => {
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
        const socket = this.getSocket();
        if (!socket || !this.isConnected()) {
            console.error('Socket is not connected');
            return Promise.reject(new Error('Socket is not connected'));
        }

        return new Promise((resolve, reject) => {
            socket.emit('leave', { roomId }, (response) => {
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
        const socket = this.getSocket();
        if (!socket || !this.isConnected()) {
            console.error('Socket is not connected');
            return Promise.reject(new Error('Socket is not connected'));
        }

        return new Promise((resolve, reject) => {
            const authUser = useAuthStore.getState().authUser;
            if (!authUser) {
                console.error('No authenticated user found');
                reject(new Error('No authenticated user found'));
                return;
            }

            const messageWithTimestamp = {
                ...message,
                timestamp: Date.now(),
                senderId: authUser.id,
                senderName: authUser.name || authUser.email
            };
            
            console.log('Sending chat message:', messageWithTimestamp);
            socket.emit('chatMessage', messageWithTimestamp, (response) => {
                if (response?.success) {
                    console.log('Message sent successfully:', response);
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
                    const error = response?.error || 'Unknown error';
                    console.error('Failed to send message:', error);
                    reject(new Error(error));
                }
            });
        });
    }

    addMessageHandler(handler) {
        const socket = this.getSocket();
        if (socket) {
            // 移除之前的监听器
            socket.off('chatMessage');
            
            // 添加新的消息监听器
            socket.on('chatMessage', (message) => {
                console.log('Received chat message:', message);
                try {
                    const formattedMessage = {
                        ...message,
                        _id: message.messageId || message._id,
                        createdAt: message.createdAt || new Date(message.timestamp).toISOString(),
                        timestamp: message.timestamp || Date.now(),
                        // 确保发送者信息存在
                        sender: message.sender || {
                            id: message.senderId,
                            name: message.senderName
                        }
                    };
                    console.log('Formatted message:', formattedMessage);
                    handler(formattedMessage);
                } catch (error) {
                    console.error('Error processing message:', error, message);
                }
            });
        }
        this.messageHandlers.add(handler);
    }

    removeMessageHandler(handler) {
        const socket = this.getSocket();
        if (socket) {
            socket.off('message', handler);
        }
        this.messageHandlers.delete(handler);
    }
}

const websocketClient = new WebSocketClient();
export default websocketClient;
