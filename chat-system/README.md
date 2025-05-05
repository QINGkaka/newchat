# NIO-CHATROOM
一个基于Netty的高性能WebSocket聊天室系统，支持实时通信、房间管理、消息持久化等功能。
## 功能特性
- 💬 实时聊天：基于WebSocket的实时消息通信
- 👥 房间管理：支持创建、加入、退出聊天室
- 🔐 用户认证：完整的用户登录和认证机制
- 💾 消息存储：使用Redis进行消息持久化
- 💪 高性能：基于Netty的高性能网络框架
- 💻 客户端UI：使用JavaFX实现的现代化界面
## 技术栈
- 后端框架：Spring Boot
- 网络框架：Netty 4.1.86- 存储系统：Redis
- 客户端UI：JavaFX 17- 构建工具：Maven
- JDK版本：21
## 快速开始

### 环境要求
- JDK 21+- Maven 3.6+
- Redis 6.0+

### 配置说明
服务器配置文件位于 `src/main/resources/application.yaml`：
```yamlserver:
  port: 8080  websocket:
    path: /ws
```redis:  host: localhost
  port: 6379```
## 项目结构
```
src/main/java/com/example/chat/
├── config/          # 配置类
├── core/            # 核心组件│   
    ├── codec/      # 消息编解码
    ├── handler/    # 消息处理器│   
    └── server/     # 服务器实现
├── dao/            # 数据访问层
├── model/          # 数据模型
├── protocol/       # 通信协议
├── service/        # 业务服务
└── util/           # 工具类```
## 开发指南
### 添加新的消息类型
1. 在 `MessageType.java` 中添加新的消息类型
2. 在 `protocol` 包中创建对应的请求/响应类
3. 在 `WebSocketServerHandler` 中实现消息处理逻辑
### 自定义存储实现
1. 实现 `MessageDao` 接口
2. 在配置中替换默认的 Redis 实现
## 性能优化
- 使用线程池处理消息- Redis连接池配置
- Netty参数调优




**核心服务器组件 (`core` 包):**

\- ChatServer.java: Netty服务器的主类，负责启动和配置WebSocket服务器

\- ChatServerInitializer.java: 配置Netty的Channel Pipeline，设置各种处理器

\- WebSocketServerHandler.java: 处理WebSocket消息的核心处理器，包含登录、聊天、房间管理等业务逻辑

\- WebSocketMessageCodec.java: WebSocket消息的编解码器，负责消息序列化和反序列化

**配置相关 (`config` 包):**

\- ChatConfig.java: Spring Boot配置类，包含服务器、Redis和线程池的配置

\- application.yaml: 应用程序配置文件，包含端口、Redis、线程池等配置项

**数据访问层 (`dao` 包):**

\- RedisMessageDao.java: 使用Redis实现消息存储的DAO实现

**服务层 (`service` 包):**

\- MessageServiceImpl.java: 消息服务实现，处理消息的存储和广播

\- UserService: 用户管理服务（接口）

\- RoomService: 聊天室管理服务（接口）

\- MessageService: 消息管理服务（接口）

**客户端组件:**

\- chat.fxml: 聊天界面的JavaFX布局文件

\- login.fxml: 登录界面的JavaFX布局文件

**工具类:**

\- MessageBroadcaster.java: 消息广播工具类，管理Channel组和消息广播

\- ChannelUtil.java: Channel管理工具类

\- JsonUtil.java: JSON序列化工具类

**项目配置文件:**

\- pom.xml: Maven项目配置文件，包含依赖项：

 \* Netty: 网络通信框架

 \* Spring Boot: Web框架

 \* Jackson: JSON处理

 \* Jedis: Redis客户端

 \* JavaFX: 客户端GUI

 \* JUnit: 测试框架

 \* Logback: 日志框架





















































# NIO-CHATROOM
