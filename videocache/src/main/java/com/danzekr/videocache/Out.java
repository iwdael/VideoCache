package com.danzekr.videocache;

public class Out {
    public static final String TAG = "dzq-proxy";

    public static void v(String content) {
        System.out.println(TAG + "/  " + content);
    }

    public static void e(String content) {
        System.err.println(TAG + "/  " + content);
    }
}
