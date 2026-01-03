package com.snowly.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.snowly.framework.Util.Mapping;
import com.snowly.framework.Util.JsonResponse;
import com.snowly.framework.Annotations.AnotController;
import com.snowly.framework.Annotations.AnotJSON;
import com.snowly.framework.Annotations.AnotURL;
import com.snowly.framework.Annotations.HTTP_Methods.AnotGetMapping;
import com.snowly.framework.Annotations.HTTP_Methods.AnotPostMapping;
import com.snowly.framework.Annotations.HTTP_Methods.AnotRequestMapping;
import com.snowly.framework.Util.ControllerScanner;
import com.snowly.framework.Util.FileUpload;
import com.snowly.framework.Util.ModelView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.math.NumberUtils;

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

    // Helper method to add mappings
    private void addMapping(HashMap<String, List<Mapping>> urlHashmapping, String basePath, String methodPath, String httpMethod, Class<?> controllerClass, Method method) {
        String fullUrl = basePath + methodPath;
        
        Mapping mapping = new Mapping(controllerClass, method);
        mapping.setHttpMethod(httpMethod);

        //?==== Start Sprint3_BIS_Accolade_Support
        boolean hasPathParams = methodPath.contains("{") && methodPath.contains("}");
        mapping.setHasPathParams(hasPathParams);
        //?==== End Sprint3_BIS_Accolade_Support
        
        //?==== START SP6_Ter ====
        if (hasPathParams) {
            mapping.buildUrlPattern(fullUrl);
            System.out.println("Mapped (" + httpMethod + " with path params): " + fullUrl + " -> " + controllerClass.getSimpleName() + "." + method.getName());
        } else {
            System.out.println("Mapped (" + httpMethod + "): " + fullUrl + " -> " + controllerClass.getSimpleName() + "." + method.getName());
        }
        //?==== END SP6_Ter ====
        
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

        //?--- SP7: GET, POST, etc.
        String requestMethod = request.getMethod();
        
        ServletContext servletContext = getServletContext();
        HashMap<String, List<Mapping>> urlHashmapping = (HashMap<String, List<Mapping>>) servletContext.getAttribute("urlHashmapping");
        
        Mapping mapping = null;
        Map<String, String> pathParams = new HashMap<>();

        //?--- SP6_Ter Change: Try exact match
        if (urlHashmapping != null && urlHashmapping.containsKey(path)) {
            List<Mapping> candidates = urlHashmapping.get(path);
            mapping = findMatchingMapping(candidates, requestMethod);
        }
        //?--- SP6_Ter Change: Try pattern matching
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

        if (mapping != null) {
            try {
                Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                
                // SP6_Ter: Merging regular params + path params
                // Object[] args = prepareMethodArgumentsWithPathParams(request, mapping, pathParams);
                
                //?=== SP8 & SP8_BIS: Prepare arguments with Map and Object binding support
                Object[] args = prepareMethodArgumentsWithBinding(request, mapping, pathParams);

                Object result = mapping.getMethod().invoke(controllerInstance, args);
                
                boolean isJsonResponse = mapping.getMethod().isAnnotationPresent(AnotJSON.class);
                if (isJsonResponse) {
                    handleJsonResponse(response, result);
                } else {
                    handleRegularResponse(request, response, result);
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

    //?==== SP6_Ter: Wrapper that handles path params + regular params
    @SuppressWarnings("unused")
    private Object[] prepareMethodArgumentsWithPathParams(HttpServletRequest request, Mapping mapping, Map<String, String> pathParams) {
        if (pathParams == null || pathParams.isEmpty()) {
            return prepareMethodArguments(request, mapping);
        }
        
        Map<String, Class<?>> paramList = mapping.getParameterList();
        Object[] args = new Object[paramList.size()];
        int i = 0;
        
        for(Map.Entry<String, Class<?>> entry : paramList.entrySet()) {
            String paramName = entry.getKey();
            Class<?> paramType = entry.getValue();
            String paramValue = null;
            
            if (pathParams.containsKey(paramName)) { 
                paramValue = pathParams.get(paramName); // use name from pathVariable first
            } else {
                paramValue = request.getParameter(paramName);  // use name from paramAnotName or paramMethodName
            }
            
            if(paramValue != null) {
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

    //?==== SP7_Helper: Find mapping that matches the HTTP method
    private Mapping findMatchingMapping(List<Mapping> candidates, String requestMethod) {
        for (Mapping candidate : candidates) {
            if (candidate.getHttpMethod().equals("ALL") || 
                candidate.getHttpMethod().equals(requestMethod)) {
                return candidate;
            }
        }
        return null;
    }

    //?==== SP8: Create Map from ALL request parameters (including multipart form fields)
    private Map<String, Object> createMapFromRequest(HttpServletRequest request, Map<String, String> pathParams) {
        Map<String, Object> resultMap = new HashMap<>();
        
        //*--- Add all path parameters
        resultMap.putAll(pathParams);
        
        //*--- Check if this is a multipart request
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            // For multipart requests, extract form fields from Parts
            try {
                Collection<Part> parts = request.getParts();
                for (Part part : parts) {
                    String fieldName = part.getName();
                    String filename = part.getSubmittedFileName();
                    
                    // Only process non-file fields (regular form inputs)
                    if (filename == null || filename.isEmpty()) {
                        // Read the text value
                        String value = new String(part.getInputStream().readAllBytes());
                        resultMap.put(fieldName, value);
                        System.out.println("Extracted form field: " + fieldName + " = " + value);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error extracting multipart form fields: " + e.getMessage());
            }
        } else {
            // Regular request - use getParameterMap()
            Map<String, String[]> allParams = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();
                
                if (values.length == 1) {
                    resultMap.put(key, values[0]);
                } else {
                    resultMap.put(key, values);
                }
            }
        }
        
        System.out.println("Created Map with " + resultMap.size() + " entries: " + resultMap.keySet());
        return resultMap;
    }

    //?==== SP8_BIS: Check if type is a custom object (not primitive/wrapper/String)
    private boolean isCustomObject(Class<?> type) {
        return !type.isPrimitive() 
            && !ClassUtils.isPrimitiveWrapper(type)
            && !type.equals(String.class)
            && !Map.class.isAssignableFrom(type)
            && !List.class.isAssignableFrom(type);
    }

    //?==== SP8_BIS: Create object instance from request parameters
    private Object createObjectFromRequest(Class<?> objectType, Map<String, String[]> allParams, String objectPrefix) {
        try {
            System.out.println("Creating object of type: " + objectType.getSimpleName() + " with prefix: '" + objectPrefix + "'");
            
            // Create instance of the object
            Object instance = objectType.getDeclaredConstructor().newInstance();
            
            // Get all fields of the object
            java.lang.reflect.Field[] fields = objectType.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                
                // Look for parameter with pattern: objectPrefix.fieldName or just fieldName
                String paramKey = objectPrefix.isEmpty() ? fieldName : objectPrefix + "." + fieldName;
                String[] paramValues = allParams.get(paramKey);
                
                // Check if field is itself a custom object (nested)
                if (isCustomObject(fieldType)) {
                    // For nested objects, check if there are ANY parameters that start with paramKey
                    boolean hasNestedParams = false;
                    String nestedPrefix = paramKey + ".";
                    
                    for (String key : allParams.keySet()) {
                        if (key.startsWith(nestedPrefix)) {
                            hasNestedParams = true;
                            break;
                        }
                    }
                    
                    System.out.println("  Looking for nested field '" + fieldName + "' with prefix: '" + paramKey + "' - Has nested params: " + hasNestedParams);
                    
                    if (hasNestedParams) {
                        System.out.println("  Field '" + fieldName + "' is a nested object, creating recursively...");
                        Object nestedObject = createObjectFromRequest(fieldType, allParams, paramKey);
                        field.set(instance, nestedObject);
                    }
                } else {
                    // For primitive/String fields, look for direct parameter
                    System.out.println("  Looking for field '" + fieldName + "' with key: '" + paramKey + "' - Found: " + (paramValues != null));
                    
                    if (paramValues != null && paramValues.length > 0) {
                        String paramValue = paramValues[0];
                        Object convertedValue = convertParameterType(paramValue, fieldType);
                        field.set(instance, convertedValue);
                        System.out.println("  Set " + objectType.getSimpleName() + "." + fieldName + " = " + paramValue);
                    }
                }
            }
            
            System.out.println("Successfully created: " + instance);
            return instance;
            
        } catch (Exception e) {
            System.err.println("Error creating object of type " + objectType.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //?==== SP8 & SP8_BIS & SP10: Handle Map parameters, Object binding + List binding
    private Object[] prepareMethodArgumentsWithBinding(HttpServletRequest request, Mapping mapping, Map<String, String> pathParams) {
        Map<String, Class<?>> paramList = mapping.getParameterList();
        Object[] args = new Object[paramList.size()];
        int i = 0;
        
        Map<String, String[]> allParams = request.getParameterMap();
        List<FileUpload> fileUploads = extractFileUploads(request);
        
        for(Map.Entry<String, Class<?>> entry : paramList.entrySet()) {
            String paramName = entry.getKey();
            Class<?> paramType = entry.getValue();
            
            //*--- SP10: Check if parameter is a List (could be FileUpload or custom objects)
            if (List.class.isAssignableFrom(paramType)) {
                Type genericType = mapping.getGenericType(paramName);
                
                if (isFileUploadList(genericType)) {
                    System.out.println("Injecting file upload list for parameter: " + paramName);
                    args[i] = fileUploads;
                } else {
                    // Regular List<CustomObject>
                    args[i] = createListFromRequest(genericType, allParams, paramName);
                }
            }
            //*--- SP8: Check if parameter is a Map (for form data)
            else if (Map.class.isAssignableFrom(paramType)) {
                System.out.println("Injecting form data map for parameter: " + paramName);
                args[i] = createMapFromRequest(request, pathParams);
            }
            //*--- SP8_BIS: Check if parameter is a custom object
            else if (isCustomObject(paramType)) {
                args[i] = createObjectFromRequest(paramType, allParams, paramName);
            }

            //*--- Regular parameter handling (from Sprint 6)
            else {
                String paramValue = null;
                
                if (pathParams.containsKey(paramName)) {
                    paramValue = pathParams.get(paramName);
                } else {
                    paramValue = request.getParameter(paramName);
                }
                
                if(paramValue != null) {
                    args[i] = convertParameterType(paramValue, paramType);
                } else {
                    System.out.println("WARNING: Parameter '" + paramName + "' is null");
                }
            }
            i++;
        }
        return args;
    }

    //?==== SP8_BIS: Create List of objects from array-indexed parameters
    private List<Object> createListFromRequest(Type genericType, Map<String, String[]> allParams, String paramName) {
        List<Object> resultList = new ArrayList<>();
        
        // Extract the actual class from List<ClassName>
        Class<?> elementClass = null;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            Type[] typeArgs = pType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                elementClass = (Class<?>) typeArgs[0];
                System.out.println("Detected List element type: " + elementClass.getSimpleName());
            }
        }
        
        if (elementClass == null) {
            System.err.println("Could not determine List element type, returning empty list");
            return resultList;
        }
        
        // Pattern to match: paramName[index].field
        String arrayPattern = paramName + "\\[(\\d+)\\](\\.(.+))?";
        Pattern pattern = Pattern.compile(arrayPattern);
        
        // Group parameters by index
        Map<Integer, Map<String, String[]>> indexedParams = new HashMap<>();
        for (String key : allParams.keySet()) {
            Matcher matcher = pattern.matcher(key);
            if (matcher.matches()) {
                int index = Integer.parseInt(matcher.group(1));
                indexedParams.putIfAbsent(index, new HashMap<>());
                indexedParams.get(index).put(key, allParams.get(key));
            }
        }
        
        System.out.println("Found " + indexedParams.size() + " indexed objects for parameter '" + paramName + "'");
        
        // Create objects for each index
        for (int idx = 0; idx < indexedParams.size(); idx++) {
            if (indexedParams.containsKey(idx)) {
                Map<String, String[]> params = indexedParams.get(idx);
                String objectPrefix = paramName + "[" + idx + "]";
                
                Object obj = createObjectFromRequest(elementClass, params, objectPrefix);
                if (obj != null) {
                    resultList.add(obj);
                }
            }
        }
        
        return resultList;
    }

    //?==== SP8_BIS Helper: Convert string parameter to the correct type
    private Object convertParameterType(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        }
        
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value);
        }
        if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(value);
        }
        
        return value;
    }

    //? Handle regular response
    private void handleRegularResponse(HttpServletRequest request, HttpServletResponse response, Object result) throws IOException, ServletException {
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
    }

    //?==== SP9: Handle JSON response
    private void handleJsonResponse(HttpServletResponse response, Object result) throws IOException {
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

    //?==== SP10: Check if Map parameter is for file uploads
    private boolean isFileUploadList(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            Type[] typeArgs = pType.getActualTypeArguments();
            
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                Class<?> elementClass = (Class<?>) typeArgs[0];
                return elementClass.equals(FileUpload.class);
            }
        }
        return false;
    }

    //?==== SP10: Extract file uploads from request
    private List<FileUpload> extractFileUploads(HttpServletRequest request) {
        List<FileUpload> fileList = new ArrayList<>();
        
        try {
            String contentType = request.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("multipart/")) {
                return fileList;
            }
            
            Collection<Part> parts = request.getParts();
            System.out.println("Found " + parts.size() + " parts in request");
            
            for (Part part : parts) {
                String fieldName = part.getName();
                String filename = part.getSubmittedFileName();
                
                if (filename != null && !filename.isEmpty()) {
                    byte[] fileBytes = part.getInputStream().readAllBytes();
                    String fileContentType = part.getContentType();
                    
                    FileUpload file = new FileUpload(fieldName, filename, fileBytes, fileContentType);
                    fileList.add(file);
                    
                    System.out.println("Extracted: " + file);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting file uploads: " + e.getMessage());
            e.printStackTrace();
        }
        
        return fileList;
    }
}