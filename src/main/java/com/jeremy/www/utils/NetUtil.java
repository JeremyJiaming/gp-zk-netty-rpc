package com.jeremy.www.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetUtil {

    public static String getLocalAddress(){
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return inetAddress.getHostAddress();//获得本机IP地址
    }
}
