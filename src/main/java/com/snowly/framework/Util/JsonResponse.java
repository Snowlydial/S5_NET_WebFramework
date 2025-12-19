package com.snowly.framework.Util;

public class JsonResponse {
    private String status;
    private int code;
    private Object data;
    
    //?=== Constructors
    public JsonResponse(String status, int code, Object data) {
        this.status = status;
        this.code = code;
        this.data = data;
    }

    //?=== Getters
    public String getStatus() { return status; }
    public int getCode() { return code; }
    public Object getData() { return data; }
    
    //?=== Setters
    public void setStatus(String status) { this.status = status; }
    public void setCode(int code) { this.code = code; }
    public void setData(Object data) { this.data = data; }

    //?=== Methods
    public static JsonResponse success(Object data) {
        return new JsonResponse("success", 200, data);
    }
    
    public static JsonResponse error(int code, String message) {
        return new JsonResponse("error", code, message);
    }
}