import { useAuthStore, globalSocket } from '../store/useAuthStore';

class WebSocketClient {
    constructor() {
        this.messageHandlers = new Set();
    }

    // 添加connect方法
    connect() {
        const store = useAuthStore.getState();
        console.log('Attempting to connect WebSocket...');
        
        // 如果已经连接，直接返回
        if (store.socketConnected) {
            console.log('WebSocket already connected');
            return true;
        }

        // 如果正在连接中，等待连接完成
        if (store.isConnecting) {
            console.log('WebSocket connection in progress');
            return false;
        }

        // 尝试建立新连接
        console.log('Initializing new WebSocket connection');
        store.connectSocket();
        return store.socketConnected;
    }

    // 添加disconnect方法
    disconnect() {
        const store = useAuthStore.getState();
        console.log('Disconnecting WebSocket...');
        if (store.socketConnected) {
            store.disconnectSocket();
        }
    }

    // 获取连接状态
    getConnectionStatus() {
        const store = useAuthStore.getState();
        if (store.isConnecting) return 'connecting';
        return store.socketConnected ? 'connected' : 'disconnected';
    }

    // 获取全局 socket 实例
    getSocket() {
        return globalSocket;
    }

    // 检查连接状态
    isConnected() {
        const store = useAuthStore.getState();
        return store.socketConnected && globalSocket?.connected;
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
                senderName: authUser.fullName || authUser.email,
                type: message.image ? 'image' : 'message'
            };
            
            console.log('Sending chat message:', messageWithTimestamp);
            socket.emit('sendMessage', messageWithTimestamp, (response) => {
                if (response?.success) {
                    console.log('Message sent successfully:', response);
                    resolve({
                        ...response,
                        message: {
                            ...messageWithTimestamp,
                            _id: response.messageId,
                            messageId: response.messageId,
                            createdAt: new Date().toISOString(),
                            content: message.content || null,
                            text: message.content || null,
                            image: message.image || null
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
        console.log('socket is: ', socket);
        if (socket) {
            socket.off('newMessage');
            
            socket.on('newMessage', (message) => {
                console.log('Received chat message:', message);
                try {
                    const formattedMessage = {
                        ...message,
                        _id: message.messageId || message._id,
                        createdAt: message.createdAt || new Date(message.timestamp).toISOString(),
                        timestamp: message.timestamp || Date.now(),
                        content: message.content || null,
                        text: message.content || null,
                        image: message.image || null
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
            socket.off('newMessage', handler);
        }
        this.messageHandlers.delete(handler);
    }
}

const websocketClient = new WebSocketClient();
export default websocketClient;
