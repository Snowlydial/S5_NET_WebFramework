package com.snowly.framework.Util;

import java.lang.reflect.Method;

public class Mapping {
    private final Class<?> controllerClass;
    private final Method method;

    public Mapping(Class<?> _controClass, Method _method) {
        this.controllerClass = _controClass;
        this.method = _method;
    }

    //? Get
    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

}