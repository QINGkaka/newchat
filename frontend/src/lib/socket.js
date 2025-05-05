import { io } from 'socket.io-client';

// 修改为 Java 后端的 Socket.IO 地址
const socket = io('http://localhost:19098', {
  autoConnect: false,
  withCredentials: true,
  reconnection: true,
  reconnectionAttempts: 5,
  reconnectionDelay: 1000,
  reconnectionDelayMax: 5000,
  timeout: 20000,
  forceBase64: false,
  jsonp: false,
  rememberUpgrade: false,
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
    // 可以在这里添加重定向到登录页面的逻辑
  }
});

socket.on('disconnect', (reason) => {
  console.log('Disconnected:', reason);
  if (reason === 'io server disconnect') {
    // 服务器主动断开连接，尝试重新连接
    socket.connect();
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
});

socket.on('reconnect_failed', () => {
  console.error('Failed to reconnect');
});

export default socket;



