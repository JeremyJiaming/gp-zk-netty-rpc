package com.jeremy.www.consumer.discovery;

public interface IServiceDiscovery {

    //根据服务名称返回服务地址
    String  discovery(String serviceName);
}
