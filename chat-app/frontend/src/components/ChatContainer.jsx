import { useChatStore } from "../store/useChatStore";
import { useEffect, useRef } from "react";
import wsClient from "../lib/websocket";

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
        getMessages(selectedUser._id);
        
        // 加入聊天室
        wsClient.joinRoom(selectedUser._id);

        // 添加消息处理器
        const messageHandler = (message) => {
            if (message.type === 'message' && 
                (message.senderId === selectedUser._id || message.senderId === authUser._id)) {
                addMessage(message);
            }
        };
        wsClient.addMessageHandler(messageHandler);

        return () => {
            wsClient.removeMessageHandler(messageHandler);
            wsClient.leaveRoom(selectedUser._id);
        };
    }, [selectedUser._id, authUser._id, getMessages, addMessage]);

    useEffect(() => {
        if (messageEndRef.current && messages) {
            messageEndRef.current.scrollIntoView({ behavior: "smooth" });
        }
    }, [messages]);

    const handleSendMessage = (text, image) => {
        const message = {
            type: 'message',
            roomId: selectedUser._id,
            text: text,
            image: image,
            senderId: authUser._id,
            receiverId: selectedUser._id
        };
        wsClient.sendMessage(message);
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
