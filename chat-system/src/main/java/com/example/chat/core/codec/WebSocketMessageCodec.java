package com.example.chat.core.codec;

import com.example.chat.model.ProtocolMessage;
import com.example.chat.util.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import java.util.List;

public class WebSocketMessageCodec extends MessageToMessageCodec<TextWebSocketFrame, ProtocolMessage> {
    
    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolMessage msg, List<Object> out) {
        String json = JsonUtil.toJson(msg);
        out.add(new TextWebSocketFrame(json));
    }
    
    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, List<Object> out) {
        String json = frame.text();
        ProtocolMessage protocolMessage = JsonUtil.fromJson(json, ProtocolMessage.class);
        out.add(protocolMessage);
    }
}

