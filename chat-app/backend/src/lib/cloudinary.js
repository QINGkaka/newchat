import {v2 as cloudinary} from "cloudinary";

import {config} from "dotenv";

config();// 这样就可以访问环境变量了

cloudinary.config({
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET,
})

export default cloudinary;
