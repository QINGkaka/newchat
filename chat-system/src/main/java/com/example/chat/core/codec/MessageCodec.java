package com.example.chat.core.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.DecoderException;
import com.example.chat.protocol.ProtocolMessage;
import com.example.chat.protocol.MessageType;
import com.example.chat.protocol.request.*;
import com.example.chat.protocol.response.*;
import com.example.chat.util.JsonUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class MessageCodec extends MessageToMessageCodec<ByteBuf, ProtocolMessage> {
    private static final int MAGIC_NUMBER = 0xCAFEBABE;
    private static final byte VERSION = 1;
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, List<Object> out) throws Exception {
        ByteBuf buf = ctx.alloc().buffer();
        try {
            // 1. 魔数 4字节
            buf.writeInt(MAGIC_NUMBER);
            
            // 2. 版本号 1字节
            buf.writeByte(VERSION);
            
            // 3. 序列化方式 1字节
            buf.writeByte(SerializerType.JSON.ordinal());
            
            // 4. 消息类型 1字节
            buf.writeByte(msg.getType().ordinal());
            
            // 5. 状态码 2字节
            buf.writeShort(msg.getStatusCode());
            
            // 6. 请求ID 36字节 - 如果为空则生成新的
            String requestId = msg.getRequestId();
            if (requestId == null) {
                requestId = UUID.randomUUID().toString();
                msg.setRequestId(requestId);
            }
            byte[] requestIdBytes = requestId.getBytes(StandardCharsets.UTF_8);
            buf.writeBytes(requestIdBytes);
            
            // 7. 正文长度 4字节
            byte[] bytes = JsonUtil.toJson(msg).getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            
            // 8. 消息正文
            buf.writeBytes(bytes);
            
            out.add(buf);
        } catch (Exception e) {
            buf.release();
            throw e;
        }
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 保存读取索引，以便需要时重置
        in.markReaderIndex();
        
        // 确保有足够的字节可读
        if (in.readableBytes() < 49) { // 4 + 1 + 1 + 1 + 2 + 36 + 4
            return;
        }
        
        // 1. 魔数 4字节
        int magicNum = in.readInt();
        if (magicNum != MAGIC_NUMBER) {
            in.resetReaderIndex();
            throw new DecoderException("Invalid magic number: " + magicNum);
        }
        
        // 2. 版本号 1字节
        byte version = in.readByte();
        if (version != VERSION) {
            in.resetReaderIndex();
            throw new DecoderException("Version not supported: " + version);
        }
        
        // 3. 序列化方式 1字节 (暂时只支持JSON)
        byte serializerType = in.readByte();
        if (serializerType != SerializerType.JSON.ordinal()) {
            in.resetReaderIndex();
            throw new DecoderException("Unsupported serializer type: " + serializerType);
        }
        
        // 4. 消息类型 1字节
        byte messageTypeCode = in.readByte();
        MessageType messageType = MessageType.fromCode(messageTypeCode);
        if (messageType == null) {
            in.resetReaderIndex();
            throw new DecoderException("Unknown message type: " + messageTypeCode);
        }
        
        // 5. 状态码 2字节
        short statusCode = in.readShort();
        
        // 6. 请求ID 36字节
        byte[] requestIdBytes = new byte[36];
        in.readBytes(requestIdBytes);
        String requestId = new String(requestIdBytes, StandardCharsets.UTF_8);
        
        // 7. 正文长度 4字节
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        
        // 8. 消息正文
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        
        // 反序列化消息
        String json = new String(bytes, StandardCharsets.UTF_8);
        ProtocolMessage decodedMessage = null;
        
        // 根据消息类型选择对应的类
        switch (messageType) {
            case CHAT_REQUEST:
                decodedMessage = JsonUtil.fromJson(json, ChatRequest.class);
                break;
            case CHAT_RESPONSE:
                decodedMessage = JsonUtil.fromJson(json, ChatResponse.class);
                break;
            case LOGIN_REQUEST:
                decodedMessage = JsonUtil.fromJson(json, LoginRequest.class);
                break;
            case LOGIN_RESPONSE:
                decodedMessage = JsonUtil.fromJson(json, LoginResponse.class);
                break;
            case LOGOUT_REQUEST:
                decodedMessage = JsonUtil.fromJson(json, LogoutRequest.class);
                break;
            case LOGOUT_RESPONSE:
                decodedMessage = JsonUtil.fromJson(json, LogoutResponse.class);
                break;
            case ROOM_REQUEST:
            case ROOM_CREATE:
            case ROOM_JOIN:
            case ROOM_LEAVE:
            case ROOM_LIST:
                decodedMessage = JsonUtil.fromJson(json, RoomRequest.class);
                break;
            case ROOM_RESPONSE:
                decodedMessage = JsonUtil.fromJson(json, RoomResponse.class);
                break;
            case HEARTBEAT_REQUEST:
                decodedMessage = JsonUtil.fromJson(json, HeartbeatRequest.class);
                break;
            case HEARTBEAT_RESPONSE:
                decodedMessage = JsonUtil.fromJson(json, HeartbeatResponse.class);
                break;
            case MESSAGE_HISTORY_REQUEST:
                decodedMessage = JsonUtil.fromJson(json, MessageHistoryRequest.class);
                break;
            case MESSAGE_HISTORY_RESPONSE:
                decodedMessage = JsonUtil.fromJson(json, MessageHistoryResponse.class);
                break;
            case ERROR:
                decodedMessage = JsonUtil.fromJson(json, ErrorMessage.class);
                break;
            case SYSTEM:
                decodedMessage = JsonUtil.fromJson(json, SystemMessage.class);
                break;
            case NOTIFICATION:
                decodedMessage = JsonUtil.fromJson(json, NotificationMessage.class);
                break;
            default:
                throw new DecoderException("Unsupported message type: " + messageType);
        }
        
        if (decodedMessage == null) {
            throw new DecoderException("Failed to decode message");
        }
        
        // 设置消息属性
        decodedMessage.setType(messageType);
        decodedMessage.setStatusCode(statusCode);
        decodedMessage.setRequestId(requestId);
        
        out.add(decodedMessage);
    }
}















