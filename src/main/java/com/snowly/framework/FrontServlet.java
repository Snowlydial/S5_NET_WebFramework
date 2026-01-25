package com.snowly.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import com.snowly.framework.Util.*;
import com.snowly.framework.Util.Sprints.AuthorizationHandler;
import com.snowly.framework.Util.Sprints.ParameterResolver;
import com.snowly.framework.Util.Sprints.ResponseHandler;
import com.snowly.framework.Annotations.*;
import com.snowly.framework.Annotations.HTTP_Methods.*;

// Contains SP6, SP6_TER declarations
// Uses SP7(GET/POST), ParameterSolver(SP8, SP8_BIS, SP10, SP11), ResponseHandler(SP5, SP9), AuthorizationHandler(SP11_BIS)
@MultipartConfig(
    maxFileSize = 1024 * 1024 * 10,      // 10 MB max file size
    maxRequestSize = 1024 * 1024 * 50,   // 50 MB max request size
    fileSizeThreshold = 1024 * 1024      // 1 MB threshold for temp files
)
public class FrontServlet extends HttpServlet {
    
    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("=== Initializing FrontServlet ===");
        HashMap<String, List<Mapping>> urlHashmapping = new HashMap<>();
        
        List<Class<?>> controllers = ControllerScanner.scanForControllers();
        for(Class<?> controllerClass : controllers) {
            AnotController controllerAnnot = controllerClass.getAnnotation(AnotController.class);
            String basePath = controllerAnnot.value();
            
            for (Method method : controllerClass.getDeclaredMethods()) {
                //?==== SP7: Method Paths
                String methodPath = null;
                String httpMethod = null;
                
                if (method.isAnnotationPresent(AnotGetMapping.class)) {
                    methodPath = method.getAnnotation(AnotGetMapping.class).value();
                    httpMethod = "GET";
                } else if (method.isAnnotationPresent(AnotPostMapping.class)) {
                    methodPath = method.getAnnotation(AnotPostMapping.class).value();
                    httpMethod = "POST";
                } else if (method.isAnnotationPresent(AnotRequestMapping.class)) {
                    AnotRequestMapping reqMapping = method.getAnnotation(AnotRequestMapping.class);
                    methodPath = reqMapping.value();
                    String[] methods = reqMapping.method();
                    
                    if (methods.length == 0) { // If no methods specified, default to ALL methods
                        httpMethod = "ALL";
                    } else { // For multiple methods, create separate mappings
                        for (String m : methods) {
                            addMapping(urlHashmapping, basePath, methodPath, m, controllerClass, method);
                        }
                        continue; // Skip the single mapping below
                    }
                }
                //*------- FallBack to old @AnotURL
                else if (method.isAnnotationPresent(AnotURL.class)) {
                    methodPath = method.getAnnotation(AnotURL.class).value();
                    httpMethod = "ALL"; // Accept both GET and POST
                }
                
                if (methodPath != null) {
                    addMapping(urlHashmapping, basePath, methodPath, httpMethod, controllerClass, method);
                }
            }
        }
        
        ServletContext servletContext = getServletContext();
        servletContext.setAttribute("urlHashmapping", urlHashmapping);
        
        System.out.println("Total mappings: " + urlHashmapping.size());
        System.out.println("=== FrontServlet Initialization Complete ===");
    }

    //?==== Used in init(): Helper method to add mappings
    private void addMapping(HashMap<String, List<Mapping>> urlHashmapping, String basePath, String methodPath, String httpMethod, Class<?> controllerClass, Method method) {
        String fullUrl = basePath + methodPath;
        
        Mapping mapping = new Mapping(controllerClass, method);
        mapping.setHttpMethod(httpMethod);

        //*--- SP3_BIS Accolade_Support
        boolean hasPathParams = methodPath.contains("{") && methodPath.contains("}");
        mapping.setHasPathParams(hasPathParams);

        //*--- SP6_Ter Preparation
        if (hasPathParams) {
            mapping.buildUrlPattern(fullUrl);
            System.out.println("Mapped (" + httpMethod + " with path params): " + fullUrl + " -> " + controllerClass.getSimpleName() + "." + method.getName());
        } else {
            System.out.println("Mapped (" + httpMethod + "): " + fullUrl + " -> " + controllerClass.getSimpleName() + "." + method.getName());
        }
        
        // Add to list of mappings for this URL
        urlHashmapping.computeIfAbsent(fullUrl, k -> new ArrayList<>()).add(mapping);
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
        String requestMethod = request.getMethod(); // returns "GET" or "POST" or others
        ServletContext servletContext = getServletContext();
        HashMap<String, List<Mapping>> urlHashmapping = (HashMap<String, List<Mapping>>) servletContext.getAttribute("urlHashmapping");
        
        Mapping mapping = null;
        Map<String, String> pathParams = new HashMap<>();

        //======== Determine the correct mapping ========
        //?--- SP6_Simple: Try parameter name exact match
        if (urlHashmapping != null && urlHashmapping.containsKey(path)) {
            List<Mapping> candidates = urlHashmapping.get(path);
            // 
            mapping = findMatchingMapping(candidates, requestMethod);
        }
        
        //?--- SP6_Ter: Try parameter pattern matching
        if (mapping == null) {
            for (Map.Entry<String, List<Mapping>> entry : urlHashmapping.entrySet()) {
                List<Mapping> candidates = entry.getValue();
                
                for (Mapping candidate : candidates) {
                    if (candidate.hasPathParams() && candidate.matchesUrl(path)) {
                        //?--- SP7: Check if HTTP method from View and inside the mapping matches
                        if (candidate.getHttpMethod().equals("ALL") || candidate.getHttpMethod().equals(requestMethod)) {
                            mapping = candidate;
                            pathParams = candidate.extractPathParams(path);
                            System.out.println("Matched pattern (" + requestMethod + "): " + candidate.getOriginalUrl() + " with params: " + pathParams);
                            break;
                        }
                    }
                }
                
                if (mapping != null) break;
            }
        }

        //======== Call the action method associated to the URL ========
        if (mapping != null) {
            try {
                //?=== SP11_BIS: Check authorization before invoking method
                if (!AuthorizationHandler.checkAuthorization(request, response, mapping.getMethod(), servletContext)) {
                    return;
                }
                
                Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                
                //?=== SP8 & SP8_BIS & SP10: Prepare arguments
                Object[] args = ParameterResolver.prepareMethodArguments(request, mapping, pathParams);
                Object result = mapping.getMethod().invoke(controllerInstance, args);
                
                //?=== SP9: Check if JSON response
                boolean isJsonResponse = mapping.getMethod().isAnnotationPresent(AnotJSON.class);
                if (isJsonResponse) {
                    ResponseHandler.handleJsonResponse(response, result);
                } else {
                    ResponseHandler.handleRegularResponse(request, response, result);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ResponseHandler.sendError(response, 500, "Error invoking method: " + e.getMessage());
            }
            
        } else {
            ResponseHandler.sendError(response, 404, "URL not found: " + path);
        }
    }

    //?==== SP7_Helper: Find mapping that matches the HTTP method
    private Mapping findMatchingMapping(List<Mapping> candidates, String requestMethod) {
        for (Mapping candidate : candidates) {
            if (candidate.getHttpMethod().equals("ALL") || candidate.getHttpMethod().equals(requestMethod)) {
                return candidate;
            }
        }
        return null;
    }
}