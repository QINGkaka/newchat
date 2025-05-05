# Socket.IO 集成测试步骤

## 1. 启动服务

1. 确保已安装所有依赖项
   ```
   mvn clean install
   ```

2. 启动 chat-system 服务
   ```
   mvn spring-boot:run
   ```

3. 验证服务是否正常运行
   - 访问 http://localhost:8080/health
   - 确认 Socket.IO 服务器状态为 "UP"

## 2. 测试连接

### 使用简单测试客户端

1. 运行 SimpleSocketClient 类
   ```
   mvn exec:java -Dexec.mainClass="com.example.chat.tools.SimpleSocketClient"
   ```

2. 观察连接是否成功，并尝试发送消息

### 使用 chat-app 前端

1. 确保 chat-app 前端配置正确
   - 检查 `chat-app/frontend/src/lib/socket.js` 中的连接地址
   - 确保指向 `http://localhost:9092`

2. 启动 chat-app 前端
   ```
   cd chat-app/frontend
   npm install
   npm start
   ```

3. 登录并检查连接状态
   - 打开浏览器控制台
   - 查看是否有 "Connected to Socket.IO server" 日志

## 3. 测试功能

### 测试用户在线状态

1. 使用两个不同的浏览器或隐私窗口登录不同用户
2. 观察用户列表中的在线状态是否正确更新
3. 关闭一个窗口，观察另一个窗口中的用户状态是否变为离线

### 测试消息发送和接收

1. 在一个窗口中向另一个用户发送消息
2. 确认消息能够正确发送和接收
3. 检查消息格式是否正确

### 测试房间功能

1. 创建一个聊天室
2. 让多个用户加入同一聊天室
3. 发送消息到聊天室
4. 确认所有用户都能收到消息

## 4. 故障排除

### 连接问题

- 检查防火墙设置
- 确认端口 9092 是否开放
- 检查 CORS 配置

### 认证问题

- 检查 token 格式
- 确认 UserService.validateToken 方法能正确处理 token

### 消息格式问题

- 检查 convertToClientFormat 方法
- 确保消息格式与 chat-app 前端期望的格式一致