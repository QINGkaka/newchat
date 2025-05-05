import { useChatStore } from "../store/useChatStore";
import { useAuthStore } from "../store/useAuthStore";

const UserList = () => {
    const { users, selectedUser, setSelectedUser } = useChatStore();
    const { onlineUsers, authUser } = useAuthStore();

    const isUserOnline = (user) => {
        console.log('Checking online status for user:', user);
        console.log('Current online users:', onlineUsers);
        
        // 如果是当前用户
        if (user.id === authUser.id) {
            console.log('This is the current user');
            return authUser.online;
        }
        
        // 检查用户是否在在线列表中
        const onlineUser = onlineUsers.find(u => u.id === user.id);
        console.log('Found online user:', onlineUser);
        return onlineUser?.online || false;
    };

    return (
        <div className="flex flex-col h-full">
            <div className="flex-1 overflow-y-auto">
                {users.map((user) => (
                    <div
                        key={user.id}
                        onClick={() => setSelectedUser(user)}
                        className={`flex items-center gap-4 p-4 cursor-pointer hover:bg-gray-100 ${
                            selectedUser?.id === user.id ? "bg-gray-100" : ""
                        }`}
                    >
                        <div className="relative">
                            <img
                                src={user.profilePicture || "/avatar.png"}
                                alt="profile pic"
                                className="size-12 rounded-full"
                            />
                            <div
                                className={`absolute bottom-0 right-0 size-3 rounded-full border-2 border-white ${
                                    isUserOnline(user) ? "bg-green-500" : "bg-gray-400"
                                }`}
                            />
                        </div>
                        <div className="flex-1">
                            <h3 className="font-semibold">{user.fullName}</h3>
                            <p className="text-sm text-gray-500">{user.email}</p>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default UserList; 