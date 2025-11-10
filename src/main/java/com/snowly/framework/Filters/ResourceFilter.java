package com.snowly.framework.Filters;

import java.io.IOException;
import java.net.URL;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

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
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String resourcePath = requestURI.substring(contextPath.length());
        
        System.out.println("[ResourceFilter] Request: " + resourcePath);
        System.out.println("[ResourceFilter] Context Path: " + contextPath);
        System.out.println("[ResourceFilter] Resource Path: " + resourcePath);
        
        if (resourcePath.isEmpty() || resourcePath.equals("/")) {
            resourcePath = "/index.html";
            httpRequest = new CustomRequestWrapper(httpRequest, resourcePath);
            System.out.println("[ResourceFilter] Redirected root to: " + resourcePath);
        }
        
        if (isStaticResource(resourcePath)) {
            System.out.println("[ResourceFilter] IS static resource: " + resourcePath);
            
            if (resourceExists(resourcePath)) {
                System.out.println("[ResourceFilter] Resource EXISTS, forwarding to default servlet");
                RequestDispatcher dispatcher = servletContext.getNamedDispatcher("default");
                dispatcher.forward(httpRequest, response);
                return;
            } else {
                System.out.println("[ResourceFilter] Resource NOT FOUND: " + resourcePath);
            }
        } else {
            System.out.println("[ResourceFilter] NOT a static resource: " + resourcePath);
        }
        
        // Pass to FrontServlet
        System.out.println("[ResourceFilter] Passing to FrontServlet: " + resourcePath);
        chain.doFilter(httpRequest, response);
    }

    private boolean isStaticResource(String path) {
        return path.matches(".*\\.(html|htm|css|js|jpg|jpeg|png|gif|ico|svg|txt|pdf|jsp)$");
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
    
    @Override
    public void destroy() {
    }
}