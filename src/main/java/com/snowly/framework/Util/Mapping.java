package com.snowly.framework.Util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.snowly.framework.Annotations.AnotController;
import com.snowly.framework.Annotations.AnotURL;

public class Mapping {
    
    // URL -> Method mapping
    private Map<String, Method> urlToMethod = new HashMap<>();
    
    // URL -> Controller Instance mapping
    private Map<String, Object> urlToController = new HashMap<>();
    
    // URL -> Controller Class mapping
    private Map<String, Class<?>> urlToControllerClass = new HashMap<>();
    
    /**
     * Add a URL mapping
     * @param url The full URL to which the method is mapped
     * @param method The method to invoke
     * @param controllerInstance The controller object instance
     * @param controllerClass The controller class
     */
    public void addMapping(String url, Method method, Object controllerInstance, Class<?> controllerClass) {
        urlToMethod.put(url, method);
        urlToController.put(url, controllerInstance);
        urlToControllerClass.put(url, controllerClass);
    }
    
    /**
     * Get the method for a URL
     */
    public Method getMethod(String url) {
        return urlToMethod.get(url);
    }
    
    /**
     * Get the controller instance for a URL
     */
    public Object getController(String url) {
        return urlToController.get(url);
    }
    
    /**
     * Get the controller class for a URL
     */
    public Class<?> getControllerClass(String url) {
        return urlToControllerClass.get(url);
    }
    
    /**
     * Check if a URL is mapped
     */
    public boolean hasMapping(String url) {
        return urlToMethod.containsKey(url);
    }
    
    /**
     * Get all mapped URLs
     */
    public Set<String> getAllUrls() {
        return urlToMethod.keySet();
    }
    
    /**
     * Get total number of mappings
     */
    public int size() {
        return urlToMethod.size();
    }
    
    /**
     * Clear all mappings
     */
    public void clear() {
        urlToMethod.clear();
        urlToController.clear();
        urlToControllerClass.clear();
    }
    
    /**
     * Print all mappings
     */
    public void printMappings() {
        System.out.println("=== URL Mappings ===");
        for (String url : urlToMethod.keySet()) {
            Method method = urlToMethod.get(url);
            Class<?> controllerClass = urlToControllerClass.get(url);
            System.out.println(url + " -> " + controllerClass.getSimpleName() + "." + method.getName() + "()");
        }
        System.out.println("Total: " + size() + " mappings");
    }

    /**
     * Build URL mapping
    */
    public static Mapping buildMapping(List<Class<?>> controllers) {
        Mapping mapping = new Mapping();
        
        if (controllers == null || controllers.isEmpty()) {
            System.out.println("No controllers found to map");
            return mapping;
        }
        
        System.out.println("\n=== Building URL Mappings ===");
        
        for (Class<?> controllerClass : controllers) {
            try {
                AnotController controllerAnnot = controllerClass.getAnnotation(AnotController.class);
                String basePath = controllerAnnot.value();
                
                // Create instance of controller (assuming no-arg constructor)
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                
                // Process all methods with @AnotURL annotation
                for (Method method : controllerClass.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(AnotURL.class)) {
                        AnotURL urlAnnot = method.getAnnotation(AnotURL.class);
                        String methodPath = urlAnnot.value();
                        
                        // Build full URL path
                        String fullUrl = normalizePath(basePath + methodPath);
                        
                        // Add to mapping
                        mapping.addMapping(fullUrl, method, controllerInstance, controllerClass);
                        
                        System.out.println("Mapped: " + fullUrl + " -> " + controllerClass.getSimpleName() + "." + method.getName() + "()");
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error processing controller: " + controllerClass.getName());
                e.printStackTrace();
            }
        }
        
        System.out.println("Total mappings created: " + mapping.size());
        return mapping;
    }

    /**
     * Normalize URL path
    */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        path = path.replace("//", "/");
        
        return path;
    }

}