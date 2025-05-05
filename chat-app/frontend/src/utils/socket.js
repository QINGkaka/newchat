import { io } from 'socket.io-client';

const socket = io('http://localhost:19098', {
    transports: ['websocket'],
    autoConnect: false,
    reconnection: true
});

export default socket;