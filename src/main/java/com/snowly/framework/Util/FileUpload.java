package com.snowly.framework.Util;

public class FileUpload {
    private final String fieldName;
    private final String filename;
    private final byte[] content;
    private final String contentType;
    
    public FileUpload(String fieldName, String filename, byte[] content, String contentType) {
        this.fieldName = fieldName;
        this.filename = filename;
        this.content = content;
        this.contentType = contentType;
    }
    
    public String getFieldName() { 
        return fieldName; 
    }
    public String getFilename() { 
        return filename; 
    }
    public byte[] getContent() { 
        return content; 
    }
    public String getContentType() { 
        return contentType; 
    }
    public int getSize() { 
        return content.length; 
    }
    public String getExtension() {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    @Override
    public String toString() {
        return "FileUpload{" +
               "fieldName='" + fieldName + '\'' +
               ", filename='" + filename + '\'' +
               ", size=" + content.length +
               ", contentType='" + contentType + '\'' +
               '}';
    }
}