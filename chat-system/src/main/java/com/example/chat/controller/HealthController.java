package com.example.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corundumstudio.socketio.SocketIOServer;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private SocketIOServer socketIOServer;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("socketio", socketIOServer != null ? "UP" : "DOWN");
        status.put("port", socketIOServer.getConfiguration().getPort());
        return status;
    }
}
