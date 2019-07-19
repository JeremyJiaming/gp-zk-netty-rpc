package com.jeremy.www.protocol;

import lombok.Data;

import java.io.Serializable;

@Data
public class InvokerProtocol implements Serializable{

    private String className;//类名
    private String methodName;//函数名称
    private Class<?>[] parames;//形参列表
    private Object[] values;//实参列表

}
