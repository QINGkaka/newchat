package com.example.chat.core.handler;

import com.example.chat.model.User;
import com.example.chat.protocol.request.LoginRequest;
import com.example.chat.protocol.response.ErrorResponse;
import com.example.chat.protocol.response.LoginResponse;
import com.example.chat.protocol.response.StatusCode;
import com.example.chat.service.UserService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AuthHandler extends SimpleChannelInboundHandler<LoginRequest> {

    private final UserService userService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequest msg) throws Exception {
        if (isAuthenticated(ctx)) {
            // 如果已经认证过，直接传递给下一个处理器
            ctx.fireChannelRead(msg);
            return;
        }
        
        //处理登录消息
        try {
            // 使用login方法进行认证
            User user = userService.login(msg.getUsername(), msg.getPassword());
            if (user != null) {
                ctx.channel().attr(AttributeKey.valueOf("userId")).set(user.getId());
                LoginResponse loginResponse = LoginResponse.builder()
                    .token(user.getId()) // 使用用户ID作为临时token
                    .userId(user.getId())
                    .username(user.getUsername())
                    .requestId(msg.getRequestId())
                    .build();
                ctx.writeAndFlush(loginResponse);
            } else {
                ErrorResponse errorResponse = ErrorResponse.create(
                    StatusCode.UNAUTHORIZED, 
                    "Invalid credentials"
                );
                ctx.writeAndFlush(errorResponse);
                ctx.close();
            }
        } catch (Exception e) {
            log.error("Authentication error", e);
            ctx.close();
        }
    }
    
    private boolean isAuthenticated(ChannelHandlerContext ctx) {
        return ctx.channel().hasAttr(AttributeKey.valueOf("userId"));
    }
}











