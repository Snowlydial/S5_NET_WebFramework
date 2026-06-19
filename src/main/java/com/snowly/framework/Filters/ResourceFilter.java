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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String resourcePath = requestURI.substring(contextPath.length());
        
        System.out.println("[ResourceFilter] Request: " + resourcePath);
        
        // Handle root path - redirect to /home
        if (resourcePath.isEmpty() || resourcePath.equals("/")) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendRedirect(contextPath + "/home");
            System.out.println("[ResourceFilter] Redirected root to: /home");
            return;
        }
        
        if (handleJSP(httpRequest, response, resourcePath, chain)) {
            return;
        }
        
        if (handleStaticResource(httpRequest, response, resourcePath)) {
            return;
        }
        
        // Not JSP or static resource - pass to FrontServlet
        System.out.println("[ResourceFilter] Passing to FrontServlet: " + resourcePath);
        chain.doFilter(httpRequest, response);
    }

    private boolean handleJSP(HttpServletRequest request, ServletResponse response, String resourcePath, FilterChain chain) throws IOException, ServletException {
        if (!resourcePath.endsWith(".jsp")) {
            return false;
        }
        
        System.out.println("[ResourceFilter] JSP file detected: " + resourcePath);
        RequestDispatcher dispatcher = servletContext.getNamedDispatcher("jsp");
        
        if (dispatcher != null) {
            System.out.println("[ResourceFilter] Forwarding to JSP servlet");
            dispatcher.forward(request, response);
        } else {
            System.out.println("[ResourceFilter] JSP servlet not found, passing through chain");
            chain.doFilter(request, response);
        }
        
        return true;
    }
    
    private boolean handleStaticResource(HttpServletRequest request, ServletResponse response, String resourcePath) throws IOException, ServletException {
        if (!isStaticResource(resourcePath)) {
            System.out.println("[ResourceFilter] NOT a static resource: " + resourcePath);
            return false;
        }
        
        System.out.println("[ResourceFilter] IS static resource: " + resourcePath);
        
        if (resourceExists(resourcePath)) {
            System.out.println("[ResourceFilter] Resource EXISTS, forwarding to default servlet");
            RequestDispatcher dispatcher = servletContext.getNamedDispatcher("default");
            dispatcher.forward(request, response);
            return true;
        } else {
            System.out.println("[ResourceFilter] Resource NOT FOUND: " + resourcePath);
            return false;
        }
    }

    private boolean isStaticResource(String path) {
        return path.matches(".*\\.(html|htm|css|js|jpg|jpeg|png|gif|ico|svg|txt|pdf|woff|woff2|ttf|eot|otf|map)$");
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