package com.limelight.binding;

public class ZeroTier {
    static {
        System.loadLibrary("zerotier-jni");
    }

    public static native int startNode(String path);
    public static native int joinNetwork(String networkId);
    public static native int leaveNetwork(String networkId);
    public static native int getNetworkStatus(String networkId);
    public static native String[] getNetworkAddresses(String networkId);
} 