package com.tmax.ck;

public class WatchSettings {
    private String k8sApiServer;
    private String bearerToken;
    private String watchUri;

    public WatchSettings() {

    }

    public void setK8sApiServer(String value) {
      k8sApiServer = value;
    }
    public void setBearerToken(String value) {
      bearerToken = value;
    }
    public void setWatchUri(String value) {
      watchUri = value;
    }
    public String getK8sApiServer() {
      return k8sApiServer;
    }
    public String getBearerToken() {
      return bearerToken;
    }
    public String getWatchUri() {
      return watchUri;
    }
    public String getFullUrl() {
      // slash 고려 필요
      return getK8sApiServer()+getWatchUri();
    }
}