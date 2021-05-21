package com.tmax.ck;

import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.ResultSet; 
// import java.sql.ResultSetMetaData;
import java.sql.SQLException; 
import java.sql.Statement;

import com.google.gson.Gson;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.Reader;


// import com.tmax.tibero.jdbc.*;
// import com.tmax.tibero.jdbc.ext.*;

public class Tibero {
    private String ip;
    private int port;
    private String database;
    private String user;
    private String password;

    private String DRIVER_NAME;
    private String TIBERO_JDBC_URL;
    private Connection conn = null;

    private class WebtobConfig {
        String ip;
        int port;
        String database;
        String user;
        String password;
    }

    // public Tibero(){
    //     this("kubernetes.docker.internal", 30428, "tibero", "hello", "tibero");
    // }

    public Tibero(){
        try {
            // create Gson instance
            Gson gson = new Gson();
            // create a reader
            Reader reader = Files.newBufferedReader(Paths.get("./kube-exporter/kube-client-sample/src/main/java/com/tmax/ck/resources/tiberoSettings.json"));
            // convert JSON string to object
            WebtobConfig webtobConfig = gson.fromJson(reader, WebtobConfig.class);
            // print object
            System.out.println(webtobConfig);
            // close reader
            reader.close();

            this.ip = webtobConfig.ip;
            this.port = webtobConfig.port;
            this.database = webtobConfig.database;
            this.user = webtobConfig.user;
            this.password = webtobConfig.password;
            this.DRIVER_NAME = "com.tmax.tibero.jdbc.TbDriver";
            this.TIBERO_JDBC_URL = "jdbc:tibero:thin:@" + ip + ":" + port + ":" + database;
        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            Class.forName(DRIVER_NAME);
            conn = DriverManager.getConnection(TIBERO_JDBC_URL, user, password);
        } catch(ClassNotFoundException e) {
            System.err.println(e);
        } catch(SQLException e) {
            System.err.println(e);
        }
    }
    
    public void executeQuery(String query) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while(rs.next()) {
                System.out.println(rs.getString(1));
            }
        } catch(SQLException e) {
            System.err.println(e);
        }
    }
    
    public void disconnect() {
        if(conn != null) {
            try {
                conn.close();
            } catch(SQLException e) {
                System.err.println(e);
            }
        }
    }
}