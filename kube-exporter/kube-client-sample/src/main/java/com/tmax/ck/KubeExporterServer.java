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
                                System.out.println("unixTime="+unixTime);
                                String pk = obj.getPrimaryKey();
                                String type = obj.getType();
                                String jsonObj = obj.getPayload();

                                // drop managedFields
                                String managedFieldsRegex = ",\"managedFields\":\\[(\\{\"manager\":.*?\\},)*\\{\"manager\":.*?\\}\\]";
                                Pattern managedFieldsPattern = Pattern.compile(managedFieldsRegex);
                                Matcher managedFieldsMatcher = managedFieldsPattern.matcher(jsonObj);
                                while (managedFieldsMatcher.find()) {
                                    int startIdx = managedFieldsMatcher.start();
                                    int endIdx = managedFieldsMatcher.end();
                                    jsonObj = jsonObj.substring(0,startIdx) + jsonObj.substring(endIdx);
                                }

                                // replace slashes in keys to underscores
                                String slashesInKeysRegex = "\"([^\":/]*/)+[^\":/]*\":";
                                Pattern slashesInKeysPattern = Pattern.compile(slashesInKeysRegex);
                                Matcher slashesInKeysMatcher = slashesInKeysPattern.matcher(jsonObj);
                                while (slashesInKeysMatcher.find()) {
                                    int startIdx = slashesInKeysMatcher.start();
                                    int endIdx = slashesInKeysMatcher.end();
                                    jsonObj = jsonObj.substring(0,startIdx) + jsonObj.substring(startIdx, endIdx).replace("/","_") + jsonObj.substring(endIdx);
                                }

                                System.out.println("jsonObj : \n" + jsonObj);

                                String xml = "<xml>" + XML.toString(new JSONObject(jsonObj)) + "</xml>";

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
                                
                                String unixTime2 = Long.toString(Instant.now().getEpochSecond());
                                System.out.println("unixTime2:"+unixTime2);
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
        String bearerToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6InpXdEFLRjdiRDRrM2M2cnlHaE05UmppclliaVl5aUF3UHhlemt0Nmk0UXcifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJkZWZhdWx0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6InN1cGVydXNlci10b2tlbi10cWNiNCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJzdXBlcnVzZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiI3MmM0ZWVhMS1jMTI2LTRmYWQtOTc2YS0yZWMzZWY1MTJkM2IiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6ZGVmYXVsdDpzdXBlcnVzZXIifQ.ZzUZyvVXcX_wx9Rr0wkfx6fCty4FFC8-YfdN_SUdXEGVSehtAl7WTsc4SUi55FIdK7H8RPy5Z8Hw4Xagw6Lk7BjRMuS_0wtURqqvnhW49Hwv3kQEF0ek1Ej0zcMUmzapVOC76A3U9jbUF32PignMga2klSFFewbrIsoabLXhzHZ-RpwrjoqY_ClFN0WEWn8PsguRErnkmZRM7Id1UQ5uqbHm4ntEo3RtD7trnmkJbIm5oC67Kf09VaLc3amANgXIQ06E6LxpI_8jAzCfiQl8z8spJ35axGmrr8tNLh8BpnKW3462oMZpuz8QlfVyas4o6Y-W61_G5neL_0YXmioBOw";
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
