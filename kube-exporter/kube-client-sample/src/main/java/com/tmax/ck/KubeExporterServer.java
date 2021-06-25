package com.tmax.ck;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.apache.commons.codec.binary.Base64;

public class KubeExporterServer {
    private ThreadPoolExecutor executor;
    private LinkedBlockingDeque<DataObject> historyInsertQueue;
    private LinkedBlockingQueue<Runnable> workQueue;
    private Thread insertThread;
    public enum State {INIT, RUNNING, STOPPED};
    private State state = State.INIT;
    private ApiClient client;
    private String kubeApiServer;
    private String bearerToken;
    private String cacrt;
    private Map<String, KubeApiExporter> exporterMap = new HashMap<>();

    public KubeExporterServer(String kubeApiServer, String bearerToken, String cacrt){
        this.kubeApiServer = kubeApiServer;
        this.bearerToken = bearerToken;
        this.cacrt = cacrt;

        client = Config.fromToken(kubeApiServer, new String(Base64.decodeBase64(bearerToken.getBytes())));
        client.setSslCaCert(new ByteArrayInputStream(Base64.decodeBase64(cacrt.getBytes())));        
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
                        while (state.equals(State.RUNNING)) {
                            DataObject tmp = historyInsertQueue.poll();
                            if (tmp != null) insertObjectList.add(tmp);
                            else break;
                        }
                        
                        for (DataObject obj : insertObjectList) {
                            writeToMysql(generateMysqlQuery(obj));
                        }
                        
                        /** clear insert list */
                        insertObjectList.clear();

                        /** wait until new DataObject is pushed */
                        object = historyInsertQueue.poll(5000, TimeUnit.MILLISECONDS);
                        if (object != null) insertObjectList.add(object);
                    } catch (InterruptedException e) { e.printStackTrace(); }

                    if (state.equals(State.STOPPED)) return;
                }
            }
        });
        insertThread.start();
    }

    public void start() {
        state = State.RUNNING;
    }

    public void stop() {
        while(!executor.isShutdown()) {
            executor.shutdown();
        }
        state = State.STOPPED;
    }
    
    public void addResource(String url) {
        if (!exporterMap.containsKey(url)) {
            exporterMap.put(url, new KubeApiExporter(url, client, historyInsertQueue));
            executor.execute(exporterMap.get(url));
            System.out.println("addResource done");
        } else {
            System.out.println("addResource skipped");
        }
    }

    public void deleteResource(String url) {
        KubeApiExporter exporterToDelete = exporterMap.get(url);
        exporterToDelete.stopExporter();
        while (exporterToDelete.isAlive()) {
            System.out.println("try to interrupt");
            exporterToDelete.interrupt();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    private String generateMysqlQuery(DataObject obj){
        String unixTime = Long.toString(Instant.now().getEpochSecond());
        String pk = obj.getPrimaryKey();
        String type = obj.getType();
        String jsonString = obj.getPayload();

        System.out.println("jsonString : " + jsonString);

        // https://stackoverflow.com/questions/47724990/invalid-json-text-sql-java-json-object
        // https://stackoverflow.com/questions/1701839/string-replaceall-single-backslashes-with-double-backslashes
        jsonString = jsonString.replaceAll("\\\\\\\"","\\\\\\\\\"");
        System.out.println("jsonString : " + jsonString);

        return "INSERT INTO ci_instance_history\n"
                    + "VALUES ("
                    + "'" + pk + "'" + ","
                    + "'" + unixTime + "'" + ","
                    + "'" + type + "'" + ","
                    + "'" + jsonString + "');";
    }

    private void writeToMysql(String query){
        MySQLConn mysql = new MySQLConn();
        mysql.connect();
        
        mysql.executeUpdate(query);
        System.out.println("\nexecuted query\n");

        mysql.disconnect();
    }
}
