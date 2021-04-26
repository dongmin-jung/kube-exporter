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

public class KubeExporterServer {
    private ThreadPoolExecutor executor;
    private LinkedBlockingDeque<DataObject> historyInsertQueue;
    private LinkedBlockingQueue<Runnable> workQueue;
    private Thread insertThread;
    private Map<String, String> exporterTargetMap;
    public enum State {INIT, RUNNING, STOPPED};
    private State state = State.INIT;
    private ApiClient client;

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
                            System.out.println("insert DataObject");
                            System.out.println(object);
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

        /** read from SA secret */
        client = Config.fromToken("https://192.168.6.171:6443", "eyJhbGciOiJSUzI1NiIsImtpZCI6InpsaTd2ME04MWpfZUp0Wm5sY1VjUW52WVRqdXo5c29mWHNPSmx4WFd1a3cifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJiamhhbi10ZXN0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6InN1cGVyLWFkbWluLXNhLXRva2VuLTd4OHJrIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6InN1cGVyLWFkbWluLXNhIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiOTIyY2Y2OWUtM2U1My00MGNkLWFmZjAtOTMzN2EyZDViMmMyIiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50OmJqaGFuLXRlc3Q6c3VwZXItYWRtaW4tc2EifQ.Dc5wkzoNHUGPjtmma9cHEhp8eJt474gGfv92uSOexSL9N2eJQwitJa7ay3u3MIJxivPKg9ljul2QXM6-OAWcaqk3lPsf4r5c5cA5OMxKLSJ0ZUNYlOqd_fFTycq1snYNktAHAjGMT_-pFO0FW5flJGGkBmPmQKM9LfPVYZTtKNw04Gx_AAGBuR1Ty0_MMNd37tLV4uYXpYB3mq3qKZi_gQ9n5DKqJUrdZDLrTBXJUearVokKFs-TxFz6SkvJ3KlN1ZfRtJ5BwQrgF9zewYqfyKtTeM3MWcC2ASLDf7X3WdrDmQd-pGCuHd1dtv_KfH73j2ObLaCnUBNs8p_7QMGqkA");
        client.setSslCaCert(new FileInputStream(new File("C:/@DEV/ca.crt")));
        client.setReadTimeout(0);
        Configuration.setDefaultApiClient(client);
    }

    public void start() {

    }
    
    public void addResource(String name, String url) {
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
