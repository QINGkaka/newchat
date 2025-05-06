import { X } from "lucide-react";
import { useAuthStore } from "../store/useAuthStore";
import { useChatStore } from "../store/useChatStore";
import { useTranslation } from "react-i18next";

const ChatHeader = () => {
    const { selectedUser, setSelectedUser } = useChatStore();
    const { onlineUsers } = useAuthStore();
    const { t } = useTranslation();

    // 判断用户是否在线
    const isUserOnline = (user) => {
        if (!user) return false;
        const onlineUser = onlineUsers.find(u => u.id === (user.id || user._id));
        return onlineUser?.online === true;
    };

    return (
        <div className="p-2.5 border-b border-base-300">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    {/* Avatar */}
                    <div className="avatar">
                        <div className="size-10 rounded-full relative">
                            <img src={selectedUser?.profilePic || "/avatar.png"} alt={selectedUser?.fullName} />
                            {isUserOnline(selectedUser) && (
                                <span className="absolute bottom-0 right-0 size-3 bg-green-500 rounded-full ring-2 ring-zinc-900" />
                            )}
                        </div>
                    </div>

                    {/* User info */}
                    <div>
                        <h3 className="font-medium">{selectedUser?.fullName}</h3>
                        <p className="text-sm text-base-content/70">
                            {isUserOnline(selectedUser) ? t("online") : t("offline")}
                        </p>
                    </div>
                </div>

                {/* Close button */}
                <button onClick={() => setSelectedUser(null)}>
                    <X />
                </button>
            </div>
        </div>
    );
};
export default ChatHeader;
