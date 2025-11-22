package com.snowly.framework.Util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import com.snowly.framework.Annotations.AnotParam;

public class Mapping {
    private final Class<?> controllerClass;
    private final Method method;
    private boolean hasPathParams;
    private Map<String, Class<?>> parameterList;

    public Mapping(Class<?> _controClass, Method _method) {
        this.controllerClass = _controClass;
        this.method = _method;
        this.parameterList = new HashMap<>();
        
        Parameter[] params = _method.getParameters();
        for(int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            String requestParamName;

            if(param.isAnnotationPresent(AnotParam.class)) {
                //?=== SP6_BIS_Support diffNaming - Use @AnotParam
                AnotParam anotParam = param.getAnnotation(AnotParam.class);
                requestParamName = anotParam.value();
            } else {
                //?=== SP6_Simple: Param Name Matching
                requestParamName = param.getName();
            }
            
            parameterList.put(requestParamName, param.getType());
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
    
    public Map<String, Class<?>> getParameterList() {
        return parameterList;
    }

    //?=== Setters
    public void setHasPathParams(boolean hasPathParams) {
        this.hasPathParams = hasPathParams;
    }
}