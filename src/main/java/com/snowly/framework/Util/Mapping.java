package com.snowly.framework.Util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.snowly.framework.Annotations.AnotParam;

public class Mapping {
    private final Class<?> controllerClass;
    private final Method method;
    private boolean hasPathParams;
    private Map<String, Class<?>> parameterList;

    //?==== SP6_Ter: For path parameter extraction
    private Pattern urlPattern;
    private ArrayList<String> pathParamNames;
    private String originalUrl;

    public Mapping(Class<?> _controClass, Method _method) {
        this.controllerClass = _controClass;
        this.method = _method;
        this.parameterList = new HashMap<>();
        this.pathParamNames = new ArrayList<>();
        
        Parameter[] params = _method.getParameters();
        for(int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            String requestParamName;

            if(param.isAnnotationPresent(AnotParam.class)) {
                //?=== SP6_BIS_Support diffNaming - Use @AnotParam
                AnotParam anotParam = param.getAnnotation(AnotParam.class);
                requestParamName = anotParam.value();
            } else {
                //?=== SP6_Simple: Param Name Matching
                requestParamName = param.getName();
            }
            
            parameterList.put(requestParamName, param.getType());
        }
    }

    //?==== START SP6_Ter ==========
    //*---- Build regex pattern for URL matching
    public void buildUrlPattern(String url) {
        this.originalUrl = url;
        
        /* NB: About building a regex
            * use \\ to let the character after the second \ be valid: \w for word, \d for date,
            * [] for custom class (if \w or \d arent enough)
            *   -> '^' followed by any char inside [] means exclude that char
            *   -> '\w-' add hyphen (-) support to w ([a-zA-Z0-9_]) which doesn't support normally
            * {} are delimiters
            * "^" says 'the pattern must start with [followingWord/character]', no need here since URL don't start w/ {
            * "(\\w+)": () is a group
            * Quantifiers (+ * ? {n} {n,} {n,m})
            *   -> + means 'dont stop at first character, capture full'
            *   -> * for zero or more. ? for zero or one. {n} for exactly n times. {n,} n or more. {n,m} between n and m
        */
        Pattern namePattern = Pattern.compile("\\{([^}]+)\\}"); // establish the pattern {(whateverThing)}
        Matcher nameMatcher = namePattern.matcher(url); // create an engine to find matches in URL, used with m.find
        
        while (nameMatcher.find()) {
            pathParamNames.add(nameMatcher.group(1)); // Extract parameter names from {(group)}, add them in list
        }
        
        // Convert /urlRequest/{smth} to /urlRequest/([^/]+)
        String regexUrl = url.replaceAll("\\{[^}]+\\}", "([^/]+)");
        this.urlPattern = Pattern.compile(regexUrl);
    }
    
    //*---- Check if URL matches mapping pattern
    public boolean matchesUrl(String actualUrl) {
        if (!hasPathParams) {
            return originalUrl.equals(actualUrl);
        }
        
        if (urlPattern == null) {
            return false;
        }
        
        return urlPattern.matcher(actualUrl).matches();
    }

    //*---- Extract path parameters from actual URL
    public Map<String, String> extractPathParams(String actualUrl) {
        Map<String, String> pathParams = new HashMap<>();
        
        if (!hasPathParams || urlPattern == null) {
            return pathParams;
        }
        
        Matcher matcher = urlPattern.matcher(actualUrl);
        if (matcher.matches()) {
            for (int i = 0; i < pathParamNames.size(); i++) {
                String paramName = pathParamNames.get(i);
                String paramValue = matcher.group(i + 1);
                pathParams.put(paramName, paramValue);
            }
        }
        
        return pathParams;
    }
    //?========== END SP6_Ter ==========

    //?=== Getters
    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }

    public boolean hasPathParams() {
        return hasPathParams;
    }
    
    public Map<String, Class<?>> getParameterList() {
        return parameterList;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    //?=== Setters
    public void setHasPathParams(boolean hasPathParams) {
        this.hasPathParams = hasPathParams;
    }
}