package com.tmax.ck;

public class Main {
    public static void main(String args[]) {
        try {
            // WebApp webapp = new WebApp(8080);
            KubeExporterServer server = new KubeExporterServer();
            server.init();
            server.start();
            server.addResource("/api/v1/namespaces/default/pods");
            server.addResource("/api/v1/namespaces/kube-system/pods");
            server.addResource("/api/v1/namespaces/default/services");
            server.addResource("/api/v1/namespaces/kube-system/services");
            server.addResource("/apis/apps/v1/namespaces/kube-system/deployments");
            server.addResource("/apis/apiextensions.k8s.io/v1/namespaces/default/customresourcedefinitions");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
