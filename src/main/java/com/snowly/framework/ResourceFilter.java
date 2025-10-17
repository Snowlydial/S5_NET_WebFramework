package com.snowly.framework;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ResourceFilter implements Filter {
    
    private ServletContext servletContext;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.servletContext = filterConfig.getServletContext();
        System.out.println("ResourceFilter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // Remove context path to get the resource path
        String resourcePath = requestURI.substring(contextPath.length());
        System.out.println("ResourceFilter: Checking resource: " + resourcePath);
        
        if (resourceExists(resourcePath)) {
            System.out.println("ResourceFilter: Resource found, forwarding to: " + resourcePath);
            RequestDispatcher dispatcher = servletContext.getRequestDispatcher(resourcePath);
            dispatcher.forward(request, response);
            return;
        }
        
        // Resource doesn't exist, let it pass through to FrontServlet
        System.out.println("ResourceFilter: Resource not found, passing to FrontServlet");
        chain.doFilter(request, response);
    }
    
    private boolean resourceExists(String resourcePath) {
        try {
            if (servletContext.getResource(resourcePath) != null) {
                String realPath = servletContext.getRealPath(resourcePath);
                if (realPath != null) {
                    java.io.File file = new java.io.File(realPath);
                    // Check if it's a file (not a directory)
                    return file.exists() && file.isFile();
                }
            }
        } catch (Exception e) {
            System.out.println("Error checking resource: " + e.getMessage());
        }
        return false;
    }
    
    @Override
    public void destroy() {
        System.out.println("ResourceFilter destroyed");
    }
}