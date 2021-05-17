package com.tmax.ck;

public class Main {
    public static void main(String args[]) {
        try {
            KubeExporterServer server = new KubeExporterServer();
			server.init();
			server.start();
            WebApp webapp = new WebApp(8080, server);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
