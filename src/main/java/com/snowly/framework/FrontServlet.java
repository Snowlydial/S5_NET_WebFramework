package com.snowly.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.snowly.framework.Util.Mapping;
import com.snowly.framework.Util.ControllerScanner;

public class FrontServlet extends HttpServlet {
    private Mapping mapping;
    
    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("=== Initializing FrontServlet ===");
        
        List<Class<?>> controllers = ControllerScanner.scanForControllers();
        
        mapping = Mapping.buildMapping(controllers);
        
        if (mapping != null) {
            mapping.printMappings();
        }
        
        System.out.println("=== FrontServlet Initialization Complete ===");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handleRequest(request, response);
    }
    
    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        
        // Get the URL the user typed
        String fullURL = request.getRequestURL().toString();
        
        // Get the URI path
        String requestURI = request.getRequestURI();
        
        // Remove context path to get the application-specific path
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());
        
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("    <title>Framework Front Controller</title>");
            out.println("    <style>");
            out.println("        body { font-family: Arial, sans-serif; margin: 40px; }");
            out.println("        .info { background: #f0f8ff; padding: 20px; border-radius: 5px; margin-bottom: 20px; }");
            out.println("        .mapping { background: #f0fff0; padding: 20px; border-radius: 5px; margin-bottom: 20px; }");
            out.println("        .error { background: #fff0f0; padding: 20px; border-radius: 5px; }");
            out.println("        .url { color: #0066cc; font-weight: bold; }");
            out.println("        .success { color: #009900; }");
            out.println("        .warning { color: #ff6600; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");

            // Check if URL is mapped
            if (mapping != null && mapping.hasMapping(path)) {
                // URL is mapped - show mapping information
                out.println("    <div class='mapping'>");
                out.println("      <h2 class='success'>URL Mapped Successfully!</h2>");
                out.println("        <p><strong>Requested URL:</strong> <span class='url'>" + path + "</span></p>");
                out.println("        <p><strong>Controller:</strong> " + mapping.getControllerClass(path).getSimpleName() + "</p>");
                out.println("        <p><strong>Method:</strong> " + mapping.getMethod(path).getName() + "()</p>");
                out.println("        <p><strong>Full URL:</strong> " + fullURL + "</p>");
                out.println("    </div>");
                
                //? sprint4: Invoke the method here
                // Method method = mapping.getMethod(path);
                // Object controller = mapping.getController(path);
                // Object result = method.invoke(controller, request, response);
            } else {
                out.println("    <div class='error'>");
                out.println("      <h2 class='warning'>404 - URL Not Found</h2>");
                out.println("        <p><strong>Requested URL:</strong> <span class='url'>" + path + "</span></p>");
                out.println("        <p>The URL you requested is not mapped to any controller method.</p>");
                out.println("    </div>");
                
                if (mapping != null && mapping.size() > 0) {
                    out.println("    <div class='info'>");
                    out.println("      <h3>Available URLs:</h3>");
                    out.println("      <ul>");
                    for (String url : mapping.getAllUrls()) {
                        out.println("        <li><span class='url'>" + url + "</span> → " + 
                                   mapping.getControllerClass(url).getSimpleName() + "." + 
                                   mapping.getMethod(url).getName() + "()</li>");
                    }
                    out.println("      </ul>");
                    out.println("    </div>");
                }
            }

            out.println("    <div class='info'>");
            out.println("      <h3>Request Information:</h3>");
            out.println("        <p><strong>Full URL:</strong> " + fullURL + "</p>");
            out.println("        <p><strong>Context Path:</strong> " + contextPath + "</p>");
            out.println("        <p><strong>URI Path:</strong> " + requestURI + "</p>");
            out.println("        <p><strong>Method:</strong> " + request.getMethod() + "</p>");
            out.println("        <p><strong>Total Mappings:</strong> " + (mapping != null ? mapping.size() : 0) + "</p>");
            out.println("    </div>");
            
            out.println("</body>");
            out.println("</html>");
        }
    }
}