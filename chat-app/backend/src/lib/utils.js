import jwt from 'jsonwebtoken';

export const generateJWT = (userId, resp) => {
    // 进行签名
    const token = jwt.sign({userId}, process.env.JWT_SECRET, {
        expiresIn: process.env.JWT_EXPIRES,
    });

    // 设置cookie
    resp.cookie('jwt', token, {
        maxAge: 7*24*60*60*1000, // 7 days in ms
        httpOnly: true, // prevent XSS attacks cross-site scripting attacks
        sameSite: 'strict', // CSRF attacks cross-site request forgery attacks
        secure: process.env.NODE_ENV !== 'development',
    })

    return resp;
}