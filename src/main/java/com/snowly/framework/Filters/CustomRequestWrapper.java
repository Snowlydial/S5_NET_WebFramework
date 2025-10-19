package com.snowly.framework.Filters;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CustomRequestWrapper extends HttpServletRequestWrapper {
    
    private String customServletPath;
    
    public CustomRequestWrapper(HttpServletRequest request, String newServletPath) {
        super(request);
        this.customServletPath = newServletPath;
    }
    
    @Override
    public String getServletPath() {
        return customServletPath;
    }
    
    @Override
    public String getPathInfo() {
        return null;
    }
}