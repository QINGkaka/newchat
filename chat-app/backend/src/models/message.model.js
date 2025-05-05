import mongoose  from "mongoose";

const messageSchema = new mongoose.Schema({
    senderId: {
        // 外键引用
        type: mongoose.Schema.Types.ObjectId,
        ref: "User",
        required: true,
    },
    receiverId: {
        // 外键引用
        type: mongoose.Schema.Types.ObjectId,
        ref: "User",
        required: true,
    },
    text: {
        type: String,
    },
    image: {
        type: String,
    },
}, { timestamps: true });

const Message = mongoose.model('Message', messageSchema);

export default Message;