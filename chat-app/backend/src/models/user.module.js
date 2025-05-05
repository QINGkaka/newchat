import mongoose from 'mongoose';

// 定义schema
const userScheme = new mongoose.Schema({
    email:{
        type: String,
        required: true,
        unique: true,
    },
    fullName: {
        type: String,
        required: true,
    },
    password: {
        type: String,
        required: true,
        minlength: 6,
    },
    profilePic: {
        type: String,
        default: '',
    },
}, { timestamps: true }); // createAt updateAt

// 定义model
const User = mongoose.model('User', userScheme);

export default User;