package com.example.bluetoothreceiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiresApi(api = Build.VERSION_CODES.O)
public class RecordListActivity extends AppCompatActivity {
    TextView networkNotConnectedTextView;
    LinearLayout recordingListLinearLayout;
    TextView userSettingTextView;
    TextView recordVideoTextView;
    ActivityResultLauncher<Intent> startActivityResultUserSetting;
    ActivityResultLauncher<Intent> startActivityResultRecording;
    //Map<Long, Integer> notSynced = new HashMap<>();

    Map<Long, TextView> notSynced = new HashMap<>();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    Thread thread;
    File recordDirectory;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);

        findView();
        initView();

        recordDirectory = new File(getFilesDir(), "record");
        addRecordList();
        synchronizeTracker();
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e("TEST", String.valueOf(AwsS3.isConnected()));
            }
        }).start();*/


        startActivityResultUserSetting = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                    }
                }
            }
        );

        startActivityResultRecording = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        addRecordList();
                        synchronizeTracker();
                    }
                }
            }
        );
    }

    private void findView() {
        networkNotConnectedTextView = findViewById(R.id.networkNotConnectedTextView);
        recordingListLinearLayout = findViewById(R.id.recordingListLinearLayout);
        userSettingTextView = findViewById(R.id.userSettingTextViiew);
        recordVideoTextView = findViewById(R.id.recordVideoTextView);
    }

    private void initView() {
        userSettingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), DeviceSettingActivity.class);
                startActivityResultUserSetting.launch(intent);
            }
        });

        recordVideoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), VideoRecordingActivity.class);
                startActivityResultRecording.launch(intent);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void addRecordList() {
        ViewGroup parent = recordingListLinearLayout;
        parent.removeAllViews();
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        File[] folderList = recordDirectory.listFiles();
        Arrays.sort(folderList);

        for(int i = 0; i < folderList.length; i++) {
            String folderName = folderList[i].getName();
            View view = createRecordView(layoutInflater, parent, recordDirectory, folderName);
            parent.addView(view, i);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private View createRecordView(LayoutInflater layoutInflater, ViewGroup parent, File recordDirectory, String folderName) {

        View view = layoutInflater.inflate(R.layout.record_cell, parent, false);

        ImageView imageView = view.findViewById(R.id.imageView);
        TextView recordingDateTextView = view.findViewById(R.id.recordingDateTextView);
        TextView fileNameTextView = view.findViewById(R.id.fileNameTextView);
        TextView isSyncedTextView = view.findViewById(R.id.isSyncedTextView);

        Long timestamp = Long.parseLong(folderName);

        if(isImageExtracted(timestamp)) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(recordDirectory.getPath() + "/" + folderName + "/image/" + folderName + ".jpg"));
        } else {
            Data inputData = new Data.Builder()
                    .putString("timestamp", String.valueOf(timestamp))
                    .build();
            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(ImageExtractWorker.class)
                    .setInputData(inputData)
                    .addTag("extractImage")
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueue(oneTimeWorkRequest);
            waitAndSetImage(timestamp, imageView);
        }

        LocalDate localDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
        recordingDateTextView.setText(localDate.toString());

        fileNameTextView.setText(folderName);

        if(isSynced(timestamp)) {
            isSyncedTextView.setText("O");
        } else {
            isSyncedTextView.setText("X");
            Log.e("TEST", "NOT SYNCED");
            Data inputData = new Data.Builder()
                    .putLong("firstTimestamp", timestamp)
                    .build();
            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(GetBVPWorker.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork("work" + timestamp, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);

            notSynced.put(timestamp, isSyncedTextView);
        }

        return view;
    }

    private boolean isImageExtracted(Long timestamp) {
        File recordDirectory = new File(getFilesDir().getPath() + "/record/" + timestamp, "image");
        return Arrays.asList(recordDirectory.list()).contains(timestamp + ".jpg");
    }

    private void waitAndSetImage(Long timestamp, ImageView imageView) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(isImageExtracted(timestamp) == false) {
                        Thread.sleep(3000);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(BitmapFactory.decodeFile(recordDirectory.getPath() + "/" + timestamp + "/image/" + timestamp + ".jpg"));
                        }
                    });
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }

    private boolean isSynced(Long timestamp){
        File recordDirectory = new File(getFilesDir().getPath() + "/record", String.valueOf(timestamp));
        String[] folderArray = recordDirectory.list();
        for(int i = 0; i < folderArray.length; i++) {
        }
        List<String> fileList = new ArrayList<>(Arrays.asList(folderArray));
        return fileList.contains("syncedBvp.csv");
    }

    private void synchronizeTracker() {
        if(thread == null || thread.isAlive() == false) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                    while(notSynced.size() != 0) {
                        Network network = connectivityManager.getActiveNetwork();
                        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

                        if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                            if(networkNotConnectedTextView.getVisibility() == View.VISIBLE) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        networkNotConnectedTextView.setVisibility(View.GONE);
                                    }
                                });
                            }
                        } else {
                            if(networkNotConnectedTextView.getVisibility() == View.GONE) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        networkNotConnectedTextView.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        }

                        Set<Long> keySet = notSynced.keySet();

                        ArrayList<Long> onSynchronize = new ArrayList<>();
                        for(Long key : keySet) {
                            TextView isSyncedTextView = notSynced.get(key);
                            Long firstTimestamp = key;
                            if(isSynced(firstTimestamp)){
                                isSyncedTextView.setText("O");
                                onSynchronize.add(key);
                            } else {
                            }
                        }

                        for(int i = 0; i < onSynchronize.size(); i++) {
                            notSynced.remove(onSynchronize.get(i));
                        }

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
            thread.start();
        }
    }
}
