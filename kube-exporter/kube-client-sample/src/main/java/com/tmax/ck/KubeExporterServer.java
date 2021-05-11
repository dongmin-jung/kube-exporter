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
                            // System.out.println("DataObject : ");
                            // System.out.println(obj);
                            
                            if(obj!=null){
                                String unixTime = Long.toString(Instant.now().getEpochSecond());
                                String pk = obj.getPrimaryKey();
                                String type = obj.getType();

                                //TODO: subtract the following match : ,"managedFields":\[({"manager":.*?},)*{"manager":.*?}\]
                                String jsonObj = obj.getPayload();
                                String managedFieldsRegex = ",\"managedFields\":\\[(\\{\"manager\":.*?\\},)*\\{\"manager\":.*?\\}\\]";
                                Pattern pattern = Pattern.compile(managedFieldsRegex);
                                Matcher matcher = pattern.matcher(jsonObj);
                                while (matcher.find()) {
                                    int startIdx = matcher.start();
                                    int endIdx = matcher.end();
                                    jsonObj = jsonObj.substring(0,startIdx) + jsonObj.substring(endIdx);
                                }
                                // System.out.println("jsonObj = " + jsonObj);

                                String xml = XML.toString(new JSONObject(jsonObj));

                                String query = "INSERT INTO \"HELLO\".\"ci_instance_history\"\n"
                                            + "VALUES ("
                                            + "'" + unixTime + "." + pk + "'" + ","
                                            + "'" + unixTime + "'" + ","
                                            + "'" + type + "'" + ","
                                            + "XMLType('" + xml + "'));";
                                System.out.println("query to execute : \n" + query);

                                tibero = new Tibero();
                                tibero.connect();
                                System.out.println("connected to tibero");
                                
                                tibero.executeQuery(query);
                                System.out.println("executed query\n\n");

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
        String bearerToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6Imt6NnpPTllzNGNnN2RwdGlhcnNLNGpGNGxiV1JreGdURF9acmhtUkN0bFUifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6InJvb3QtdG9rZW4tbTcyd3MiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoicm9vdCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6Ijc3OGUyZTYwLWY2YzItNDZmOS05NzY4LWQ5ZGIyOTZiMWI2YSIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0OnJvb3QifQ.QJZx1w9dH3-T11o4-l3oIZve2xT74aBMmG89aRtbKvsTSOJzktGq3-qo4ydU6PvgZi6DOeVNa93J9XvBrWtQCqXX3kTwBIt551l4d5vGy8SZLEDWWsXk58YFTb8i3aesGT88V3B_QT-9BRaqixwssLXucEgeSkbp_HhbTQaocxDh79QXoYMP-hBFI9lTpNJT_DhQM9iY406PwK-hddoSGmP_cD6hPfBMr53Ht6e9SnuFf4_zdDfFpiONNy-a-SDrXFd45_JNoRmFRwy3LxwK8QkBnpuLfenZEaZI1meJIXN4CVnRIKl_jHwFqUVRqdfE6_JROZtakdbgUc25aiybYQ";
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
