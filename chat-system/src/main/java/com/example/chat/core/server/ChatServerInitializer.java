package com.example.chat.core.server;

import com.example.chat.core.handler.HeartbeatHandler;
import com.example.chat.core.handler.WebSocketMessageCodec;
import com.example.chat.core.handler.WebSocketServerHandler;
import com.example.chat.service.MessageService;
import com.example.chat.service.RoomService;
import com.example.chat.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ChatServerInitializer extends ChannelInitializer<SocketChannel> {
    private static final String WEBSOCKET_PATH = "/ws";
    
    private final UserService userService;
    private final RoomService roomService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    
    // 存储用户ID和Channel的映射
    private final Map<String, io.netty.channel.ChannelHandlerContext> userChannels = new ConcurrentHashMap<>();

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // HTTP协议相关的处理器
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());

        // WebSocket协议相关的处理器
        pipeline.addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true));
        pipeline.addLast(new WebSocketMessageCodec());

        // 空闲检测处理器
        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
        pipeline.addLast(new HeartbeatHandler());

        // 业务逻辑处理器
        pipeline.addLast(new WebSocketServerHandler(userService, roomService, messageService, objectMapper, userChannels));
    }
}




