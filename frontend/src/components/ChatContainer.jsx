import { useChatStore } from "../store/useChatStore";
import { useEffect, useRef, useMemo } from "react";
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
        console.log("*** selectedUser is: ", selectedUser);
        if (!selectedUser?.id || !authUser?.id) return;

        getMessages(selectedUser.id);
        
        // 检查 WebSocket 连接状态
        if (wsClient.getConnectionStatus() === 'connected') {
            // 加入聊天室
            wsClient.joinRoom(selectedUser.id).catch(error => {
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
            // 检查消息是否属于当前聊天
            if ((message.senderId === selectedUser.id && message.receiverId === authUser.id) || 
                (message.senderId === authUser.id && message.receiverId === selectedUser.id)) {
                console.log('Message belongs to current chat, adding to messages');
                const formattedMessage = {
                    ...message,
                    _id: message.messageId || message._id,
                    timestamp: message.timestamp || Date.now(),
                    createdAt: message.createdAt || new Date(message.timestamp).toISOString(),
                    content: message.content || message.text
                };
                addMessage(formattedMessage);
            } else {
                console.log('Message does not belong to current chat', {
                    messageSenderId: message.senderId,
                    messageReceiverId: message.receiverId,
                    selectedUserId: selectedUser.id,
                    authUserId: authUser.id
                });
            }
        };

        // 添加消息处理器
        wsClient.addMessageHandler(messageHandler);

        // 清理函数
        return () => {
            wsClient.removeMessageHandler(messageHandler);
            if (wsClient.getConnectionStatus() === 'connected') {
                wsClient.leaveRoom(selectedUser.id).catch(error => {
                    console.error('Failed to leave room:', error);
                });
            }
        };
    }, [selectedUser?.id, authUser?.id, getMessages, addMessage]);

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
            roomId: null,
            content: text,
            image: image,
            senderId: authUser.id,
            receiverId: selectedUser.id,
            timestamp: Date.now()
        };

        try {
            const response = await wsClient.sendMessage(message);
            console.log('Message sent successfully:', response);
            
            // 使用服务器返回的消息ID和时间戳
            if (response.message) {
                const formattedMessage = {
                    ...response.message,
                    _id: response.message.messageId || response.message._id,
                    timestamp: response.message.timestamp || Date.now(),
                    createdAt: response.message.createdAt || new Date().toISOString()
                };
                addMessage(formattedMessage);
            }
        } catch (error) {
            console.error('Failed to send message:', error);
            toast.error('发送消息失败');
        }
    };

    // 对消息进行排序
    const sortedMessages = useMemo(() => {
        return [...messages].sort((a, b) => {
            const timestampA = a.timestamp || new Date(a.createdAt).getTime();
            const timestampB = b.timestamp || new Date(b.createdAt).getTime();
            return timestampA - timestampB;
        });
    }, [messages]);

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
                {sortedMessages.map((message) => {
                    const messageId = message._id || message.messageId || `${message.senderId}-${message.timestamp}`;
                    return (
                        <div
                            key={messageId}
                            className={`chat ${message.senderId === authUser.id ? "chat-end" : "chat-start"}`}
                        >
                            <div className="chat-image avatar">
                                <div className="size-10 rounded-full border">
                                    <img
                                        src={
                                            message.senderId === authUser.id
                                                ? authUser.profilePic || "/avatar.png"
                                                : selectedUser.profilePic || "/avatar.png"
                                        }
                                        alt="profile pic"
                                    />
                                </div>
                            </div>
                            <div className="chat-header mb-1">
                                <time className="text-xs opacity-50 ml-1">
                                    {formatMessageTime(message.timestamp || message.createdAt)}
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
                                {(message.content || message.text) && (
                                    <p>{message.content || message.text}</p>
                                )}
                            </div>
                        </div>
                    );
                })}
                <div ref={messageEndRef} />
            </div>

            <MessageInput onSendMessage={handleSendMessage} />
        </div>
    );
};
export default ChatContainer;
