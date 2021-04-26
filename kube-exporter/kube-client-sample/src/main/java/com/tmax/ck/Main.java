package com.tmax.ck;

public class Main {
    public static void main(String args[]) {
        try {
            KubeExporterServer server = new KubeExporterServer();
            server.init();
            server.start();
            server.addResource("Service", "/api/v1/namespaces/bjhan-test/services");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
