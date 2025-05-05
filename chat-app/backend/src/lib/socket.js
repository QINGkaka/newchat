import {Server} from 'socket.io';
import http from 'http';
import express from 'express';
import jwt from 'jsonwebtoken';

const app = express();
const server = http.createServer(app);

// 记录在线用户 {userId:socketId}
const onlineUsersMap = {}

export function getReceiverSocketId(userId) {
    return onlineUsersMap[userId]
}

const io = new Server(server, {
    cors: {
        origin: "http://localhost:5173",
        methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
        allowedHeaders: ['Content-Type', 'Authorization', 'Accept'],
        credentials: true
    },
    path: '/socket.io',
    transports: ['websocket'],
    pingTimeout: 60000,
    pingInterval: 25000
});

// 添加认证中间件
io.use((socket, next) => {
    try {
        const token = socket.handshake.auth.token;
        if (!token) {
            return next(new Error('Authentication error: Token not provided'));
        }
        
        // 移除 Bearer 前缀
        const tokenWithoutBearer = token.replace('Bearer ', '');
        const decoded = jwt.verify(tokenWithoutBearer, process.env.JWT_SECRET);
        socket.userId = decoded.userId;
        next();
    } catch (error) {
        next(new Error('Authentication error: ' + error.message));
    }
});

// on(event, listener)
io.on('connection', (socket) => {
    console.log('a user connected.', "socket id:", socket.id);
    // 通过握手获取登陆用户的id
    const userId = socket.userId;
    if (userId) {
        onlineUsersMap[userId] = socket.id;
    }

    // 广播给其他用户
    io.emit('getOnlineUsers', Object.keys(onlineUsersMap));

    socket.on('disconnect',()=>{
        console.log('a user disconnected', socket.id);
        delete onlineUsersMap[userId]; // 删除这个键值对
        io.emit('getOnlineUsers', Object.keys(onlineUsersMap));
    })
});

export {io, app, server};
