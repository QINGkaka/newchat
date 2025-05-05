package com.example.chat.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;

/**
 * 简单的Socket.IO测试客户端，使用HTTP长轮询方式
 * 这是一个简化版，仅用于测试连接
 */
public class SimpleSocketClient {

    public static void main(String[] args) {
        try {
            System.out.println("简单Socket.IO测试客户端启动...");
            System.out.println("尝试连接到服务器: http://localhost:9092");
            
            // 1. 建立初始连接
            URL url = URI.create("http://localhost:9092/socket.io/?EIO=4&transport=polling&token=test-token").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                System.out.println("连接成功! 服务器响应: " + response.toString());
                
                // 从响应中提取sid
                String sid = extractSid(response.toString());
                if (sid != null) {
                    System.out.println("会话ID: " + sid);
                    
                    // 2. 发送一个消息
                    sendMessage(sid);
                }
            } else {
                System.out.println("连接失败，响应码: " + responseCode);
            }
            
        } catch (Exception e) {
            System.out.println("测试客户端出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String extractSid(String response) {
        // 简单解析，实际应该使用JSON解析器
        int sidStart = response.indexOf("\"sid\":\"") + 7;
        if (sidStart > 6) {
            int sidEnd = response.indexOf("\"", sidStart);
            if (sidEnd > sidStart) {
                return response.substring(sidStart, sidEnd);
            }
        }
        return null;
    }
    
    private static void sendMessage(String sid) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            
            if ("exit".equals(input)) {
                break;
            }
            
            // 构建消息
            String message = "42[\"sendMessage\",{\"text\":\"" + input + "\",\"receiverId\":\"test-user\"}]";
            
            // 发送消息
            URL url = URI.create("http://localhost:9092/socket.io/?EIO=4&transport=polling&sid=" + sid).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            
            // 添加消息长度前缀
            String payload = message.length() + ":" + message;
            
            OutputStream os = conn.getOutputStream();
            os.write(payload.getBytes());
            os.flush();
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                System.out.println("消息已发送");
            } else {
                System.out.println("发送失败，响应码: " + responseCode);
            }
            
            // 轮询获取响应
            pollForMessages(sid);
        }
        
        scanner.close();
    }
    
    private static void pollForMessages(String sid) throws Exception {
        URL url = URI.create("http://localhost:9092/socket.io/?EIO=4&transport=polling&sid=" + sid).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            String responseStr = response.toString();
            if (responseStr.contains("42[\"newMessage\"")) {
                System.out.println("收到新消息: " + responseStr);
            }
        }
    }
}
