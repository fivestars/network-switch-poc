package com.fivestars.networkswitchpoc;

import java.util.HashMap;

public class Parse {

    public static ParsedNetworkData parseNetworkInfo(String networkData) {

        ParsedNetworkData pNetData = new ParsedNetworkData();

        String[] parsedNetworkData = networkData.split("\n");
        String wlanData = "";

        //if(!networkData.isEmpty()) { parsedNetworkData = networkData.split("\n"); }

        if (pNetData.rxData.size() > 0) pNetData.rxData.clear();

        for (String parsedNetworkDatum : parsedNetworkData) {
            if (parsedNetworkDatum.contains("wlan")) {
                wlanData = parsedNetworkDatum;
            }
        }
        String[] linesData = wlanData.split("\\\\");

        String[] rxKeyData = linesData[2].replaceAll(" +", " ").replace(
                "RX:", "").trim().split(" ");
        String[] rxValueData = linesData[3].trim().replaceAll(" +", " ").split(" ");
        String[] txKeyData = linesData[4].replaceAll(" +", " ").replace(
                "TX:", "").trim().split(" ");
        String[] txValueData = linesData[5].trim().replaceAll(" +", " ").split(" ");

        for (int i = 0; i < rxValueData.length; i++) {
            pNetData.rxData.put(rxKeyData[i], Long.parseLong(rxValueData[i]));
        }

        for (int i = 0; i < txValueData.length; i++) {
            pNetData.txData.put(txKeyData[i], Long.parseLong(txValueData[i]));
        }

        return pNetData;
    }

    public static class ParsedNetworkData {
        String macAddress;
        HashMap<String, Long> rxData = new HashMap<>();
        HashMap<String, Long> txData = new HashMap<>();
    }
}
