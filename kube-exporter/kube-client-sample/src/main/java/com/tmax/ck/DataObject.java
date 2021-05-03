package com.tmax.ck;

public class DataObject {
    private String primaryKey;
    private String type;
    private String payload;

    public DataObject() {
        super();
    }

    public DataObject(String primaryKey, String type, String payload) {
        this.primaryKey = primaryKey;
        this.type = type;
        this.payload = payload;
    }

    public String toString() {
        return "\n\n\nprimaryKey : " + primaryKey + ", \n\npayload : \n" + payload;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }
}
