package com.dys.rpc.manager;

import com.alibaba.nacos.api.exception.NacosException;
import com.dys.rpc.annotation.RpcServer;
import com.dys.rpc.annotation.RpcServerScan;
import com.dys.rpc.factory.ServiceFactory;
import com.dys.rpc.nettyHandler.HeartBeatServerHandler;
import com.dys.rpc.nettyHandler.PingMessageHandler;
import com.dys.rpc.nettyHandler.RpcRequestMessageHandler;
import com.dys.rpc.protocol.MessageCodecSharable;
import com.dys.rpc.protocol.ProcotolFrameDecoder;
import com.dys.rpc.registery.NacosServerRegistry;
import com.dys.rpc.registery.ServerRegistry;
import com.dys.rpc.utils.PackageScanUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Rpc服务端管理器
 */
public class RpcServiceManager {

    protected String host;
    protected int port;
    protected ServerRegistry serverRegistry;
    protected ServiceFactory serviceFactory;
    NioEventLoopGroup worker = new NioEventLoopGroup();
    NioEventLoopGroup boss = new NioEventLoopGroup();
    ServerBootstrap bootstrap = new ServerBootstrap();

    public RpcServiceManager(String host, int port) {
        this.host = host;
        this.port = port;
        serverRegistry = new NacosServerRegistry();
        serviceFactory = new ServiceFactory();
        autoRegistry();
    }

    /**
     * 开启服务
     */
    public void start() {
        //日志
        LoggingHandler LOGGING = new LoggingHandler(LogLevel.DEBUG);
        //消息节码器
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        //RPC请求处理器
        RpcRequestMessageHandler RPC_HANDLER = new RpcRequestMessageHandler();
        //心跳处理器
        HeartBeatServerHandler HEARTBEAT_SERVER = new HeartBeatServerHandler();
        //心跳请求的处理器
        PingMessageHandler PING_MESSAGE = new PingMessageHandler();
        try {
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 256)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new ProcotolFrameDecoder());//定长解码器
                            pipeline.addLast(MESSAGE_CODEC);
                            pipeline.addLast(LOGGING);
                            //pipeline.addLast(HEARTBEAT_SERVER);
                            //pipeline.addLast(PING_MESSAGE);
                            pipeline.addLast(RPC_HANDLER);
                        }
                    });
            //绑定端口
            Channel channel = bootstrap.bind(port).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("启动服务出错");
        }finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }
    }

    /**
     * 自动扫描@RpcServer注解  注册服务
     */
    public void autoRegistry() {
        String mainClassPath = PackageScanUtils.getStackTrace();
        Class<?> mainClass;
        try {
            mainClass = Class.forName(mainClassPath);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("启动类为找到");
        }
        if (mainClass.isAnnotationPresent(RpcServer.class)) {
            throw new RuntimeException("启动类缺少@RpcServer 注解");
        }
        String annotationValue = mainClass.getAnnotation(RpcServerScan.class).value();
        //如果注解路径的值是空，则等于main父路径包下
        if ("".equals(annotationValue)) {
            annotationValue = mainClassPath.substring(0, mainClassPath.lastIndexOf("."));
        }
        //获取所有类的set集合
        Set<Class<?>> set = PackageScanUtils.getClasses(annotationValue);
        for (Class<?> c : set) {
            //只有有@RpcServer注解的才注册
            if (c.isAnnotationPresent(RpcServer.class)) {
                String ServerNameValue = c.getAnnotation(RpcServer.class).name();
                Object object;
                try {
                    object = c.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    System.err.println("创建对象" + c + "发生错误");
                    continue;
                }
                //注解的值如果为空，使用类名
                if ("".equals(ServerNameValue)) {
                   addServer(object,c.getCanonicalName());
                } else {
                    addServer(object, ServerNameValue);
                }
            }
        }
    }

    /**
     * 添加对象到工厂和注册到注册中心
     *
     * @param server
     * @param serverName
     * @param <T>
     * @throws NacosException
     */
    public <T> void addServer(T server, String serverName) {
        serviceFactory.addServiceProvider(server, serverName);
        serverRegistry.register(serverName, new InetSocketAddress(host, port));
    }

}
