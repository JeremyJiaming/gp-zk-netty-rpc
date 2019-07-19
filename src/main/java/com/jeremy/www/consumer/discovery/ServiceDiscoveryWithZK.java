package com.jeremy.www.consumer.discovery;

import com.jeremy.www.conf.ZkConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.ArrayList;
import java.util.List;

public class ServiceDiscoveryWithZK implements IServiceDiscovery{
    CuratorFramework curatorFramework = null;
    List<String> serviceRepos = new ArrayList(); //服务地址的本地缓存

    {
        //初始化zk的链接，会话超时时间是5s，衰减重试
        curatorFramework = CuratorFrameworkFactory.builder().
                connectString(ZkConfig.CONNECTION_STR).sessionTimeoutMs(5000).
                retryPolicy(new ExponentialBackoffRetry(1000,3)).
                namespace("registry")
                .build();
        curatorFramework.start();
    }


    public String discovery(String serviceName) {
        //完成服务地址的查找
        String path = "/" + serviceName;
        if(serviceRepos.isEmpty()){
            try {
                serviceRepos = curatorFramework.getChildren().forPath(path);
                registryWatch(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //针对已有地址做负载均衡
        LoadBalanceStrategy loadBalance = new RandomLoadBalance();
        return loadBalance.selectHost(serviceRepos);
    }

    private void registryWatch(final String path) throws Exception {
        PathChildrenCache nodeCache = new PathChildrenCache(curatorFramework, path, true);
        PathChildrenCacheListener nodeCacheListener = new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework curatorFramework1, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                System.out.println("客户端收到节点变更的事件");
                serviceRepos = curatorFramework1.getChildren().forPath(path);//再次更新本地的缓存
            }
        };
        nodeCache.getListenable().addListener(nodeCacheListener);
        nodeCache.start();
    }
}
