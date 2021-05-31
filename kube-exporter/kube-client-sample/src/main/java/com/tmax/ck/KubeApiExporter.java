package com.tmax.ck;

import java.util.concurrent.LinkedBlockingDeque;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.kubernetes.client.openapi.ApiClient;

import okhttp3.Call;
import okhttp3.Response;

public class KubeApiExporter extends Thread {
    private String url;
    private ApiClient client;
    private LinkedBlockingDeque<DataObject> insertQueue;
    private Gson gson = new Gson();
    private enum STATE {RUNNING, STOPPED};
    private STATE state = STATE.RUNNING;

    public KubeApiExporter(String url, ApiClient client, LinkedBlockingDeque<DataObject> insertQueue) {
        this.url = url;
        this.client = client;
        this.insertQueue = insertQueue;
    }

    @Override
    public void run() {
        try {
            System.out.println("start sync : " + url);

            String line = null;
            Call call = GenericCaller.makeCall(url, client, true);
            Response response = call.execute();
            
            while(state.equals(STATE.RUNNING)) {
                /** connect or reconnect */
                if (call.isCanceled()) {
                    call = GenericCaller.makeCall(url, client, true);
                    response = call.execute();
                }

                /** read chunked response */
                line = response.body().source().readUtf8Line();

                /** enqueue DataObject */
                if (line != null) {
                    JsonObject jsonObject = gson.fromJson(line, JsonObject.class);
                    DataObject insertObject = null;
                    String key = getPK(jsonObject);
                    String type = getType(jsonObject);
                    String payload = getPayload(jsonObject);
                    insertObject = new DataObject(key, type, payload);
                    insertQueue.push(insertObject);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopExporter() {
        state = STATE.STOPPED;
    }

    public void runExporter() {
        state = STATE.RUNNING;
    }

    private String getPayload(JsonObject jsonObject) {
        return jsonObject.get("object").toString();
    }

    private String getPK(JsonObject jsonObject) {
        // System.out.println(jsonObject.toString());
        try {
            String namespace = jsonObject.get("object").getAsJsonObject().get("metadata").getAsJsonObject().get("namespace").getAsString();
            String kind = jsonObject.get("object").getAsJsonObject().get("kind").getAsString();
            String name = jsonObject.get("object").getAsJsonObject().get("metadata").getAsJsonObject().get("name").getAsString();
            String uid = jsonObject.get("object").getAsJsonObject().get("metadata").getAsJsonObject().get("uid").getAsString();
            String version = jsonObject.get("object").getAsJsonObject().get("metadata").getAsJsonObject().get("resourceVersion").getAsString();
            return client.getBasePath() + "::" + namespace + "::" + kind + "::" + name + "::" + uid + "::" + version;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getType(JsonObject jsonObject) {
        String type = jsonObject.get("object").getAsJsonObject().get("kind").getAsString();
        return type;
    }
    
    public ApiClient getClient() {
        return client;
    }

    public String getUrl() {
        return url;
    }
}