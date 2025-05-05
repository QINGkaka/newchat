package com.example.chat.tools;

import com.example.chat.core.codec.MessageCodec;
import com.example.chat.protocol.MessageType;
import com.example.chat.protocol.request.ChatRequest;
import com.example.chat.protocol.request.HeartbeatRequest;
import com.example.chat.protocol.request.RoomRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class ProtocolTestTool {
    private static final int MAX_FRAME_LENGTH = 65535; // 增加最大帧长度
    private static final int LENGTH_FIELD_OFFSET = 45; // 修正长度字段偏移量 (4 + 1 + 1 + 1 + 2 + 36)
    
    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, 4, 0, 0))
                            .addLast(new MessageCodec())
                            .addLast(new SimpleChannelInboundHandler<Object>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                                    System.out.println("Received response: " + msg);
                                }
                            });
                    }
                });

            Channel channel = bootstrap.connect("localhost", 8888).sync().channel();

            // 测试不同类型的消息
            testChatMessage(channel);
            testHeartbeat(channel);
            testRoomOperations(channel);

            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static void testChatMessage(Channel channel) throws InterruptedException {
        System.out.println("Testing chat messages...");
        ChatRequest request = ChatRequest.builder()
            .type(MessageType.CHAT_REQUEST)
            .sender("user1")
            .content("Hello, world!")
            .roomId("room1")
            .build();
        
        channel.writeAndFlush(request);
        Thread.sleep(1000);
    }

    private static void testHeartbeat(Channel channel) throws InterruptedException {
        System.out.println("Testing heartbeat...");
        HeartbeatRequest request = HeartbeatRequest.builder()
            .build();
        
        channel.writeAndFlush(request);
        Thread.sleep(1000);
    }

    private static void testRoomOperations(Channel channel) throws InterruptedException {
        System.out.println("Testing room operations...");
        
        // 创建房间
        RoomRequest createRoom = RoomRequest.builder()
            .type(MessageType.ROOM_CREATE)
            .roomId("NewRoom")
            .build();
        channel.writeAndFlush(createRoom);
        Thread.sleep(1000);

        // 加入房间
        RoomRequest joinRoom = RoomRequest.builder()
            .type(MessageType.ROOM_JOIN)
            .roomId("NewRoom")
            .build();
        channel.writeAndFlush(joinRoom);
        Thread.sleep(1000);
    }
}


