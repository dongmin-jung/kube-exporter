package com.tmax.ck;

import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.ResultSet; 
import java.sql.ResultSetMetaData; 
import java.sql.SQLException; 
import java.sql.Statement;

import com.tmax.tibero.jdbc.*;
import com.tmax.tibero.jdbc.ext.*;

public class Tibero {
    private String ip;// = "172.23.4.101";
    private int port;// = 31516;
    private String database;// = "tibero";
    private String user;// = "hello";
    private String password;// = "tibero";

    private String DRIVER_NAME;
    private String TIBERO_JDBC_URL;
    private Connection conn = null;

    public Tibero(){
        this("kubernetes.docker.internal", 30428, "tibero", "hello", "tibero");
        // this("172.23.4.101", 31516, "tibero", "hello", "tibero");
    }

    public Tibero(String ip, int port, String database, String user, String password){
        this.ip = ip;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.DRIVER_NAME = "com.tmax.tibero.jdbc.TbDriver";
        this.TIBERO_JDBC_URL = "jdbc:tibero:thin:@" + ip + ":" + port + ":" + database;
    }

    public void connect() {
        try {
            Class.forName(DRIVER_NAME);
            System.out.println(TIBERO_JDBC_URL);
            System.out.println(user);
            System.out.println(password);

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