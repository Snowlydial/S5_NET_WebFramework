package com.snowly.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

import com.snowly.framework.Util.Mapping;
import com.snowly.framework.Annotations.AnotController;
import com.snowly.framework.Annotations.AnotURL;
import com.snowly.framework.Util.ControllerScanner;
import com.snowly.framework.Util.ModelView;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.math.NumberUtils;

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

                    //?==== Start Sprint3_BIS_Accolade_Support
                    Mapping mapping = new Mapping(controllerClass, method);
                    mapping.setHasPathParams(methodPath.contains("{") && methodPath.contains("}"));
                    //?==== End Sprint3_BIS_Accolade_Support

                    urlHashmapping.put(fullUrl, mapping);
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
        
        ServletContext servletContext = getServletContext();
        HashMap<String, Mapping> urlHashmapping = (HashMap<String, Mapping>) servletContext.getAttribute("urlHashmapping");
        
        if (urlHashmapping != null && urlHashmapping.containsKey(path)) {
            Mapping mapping = urlHashmapping.get(path);
            
            try {
                Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                
                Object[] args = prepareMethodArguments(request, mapping);
                
                Object result = mapping.getMethod().invoke(controllerInstance, args);
                
                if (result instanceof ModelView) {
                    ModelView mv = (ModelView) result;

                    //?---- SP5: Add data for the view if any
                    HashMap<String, Object> modelViewData = mv.getData();
                    if(modelViewData.isEmpty()) {
                        System.out.println("!!!!!!! ModelView Data is EMPTY !!!!!!!");
                    }

                    for(Map.Entry<String, Object> entry : modelViewData.entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }

                    String viewName = mv.getView();
                    if (!viewName.startsWith("/")) {
                        viewName = "/" + viewName;
                    }
                    request.getRequestDispatcher(viewName).forward(request, response);
                    
                } else if (result instanceof String) {
                    response.setContentType("text/html;charset=UTF-8");
                    try (PrintWriter out = response.getWriter()) {
                        out.println((String) result);
                    }
                    
                } else {
                    sendError(response, 500, "Unsupported return type: " + 
                             (result != null ? result.getClass().getName() : "null"));
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(response, 500, "Error invoking method: " + e.getMessage());
            }
            
        } else {
            sendError(response, 404, "URL not found: " + path);
        }
    }

    //?==== SP6: 
    private Object[] prepareMethodArguments(HttpServletRequest request, Mapping mapping) {
        Map<String, Class<?>> paramList = mapping.getParameterList();
        Object[] args = new Object[paramList.size()];
        int i = 0;
        
        /* NB: About the foreach syntax below
            * map.entryset returns a set of all key-value pairs
            * since maps store pairs, we need to itterate on "entries" 
            * Map.Entry represent one key-value pair
        */
        for(Map.Entry<String, Class<?>> entry : paramList.entrySet()) {
            String paramName = entry.getKey();
            Class<?> paramType = entry.getValue();
            String paramValue = request.getParameter(paramName); // name matching
            
            if(paramValue != null) {
                //*---- Conversion happenning
                args[i] = ClassUtils.isPrimitiveOrWrapper(paramType) 
                    ? NumberUtils.createNumber(paramValue) 
                    : paramValue;
            } else {
                System.out.println("WARNING: Parameter '" + paramName + "' is null");
            }
            i++;
        }
        return args;
    }

    private void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><title>Error " + statusCode + "</title></head>");
            out.println("<body>");
            out.println("<h1>Error " + statusCode + "</h1>");
            out.println("<p>" + message + "</p>");
            out.println("</body></html>");
        }
    }
}