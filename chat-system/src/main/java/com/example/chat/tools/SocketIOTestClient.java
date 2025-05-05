package com.example.chat.tools;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Scanner;

public class SocketIOTestClient {

    public static void main(String[] args) throws URISyntaxException {
        // 连接选项
        IO.Options options = IO.Options.builder()
                .setForceNew(true)
                .build();
        
        // 添加认证信息
        options.query = "token=test-token";
        
        // 创建Socket.IO客户端
        Socket socket = IO.socket("http://localhost:9092", options);
        
        // 连接事件
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Connected to server");
                
                // 获取在线用户
                socket.emit("getOnlineUsers");
            }
        });
        
        // 断开连接事件
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Disconnected from server");
            }
        });
        
        // 在线用户事件
        socket.on("getOnlineUsers", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("Online users: " + args[0]);
            }
        });
        
        // 新消息事件
        socket.on("newMessage", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("New message: " + args[0]);
            }
        });
        
        // 用户状态事件
        socket.on("userStatus", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                System.out.println("User status: " + args[0]);
            }
        });
        
        // 连接服务器
        socket.connect();
        
        // 命令行交互
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            
            if ("exit".equals(input)) {
                break;
            } else if (input.startsWith("send ")) {
                String message = input.substring(5);
                JSONObject msgObj = new JSONObject();
                msgObj.put("text", message);
                msgObj.put("receiverId", "test-user");
                
                socket.emit("sendMessage", msgObj, new Ack() {
                    @Override
                    public void call(Object... args) {
                        System.out.println("Message sent, ack: " + args[0]);
                    }
                });
            }
        }
        
        // 关闭连接
        socket.disconnect();
        scanner.close();
    }
    
    // 确认回调接口
    interface Ack {
        void call(Object... args);
    }
}