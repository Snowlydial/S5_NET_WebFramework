package com.snowly.framework;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class FrontServlet extends HttpServlet {
    
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
        String requestURL = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        String fullURL = queryString != null ? requestURL + "?" + queryString : requestURL;
        
        // Get the URI path
        String requestURI = request.getRequestURI();
        
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("    <title>Framework Front Controller</title>");
            out.println("    <style>");
            out.println("        body { font-family: Arial, sans-serif; margin: 40px; }");
            out.println("        .info { background: #f0f8ff; padding: 20px; border-radius: 5px; }");
            out.println("        .url { color: #0066cc; font-weight: bold; }");
            out.println("    </style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class='info'>");
            out.println("      <h2>Request Information:</h2>");
            out.println("        <p><strong>Full URL:</strong> <span class='url'>" + fullURL + "</span></p>");
            out.println("        <p><strong>Context Path:</strong> " + request.getContextPath() + "</p>");
            out.println("        <p><strong>URI Path:</strong> <span class='url'>" + requestURI + "</span></p>");
            out.println("        <p><strong>Method:</strong> " + request.getMethod() + "</p>");
            out.println("    </div>");
            out.println("    <p><em>Request handled by the Front Controller servlet.</em></p>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}