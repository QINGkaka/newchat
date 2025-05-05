import { useChatStore } from "../store/useChatStore";
import { useEffect } from "react";
import wsClient from "../lib/websocket";

import Sidebar from "../components/Sidebar";
import NoChatSelected from "../components/NoChatSelected";
import ChatContainer from "../components/ChatContainer";

function HomePage() {
    const {selectedUser, addMessage} = useChatStore();

    useEffect(() => {
        try {
            // 连接WebSocket
            wsClient.connect();
            console.log("*** wsClient is: ", wsClient);

            // 添加消息处理器
            const messageHandler = (message) => {
                if (message.type === 'message') {
                    addMessage(message);
                }
            };
            wsClient.addMessageHandler(messageHandler);

            // 组件卸载时清理
            return () => {
                wsClient.removeMessageHandler(messageHandler);
                wsClient.disconnect();
            };
        } catch (error) {
            console.error('WebSocket connection error:', error);
        }
    }, [addMessage]);

    return (
        <div className="h-screen bg-base-200">
            <div className="flex items-center justify-center pt-20 px-4">
                <div className="bg-base-100 rounded-lg shadow-cl w-full max-w-6xl h-[calc(100vh-8rem)]">
                    <div className="flex h-full rounded-lg overflow-hidden">
                        <Sidebar />

                        {!selectedUser ? <NoChatSelected /> : <ChatContainer />}
                    </div>
                </div>
            </div>
        </div>
    )
}

export default HomePage;