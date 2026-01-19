package com.snowly.framework.Util;

import jakarta.servlet.http.HttpSession;
import java.util.*;

//?==== SP11: Session Map wrapper - bridges Map<String, Object> to HttpSession
public class SessionMap implements Map<String, Object> {
    private final HttpSession session;
    
    public SessionMap(HttpSession session) {
        this.session = session;
    }
    
    @Override
    public Object get(Object key) {
        return session.getAttribute(key.toString());
    }
    
    @Override
    public Object put(String key, Object value) {
        Object oldValue = session.getAttribute(key);
        session.setAttribute(key, value);
        System.out.println("Session: Set '" + key + "' = " + value);
        return oldValue;
    }
    
    @Override
    public Object remove(Object key) {
        Object oldValue = session.getAttribute(key.toString());
        session.removeAttribute(key.toString());
        System.out.println("Session: Removed '" + key + "'");
        return oldValue;
    }
    
    @Override
    public boolean containsKey(Object key) {
        return session.getAttribute(key.toString()) != null;
    }
    
    @Override
    public void clear() {
        Enumeration<String> attrs = session.getAttributeNames();
        while (attrs.hasMoreElements()) {
            session.removeAttribute(attrs.nextElement());
        }
        System.out.println("Session: Cleared all attributes");
    }
    
    @Override
    public int size() {
        int count = 0;
        Enumeration<String> attrs = session.getAttributeNames();
        while (attrs.hasMoreElements()) {
            attrs.nextElement();
            count++;
        }
        return count;
    }
    
    @Override
    public boolean isEmpty() {
        return !session.getAttributeNames().hasMoreElements();
    }
    
    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        Enumeration<String> attrs = session.getAttributeNames();
        while (attrs.hasMoreElements()) {
            keys.add(attrs.nextElement());
        }
        return keys;
    }
    
    @Override
    public Collection<Object> values() {
        List<Object> values = new ArrayList<>();
        Enumeration<String> attrs = session.getAttributeNames();
        while (attrs.hasMoreElements()) {
            values.add(session.getAttribute(attrs.nextElement()));
        }
        return values;
    }
    
    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> entries = new HashSet<>();
        Enumeration<String> attrs = session.getAttributeNames();
        while (attrs.hasMoreElements()) {
            String key = attrs.nextElement();
            entries.add(new AbstractMap.SimpleEntry<>(key, session.getAttribute(key)));
        }
        return entries;
    }
    
    @Override
    public boolean containsValue(Object value) {
        Enumeration<String> attrs = session.getAttributeNames();
        while (attrs.hasMoreElements()) {
            Object attrValue = session.getAttribute(attrs.nextElement());
            if (Objects.equals(attrValue, value)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void putAll(Map<? extends String, ?> m) {
        for (Entry<? extends String, ?> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
    
    public void invalidate() {
        session.invalidate();
        System.out.println("Session: Invalidated");
    }
}