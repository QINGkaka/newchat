import User from "../models/user.module.js";
import bcrypt from "bcryptjs";
import jwt from 'jsonwebtoken';
import cloudinary from "../lib/cloudinary.js";

export const signup = async (req, resp) => {
    try {
        const {fullName, email, password} = req.body;
        if (!fullName || !email || !password) {
            return resp.status(400).json({message:"所有字段都是必填项"});
        }
        // 检查密码长度
        if (password.length < 6) {
            return resp.status(400).json({message: "密码至少需要6位"});
        }
        // 检查邮箱是否注册过
        const user = await User.findOne({email});
        if (user) {
            return resp.status(400).json({message: "邮箱已经被注册，请更换邮箱"});
        }
        // 对密码哈希 bcryptjs
        // begin
        //获取salt盐值
        const salt = await bcrypt.genSalt(10);
        // 使用salt进行哈希
        const hashedPassword = await bcrypt.hash(password, salt);
        // end
        // 创建新用户
        const newUser = new User({
            fullName, email, password: hashedPassword,
        })
        // 创建新用户成功
        if (newUser) {
            await newUser.save();
            // 生成jwt token
            const token = jwt.sign({ userId: newUser._id }, process.env.JWT_SECRET, {
                expiresIn: process.env.JWT_EXPIRES
            });
            // 201 created
            resp.status(201).json({
                user: {
                    _id: newUser._id,
                    fullName: newUser.fullName,
                    email: newUser.email,
                    profilePic: newUser.profilePic,
                },
                token
            });
        } else {
            resp.status(400).json({message: "用户数据无效"});
        }
    } catch (error) {
        console.error("Error in signup controller", error);
        resp.status(500).json({message: "Internal server Error"});
    }
}

export const login = async (req, resp) => {
    const {email, password} = req.body;
    try {
        // 数据库查询 by email
        const user = await User.findOne({email});
        if (!user) {
            return resp.status(400).json({message:"认证信息错误"});
        }
        // 密码比对 bcrypt.compare
        const isPasswordCorrect = await bcrypt.compare(password, user.password);
        if (!isPasswordCorrect) {
            return resp.status(400).json({message:"认证信息错误"});
        }

        // 生成jwt token
        const token = jwt.sign({ userId: user._id }, process.env.JWT_SECRET, {
            expiresIn: process.env.JWT_EXPIRES
        });

        resp.status(200).json({
            user: {
                _id: user._id,
                fullName: user.fullName,
                email: user.email,
                profilePic: user.profilePic,
            },
            token
        });
    } catch (error) {
        console.error("Error in login controller", error);
        resp.status(500).json({message: "Internal server Error"});
    }
}

export const logout = (req, resp) => {
    try {
        // 清除jwt
        resp.cookie('jwt',"", {maxAge: 0});
        resp.status(200).json({message: "Logged out successfully"});
    } catch (error) {
        console.error("Error in logout controller", error);
        resp.status(500).json({message: "Internal server Error"});
    }
}

export const updateProfile = async (req, resp) => {
    try {
        const {profilePic} = req.body;
        //  在protectRoute添加user到req的
        const userId = req.user._id;
        if (!profilePic) {
            return resp.status(400).json({message:"没有上传图片"});
        }
        const uploadResponse = await cloudinary.uploader.upload(profilePic);
        // If you set new: true, findOneAndUpdate() will instead give you the object after update was applied.
        const updatedUser = await User.findByIdAndUpdate(userId,
            {profilePic: uploadResponse.secure_url},
            {new:true}
        );
        resp.status(200).json({authUser:updatedUser,})
    } catch (error) {
        console.error("Error in updateProfile controller", error);
        resp.status(500).json({message: "Internal server Error"});
    }
}

export const checkAuth = (req, resp) => {
    try {
        const token = jwt.sign({ userId: req.user._id }, process.env.JWT_SECRET, {
            expiresIn: process.env.JWT_EXPIRES
        });
        resp.status(200).json({
            authenticated: true,
            user: {
                _id: req.user._id,
                fullName: req.user.fullName,
                email: req.user.email,
                profilePic: req.user.profilePic,
            },
            token
        });
    } catch (error) {
        console.log("Error in checkAuth controller", error.message);
        resp.status(500).json({ message: "Internal Server Error" });
    }
}