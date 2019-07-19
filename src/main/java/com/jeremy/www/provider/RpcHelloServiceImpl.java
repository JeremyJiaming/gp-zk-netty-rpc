package com.jeremy.www.provider;

import com.jeremy.www.api.IRpcHelloService;

public class RpcHelloServiceImpl implements IRpcHelloService{

    public String hello(String name) {
        return "Hello" + name +"!";
    }
}
