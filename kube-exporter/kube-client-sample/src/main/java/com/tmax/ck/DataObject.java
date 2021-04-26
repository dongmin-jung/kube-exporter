package com.tmax.ck;

public class DataObject {
    private String action;
    private String primaryKey;
    private String payload;

    public DataObject() {
        super();
    }

    public DataObject(String action, String primaryKey, String payload) {
        this.action = action;
        this.primaryKey = primaryKey;
        this.payload = payload;
    }

    public String toString() {
        return "primaryKey : " + primaryKey + ", payload : " + payload;
    }

    public String getAction() {
        return action;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public String getPayload() {
        return payload;
    }
}
