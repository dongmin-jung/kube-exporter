package com.tmax.ck;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;

import org.json.*;

public class KubeExporterServer {
    private ThreadPoolExecutor executor;
    private LinkedBlockingDeque<DataObject> historyInsertQueue;
    private LinkedBlockingQueue<Runnable> workQueue;
    private Thread insertThread;
    private Map<String, String> exporterTargetMap;
    public enum State {INIT, RUNNING, STOPPED};
    private State state = State.INIT;
    private ApiClient client;
    public Tibero tibero;

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
                            if (tmp != null)
                                insertObjectList.add(tmp);
                            else
                                break;
                        }
                        
                        for (DataObject obj : insertObjectList) {
                            /** batch insert logic here */
                            System.out.println("DataObject : ");
                            System.out.println(obj);
                            
                            if(obj!=null){
                                String uid = obj.getPrimaryKey();
                                String type = obj.getType();
                                String xml = XML.toString(new JSONObject(obj.getPayload()));
                                String query = "INSERT INTO \"HELLO\".\"ci_instance\"\n"
                                            + "VALUES("
                                            + "\"" + uid + "\"" + ","
                                            + "\"" + type + "\"" + ","
                                            + "sys.XMLType.createXML('" + xml + "'));";
                                System.out.println("query to execute : \n" + query);

                                tibero = new Tibero();
                                tibero.connect();
                                System.out.println("connected to tibero");
                                
                                tibero.executeQuery(query);
                                System.out.println("executed query\n\n");

                                tibero.executeQuery("SELECT * FROM  \"HELLO\".\"ci_instance\"");

                                tibero.disconnect();
                                System.out.println("\n\ndisconnected from tibero");
                            }
                        }
                        
                        /** clear insert list */
                        insertObjectList.clear();

                        /** wait until new DataObject is pushed */
                        object = historyInsertQueue.poll(5000, TimeUnit.MILLISECONDS);
                        insertObjectList.add(object);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (state == State.STOPPED) {
                        return;
                    }
                }
            }
        });
        insertThread.start();

        /** TODO: read from SA secret */
        String bearerToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6Inc2X1BmdWZRS1hRR3JleWdmbXJQN0tnRC1lQkFYZG5reVpQUGloYm5aNGcifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6Imt1YmUtZXhwb3J0ZXItdG9rZW4tcWJ6a2oiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoia3ViZS1leHBvcnRlciIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6IjhmYzIwNDM4LTRjNWQtNDAxMi1hNzBmLTdkMGVkMzkwZWUwZSIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0Omt1YmUtZXhwb3J0ZXIifQ.kGWmWHfrfy_thbZl-VL4zLwoL1Y0WHl5Wg6FujBjA8vpTmSzhNRdeSuph3xUJYeeYji1BpInc3M_2zVYr8oA1hdHPThbYLOiPYJxtUXolO-HoZ8pqXryjj6F8FeFZwm_Qfurv8FQ3m77k29vJmPpJB3bfTExE3DNIRuGwSl_eMrIYtkj5c7PHKQFUj_inRCVn1-mRRDhcHk7EJ-SCXPQdt1EqCwX-aBgGxzFxMpi6e0KMrI3M_gXp_MtToxzY5ZXdgEHTN1nJ804AMqoA6WZ5AmbtN3nHrhps-cJWmYAMPadKrlTmlWuW_zXnb-IF-Pi71bdSLktn2fXpx_vKe5oPw";
        String kubeApiServer = "https://kubernetes.docker.internal:6443";
        client = Config.fromToken(kubeApiServer, bearerToken);

        // String currentPath = new File("").getAbsolutePath();
        File cacert = new File("./kube-exporter/kube-client-sample/src/main/java/com/tmax/ck/resources/ca.crt");
        // System.out.println("ca.crt 존재 여부? " + cacert.exists());
        client.setSslCaCert(new FileInputStream(cacert));
        client.setReadTimeout(0);
        Configuration.setDefaultApiClient(client);
    }

    public void start() {

    }
    
    public void addResource(String url) {
        /** test */
        KubeApiExporter kubeApiExporter = new KubeApiExporter(url, client, historyInsertQueue);
        executor.execute(kubeApiExporter);
    }

    public void stop() {
        while(!executor.isShutdown()) {
            executor.shutdown();
        }
        state = State.STOPPED;
    }
}
