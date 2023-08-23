package com.example.bluetoothreceiver;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@RequiresApi(api = Build.VERSION_CODES.O)
public class GetBVPWorker extends Worker {

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public GetBVPWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("DeviceSettings", Context.MODE_PRIVATE);
        Long firstTimestamp = getInputData().getLong("firstTimestamp", 0);
        Long lastTimestamp = getLastTimestamp(firstTimestamp);
        Log.e("TEST", firstTimestamp + " " + lastTimestamp);
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        while(isSynced(firstTimestamp) == false) {
            Network network = connectivityManager.getActiveNetwork();
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            AwsS3 awsS3 = new AwsS3(sharedPreferences.getString("accessKey", null),
                    sharedPreferences.getString("secretKey", null),
                    null,
                    sharedPreferences.getString("bucket", null),
                    sharedPreferences.getString("beforeDate", null),
                    sharedPreferences.getString("afterDate", null));
            if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {

                LocalDate localDate = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    localDate = Instant.ofEpochMilli(firstTimestamp).atZone(ZoneId.systemDefault()).toLocalDate();
                }
                ArrayList<String> fileNameList;
                if((fileNameList = awsS3.isDownloadable(localDate.format(dateTimeFormatter), firstTimestamp, lastTimestamp)).size() != 0){
                    awsS3.downloadWithTransferUtility(getApplicationContext(), firstTimestamp, lastTimestamp, localDate.format(dateTimeFormatter), fileNameList);
                } else {
                }
            } else {

            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return Result.success();
    }

    private long getLastTimestamp(long firstTimestamp) {
        File imageDirectory = new File(getApplicationContext().getFilesDir().getPath() + "/record/" + firstTimestamp + "/image");
        String[] imageNameList = imageDirectory.list();
        Long lastTimestamp = null;
        if(imageNameList != null) {
            Arrays.sort(imageNameList);
            String lastFileName = imageNameList[imageNameList.length - 1];
            lastTimestamp = Long.parseLong(lastFileName.substring(0, lastFileName.length() - 4));
        }
        return lastTimestamp;
    }

    private boolean isSynced(Long timestamp){
        File recordDirectory = new File(getApplicationContext().getFilesDir().getPath() + "/record", String.valueOf(timestamp));
        String[] folderArray = recordDirectory.list();
        for(int i = 0; i < folderArray.length; i++) {
        }
        List<String> fileList = new ArrayList<>(Arrays.asList(folderArray));
        return fileList.contains("syncedBvp.csv");
    }
}
