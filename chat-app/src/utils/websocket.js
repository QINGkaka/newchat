import useAuthStore from '../stores/useAuthStore';

// 连接 WebSocket
export const connectSocket = (user) => {
  if (!user) return;
  useAuthStore.getState().connectSocket(user);
};

// 断开 WebSocket 连接
export const disconnectSocket = () => {
  useAuthStore.getState().disconnectSocket();
};

// 获取当前 WebSocket 实例
export const getSocket = () => useAuthStore.getState().socket;

// 获取在线用户列表
export const getOnlineUsers = () => useAuthStore.getState().onlineUsers;