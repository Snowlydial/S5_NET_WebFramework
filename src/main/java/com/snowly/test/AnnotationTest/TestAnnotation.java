package com.snowly.test.AnnotationTest;

import com.snowly.framework.Annotations.AnotURL;
import java.lang.reflect.Method;

public class TestAnnotation {
    
    //?==== Methods for testing
    @AnotURL("/home")
    public void homePage() {
        System.out.println("Home page method");
    }
    
    @AnotURL("/users")
    public void usersPage() {
        System.out.println("Users page method");
    }
    
    @AnotURL("/products/list")
    public void productsList() {
        System.out.println("Products list method");
    }
    
    @AnotURL  // Using default value (empty string)
    public void defaultMethod() {
        System.out.println("Default method");
    }
    
    public void noAnnotation() {
        System.out.println("No annotation method");
    }
    
    //?==== MAIN
    public static void main(String[] args) {
        System.out.println("=== Testing @AnotURL Annotation ===\n");
        
        // Get the class
        Class<TestAnnotation> clazz = TestAnnotation.class;
        
        // Get all methods
        Method[] methods = clazz.getDeclaredMethods();
        
        int annotatedCount = 0;
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(AnotURL.class)) {
                // Get the annotation
                AnotURL annotation = method.getAnnotation(AnotURL.class);
                
                // Get the URL value
                String url = annotation.value();
                
                System.out.println("Method: " + method.getName());
                System.out.println("URL: " + (url.isEmpty() ? "(empty/default)" : url));
                System.out.println();
                
                annotatedCount++;
            }
        }
        
        System.out.println("---");
        System.out.println("Total methods found: " + methods.length);
        System.out.println("Methods with @AnotURL: " + annotatedCount);
        System.out.println("Methods without annotation: " + (methods.length - annotatedCount));
    }
}
