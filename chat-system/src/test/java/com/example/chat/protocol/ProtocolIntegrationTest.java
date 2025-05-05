package com.example.chat.protocol;

import com.example.chat.core.codec.MessageCodec;
import com.example.chat.protocol.request.ChatRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolIntegrationTest {
    private static final int MAX_FRAME_LENGTH = 65535; // 增加最大帧长度
    private static final int LENGTH_FIELD_OFFSET = 45; // 修正长度字段偏移量 (4 + 1 + 1 + 1 + 2 + 36)
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private EventLoopGroup clientGroup;
    private Channel serverChannel;
    private Channel clientChannel;
    private CountDownLatch messageLatch;

    @BeforeEach
    public void setup() throws InterruptedException {
        messageLatch = new CountDownLatch(1);
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        clientGroup = new NioEventLoopGroup();

        // 启动服务器
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, 4, 0, 0))
                        .addLast(new MessageCodec())
                        .addLast(new SimpleChannelInboundHandler<ProtocolMessage>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ProtocolMessage msg) {
                                // 收到消息后释放锁
                                messageLatch.countDown();
                            }
                        });
                }
            });
        serverChannel = serverBootstrap.bind(8888).sync().channel();

        // 启动客户端
        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(clientGroup)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline()
                        .addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, 4, 0, 0))
                        .addLast(new MessageCodec());
                }
            });
        clientChannel = clientBootstrap.connect("localhost", 8888).sync().channel();
    }

    @Test
    public void testMessageTransmission() throws InterruptedException {
        // 创建测试消息
        ChatRequest request = ChatRequest.builder()
                .type(MessageType.CHAT_REQUEST)
                .sender("user1")
                .content("Hello, world!")
                .roomId("room1")
                .build();

        // 发送消息
        clientChannel.writeAndFlush(request);

        // 等待服务器接收消息
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Message was not received within 5 seconds");
    }

    @AfterEach
    public void teardown() {
        if (clientChannel != null) {
            clientChannel.close();
        }
        if (serverChannel != null) {
            serverChannel.close();
        }
        clientGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}

