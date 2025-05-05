import { createContext, useContext, useEffect } from 'react';
import { useAuthContext } from './AuthContext';
import io from 'socket.io-client';
import { useUserContext } from './UserContext';

const SocketContext = createContext();

export const useSocketContext = () => {
  return useContext(SocketContext);
};

export const SocketContextProvider = ({ children }) => {
  const { authUser } = useAuthContext();
  const { setOnlineUsers, addMessage } = useUserContext();

  // 修改为 Java 后端的 Socket.IO 地址
  const socket = io('http://localhost:19098', {
    autoConnect: false,
    withCredentials: true,
    transports: ['websocket', 'polling'],
    path: '/socket.io',
    extraHeaders: {
      "Authorization": `Bearer ${localStorage.getItem('token') || ''}`
    }
  });

  useEffect(() => {
    if (authUser) {
      socket.connect();
    }

    return () => {
      socket.disconnect();
    };
  }, [authUser]);

  useEffect(() => {
    if (!socket) return;

    socket.on('getOnlineUsers', (users) => {
      setOnlineUsers(users);
    });

    socket.on('newMessage', (message) => {
      if (message.senderId !== authUser?._id) {
        addMessage(message);
      }
    });

    return () => {
      socket.off('getOnlineUsers');
      socket.off('newMessage');
    };
  }, [socket, authUser, setOnlineUsers, addMessage]);

  return (
    <SocketContext.Provider value={{ socket }}>
      {children}
    </SocketContext.Provider>
  );
};