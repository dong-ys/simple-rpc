package com.dys.rpc.service;

import com.dys.rpc.annotation.RpcServer;

@RpcServer
public class HelloServiceImpl2 implements HelloService{
    @Override
    public String sayHello(String name) {
        return "HelloService2, "+name;
    }
}
