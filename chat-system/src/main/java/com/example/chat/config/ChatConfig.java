package com.example.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "chat")
public class ChatConfig {
    private Server server = new Server();
    private Redis redis = new Redis();
    private ThreadPool threadPool = new ThreadPool();

    @Data
    public static class Server {
        private int port = 8080;
        private String websocketPath = "/ws";
    }

    @Data
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
    }

    @Data
    public static class ThreadPool {
        private int coreSize = 4;
        private int maxSize = 8;
        private int queueCapacity = 1000;
        private int keepAliveSeconds = 60;
    }
}