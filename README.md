# simple-rpc
netty+nacos手写简易RPC框架

项目架构：

```java
rpc
├── annotation -- 自定义注解
├── config -- 序列化配置
├── facotry -- 提供服务工厂
├── loadBalancer -- 负载均衡
├── manager -- 客户端和服务端的管理器
├── message -- 自定义协议
├── nettyHandler -- netty处理器
├── protocol -- 消息编码解码器
├── registery -- 注册中心
├── service -- 本地接口实例
├── utils -- 工具类
├── message -- 序列化配置
├── RpcClient -- 客户端测试
└── RpcServer -- 服务端测试
```

设计要点：

1. 使用Netty(基于NIO)实现网络传输
2. 自定义通信协议，自己设计简易的Message作为传输的消息体，协议的设计如下：
   * 4字节的魔数
   * 1字节的版本
   * 1字节的序列化方式
   * 1字节的指令类型
   * 消息体长度以及内容

3. 可在配置文件中配置指定的序列化方式
4. 实现简易的负载均衡，包含随机获取以及轮询获取
5. 使用Nacos作为服务注册中心
6. 使用Future包装返回结果，异步接收

