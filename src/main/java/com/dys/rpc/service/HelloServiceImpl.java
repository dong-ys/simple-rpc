package com.dys.rpc.service;

import com.dys.rpc.annotation.RpcServer;

@RpcServer
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "HelloService1, " + name;
    }
}
