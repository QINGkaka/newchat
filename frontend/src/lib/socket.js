import { io } from 'socket.io-client';

// 修改为 Java 后端的 Socket.IO 地址
const socket = io('http://localhost:19098', {
  autoConnect: false,
  withCredentials: true,
  reconnection: true,
  reconnectionAttempts: Infinity, // 无限重试
  reconnectionDelay: 1000,
  reconnectionDelayMax: 5000,
  timeout: 20000,
  forceBase64: false,
  jsonp: false,
  rememberUpgrade: true, // 记住升级
  transports: ['websocket'],
  upgrade: false, // 禁用传输升级
  extraHeaders: {
    "Authorization": `Bearer ${localStorage.getItem('token') || ''}`
  },
  query: {
    token: localStorage.getItem('token') || ''
  }
});

// 添加连接事件监听
socket.on('connect', () => {
  console.log('Connected to Socket.IO server');
});

socket.on('connect_error', (error) => {
  console.error('Connection error:', error);
  if (error.message.includes('authentication')) {
    console.error('Authentication failed, please login again');
  }
});

socket.on('disconnect', (reason) => {
  console.log('Disconnected:', reason);
  // 对于任何断开连接的情况都尝试重新连接
  if (!socket.connected) {
    setTimeout(() => {
      console.log('Attempting to reconnect...');
      socket.connect();
    }, 1000);
  }
});

socket.on('error', (error) => {
  console.error('Socket error:', error);
});

socket.on('reconnect', (attemptNumber) => {
  console.log('Reconnected after', attemptNumber, 'attempts');
});

socket.on('reconnect_attempt', (attemptNumber) => {
  console.log('Reconnection attempt', attemptNumber);
});

socket.on('reconnect_error', (error) => {
  console.error('Reconnection error:', error);
  // 重连错误时，延迟后重试
  setTimeout(() => {
    if (!socket.connected) {
      socket.connect();
    }
  }, 2000);
});

socket.on('reconnect_failed', () => {
  console.error('Failed to reconnect');
  // 即使在重连失败后也继续尝试
  setTimeout(() => {
    if (!socket.connected) {
      socket.connect();
    }
  }, 5000);
});

export default socket;



