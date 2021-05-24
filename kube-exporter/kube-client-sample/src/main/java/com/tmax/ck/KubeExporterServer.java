package com.tmax.ck;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;

import org.json.JSONObject;
import org.json.XML;

import java.io.ByteArrayInputStream;

public class KubeExporterServer {
    private ThreadPoolExecutor executor;
    private LinkedBlockingDeque<DataObject> historyInsertQueue;
    private LinkedBlockingQueue<Runnable> workQueue;
    private Thread insertThread;
    private Map<String, String> exporterTargetMap;
    public enum State {INIT, RUNNING, STOPPED};
    private State state = State.INIT;
    private ApiClient client;
    private String kubeApiServer;
    private String bearerToken;
    private String cacrt;

    public KubeExporterServer(String kubeApiServer, String bearerToken, String cacrt){
        this.kubeApiServer = kubeApiServer;
        this.bearerToken = bearerToken;
        this.cacrt = cacrt;
        client = Config.fromToken(kubeApiServer, bearerToken);
        client.setSslCaCert(new ByteArrayInputStream(cacrt.getBytes()));        
        client.setReadTimeout(0);
        Configuration.setDefaultApiClient(client);
    }

    public void init() throws FileNotFoundException {
        state = State.INIT;
        workQueue = new LinkedBlockingQueue<>(1000);
        executor = new ThreadPoolExecutor(100, 100, 1024L, TimeUnit.MILLISECONDS, workQueue);
        historyInsertQueue = new LinkedBlockingDeque<DataObject>(1024);

        insertThread = new Thread(new Runnable(){
            @Override
            public void run() {
                DataObject object = null;
                List<DataObject> insertObjectList = new ArrayList<>();
                while (true) {
                    try {
                        /** polling until historyInsertQueue is empty */
                        while (true) {
                            DataObject tmp = historyInsertQueue.poll();
                            if (tmp != null) insertObjectList.add(tmp);
                            else break;
                        }
                        
                        for (DataObject obj : insertObjectList) {
                            if(obj!=null) writeToTibero(generateTiberoQuery(obj));
                        }
                        
                        /** clear insert list */
                        insertObjectList.clear();

                        /** wait until new DataObject is pushed */
                        object = historyInsertQueue.poll(5000, TimeUnit.MILLISECONDS);
                        insertObjectList.add(object);
                    } catch (InterruptedException e) { e.printStackTrace(); }

                    if (state == State.STOPPED) return;
                }
            }
        });
        insertThread.start();
    }

    public void start() {

    }
    
    public void addResource(String url) {
        /** test */
        // TODO: 중복된 것 처리
        KubeApiExporter kubeApiExporter = new KubeApiExporter(url, client, historyInsertQueue);
        executor.execute(kubeApiExporter);
    }

    public void stop() {
        while(!executor.isShutdown()) {
            executor.shutdown();
        }
        state = State.STOPPED;
    }

    public String getKubeApiServer(){
        return kubeApiServer;
    }
    public String getBearerToken(){
        return bearerToken;
    }
    public String getCacrt(){
        return cacrt;
    }

    private String dropManagedFields(String jsonObj){
        // drop managedFields
        String managedFieldsRegex = ",\"managedFields\":\\[(\\{\"manager\":.*?\\},)*\\{\"manager\":.*?\\}\\]";
        Pattern managedFieldsPattern = Pattern.compile(managedFieldsRegex);
        Matcher managedFieldsMatcher = managedFieldsPattern.matcher(jsonObj);
        while (managedFieldsMatcher.find()) {
            int startIdx = managedFieldsMatcher.start();
            int endIdx = managedFieldsMatcher.end();
            jsonObj = jsonObj.substring(0,startIdx) + jsonObj.substring(endIdx);
        }
        return jsonObj;
    }

    private String replaceSlashesInKeysToUnderscores(String jsonObj){
        // replace slashes in keys to underscores
        String slashesInKeysRegex = "\"([^\":/]*/)+[^\":/]*\":";
        Pattern slashesInKeysPattern = Pattern.compile(slashesInKeysRegex);
        Matcher slashesInKeysMatcher = slashesInKeysPattern.matcher(jsonObj);
        while (slashesInKeysMatcher.find()) {
            int startIdx = slashesInKeysMatcher.start();
            int endIdx = slashesInKeysMatcher.end();
            jsonObj = jsonObj.substring(0,startIdx) + jsonObj.substring(startIdx, endIdx).replace("/","_") + jsonObj.substring(endIdx);
        }
        return jsonObj;
    }

    private String generateTiberoQuery(DataObject obj){
        String unixTime = Long.toString(Instant.now().getEpochSecond());
        String pk = obj.getPrimaryKey();
        String type = obj.getType();
        String jsonObj = obj.getPayload();

        jsonObj = dropManagedFields(jsonObj);
        jsonObj = replaceSlashesInKeysToUnderscores(jsonObj);

        // System.out.println("jsonObj : \n" + jsonObj);

        String xml = "<xml>" + XML.toString(new JSONObject(jsonObj)) + "</xml>";

        return "INSERT INTO \"HELLO\".\"ci_instance_history\"\n"
                    + "VALUES ("
                    + "'" + pk + "'" + ","
                    + "'" + unixTime + "'" + ","
                    + "'" + type + "'" + ","
                    + "XMLType('" + xml + "'));";
    }

    private void writeToTibero(String query){
        Tibero tibero = new Tibero();
        tibero.connect();
        
        tibero.executeQuery(query);
        System.out.println("\nexecuted query\n");

        tibero.disconnect();
    }
}
