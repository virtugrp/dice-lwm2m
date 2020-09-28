package org.eclipse.leshan.server.demo.utils;

public class AppConfigs {

    private static String lwm2mBaseUrl;
    private static String lwm2mAllowedPaths;
    private static String[] lwm2mAllowedPathsArray;
    private static String diceBaseUrl;
    private static String diceTestDeviceToken;

    public AppConfigs(String lwm2mBaseUrl, String lwm2mAllowedPaths,String diceBaseUrl, String diceTestDeviceToken) {
        this.lwm2mBaseUrl = lwm2mBaseUrl;
        this.lwm2mAllowedPaths = lwm2mAllowedPaths;
        this.lwm2mAllowedPathsArray = lwm2mAllowedPaths.split(",");
        this.diceBaseUrl = diceBaseUrl;
        this.diceTestDeviceToken = diceTestDeviceToken;
    }

    public static String getLwm2mBaseUrl() {
        return lwm2mBaseUrl;
    }

    public static String getLwm2mAllowedPaths() {
        return lwm2mAllowedPaths;
    }

    public static String[] getLwm2mAllowedPathsArray() {
        return lwm2mAllowedPathsArray;
    }

    public static String getDiceBaseUrl() {
        return diceBaseUrl;
    }

    public static String getDiceTestDeviceToken() {
        return diceTestDeviceToken;
    }
}
