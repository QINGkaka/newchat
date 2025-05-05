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
        const { messages } = get();
        
        // 检查消息是否已存在
        const messageExists = messages.some(m => 
            m._id === message._id || 
            m.messageId === message.messageId ||
            (m.senderId === message.senderId && 
             m.timestamp === message.timestamp)
        );

        if (!messageExists) {
            set({ messages: [...messages, message] });
        }
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

    // 订阅消息
    subscribeToMessages: () => {
        const { selectedUser } = get();
        if (!selectedUser) return;

        const socket = useAuthStore.getState().socket;

        // 将newMessage改为sendMessage
        socket.on("sendMessage", (newMessage) => {
            const isMessageSentFromSelectedUser = newMessage.senderId === selectedUser._id;
            if (!isMessageSentFromSelectedUser) return;

            // 将新消息追加到末尾
            set({
                messages: [...get().messages, newMessage],
            });
        });
    },

    // 取消订阅，如登出等
    unsubscribeFromMessages: () => {
        const socket = useAuthStore.getState().socket;
        socket.off("sendMessage");
    },

    setSelectedUser: (selectedUser) => {
        console.log('Setting selected user:', selectedUser);
        set({ selectedUser });
    }
}));
