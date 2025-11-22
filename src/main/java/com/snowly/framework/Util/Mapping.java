package com.snowly.framework.Util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class Mapping {
    private final Class<?> controllerClass;
    private final Method method;
    private boolean hasPathParams;
    private Map<String, Class<?>> parameterTypes;

    public Mapping(Class<?> _controClass, Method _method) {
        this.controllerClass = _controClass;
        this.method = _method;
        this.parameterTypes = new HashMap<>();
        
        for(Parameter param : _method.getParameters()) {
            parameterTypes.put(param.getName(), param.getType());
        }
    }

    //?=== Getters
    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public boolean hasPathParams() {
        return hasPathParams;
    }
    
    public Map<String, Class<?>> getParameterTypes() {
        return parameterTypes;
    }

    //?=== Setters
    public void setHasPathParams(boolean hasPathParams) {
        this.hasPathParams = hasPathParams;
    }
}