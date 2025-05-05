import {create} from 'zustand';
import axiosInstance from '../lib/axios';
import {toast} from "react-hot-toast";
import io from "socket.io-client";

const BASE_URL = 'http://localhost:19098';

export const useAuthStore = create((set, get) => ({
    authUser: null,
    isSigningUp: false,
    isLoggingIn: false,
    isUpdatingProfile: false,
    isCheckingAuth: true,
    onlineUsers:[],
    socket:null,
    socketConnected: false,

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
            await axiosInstance.post('/api/auth/logout');
            localStorage.removeItem('authUser');
            set({authUser:null});
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
            const userData = {
                ...response.data.user,
                token: response.data.token
            };
            localStorage.setItem('authUser', JSON.stringify(userData));
            set({authUser: userData});
            toast.success('用户登陆成功');
            get().connectSocket();
        } catch (error) {
            console.error('Login error:', error);
            if (error.response) {
                console.error('Error response:', error.response.data);
                toast.error(error.response.data.message || "登录失败");
            } else {
                toast.error("登录失败");
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

    connectSocket: () => {
        const { authUser } = get();
        if (!authUser?.token) {
            console.error('No authenticated user found');
            return;
        }

        console.log('Current auth user:', authUser);

        // 如果已经有连接，先断开
        if (get().socket) {
            console.log('Disconnecting existing socket...');
            get().socket.disconnect();
        }

        try {
            console.log('Connecting to socket with user:', authUser.id);
            
            const socket = io('http://localhost:19098', {
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

            socket.on('connect', () => {
                console.log('Socket connected successfully');
                // 发送认证消息
                socket.emit('authenticate', { token: authUser.token }, (response) => {
                    if (response.success) {
                        console.log('Socket authenticated successfully');
                        // 更新用户在线状态
                        set(state => ({
                            ...state,
                            authUser: {
                                ...state.authUser,
                                online: true
                            },
                            socketConnected: true
                        }));
                        // 请求在线用户列表
                        socket.emit('getOnlineUsers');
                    } else {
                        console.error('Socket authentication failed:', response.error);
                    }
                });
            });

            socket.on('disconnect', (reason) => {
                console.log('Socket disconnected:', reason);
                // 更新用户在线状态
                set(state => ({
                    ...state,
                    authUser: {
                        ...state.authUser,
                        online: false
                    },
                    socketConnected: false
                }));
            });

            socket.on('onlineUsers', (users) => {
                console.log('Received online users:', users);
                // 更新在线用户列表
                set({ onlineUsers: users });
                
                // 更新当前用户的在线状态
                const currentUser = users.find(user => user.id === authUser.id);
                console.log('Current user in online list:', currentUser);
                
                if (currentUser) {
                    console.log('Setting current user as online');
                    set(state => ({
                        ...state,
                        authUser: {
                            ...state.authUser,
                            online: true
                        }
                    }));
                }
            });

            // 添加错误处理
            socket.on('connect_error', (error) => {
                console.error('Socket connection error:', error);
                set(state => ({
                    ...state,
                    socketConnected: false
                }));
            });

            socket.on('error', (error) => {
                console.error('Socket error:', error);
            });

            set({ socket });
        } catch (error) {
            console.error('Failed to initialize socket:', error);
        }
    },

    disconnectSocket() {
        const {socket} = get();
        if (socket) {
            console.log('Disconnecting socket...');
            socket.disconnect();
            set({socket: null, socketConnected: false});
        }
    }
}));