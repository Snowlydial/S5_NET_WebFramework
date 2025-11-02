package com.snowly.framework.Util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
}