package com.snowly.framework.Util.Sprints;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.apache.commons.lang3.ClassUtils;

import com.snowly.framework.Annotations.AnotSession;
import com.snowly.framework.Util.FileUpload;
import com.snowly.framework.Util.Mapping;
import com.snowly.framework.Util.SessionMap;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Contains SP8, SP8_BIS, SP10, SP11 declarations
public class ParameterResolver {

    //?==== SP8 & SP8_BIS & SP10 & SP11: Handle Map parameters, Object binding, List binding, Session
    public static Object[] prepareMethodArguments(HttpServletRequest request, Mapping mapping, Map<String, String> pathParams) {
        Map<String, Class<?>> paramList = mapping.getParameterList();
        Object[] args = new Object[paramList.size()];
        int i = 0;
        
        Map<String, String[]> allParams = request.getParameterMap();
        List<FileUpload> fileUploads = extractFileUploads(request);
        
        //?--- SP11: Get method parameters to check for @AnotSession
        Parameter[] parameters = mapping.getMethod().getParameters();
        
        for(Map.Entry<String, Class<?>> entry : paramList.entrySet()) {
            String paramName = entry.getKey();
            Class<?> paramType = entry.getValue();
            Parameter currentParam = parameters[i];
            
            //*--- SP11: Check if parameter has @AnotSession annotation
            if (currentParam.isAnnotationPresent(AnotSession.class)) {
                if (Map.class.isAssignableFrom(paramType)) {
                    HttpSession session = request.getSession(true); // Create session if doesn't exist
                    args[i] = new SessionMap(session);
                    System.out.println("Injecting session map for parameter: " + paramName);
                } else {
                    System.err.println("WARNING: @AnotSession used on non-Map parameter: " + paramName);
                    args[i] = null;
                }
            }
            //*--- SP10: Check if parameter is a List (could be FileUpload or custom objects)
            else if (List.class.isAssignableFrom(paramType)) {
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

    //?==== SP8: Create Map from ALL request parameters (including multipart form fields)
    private static Map<String, Object> createMapFromRequest(HttpServletRequest request, Map<String, String> pathParams) {
        Map<String, Object> resultMap = new HashMap<>();
        
        //*--- Add all path parameters
        resultMap.putAll(pathParams);
        
        //*--- Check if this is a multipart request
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            //*--- For multipart requests, extract form fields from Parts
            try {
                Collection<Part> parts = request.getParts();
                for (Part part : parts) {
                    String fieldName = part.getName();
                    String filename = part.getSubmittedFileName();
                    
                    //*--- Only process non-file fields (regular form inputs)
                    if (filename == null || filename.isEmpty()) {
                        String value = new String(part.getInputStream().readAllBytes());
                        resultMap.put(fieldName, value);
                        System.out.println("Extracted form field: " + fieldName + " = " + value);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error extracting multipart form fields: " + e.getMessage());
            }
        } else {
            //*--- Regular request - use getParameterMap()
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
    private static boolean isCustomObject(Class<?> type) {
        return !type.isPrimitive() 
            && !ClassUtils.isPrimitiveWrapper(type)
            && !type.equals(String.class)
            && !Map.class.isAssignableFrom(type)
            && !List.class.isAssignableFrom(type);
    }

    //?==== SP8_BIS: Create object instance from request parameters
    private static Object createObjectFromRequest(Class<?> objectType, Map<String, String[]> allParams, String objectPrefix) {
        try {
            System.out.println("Creating object of type: " + objectType.getSimpleName() + " with prefix: '" + objectPrefix + "'");
            
            Object instance = objectType.getDeclaredConstructor().newInstance();
            java.lang.reflect.Field[] fields = objectType.getDeclaredFields();
            
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                
                String paramKey = objectPrefix.isEmpty() ? fieldName : objectPrefix + "." + fieldName;
                String[] paramValues = allParams.get(paramKey);
                
                if (isCustomObject(fieldType)) {
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

    //?==== SP8_BIS: Create List of objects from array-indexed parameters
    private static List<Object> createListFromRequest(Type genericType, Map<String, String[]> allParams, String paramName) {
        List<Object> resultList = new ArrayList<>();
        
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
        
        String arrayPattern = paramName + "\\[(\\d+)\\](\\.(.+))?";
        Pattern pattern = Pattern.compile(arrayPattern);
        
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
    private static Object convertParameterType(String value, Class<?> targetType) {
        if (targetType == String.class) return value;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(value);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(value);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(value);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(value);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(value);
        if (targetType == short.class || targetType == Short.class) return Short.parseShort(value);
        if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(value);
        
        return value;
    }

    //?==== SP10: Check if List parameter is for file uploads (List<FileUpload>)
    private static boolean isFileUploadList(Type genericType) {
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
    private static List<FileUpload> extractFileUploads(HttpServletRequest request) {
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