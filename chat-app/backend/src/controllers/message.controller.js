import User from "../models/user.module.js";
import Message from "../models/message.model.js";
import cloudinary from "../lib/cloudinary.js";
import { getReceiverSocketId } from "../lib/socket.js";
import { io } from "../lib/socket.js";

export const getUsersForSidebar = async (req, res) => {
    try {
        // 获取登陆用户id
        const loggedInUserId = req.user._id;
        // 从数据库查询除了该用户的其他用户
        const filteredUsers = await User.find({_id:{$ne:loggedInUserId}}).select('-password');
        // 将找出的用户加入响应中
        console.log('filteredUsers' ,filteredUsers);
        return res.status(200).json(filteredUsers);
    } catch(error) {
        console.error("error in getUsersForSidebar",error);
        return res.status(500).json({message:"Internal Server Error"});
    }
}

export const getMessages = async (req, res) => {
    try {
        const {id:userToChatId} = req.params;
        const myId = req.user._id;

        // 找出两人之间的聊天记录
        const messages = await Message.find({
            $or: [
                {senderId: myId, receiverId: userToChatId},
                {senderId: userToChatId, receiverId: myId}
            ]
        })

        res.status(200).json(messages);
    } catch (error) {
        console.error("error in getMessages",error);
        res.status(500).json({message:"Internal Server Error"});
    }
}

export const sendMessage = async (req, res) => {
    try {
        const {text, image} = req.body;
        const {id:receiverId} = req.params;
        const myId = req.user._id;

        let imageUrl;
        if (image) {
            // 上传base64编码的图片
            const uploadresonse = await cloudinary.uploader.upload(image);
            imageUrl = uploadresonse.secure_url;
        }

        // 创建消息
        const newMessage = new Message({
            senderId: myId,
            receiverId,
            text,
            image:imageUrl
        });

        // 存储到数据库
         await newMessage.save();

        
         const receiverSocketId = getReceiverSocketId(receiverId);
         if (receiverSocketId) {
            // 1对1发消息
            io.to(receiverSocketId).emit("newMessage", newMessage);
         }

        // 201 created
        res.status(201).json(newMessage);
    } catch (error) {
        console.error("error in sendMessage",error);
        res.status(500).json({message:"Internal Server Error"});
    }
}