package com.example.chat.controller;

import com.example.chat.model.ChatRoom;
import com.example.chat.service.JwtService;
import com.example.chat.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    @Autowired
    private RoomService roomService;
    @Autowired
    private JwtService jwtService;

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    @GetMapping("/me")
    public ResponseEntity<List<ChatRoom>> getUserRooms(@RequestHeader("Authorization") String authHeader){
        try {
            String token = authHeader.replace("Bearer ", "");
            String currentUserId = jwtService.validateToken(token);

            if (currentUserId == null) {
                return ResponseEntity.status(401).build();
            }

            return ResponseEntity.ok(roomService.getUserRooms(currentUserId));

        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("")
    public ResponseEntity<String> createChatRoom(@RequestBody ChatRoom chatRoom){
        try {
            roomService.createRoom(chatRoom.getName(),chatRoom.getCreatorId());
            return ResponseEntity.status(201).build();
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteChatRoom(@RequestParam String roomId){
        try {
            roomService.deleteRoom(roomId);
            return ResponseEntity.status(201).build();
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<String>> getAllUsersFromRoom(@RequestParam String roomId){
        try {
            return ResponseEntity.ok(roomService.getRoomUsers(roomId));
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/user")
    public ResponseEntity<String> addUserToRoom(@RequestParam String roomId,@RequestParam String userId){
        try {
            roomService.addUserToRoom(roomId,userId);
            return ResponseEntity.status(201).build();
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUserFromRoom(@RequestParam String roomId,@RequestParam String userId){
        try {
            roomService.removeUserFromRoom(roomId,userId);
            return ResponseEntity.status(201).build();
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).build();
        }
    }
}

