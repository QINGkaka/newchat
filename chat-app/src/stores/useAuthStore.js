import { create } from 'zustand';
import { io } from 'socket.io-client';

const SOCKET_URL = 'http://localhost:19098';

const useAuthStore = create((set, get) => {
  let socket = null;
  let heartbeatTimer = null;

  const clearConnection = () => {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer);
      heartbeatTimer = null;
    }
    if (socket) {
      socket.removeAllListeners();
      socket.disconnect();
      socket = null;
    }
  };

  return {
    user: null,
    onlineUsers: [],
    isAuthenticated: false,
    
    setUser: (user) => set({ user, isAuthenticated: !!user }),
    
    connectSocket: (user) => {
      if (!user) return;
      
      // 清理现有连接
      clearConnection();
      
      try {
        // 创建新连接
        socket = io(SOCKET_URL, {
          auth: { token: localStorage.getItem('token') },
          transports: ['websocket'],
          reconnection: false,
          forceNew: true,
          autoConnect: false,
          timeout: 10000,
          query: { userId: user.id }
        });
        
        // 连接事件处理
        socket.on('connect', () => {
          console.log('Socket connected');
          
          // 设置心跳
          heartbeatTimer = setInterval(() => {
            if (socket?.connected) {
              socket.emit('ping');
            }
          }, 20000);
          
          // 请求在线用户列表
          socket.emit('getOnlineUsers');
        });
        
        socket.on('disconnect', () => {
          console.log('Socket disconnected');
          clearConnection();
        });
        
        socket.on('error', (error) => {
          console.error('Socket error:', error);
          if (error.error === 'ALREADY_CONNECTED') {
            clearConnection();
          }
        });
        
        socket.on('onlineUsers', (users) => {
          console.log('Received online users:', users);
          set({ onlineUsers: users });
        });
        
        socket.on('userStatus', (data) => {
          const { onlineUsers } = get();
          const updatedUsers = onlineUsers.map(user => 
            user._id === data.userId 
              ? { ...user, online: data.online }
              : user
          );
          set({ onlineUsers: updatedUsers });
        });
        
        // 开始连接
        socket.connect();
        
      } catch (error) {
        console.error('Failed to create socket connection:', error);
        clearConnection();
      }
    },
    
    disconnectSocket: () => {
      clearConnection();
      set({ onlineUsers: [] });
    },
    
    getSocket: () => socket,
    
    isSocketConnected: () => socket?.connected || false
  };
});

export default useAuthStore; 