package com.example.SmartLock3G.tools;

import android.content.Context;
import android.content.SharedPreferences;


public class Pref {

    private static final String DEFAULT_HOST = "39.96.68.13";
    private static final int DEFAULT_PORT = 10086;
    private static final String DEFAULT_IDCODE = "CONN_9527";
    private static Pref instance = null;
    private final SharedPreferences p;

    private Pref(Context context) {
        p = context.getSharedPreferences("data", Context.MODE_PRIVATE);
    }

    public static Pref getInstance(Context context) {
        if (instance == null) {
            instance = new Pref(context);
        }
        return instance;
    }

    public String getHost() {
        return p.getString("host", DEFAULT_HOST);
    }

    public void setHost(String hostStr) {
        p.edit().putString("host", hostStr).apply();
    }

    public int getPort() {
        return p.getInt("IdCode", DEFAULT_PORT);
    }

    public void setPort(int port) {
        p.edit().putInt("port", port).apply();
    }

    public String getIdCode() {
        return p.getString("IdCode", DEFAULT_IDCODE);
    }

    public void setIdCode(String IdCode) {
        p.edit().putString("IdCode", IdCode).apply();
    }
}

