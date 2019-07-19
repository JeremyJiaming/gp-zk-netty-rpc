package com.jeremy.www.consumer.proxy;

import com.jeremy.www.consumer.discovery.IServiceDiscovery;
import com.jeremy.www.consumer.discovery.ServiceDiscoveryWithZK;
import com.jeremy.www.protocol.InvokerProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RpcProxy {
    private static IServiceDiscovery serviceDiscovery = new ServiceDiscoveryWithZK();

    public static <T>T create(Class<?> clazz) {
        String discovery = serviceDiscovery.discovery(clazz.getSimpleName());
        //clazz本身传进来就是interface
        MethodProxy proxy = new MethodProxy(clazz,discovery);
        Class<?> [] interfaces = clazz.isInterface()? new Class[]{clazz} : clazz.getInterfaces();
        T result = (T) Proxy.newProxyInstance(clazz.getClassLoader(),interfaces,proxy);
        return result;
    }

    private static class MethodProxy implements InvocationHandler{
        private Class<?> clazz;
        private String serviceAddress = null;
        private int servicePort;

        public MethodProxy(Class<?> clazz,String discovery) {
            this.clazz = clazz;

            System.out.println(discovery);
            if (discovery != null) {
                serviceAddress = discovery.split(":")[0];
                servicePort = Integer.valueOf(discovery.split(":")[1]);
            }
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            //如果传进来是一个已实现的具体类（本次演示略过此逻辑）
            if(Object.class.equals(method.getDeclaringClass())){
                try{
                    return method.invoke(this,args);
                }catch (Throwable t){
                    t.printStackTrace();
                }
                //如果传进来的是一个接口（核心）
            }else{
                return rpcInvoke(proxy,method,args);
            }
            return null;
        }

        /**
         * 实现接口的核心方法
         * @param proxy
         * @param method
         * @param args
         * @return
         */
        private Object rpcInvoke(Object proxy, Method method, Object[] args) {
            //传输协议封装
            InvokerProtocol msg = new InvokerProtocol();
            msg.setClassName(this.clazz.getSimpleName());
            msg.setMethodName(method.getName());
            msg.setValues(args);
            msg.setParames(method.getParameterTypes());

            final RpcProxyHandler consumerHandler = new RpcProxyHandler();
            EventLoopGroup group = new NioEventLoopGroup();

            try {
                Bootstrap b = new Bootstrap();
                b.group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY,true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                //自定义协议解码器
                                /** 入参有5个，分别解释如下
                                 maxFrameLength：框架的最大长度。如果帧的长度大于此值，则将抛出TooLongFrameException。
                                 lengthFieldOffset：长度字段的偏移量：即对应的长度字段在整个消息数据中得位置
                                 lengthFieldLength：长度字段的长度：如：长度字段是int型表示，那么这个值就是4（long型就是8）
                                 lengthAdjustment：要添加到长度字段值的补偿值
                                 initialBytesToStrip：从解码帧中去除的第一个字节数
                                 */
                                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                                //自定义协议编码器
                                pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                                //对象参数类型编码器
                                pipeline.addLast("encoder", new ObjectEncoder());
                                //对象参数类型解码器
                                pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
                                pipeline.addLast("handler",consumerHandler);
                            }
                        });
                ChannelFuture future = b.connect(serviceAddress,servicePort).sync();
                future.channel().writeAndFlush(msg).sync();
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                group.shutdownGracefully();
            }
            return consumerHandler.getResponse();
        }
    }
}
