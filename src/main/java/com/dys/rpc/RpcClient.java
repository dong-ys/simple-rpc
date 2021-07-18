package com.dys.rpc;

import com.dys.rpc.manager.ClientProxy;
import com.dys.rpc.manager.RpcClientManager;
import com.dys.rpc.service.HelloService;
import com.dys.rpc.service.HelloServiceImpl2;

/**
 * 客户端测试
 *
 * @author chenlei
 */
public class RpcClient {
    public static void main(String[] args) {
        RpcClientManager clientManager = new RpcClientManager();
        //创建代理对象
        HelloService service = new ClientProxy(clientManager).getProxyService(HelloServiceImpl2.class);
        System.out.println(service.sayHello("zhangsan"));
    }
}
