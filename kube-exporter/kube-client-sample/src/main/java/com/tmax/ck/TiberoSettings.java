package com.tmax.ck;

public class TiberoSettings {
    public String tiberoIp;// = "172.23.4.101";
    public int tiberoPort;// = 31516;
    public String tiberoDatabase;// = "tibero";
    public String tiberoUser;// = "hello";
    public String tiberoPassword;// = "tibero";
    public TiberoSettings(){
      this("172.23.4.101", 31516, "tibero", "hello", "tibero");
    }
    public TiberoSettings(String tiberoIp, int tiberoPort, String tiberoDatabase, String tiberoUser, String tiberoPassword){
      this.tiberoIp = tiberoIp;
      this.tiberoPort = tiberoPort;
      this.tiberoDatabase = tiberoDatabase;
      this.tiberoUser = tiberoUser;
      this.tiberoPassword = tiberoPassword;
    }
}