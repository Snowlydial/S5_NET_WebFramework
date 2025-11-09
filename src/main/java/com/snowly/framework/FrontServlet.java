package com.snowly.framework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.lang.reflect.Method;

import com.snowly.framework.Util.Mapping;
import com.snowly.framework.Annotations.AnotController;
import com.snowly.framework.Annotations.AnotURL;
import com.snowly.framework.Util.ControllerScanner;
import com.snowly.framework.Util.ModelView;

public class FrontServlet extends HttpServlet {
    
    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("=== Initializing FrontServlet ===");
        
        HashMap<String, Mapping> urlHashmapping = new HashMap<>();
        
        List<Class<?>> controllers = ControllerScanner.scanForControllers();
        for(Class<?> controllerClass : controllers) {
            AnotController controllerAnnot = controllerClass.getAnnotation(AnotController.class);
            String basePath = controllerAnnot.value();
            
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(AnotURL.class)) {
                    AnotURL urlAnnot = method.getAnnotation(AnotURL.class);
                    String methodPath = urlAnnot.value();
                    String fullUrl = basePath + methodPath;
                    Mapping classAndMethod = new Mapping(controllerClass, method);
                    urlHashmapping.put(fullUrl, classAndMethod);
                    System.out.println("Mapped: " + fullUrl + " -> " + controllerClass.getSimpleName() + "." + method.getName());
                }
            }
        }
        
        ServletContext servletContext = getServletContext();
        servletContext.setAttribute("urlHashmapping", urlHashmapping);
        
        System.out.println("Total mappings: " + urlHashmapping.size());
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
    
    @SuppressWarnings("unchecked")
    private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestURI.substring(contextPath.length());
        String fullURL = request.getRequestURL().toString();
        
        if (path.matches(".*\\.(html|htm|css|js|jpg|jpeg|png|gif|ico|txt|pdf|jsp)$")) {
            ServletContext servletContext = getServletContext();
            RequestDispatcher dispatcher = servletContext.getNamedDispatcher("default");
            if (dispatcher != null) {
                dispatcher.forward(request, response);
                return;
            }
        }
        
        response.setContentType("text/html;charset=UTF-8");
        
        ServletContext servletContext = getServletContext();
        HashMap<String, Mapping> urlHashmapping = (HashMap<String, Mapping>) servletContext.getAttribute("urlHashmapping");
        
        // Check if URL is mapped
        if (urlHashmapping != null && urlHashmapping.containsKey(path)) {
            Mapping mapping = urlHashmapping.get(path);
            
            try {
                Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                Object result = mapping.getMethod().invoke(controllerInstance);
                
                // Check the return type
                if (result instanceof ModelView) { // Sprint 4 bis: ModelView return - forward to JSP
                    ModelView mv = (ModelView) result;
                    String viewName = mv.getView();
                    request.getRequestDispatcher("/" + viewName).forward(request, response);
                    return;
                    
                } else if (result instanceof String) { // Sprint 4: String return - display directly
                    String htmlContent = (String) result;
                    try (PrintWriter out = response.getWriter()) {
                        out.println(htmlContent);
                    }
                    return;
                    
                } else {
                    renderErrorPage(response, "Unsupported Return Type", "Method returned: " + (result != null ? result.getClass().getName() : "null"));
                    return;
                }
                
            } catch (Exception e) {
                renderErrorPage(response, "Error Invoking Method", 
                    "Error: " + e.getMessage() + "<br>" +
                    "Controller: " + mapping.getControllerClass().getSimpleName() + "<br>" +
                    "Method: " + mapping.getMethod().getName() + "()");
                e.printStackTrace();
                return;
            }
        }
        render404Page(response, path, fullURL, requestURI, contextPath, urlHashmapping);
    }
    
    private void render404Page(HttpServletResponse response, String path, String fullURL, String requestURI, 
                               String contextPath, HashMap<String, Mapping> urlHashmapping) throws IOException {
        
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("    <title>404 - Not Found</title>");
            out.println("    <style>");
            out.println("        body { font-family: Arial, sans-serif; margin: 40px; }");
            out.println("        .info { background: #f0f8ff; padding: 20px; border-radius: 5px; margin-bottom: 20px; }");
            out.println("        .error { background: #fff0f0; padding: 20px; border-radius: 5px; margin-bottom: 20px; }");
            out.println("        .url { color: #0066cc; font-weight: bold; }");
            out.println("        .warning { color: #ff6600; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            
            out.println("    <div class='error'>");
            out.println("      <h2 class='warning'>404 - URL Not Found</h2>");
            out.println("        <p><strong>Requested URL:</strong> <span class='url'>" + path + "</span></p>");
            out.println("        <p>The URL you requested is not mapped to any controller method.</p>");
            out.println("    </div>");
            
            if (urlHashmapping != null && !urlHashmapping.isEmpty()) {
                out.println("    <div class='info'>");
                out.println("      <h3>Available URLs:</h3>");
                out.println("      <ul>");
                for (String url : urlHashmapping.keySet()) {
                    Mapping mapping = urlHashmapping.get(url);
                    out.println("        <li><span class='url'>" + url + "</span> → " + 
                               mapping.getControllerClass().getSimpleName() + "." + 
                               mapping.getMethod().getName() + "()</li>");
                }
                out.println("      </ul>");
                out.println("    </div>");
            }
            
            out.println("    <div class='info'>");
            out.println("      <h3>Request Information:</h3>");
            out.println("        <p><strong>Full URL:</strong> " + fullURL + "</p>");
            out.println("        <p><strong>Context Path:</strong> " + contextPath + "</p>");
            out.println("        <p><strong>URI Path:</strong> " + requestURI + "</p>");
            out.println("        <p><strong>Total Mappings:</strong> " + urlHashmapping.size() + "</p>");
            out.println("    </div>");
            
            out.println("</body>");
            out.println("</html>");
        }
    }
    
    private void renderErrorPage(HttpServletResponse response, String title, String message) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("    <title>" + title + "</title>");
            out.println("    <style>");
            out.println("        body { font-family: Arial, sans-serif; margin: 40px; }");
            out.println("        .error { background: #fff0f0; padding: 20px; border-radius: 5px; }");
            out.println("        .warning { color: #ff6600; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class='error'>");
            out.println("      <h2 class='warning'>" + title + "</h2>");
            out.println("      <p>" + message + "</p>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}