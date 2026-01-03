package com.snowly.framework.Util.Sprints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.snowly.framework.Util.JsonResponse;
import com.snowly.framework.Util.ModelView;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Contains SP5, SP9 declarations
public class ResponseHandler {

    //? Handle regular response
    public static void handleRegularResponse(HttpServletRequest request, HttpServletResponse response, Object result) 
            throws IOException, ServletException {
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
            if (viewName.startsWith("redirect:")) {
                String target = viewName.substring("redirect:".length());
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    response.sendRedirect(target);
                } else {
                    String contextPath = request.getContextPath();
                    if (!target.startsWith("/")) {
                        target = "/" + target;
                    }
                    response.sendRedirect(contextPath + target);
                }
                return;
            }

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
    }

    //?==== SP9: Handle JSON response
    public static void handleJsonResponse(HttpServletResponse response, Object result) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonResponse jsonResponse;
        
        try {
            if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;
                if(mv.getData().isEmpty()) {
                    jsonResponse = JsonResponse.error(404, "Resource not found, ModelView Data is Empty");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    jsonResponse = JsonResponse.success(mv.getData());
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            } else if (result instanceof String) { 
                jsonResponse = JsonResponse.success(result);
                response.setStatus(HttpServletResponse.SC_OK);
            } else if (result instanceof List) {
                if( ((List<?>) result).isEmpty()) {
                    jsonResponse = JsonResponse.error(404, "Resource not found, List is Empty");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    jsonResponse = JsonResponse.success(result);
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            } else if (result instanceof Map) {
                if( ((Map<?,?>) result).isEmpty()) {
                    jsonResponse = JsonResponse.error(404, "Resource not found, Map is Empty");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    jsonResponse = JsonResponse.success(result);
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            } else {
                if(result == null) {
                    jsonResponse = JsonResponse.error(404, "Resource not found, Object is Null");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    jsonResponse = JsonResponse.success(result);
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            }
            
            String json = gson.toJson(jsonResponse);
            
            try (PrintWriter out = response.getWriter()) {
                out.print(json);
            }
            
            System.out.println("JSON Response sent (" + response.getStatus() + "): " + json);
            
        } catch (Exception e) {
            //*--- Error during JSON serialization
            System.err.println("Error creating JSON response: " + e.getMessage());
            e.printStackTrace();
            
            jsonResponse = JsonResponse.error(500, "Internal server error: " + e.getMessage());
            String errorJson = gson.toJson(jsonResponse);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            try (PrintWriter out = response.getWriter()) {
                out.print(errorJson);
            }
        }
    }

    public static void sendError(HttpServletResponse response, int statusCode, String message) throws IOException {
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