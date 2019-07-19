package com.jeremy.www.registry.registryCenter;

public interface IRegistryCenter {

    /**
     * 实现服务的管理
     * @param serviceName       服务注册名称
     * @param serviceAddress    服务注册地址
     */
    void registry(String serviceName, String serviceAddress);
}
