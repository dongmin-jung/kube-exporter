package com.tmax.ck;

import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.sql.Statement;

import com.google.gson.Gson;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.Reader;


public class MySQLConn {

    private String JDBC_DRIVER;
    private String DB_URL;

    private String USERNAME;
    private String PASSWORD;
    private Connection conn;


    private class MysqlConfig {
        String JDBC_DRIVER;
        String DB_URL;
        String USERNAME;
        String PASSWORD;
    }

    public MySQLConn(){
        try {
            // create Gson instance
            Gson gson = new Gson();
            // create a reader
            Reader reader = Files.newBufferedReader(Paths.get("./kube-exporter/kube-client-sample/src/main/java/com/tmax/ck/resources/mysqlSettings.json"));
            // convert JSON string to object
            MysqlConfig mysqlConfig = gson.fromJson(reader, MysqlConfig.class);
            // print object
            System.out.println(mysqlConfig);
            // close reader
            reader.close();

            this.JDBC_DRIVER = mysqlConfig.JDBC_DRIVER;
            this.DB_URL = mysqlConfig.DB_URL;
            this.USERNAME = mysqlConfig.USERNAME;
            this.PASSWORD = mysqlConfig.PASSWORD;
        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
        } catch(ClassNotFoundException e) {
            System.err.println(e);
        } catch(SQLException e) {
            System.err.println(e);
        }
    }
    
    // SELECT는 executeQuery, 
    // INSERT, UPDATE, DELETE는 executeUpdate를 사용해야 한다.

    public void executeUpdate(String query) {
        try {
            Statement stmt = conn.createStatement();
            int result = stmt.executeUpdate(query);
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
