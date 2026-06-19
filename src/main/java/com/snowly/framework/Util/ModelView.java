package com.snowly.framework.Util;

import java.util.HashMap;

public class ModelView {
    private String view;
    private HashMap<String, Object> data;
    
    public ModelView() {
        this.data = new HashMap<>();
    }

    public ModelView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }
    
    //?--- Get
    public String getView() {
        return view;
    }
    public HashMap<String, Object> getData() {
        return data;
    }

    //?--- Set
    public void setView(String view) {
        this.view = view;
    }
    public void addData(String key, Object value) {
        data.put(key, value);        
    }
}