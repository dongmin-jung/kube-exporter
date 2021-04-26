package com.tmax.ck;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import okhttp3.Call;
import okhttp3.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Example {
    public static void main(String[] args) throws IOException, ApiException{
        ApiClient client = Config.fromToken("https://192.168.6.171:6443", "eyJhbGciOiJSUzI1NiIsImtpZCI6InpsaTd2ME04MWpfZUp0Wm5sY1VjUW52WVRqdXo5c29mWHNPSmx4WFd1a3cifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJiamhhbi10ZXN0Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6InN1cGVyLWFkbWluLXNhLXRva2VuLTd4OHJrIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6InN1cGVyLWFkbWluLXNhIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiOTIyY2Y2OWUtM2U1My00MGNkLWFmZjAtOTMzN2EyZDViMmMyIiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50OmJqaGFuLXRlc3Q6c3VwZXItYWRtaW4tc2EifQ.Dc5wkzoNHUGPjtmma9cHEhp8eJt474gGfv92uSOexSL9N2eJQwitJa7ay3u3MIJxivPKg9ljul2QXM6-OAWcaqk3lPsf4r5c5cA5OMxKLSJ0ZUNYlOqd_fFTycq1snYNktAHAjGMT_-pFO0FW5flJGGkBmPmQKM9LfPVYZTtKNw04Gx_AAGBuR1Ty0_MMNd37tLV4uYXpYB3mq3qKZi_gQ9n5DKqJUrdZDLrTBXJUearVokKFs-TxFz6SkvJ3KlN1ZfRtJ5BwQrgF9zewYqfyKtTeM3MWcC2ASLDf7X3WdrDmQd-pGCuHd1dtv_KfH73j2ObLaCnUBNs8p_7QMGqkA");
        client.setSslCaCert(new FileInputStream(new File("C:/@DEV/ca.crt")));
        client.setReadTimeout(Integer.MAX_VALUE);
        // ApiClient client = Config.fromConfig("C:/@DEV/config.txt");
        Configuration.setDefaultApiClient(client);

        try {
            Call call = GenericCaller.makeCall("/api/v1/namespaces/bjhan-test/services", client, true);
            Response response = call.execute();
            String line = null;
            while(true) {
                System.out.println("loop");
                line = response.body().source().readUtf8Line();
                if (line == null) {
                    break;
                } else {
                    System.out.println(line);
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}