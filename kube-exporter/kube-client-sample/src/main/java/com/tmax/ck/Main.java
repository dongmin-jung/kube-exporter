package com.tmax.ck;

public class Main {
    public static void main(String args[]) {
        try {
            WebApp webapp = new WebApp(8080);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
