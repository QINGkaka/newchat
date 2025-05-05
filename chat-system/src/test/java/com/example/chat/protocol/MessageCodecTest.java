package com.example.chat.protocol;

import com.example.chat.core.codec.MessageCodec;
import com.example.chat.protocol.request.ChatRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageCodecTest {

    private static final int MAX_FRAME_LENGTH = 65535; // 增加最大帧长度
    private static final int LENGTH_FIELD_OFFSET = 45; // 修正长度字段偏移量 (4 + 1 + 1 + 1 + 2 + 36)

    @Test
    public void testEncodeAndDecode() {
        // 1. 创建测试用的EmbeddedChannel
        EmbeddedChannel channel = new EmbeddedChannel(
            new LengthFieldBasedFrameDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, 4, 0, 0),
            new MessageCodec()
        );

        // 2. 创建测试消息
        ChatRequest original = ChatRequest.builder()
            .type(MessageType.CHAT_REQUEST)
            .sender("user1")
            .content("Hello, world!")
            .roomId("room1")
            .build();

        // 3. 写入消息（触发编码）
        assertTrue(channel.writeOutbound(original));
        
        // 4. 读取编码后的ByteBuf
        ByteBuf encoded = channel.readOutbound();
        
        // 5. 写入ByteBuf（触发解码）
        assertTrue(channel.writeInbound(encoded));
        
        // 6. 读取解码后的消息
        ProtocolMessage decoded = channel.readInbound();
        
        // 7. 验证解码后的消息
        assertNotNull(decoded);
        assertTrue(decoded.getType() == MessageType.CHAT_REQUEST);
        if (decoded.getType() == MessageType.CHAT_REQUEST) {
            ChatRequest decodedRequest = (ChatRequest) decoded;
            
            assertEquals(original.getRequestId(), decodedRequest.getRequestId());
            assertEquals(original.getSender(), decodedRequest.getSender());
            assertEquals(original.getContent(), decodedRequest.getContent());
            assertEquals(original.getRoomId(), decodedRequest.getRoomId());
        }
        
        // 8. 清理资源
        channel.finish();
    }

    @Test
    public void testMagicNumber() {
        EmbeddedChannel channel = new EmbeddedChannel(
            new MessageCodec()
        );
        
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        try {
            // 写入错误的魔数
            buf.writeInt(0x12345678); // 使用不同的魔数
            buf.writeByte(1); // 版本
            buf.writeByte(0); // 序列化方式
            buf.writeByte(0); // 消息类型
            buf.writeShort(0); // 状态码
            
            // 写入请求ID
            byte[] requestIdBytes = new byte[36];
            buf.writeBytes(requestIdBytes);
            
            // 写入消息长度和内容
            buf.writeInt(0); // 消息长度
            
            assertThrows(DecoderException.class, () -> {
                channel.writeInbound(buf.retain());
            });
        } finally {
            buf.release();
            channel.close();
        }
    }
}



