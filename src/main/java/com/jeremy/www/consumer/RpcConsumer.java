package com.jeremy.www.consumer;

import com.jeremy.www.api.IRpcHelloService;
import com.jeremy.www.api.IRpcService;
import com.jeremy.www.consumer.proxy.RpcProxy;

public class RpcConsumer {
    public static void main(String[] args) {
        IRpcHelloService rpcHelloService = RpcProxy.create(IRpcHelloService.class);

        System.out.println(rpcHelloService.hello("Tom老师"));

        IRpcService rpcService = RpcProxy.create(IRpcService.class);

        System.out.println("8 + 2 = " + rpcService.add(8, 2));
        System.out.println("8 - 2 = " + rpcService.sub(8, 2));
        System.out.println("8 * 2 = " + rpcService.mult(8, 2));
        System.out.println("8 / 2 = " + rpcService.div(8, 2));
    }
}
