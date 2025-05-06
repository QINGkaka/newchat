import { useChatStore } from "../store/useChatStore";
import { useAuthStore } from "../store/useAuthStore";
import { useEffect, useMemo, useCallback } from "react";
import { globalSocket } from "../store/useAuthStore";
import './UserList.css';

const UserList = () => {
    const { users, selectedUser, setSelectedUser } = useChatStore();
    const { onlineUsers, authUser, socketConnected } = useAuthStore();
    const { updateOnlineStatus } = useChatStore();

    useEffect(() => {
        // 当WebSocket连接建立时，请求更新在线用户列表
        if (socketConnected && globalSocket) {
            console.log('WebSocket connected, requesting online users update');
            globalSocket.emit('getOnlineUsers');
        }
    }, [socketConnected]);

    // 使用useCallback优化isUserOnline函数
    const isUserOnline = useCallback((user) => {
        if (!user) return false;
        
        // 如果是当前用户且WebSocket已连接，则显示为在线
        if (user.id === authUser?.id) {
            return socketConnected;
        }
        
        // 查找用户在在线列表中的状态
        const onlineUser = onlineUsers.find(u => u.id === user.id);
        const isOnline = onlineUser?.online === true;
        
        // 更新在线状态缓存
        updateOnlineStatus(user.id, isOnline);
        
        return isOnline;
    }, [onlineUsers, socketConnected, authUser]);

    // 使用useMemo优化在线用户列表
    const onlineUsersList = useMemo(() => {
        return users.filter(user => isUserOnline(user));
    }, [users, isUserOnline]);

    // 计算真实在线用户数量
    const onlineCount = useMemo(() => {
        return onlineUsersList.length;
    }, [onlineUsersList]);

    // 使用useMemo优化用户列表渲染
    const userListItems = useMemo(() => {
        return users.map(user => {
            const isOnline = isUserOnline(user);
            return (
                <div
                    key={user.id}
                    className={`user-item ${selectedUser?.id === user.id ? 'selected' : ''}`}
                    onClick={() => setSelectedUser(user)}
                >
                    <div className="user-info">
                        <span className="username">{user.username}</span>
                        <div className="user-status">
                            <span className={`status-indicator ${isOnline ? 'online' : 'offline'}`} />
                            <span className="status-text">
                                {isOnline ? '在线' : '离线'}
                            </span>
                        </div>
                    </div>
                </div>
            );
        });
    }, [users, selectedUser, isUserOnline, setSelectedUser]);

    return (
        <div className="user-list-container">
            <div className="user-list-header">
                <h2>联系人</h2>
                <div className="online-count">
                    {onlineCount} 位用户在线
                </div>
            </div>
            <div className="user-list">
                {userListItems}
            </div>
        </div>
    );
};

export default UserList; 