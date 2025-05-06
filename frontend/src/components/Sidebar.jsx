import { useEffect, useState } from "react";
import { useChatStore } from "../store/useChatStore";
import { useAuthStore } from "../store/useAuthStore";
import SidebarSkeleton from "./skeletons/SidebarSkeleton";
import { Users } from "lucide-react";
import {useTranslation} from 'react-i18next';

const Sidebar = () => {
  const {t} = useTranslation();
  const { getUsers, users, selectedUser, setSelectedUser, isUsersLoading } = useChatStore();
  const { onlineUsers } = useAuthStore();
  
  const [showOnlineOnly, setShowOnlineOnly] = useState(false);

  useEffect(() => {
    getUsers();
  }, [getUsers]);

  // 判断用户是否在线
  const isUserOnline = (user) => {
    const onlineUser = onlineUsers.find(u => u.id === (user.id || user._id));
    return onlineUser?.online === true;
  };

  // 计算在线用户数量
  const onlineCount = users.filter(isUserOnline).length;

  const filteredUsers = showOnlineOnly
    ? users.filter(isUserOnline)
    : users;

  console.log("在线用户状态:", {
    totalUsers: users.length,
    onlineCount,
    onlineUsers: onlineUsers.filter(u => u.online).map(u => u.username)
  });

  if (isUsersLoading) return <SidebarSkeleton />;

  return (
    <aside className="h-full w-20 lg:w-72 border-r border-base-300 flex flex-col transition-all duration-200">
      <div className="border-b border-base-300 w-full p-5">
        <div className="flex items-center gap-2">
          <Users className="size-6" />
          <span className="font-medium hidden lg:block">{t('contacts')}</span>
        </div>
        <div className="mt-3 hidden lg:flex items-center gap-2">
          <label className="cursor-pointer flex items-center gap-2">
            <input
              type="checkbox"
              checked={showOnlineOnly}
              onChange={(e) => setShowOnlineOnly(e.target.checked)}
              className="checkbox checkbox-sm"
            />
            <span className="text-sm">{t('showOnlineOnly')}</span>
          </label>
          <span className="text-xs text-zinc-500">({onlineCount} {t('online')})</span>
        </div>
      </div>

      <div className="overflow-y-auto w-full py-3">
        {filteredUsers.map((user) => {
          const isOnline = isUserOnline(user);
          const userId = user.id || user._id;
          return (
            <button
              key={`user-${userId}`}
              onClick={() => setSelectedUser(user)}
              className={`
                w-full p-3 flex items-center gap-3
                hover:bg-base-300 transition-colors
                ${selectedUser?.id === userId || selectedUser?._id === userId ? "bg-base-300 ring-1 ring-base-300" : ""}
              `}
            >
              <div className="relative mx-auto lg:mx-0">
                <img
                  src={user.profilePicture || "/avatar.png"}
                  alt={user.name}
                  className="size-12 object-cover rounded-full"
                />
                {isOnline && (
                  <span
                    className="absolute bottom-0 right-0 size-3 bg-green-500
                    rounded-full ring-2 ring-zinc-900"
                  />
                )}
              </div>

              <div className="hidden lg:block text-left min-w-0">
                <div className="font-medium truncate">{user.fullName}</div>
                <div className="text-sm text-zinc-400">
                  {isOnline ? t('online') : t('offline')}
                </div>
              </div>
            </button>
          );
        })}

        {filteredUsers.length === 0 && (
          <div className="text-center text-zinc-500 py-4">{t('noOnlineUsers')}</div>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
