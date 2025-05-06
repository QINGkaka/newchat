import {create} from 'zustand';
import axiosInstance from '../lib/axios';
import {toast} from "react-hot-toast";
import io from "socket.io-client";

const BASE_URL = 'http://localhost:19098';

// 全局 socket 实例
export let globalSocket = null;
let reconnectTimer = null;
let heartbeatTimer = null;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 1000;
const HEARTBEAT_INTERVAL = 30000;

export const useAuthStore = create((set, get) => ({
    authUser: null,
    isSigningUp: false,
    isLoggingIn: false,
    isUpdatingProfile: false,
    isCheckingAuth: true,
    onlineUsers: [],
    socketConnected: false,
    isConnecting: false,
    reconnectAttempts: 0,

    // 清理所有计时器和连接
    cleanup() {
        console.log('Cleaning up socket connection...');
        if (heartbeatTimer) {
            clearInterval(heartbeatTimer);
            heartbeatTimer = null;
        }
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
        if (globalSocket) {
            // 移除所有事件监听器
            globalSocket.removeAllListeners();
            globalSocket.disconnect();
            globalSocket = null;
        }
        set({ 
            socketConnected: false, 
            isConnecting: false,
            reconnectAttempts: 0,
            onlineUsers: []
        });
    },

    // 启动心跳检测
    startHeartbeat() {
        if (heartbeatTimer) {
            clearInterval(heartbeatTimer);
        }
        
        // 每30秒发送一次心跳
        heartbeatTimer = setInterval(() => {
            if (globalSocket?.connected) {
                console.log('Sending heartbeat ping...');
                globalSocket.emit('ping');
            } else {
                console.warn('Socket not connected, skipping heartbeat');
                get().reconnectSocket();
            }
        }, HEARTBEAT_INTERVAL);
    },

    // 重连逻辑
    async reconnectSocket() {
        const { isConnecting, reconnectAttempts } = get();
        
        if (isConnecting || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                console.error('Max reconnection attempts reached');
                get().cleanup();
            }
            return;
        }

        console.log(`Attempting to reconnect... (attempt ${reconnectAttempts + 1}/${MAX_RECONNECT_ATTEMPTS})`);
        
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
        }

        set(state => ({ 
            isConnecting: true,
            reconnectAttempts: state.reconnectAttempts + 1
        }));

        reconnectTimer = setTimeout(() => {
            get().connectSocket();
        }, RECONNECT_DELAY * Math.pow(2, reconnectAttempts)); // 指数退避
    },

    // 连接 WebSocket
    connectSocket() {
        const { authUser, isConnecting } = get();
        console.log('Connecting socket for user:', authUser?.username);
        if (!authUser?.token || isConnecting || globalSocket?.connected) {
            console.log('Socket connection skipped:', {
                hasToken: !!authUser?.token,
                isConnecting,
                isConnected: globalSocket?.connected
            });
            return;
        }

        // 清理现有连接
        get().cleanup();
        set({ isConnecting: true });

        try {
            globalSocket = io(BASE_URL, {
                transports: ['websocket'],
                withCredentials: true,
                reconnection: false, // 我们自己管理重连
                timeout: 20000,
                auth: { token: authUser.token },
                query: { token: authUser.token }
            });

            globalSocket.on('connect', () => {
                console.log('Socket connected successfully');
                set({ 
                    socketConnected: true,
                    isConnecting: false,
                    reconnectAttempts: 0
                });
                get().startHeartbeat();
                
                // 连接后立即请求在线用户列表
                console.log('Requesting online users list...');
                globalSocket.emit('getOnlineUsers');
            });

            // 添加用户状态更新处理
            globalSocket.on('userStatus', (data) => {
                console.log('Received user status update:', data);
                const { userId, online, lastUpdate } = data;
                get().updateUserStatus(userId, online, lastUpdate);
            });

            globalSocket.on('onlineUsers', (users) => {
                console.log('Received online users list:', users);
                get().updateOnlineUsers(users);
            });

            // 添加重连成功后的处理
            globalSocket.on('reconnect', () => {
                console.log('Socket reconnected, requesting online users list...');
                globalSocket.emit('getOnlineUsers');
            });

            globalSocket.on('connect_error', (error) => {
                console.error('Socket connection error:', error);
                if (error.message.includes('ALREADY_CONNECTED')) {
                    // 如果是重复连接错误，清理后重试
                    console.log('Handling ALREADY_CONNECTED error...');
                    get().cleanup();
                    setTimeout(() => {
                        console.log('Retrying connection...');
                        get().connectSocket();
                    }, 1000);
                } else {
                    get().reconnectSocket();
                }
            });

            globalSocket.on('disconnect', (reason) => {
                console.log('Socket disconnected:', reason);
                set({ socketConnected: false });
                if (reason === 'io server disconnect' || reason === 'transport close') {
                    get().reconnectSocket();
                }
            });

            globalSocket.on('error', (error) => {
                console.error('Socket error:', error);
                get().reconnectSocket();
            });

        } catch (error) {
            console.error('Failed to initialize socket:', error);
            get().reconnectSocket();
        }
    },

    async checkAuth() {
        try {
            console.log('Checking auth...');
            const authUser = JSON.parse(localStorage.getItem('authUser'));
            if (!authUser?.token) {
                set({authUser: null, isCheckingAuth: false});
                return;
            }
            
            const response = await axiosInstance.get('/api/auth/check');
            console.log('Auth check response:', response.data);
            
            if (response.data.authenticated && response.data.user) {
                const userData = {
                    ...response.data.user,
                    token: response.data.token || authUser.token
                };
                console.log('Setting user data:', userData);
                localStorage.setItem('authUser', JSON.stringify(userData));
                set({authUser: userData, isCheckingAuth: false});
                get().connectSocket();
            } else {
                console.log('Authentication failed, clearing user data');
                localStorage.removeItem('authUser');
                set({authUser: null, isCheckingAuth: false});
            }
        } catch (error) {
            console.error('Auth check error:', error);
            localStorage.removeItem('authUser');
            set({authUser: null, isCheckingAuth: false});
            if (error.response) {
                console.error('Error response:', error.response.data);
            }
        }
    },

    async signup(data) {
        set({isSigningUp: true});
        try {
            console.log('Signing up with data:', data);
            const response = await axiosInstance.post("/api/auth/register", data);
            console.log('Signup response:', response.data);
            const userData = {
                ...response.data.user,
                token: response.data.token
            };
            localStorage.setItem('authUser', JSON.stringify(userData));
            set({authUser: userData});
            toast.success('用户创建成功');
            get().connectSocket();
        } catch (error) {
            console.error('Signup error:', error);
            if (error.response) {
                console.error('Error response:', error.response.data);
                toast.error(error.response.data.message || "用户创建失败");
            } else {
                toast.error("用户创建失败");
            }
        } finally {
            set({isSigningUp:false});
        }
    },

    async logout() {
        try {
            // 先清理 WebSocket 连接
            get().cleanup();
            
            await axiosInstance.post('/api/auth/logout');
            localStorage.removeItem('authUser');
            set({
                authUser: null,
                onlineUsers: [],
                socketConnected: false,
                isConnecting: false,
                reconnectAttempts: 0
            });
            toast.success('用户已经登出');
        } catch (error) {
            console.error('Logout error:', error);
            toast.error("登出失败");
        }
    },

    async login(data) {
        set({isLoggingIn:true});
        try {
            console.log('Logging in with data:', data);
            const response = await axiosInstance.post('/api/auth/login', data);
            console.log('Login response:', response.data);
            if (response.data.user && response.data.token) {
                const userData = {
                    ...response.data.user,
                    token: response.data.token
                };
                localStorage.setItem('authUser', JSON.stringify(userData));
                set({authUser: userData});
                toast.success('用户登陆成功');
                get().connectSocket();
            } else {
                console.error('Invalid login response:', response.data);
                toast.error("登录失败：服务器响应格式错误");
            }
        } catch (error) {
            console.error('Login error:', error);
            if (error.response) {
                console.error('Error response:', error.response.data);
                toast.error(error.response.data.error || "登录失败");
            } else {
                toast.error("登录失败：无法连接到服务器");
            }
        } finally {
            set({isLoggingIn:false});
        }
    },

    async updateProfile(data) {
        set({isUpdatingProfile:true});
        try {
            const response = await axiosInstance.put('/api/auth/update-profile',data);
            const updatedUser = {
                ...response.data,
                token: get().authUser.token
            };
            localStorage.setItem('authUser', JSON.stringify(updatedUser));
            set({authUser: updatedUser});
            toast('更换头像成功');
        } catch (error) {
            console.error('Update profile error:', error);
            toast("更换头像失败");
        } finally {
            set({isUpdatingProfile:false})
        }
    },

    disconnectSocket() {
        console.log('Disconnecting socket...');
        get().cleanup();
    },

    subscribeToUserStatus: () => {
        const socket = get().socket;
        if (!socket) return;

        socket.on("userStatus", (data) => {
            console.log("Received user status update:", data);
            const { onlineUsers } = get();
            const updatedUsers = onlineUsers.map(user => {
                if (user.id === data.userId) {
                    return { ...user, online: data.online };
                }
                return user;
            });
            set({ onlineUsers: updatedUsers });
        });
    },

    unsubscribeFromUserStatus: () => {
        const socket = get().socket;
        if (socket) {
            socket.off("userStatus");
        }
    },

    // 更新在线用户列表
    updateOnlineUsers(users) {
        console.log('Updating online users:', users);
        set(state => {
            // 确保每个用户都有必要的字段
            const updatedUsers = users.map(user => ({
                id: user.id,
                username: user.username,
                online: user.online === true,
                lastUpdate: user.lastUpdate || Date.now()
            }));

            console.log('Updated online users with status:', updatedUsers.map(u => ({
                username: u.username,
                online: u.online
            })));
            return { onlineUsers: updatedUsers };
        });
    },

    // 更新单个用户状态
    updateUserStatus(userId, isOnline, lastUpdate) {
        console.log(`Updating user ${userId} status to ${isOnline}`);
        set(state => {
            const userExists = state.onlineUsers.some(user => user.id === userId);
            
            if (!userExists && globalSocket?.connected) {
                console.log('User not in list, requesting full update...');
                globalSocket.emit('getOnlineUsers');
                return state;
            }

            const updatedUsers = state.onlineUsers.map(user => 
                user.id === userId 
                    ? {
                        ...user,
                        online: isOnline === true,
                        lastUpdate: lastUpdate || Date.now()
                    }
                    : user
            );

            console.log('Updated users after status change:', updatedUsers.map(u => ({
                username: u.username,
                online: u.online
            })));

            return { ...state, onlineUsers: updatedUsers };
        });
    }
}));