package com.snowly.framework.Filters;

import java.io.IOException;
import java.net.URL;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ResourceFilter implements Filter {
    
    private ServletContext servletContext;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.servletContext = filterConfig.getServletContext();
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Get the full path from URL (/web-framework/anything.html)
        String requestURI = httpRequest.getRequestURI();
        
        // Get base path of web app (/web-framework)
        String contextPath = httpRequest.getContextPath();
        
        // Get the resource path relative to web app (/anything.html)
        String resourcePath = requestURI.substring(contextPath.length());
        
        // If resourcePath is empty or just "/", default to "/index.html"
        if (resourcePath.isEmpty() || resourcePath.equals("/")) {
            resourcePath = "/index.html";
            httpRequest = new CustomRequestWrapper(httpRequest, resourcePath);
        }
        
        httpResponse.addHeader("X-Debug-URI", requestURI);
        httpResponse.addHeader("X-Debug-Context", contextPath);
        httpResponse.addHeader("X-Debug-Resource", resourcePath);
        
        //* Check if the resource exists
        if (resourceExists(resourcePath)) {
            httpResponse.addHeader("X-Debug-Status", "Resource Found - Using Default Servlet");
            RequestDispatcher dispatcher = servletContext.getNamedDispatcher("default");
            dispatcher.forward(httpRequest, response);
            return;
        }
        
        //* Resource doesn't exist, pass to FrontServlet
        httpResponse.addHeader("X-Debug-Status", "Resource Not Found - Passing to FrontServlet");
        chain.doFilter(httpRequest, response);
    }
    
    private boolean resourceExists(String resourcePath) {
        try {
            URL resourceURL = servletContext.getResource(resourcePath);
            
            if (resourceURL != null) {
                String realPath = servletContext.getRealPath(resourcePath);
                if (realPath != null) {
                    java.io.File file = new java.io.File(realPath);
                    return file.exists() && file.isFile();
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
}