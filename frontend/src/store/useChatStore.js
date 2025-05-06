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

    getUsers: async () => {
        set({ isUsersLoading: true });
        try {
            console.log('Fetching users...');
            const res = await axiosInstance.get("/api/messages/users");
            console.log('Users response:', res.data);
            set({ users: res.data });
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
        set({ isMessagesLoading: true });
        try {
            const res = await axiosInstance.get(`/api/messages/${userId}`);
            console.log('Messages response:', res.data);
            set({ messages: res.data });
        } catch (error) {
            console.error('Error fetching messages:', error);
            toast.error(error.response?.data?.message || '获取消息失败');
        } finally {
            set({ isMessagesLoading: false });
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
                return {
                    messages: [...state.messages, message].sort((a, b) => 
                        (a.timestamp || a.createdAt) - (b.timestamp || b.createdAt)
                    )
                };
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
    }
}));
