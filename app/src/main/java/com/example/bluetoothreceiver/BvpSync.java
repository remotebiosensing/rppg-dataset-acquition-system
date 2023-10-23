package com.example.bluetoothreceiver;

import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class BvpSync {

    public static void synchronize(String filePath) {
        File tempBvpFile = new File(filePath, "tempBvp.csv");
        ArrayList<Long> timestampList = readImageTimestamp(filePath);
        ArrayList<Pair<Long, Float>> bvpList = readBvpTimestamp(filePath);
        File syncedBvpFile = new File(filePath, Config.FILE_SYNCED_BVP + Config.CSV_FOOTER);

        writeSyncedBvp(tempBvpFile, timestampList, bvpList, syncedBvpFile);

    }

    private static float linearInterpolate(long videoTimestamp, Pair<Long, Float> prev, Pair<Long, Float> next) {
        float ratio = ((float)(videoTimestamp - prev.first)) / (next.first - prev.first);
        return prev.second + ratio * (next.second - prev.second);
    }

    private static ArrayList<Long> readImageTimestamp(String filePath) {
        File imageTimestampFile = new File(filePath, "imageTimestamp.csv");
        ArrayList<Long> timestampList = new ArrayList<>();
        BufferedReader br;
        String line;

        try {
            br = new BufferedReader(new FileReader(imageTimestampFile));
            while((line = br.readLine()) != null) {
                long timestamp = Long.parseLong(line);
                timestampList.add(timestamp);
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return timestampList;
    }

    private static ArrayList<Pair<Long, Float>> readBvpTimestamp(String filePath) {
        File rawBvpFile = new File(filePath, "rawBvp.csv");
        ArrayList<Pair<Long, Float>> bvpList = new ArrayList<>();

        BufferedReader br;
        String line;
        try {
            br = new BufferedReader(new FileReader(rawBvpFile));
            while((line = br.readLine()) != null) {
                String[] splitLine = line.split(",");
                long timestamp = Long.parseLong(splitLine[0]);
                float bvp = Float.parseFloat(splitLine[1]);
                bvpList.add(new Pair<>(timestamp, bvp));
            }
            br.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bvpList;
    }

    private static void writeSyncedBvp(File tempBvpFile, ArrayList<Long> timestampList, ArrayList<Pair<Long, Float>> bvpList, File syncedBvpFile) {

        BufferedWriter bw;
        try {
            int j = 1;
            bw = new BufferedWriter(new FileWriter(tempBvpFile));
            for(int i = 0; i < bvpList.size() - 2; i++) {
                if(bvpList.get(i).first <= timestampList.get(j) && timestampList.get(j) <= bvpList.get(i + 1).first) {
                    float predictBvp = linearInterpolate(timestampList.get(j), bvpList.get(i), bvpList.get(i + 1));
                    bw.write(timestampList.get(j) + "," + predictBvp + "\n");
                    j++;
                }
            }
            bw.close();
            tempBvpFile.renameTo(syncedBvpFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
