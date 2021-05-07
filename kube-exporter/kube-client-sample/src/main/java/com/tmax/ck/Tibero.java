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

    public Tibero(){
        this("172.23.4.101", 31516, "tibero", "hello", "tibero");
    }

    public Tibero(String ip, int port, String database, String user, String password){
        this.ip = ip;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    private final String DRIVER_NAME = "com.tmax.tibero.jdbc.TbDriver";
    private final String TIBERO_JDBC_URL = "jdbc:tibero:thin:@" + ip + ":" + port + ":" + database;
    
    private Connection conn = null;

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
    
    // public static void main(String[] args) {
    //     Tibero tibero = new Tibero();
        
    //     tibero.connect();
    //     tibero.executeQuery();
    //     tibero.disconnect();
    // }
}