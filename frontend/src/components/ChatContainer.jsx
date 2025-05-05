import { useChatStore } from "../store/useChatStore";
import { useEffect, useRef } from "react";
import wsClient from "../lib/websocket";
import { toast } from "react-hot-toast";

import ChatHeader from "./ChatHeader";
import MessageInput from "./MessageInput.jsx";
import MessageSkeleton from "./skeletons/MessageSkeleton";
import { useAuthStore } from "../store/useAuthStore";
import { formatMessageTime } from "../lib/utils";
import { useTranslation } from "react-i18next";

const ChatContainer = () => {
    const {
        messages,
        getMessages,
        isMessagesLoading,
        selectedUser,
        addMessage
    } = useChatStore();
    const { authUser } = useAuthStore();
    const messageEndRef = useRef(null);

    useEffect(() => {
        if (!selectedUser?._id) return;

        getMessages(selectedUser._id);
        
        // 检查 WebSocket 连接状态
        if (wsClient.getConnectionStatus() === 'connected') {
            // 加入聊天室
            wsClient.joinRoom(selectedUser._id).catch(error => {
                console.error('Failed to join room:', error);
                toast.error('加入聊天室失败');
            });
        } else {
            console.error('WebSocket is not connected');
            toast.error('WebSocket 连接已断开');
        }

        // 添加消息处理器
        const messageHandler = (message) => {
            console.log('Received message in ChatContainer:', message);
            if (message.type === 'message' && 
                (message.senderId === selectedUser._id || message.senderId === authUser._id)) {
                // 确保消息有正确的格式
                const formattedMessage = {
                    ...message,
                    _id: message.messageId || message._id,
                    createdAt: message.createdAt || new Date(message.timestamp).toISOString(),
                    timestamp: message.timestamp || Date.now()
                };
                addMessage(formattedMessage);
            }
        };
        wsClient.addMessageHandler(messageHandler);

        return () => {
            wsClient.removeMessageHandler(messageHandler);
            if (wsClient.getConnectionStatus() === 'connected') {
                wsClient.leaveRoom(selectedUser._id).catch(error => {
                    console.error('Failed to leave room:', error);
                });
            }
        };
    }, [selectedUser._id, authUser._id, getMessages, addMessage]);

    useEffect(() => {
        if (messageEndRef.current && messages) {
            messageEndRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages]);

    const handleSendMessage = async (text, image) => {
        if (!wsClient.getConnectionStatus() === 'connected') {
            toast.error('WebSocket 连接已断开');
            return;
        }

        const message = {
            type: 'message',
            roomId: selectedUser._id,
            text: text,
            image: image,
            senderId: authUser._id,
            receiverId: selectedUser._id,
            timestamp: Date.now()
        };

        try {
            const response = await wsClient.sendMessage(message);
            console.log('Message sent successfully:', response);
            
            // 使用服务器返回的消息ID和时间戳
            if (response.message) {
                addMessage(response.message);
            }
        } catch (error) {
            console.error('Failed to send message:', error);
            toast.error('发送消息失败');
        }
    };

    if (isMessagesLoading) {
        return (
            <div className="flex-1 flex flex-col overflow-auto">
                <ChatHeader />
                <MessageSkeleton />
                <MessageInput onSendMessage={handleSendMessage} />
            </div>
        );
    }

    return (
        <div className="flex-1 flex flex-col overflow-auto">
            <ChatHeader />

            <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {messages.map((message) => (
                    <div
                        key={message._id || message.messageId}
                        className={`chat ${message.senderId === authUser._id ? "chat-end" : "chat-start"}`}
                        ref={messageEndRef}
                    >
                        <div className="chat-image avatar">
                            <div className="size-10 rounded-full border">
                                <img
                                    src={
                                        message.senderId === authUser._id
                                            ? authUser.profilePic || "/avatar.png"
                                            : selectedUser.profilePic || "/avatar.png"
                                    }
                                    alt="profile pic"
                                />
                            </div>
                        </div>
                        <div className="chat-header mb-1">
                            <time className="text-xs opacity-50 ml-1">
                                {formatMessageTime(message.createdAt)}
                            </time>
                        </div>
                        <div className="chat-bubble flex flex-col">
                            {message.image && (
                                <img
                                    src={message.image}
                                    alt="Attachment"
                                    className="sm:max-w-[200px] rounded-md mb-2"
                                />
                            )}
                            {message.text && <p>{message.text}</p>}
                        </div>
                    </div>
                ))}
            </div>

            <MessageInput onSendMessage={handleSendMessage} />
        </div>
    );
};
export default ChatContainer;
