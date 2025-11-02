package com.snowly.framework.Util;

import com.snowly.framework.Annotations.AnotController;
import com.snowly.framework.Annotations.AnotURL;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

public class ControllerScanner {
    
    /**
     * Scans ALL classes in the classpath
     * Works with any project structure
     */
    public static List<Class<?>> scanForControllers() {
        List<Class<?>> controllers = new ArrayList<>();
        
        try {
            // Get the root of the classpath (where compiled classes are)
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource("");
            
            if (resource == null) {
                System.out.println("No classpath root found");
                return controllers;
            }
            
            System.out.println("Scanning classpath root: " + resource);
            
            File rootDir = new File(resource.getFile());
            
            // Scan from root with empty package prefix
            scanDirectory(rootDir, "", controllers);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return controllers;
    }
    
    /**
     * Recursively scan directory for controller classes
     */
    private static void scanDirectory(File dir, String packageName, List<Class<?>> controllers) {
        if (!dir.exists() || !dir.isDirectory()) return;
        
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Build package name
                String newPackage = packageName.isEmpty() ? 
                    file.getName() : packageName + "." + file.getName();
                
                // Recurse into subdirectories
                scanDirectory(file, newPackage, controllers);
                
            } else if (file.getName().endsWith(".class")) {
                if (file.getName().contains("$")) { // Skip nested/inner classes
                    continue;
                }
                
                // Build full class name
                String className = packageName.isEmpty() ? 
                    file.getName().replace(".class", "") :
                    packageName + "." + file.getName().replace(".class", "");
                
                try {
                    Class<?> clazz = Class.forName(className);
                    
                    if (clazz.isAnnotationPresent(AnotController.class)) {
                        controllers.add(clazz);
                        System.out.println("Found controller: " + className);
                    }
                    
                } catch (ClassNotFoundException e) {
                    System.out.println("Could not load: " + className);
                } catch (NoClassDefFoundError e) {
                    // Skip classes with missing dependencies (like servlet classes)
                    System.out.println("Skipped (missing deps): " + className);
                } catch (Throwable t) {
                    // Skip any other problematic classes
                    System.out.println("Skipped (error): " + className + " - " + t.getMessage());
                }
            }
        }
    }
    
    /**
     * Extract URL mappings from a controller
     */
    public static void printControllerMappings(Class<?> controller) {
        AnotController controllerAnnot = controller.getAnnotation(AnotController.class);
        String basePath = controllerAnnot.value();
        
        System.out.println("\nController: " + controller.getSimpleName());
        System.out.println("  Full name: " + controller.getName());
        System.out.println("  Base path: " + basePath);
        System.out.println("  URL Mappings:");
        
        int methodCount = 0;
        for (Method method : controller.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AnotURL.class)) {
                AnotURL urlAnnot = method.getAnnotation(AnotURL.class);
                String fullUrl = basePath + urlAnnot.value();
                System.out.println("    " + fullUrl + " -> " + method.getName() + "()");
                methodCount++;
            }
        }
        
        if (methodCount == 0) {
            System.out.println("    (no URL mappings found)");
        }
    }

}