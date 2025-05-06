import { create } from "zustand";
import toast from "react-hot-toast";
import axiosInstance  from "../lib/axios";
import { useAuthStore } from "./useAuthStore";

export const useChatStore = create((set, get) => ({
    messages: [],
    users: [],
    selectedUser: null,
    isUsersLoading: false,
    isMessagesLoading: false,
    onlineStatusCache: new Map(),

    updateOnlineStatus: (userId, status) => {
        set(state => {
            const newCache = new Map(state.onlineStatusCache);
            newCache.set(userId, {
                status,
                lastUpdated: Date.now()
            });
            return { onlineStatusCache: newCache };
        });
    },

    getUsers: async () => {
        set({ isUsersLoading: true });
        try {
            console.log('Fetching users...');
            const res = await axiosInstance.get("/api/messages/users");
            console.log('Users response:', res.data);
            set({ users: res.data });
            // 初始化在线状态缓存
            const newCache = new Map();
            res.data.forEach(user => {
                newCache.set(user.id, {
                    status: false,
                    lastUpdated: Date.now()
                });
            });
            set({ onlineStatusCache: newCache });
        } catch (error) {
            console.error('Error fetching users:', error);
            if (error.response) {
                console.error('Error response:', error.response.data);
                toast.error(error.response.data.message || '获取用户列表失败');
            } else if (error.request) {
                console.error('Error request:', error.request);
                toast.error('无法连接到服务器');
            } else {
                console.error('Error message:', error.message);
                toast.error('获取用户列表时发生错误');
            }
        } finally {
            set({ isUsersLoading: false });
        }
    },

    getMessages: async (userId) => {
        try {
            const response = await axiosInstance.get(`/api/messages/${userId}`);
            // 确保消息按时间戳升序排序
            const sortedMessages = response.data.sort((a, b) => {
                const timestampA = a.timestamp || new Date(a.createdAt).getTime();
                const timestampB = b.timestamp || new Date(b.createdAt).getTime();
                return timestampA - timestampB;
            });
            set({ messages: sortedMessages, isMessagesLoading: false });
        } catch (error) {
            console.error('Error fetching messages:', error);
            set({ isMessagesLoading: false });
            toast.error('获取消息历史失败');
        }
    },

    addMessage: (message) => {
        console.log('Adding message:', message);
        set(state => {
            // 检查消息是否已存在
            const messageExists = state.messages.some(m => 
                m._id === message._id || 
                m.messageId === message.messageId ||
                (m.senderId === message.senderId && 
                 m.timestamp === message.timestamp &&
                 m.content === message.content)
            );

            if (!messageExists) {
                console.log('Message does not exist, adding to store:', message);
                // 确保所有消息都有时间戳
                const messageWithTimestamp = {
                    ...message,
                    timestamp: message.timestamp || Date.now(),
                    createdAt: message.createdAt || new Date(message.timestamp || Date.now()).toISOString()
                };
                
                // 使用插入排序优化性能
                const messages = [...state.messages];
                let insertIndex = messages.length;
                while (insertIndex > 0 && 
                       (messages[insertIndex - 1].timestamp || new Date(messages[insertIndex - 1].createdAt).getTime()) > 
                       (messageWithTimestamp.timestamp || new Date(messageWithTimestamp.createdAt).getTime())) {
                    insertIndex--;
                }
                messages.splice(insertIndex, 0, messageWithTimestamp);
                
                return { messages };
            }
            return state;
        });
    },

    sendMessage: async (messageData) => {
        const { selectedUser, messages } = get();
        try {
            const res = await axiosInstance.post(`/messages/send/${selectedUser._id}`, messageData);
            set({ messages: [...messages, res.data] });
        } catch (error) {
            toast.error(error.response.data.message);
        }
    },

    // 移除不需要的订阅方法，因为我们使用WebSocket客户端处理消息
    subscribeToMessages: () => {},
    unsubscribeFromMessages: () => {},

    setSelectedUser: (selectedUser) => {
        console.log('Setting selected user:', selectedUser);
        set({ selectedUser });
    },

    // 更新用户资料
    updateUserProfile: (userId, profileData) => {
        set(state => {
            // 更新用户列表中的用户资料
            const updatedUsers = state.users.map(user => {
                if (user.id === userId) {
                    return { ...user, ...profileData };
                }
                return user;
            });

            // 如果是当前选中的用户，也更新selectedUser
            let updatedSelectedUser = state.selectedUser;
            if (state.selectedUser?.id === userId) {
                updatedSelectedUser = { ...state.selectedUser, ...profileData };
            }

            return {
                users: updatedUsers,
                selectedUser: updatedSelectedUser
            };
        });
    }
}));
