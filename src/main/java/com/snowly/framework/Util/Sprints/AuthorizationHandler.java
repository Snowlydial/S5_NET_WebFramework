package com.snowly.framework.Util.Sprints;

import com.snowly.framework.Annotations.Authorized;
import com.snowly.framework.Annotations.AnotRole;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Contains SP11_BIS declarations
public class AuthorizationHandler {
    
    //?==== SP11_BIS: Check if user is authorized to access the method
    public static boolean checkAuthorization(HttpServletRequest request, HttpServletResponse response, Method method, ServletContext servletContext) throws IOException {
        
        //*--- Get the session auth and role keys from web.xml configuration
        String authKey = servletContext.getInitParameter("session.auth.key");
        if (authKey == null || authKey.isEmpty()) {
            authKey = "isLoggedIn";  // Default auth flag key
            System.out.println("WARNING: session.auth.key not configured in web.xml, using default: 'isLoggedIn'");
        }

        String roleKey = servletContext.getInitParameter("session.role.key");
        if (roleKey == null || roleKey.isEmpty()) {
            roleKey = "userRole"; // Default role key
            System.out.println("WARNING: session.role.key not configured in web.xml, using default: 'userRole'");
        }

        HttpSession session = request.getSession(false);
        
        //*--- Check @Authorized annotation
        if (method.isAnnotationPresent(Authorized.class)) {
            if (!isLoggedIn(session, authKey)) {
                sendAuthError(response, request, 403, "Access Denied", "You must be logged in to access this resource.");
                return false;
            }
            return true;  // User is logged in
        }
        
        //*--- Check @AnotRole annotation (requires specific role)
        if (method.isAnnotationPresent(AnotRole.class)) {
            AnotRole roleAnnotation = method.getAnnotation(AnotRole.class);
            String[] requiredRoles = roleAnnotation.value();
            
            if (!isLoggedIn(session, authKey)) {
                sendAuthError(response, request, 403, "Access Denied", "You must be logged in to access this resource.");
                return false;
            }
            
            if (!hasRequiredRole(session, roleKey, requiredRoles)) {
                sendAuthError(response, request, 403, "Forbidden", 
                            "You do not have permission to access this resource. Required role(s): " + 
                            Arrays.toString(requiredRoles));
                return false;
            }
            return true;  // User has required role
        }
        
        //*--- No annotation = anonymous access
        return true;
    }
    
    //?==== SP11_BIS: Check if logged user has role in session
    private static boolean isLoggedIn(HttpSession session, String authKey) {
        if (session == null) {
            return false;
        }

        Object val = session.getAttribute(authKey);
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return Boolean.parseBoolean((String) val);

        return true;
    }

    //?==== SP11_BIS: Check if user Role meet requirements role
    private static boolean hasRequiredRole(HttpSession session, String roleKey, String[] requiredRoles) {
        if (session == null) {
            return false;
        }

        Object roleValue = session.getAttribute(roleKey);
        System.out.println("User role value from session (key='" + roleKey + "'): " + roleValue);
        if (roleValue == null) {
            return false;
        }
        
        //*--- Get user's roles (support both String and List)
        List<String> userRoles = new ArrayList<>();
        
        if (roleValue instanceof String) {
            userRoles.add(((String) roleValue).toLowerCase());
        } else if (roleValue instanceof List<?>) {
            for (Object role : (List<?>) roleValue) {
                if (role instanceof String) {
                    userRoles.add(((String) role).toLowerCase());
                }
            }
        } else {
            System.err.println("WARNING: Role value is not String or List: " + roleValue.getClass());
            return false;
        }
        
        //*--- Check if user has any of the required roles
        for (String requiredRole : requiredRoles) {
            if (userRoles.contains(requiredRole.toLowerCase())) {
                System.out.println("Authorization SUCCESS: User has role '" + requiredRole + "'");
                return true;
            }
        }
        
        System.out.println("Authorization FAILED: User roles " + userRoles + " do not match required roles " + Arrays.toString(requiredRoles));
        return false;
    }
    
    //?==== SP11_BIS: Send authorization error with back button
    private static void sendAuthError(HttpServletResponse response, HttpServletRequest request,
                                     int statusCode, String title, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("text/html;charset=UTF-8");
        
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>" + title + "</title>");
            out.println("<style>");
            out.println("body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }");
            out.println(".container { max-width: 600px; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); margin: 0 auto; }");
            out.println("h1 { color: #333; margin-bottom: 20px; }");
            out.println("p { color: #7c8a9a; margin: 20px 0; line-height: 1.6; }");
            out.println(".error-code { font-size: 48px; color: #ce335d; font-weight: bold; margin: 20px 0; text-align: center; }");
            out.println(".button-group { margin-top: 30px; text-align: center; }");
            out.println("button { padding: 12px 24px; margin: 5px; font-size: 16px; cursor: pointer; border: none; border-radius: 4px; background: #007bff; color: white; }");
            out.println("button:hover { background: #7c8a9a; }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div class='container'>");
            out.println("<div class='error-code'>" + statusCode + "</div>");
            out.println("<h1>" + title + "</h1>");
            out.println("<p>" + message + "</p>");
            out.println("<div class='button-group'>");
            
            //*--- Always include browser back button as fallback
            out.println("<button class='browser-back' onclick='window.history.back()'>← Browser Back</button>");
            
            out.println("</div>");
            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}