package com.jeremy.www.registry;

import com.jeremy.www.registry.registryCenter.IRegistryCenter;
import com.jeremy.www.registry.registryCenter.RegistryCenterWithZk;
import com.jeremy.www.utils.NetUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RpcRegistry {
    private int port;

    //zk中保存服务名称与服务地址的映射
    IRegistryCenter registryCenter = new RegistryCenterWithZk();

    //保存服务名称与服务调用的映射
    public static ConcurrentHashMap<String,Object> registryMap = new ConcurrentHashMap<String, Object>();

    //保存所有相关的服务类
    private List<String> classNames = new ArrayList<String>();

    public RpcRegistry(int port) {
        this.port = port;
        //完成递归扫描
        scannerClass("com.jeremy.www.provider");
        doRegister();
    }

    public void start() {
        //向zk注册服务名称及服务地址
        for (Map.Entry<String, Object> entry : registryMap.entrySet()) {
            registryCenter.registry(entry.getKey(), NetUtil.getLocalAddress() + ":" + port);
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            //自定义协议解码器
                            /** 入参有5个，分别解释如下
                             maxFrameLength：框架的最大长度。如果帧的长度大于此值，则将抛出TooLongFrameException。
                             lengthFieldOffset：长度字段的偏移量：即对应的长度字段在整个消息数据中得位置
                             lengthFieldLength：长度字段的长度。如：长度字段是int型表示，那么这个值就是4（long型就是8）
                             lengthAdjustment：要添加到长度字段值的补偿值
                             initialBytesToStrip：从解码帧中去除的第一个字节数
                             */
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                            //自定义协议编码器
                            pipeline.addLast(new LengthFieldPrepender(4));
                            //对象参数类型编码器
                            pipeline.addLast("encoder", new ObjectEncoder());
                            //对象参数类型解码器
                            pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
                            pipeline.addLast(new RegistryHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(port).sync();
            System.out.println("GP RPC Registry start listen at" + port);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }



    /**
     * 递归扫描
     * @param packageName
     */
    private void scannerClass(String packageName) {
        URL url = this.getClass().getClassLoader().getResource(packageName.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for(File file : dir.listFiles()){
            //如果是一个文件夹，继续递归
            if(file.isDirectory()){
                scannerClass(packageName + "." + file.getName());
            }else{
                classNames.add(packageName + "." + file.getName().replace(".class","").trim());
            }
        }
    }

    /**
     * 完成注册
     */
    private void doRegister() {
        if(classNames.size() == 0){
            return;
        }
        for(String className:classNames){
            try {
                Class<?> clazz = Class.forName(className);
                Class<?> i = clazz.getInterfaces()[0];
                registryMap.put(i.getSimpleName(),clazz.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    public static void main(String[] args) {
        new RpcRegistry(7777).start();
    }
}