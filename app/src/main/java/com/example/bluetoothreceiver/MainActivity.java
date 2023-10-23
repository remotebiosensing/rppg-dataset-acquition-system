package com.example.bluetoothreceiver;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File file;
        if(!(file = new File(getApplicationContext().getFilesDir().getPath(), Config.CHILD_RECORD_DIR)).exists()) {
            if(file.mkdir()){
                Log.d(TAG, "Child File Create Success !!");
            }else{
                Log.d(TAG, "Child File is Existed !!");
            }
        }

        Intent intent = new Intent(this, RecordListActivity.class);
        startActivity(intent);

        finish();
    }
}
