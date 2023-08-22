package com.example.bluetoothreceiver;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File file;
        if((file = new File(getApplicationContext().getFilesDir().getPath(), "record")).exists() == false) {
            file.mkdir();
        }

        Intent intent = new Intent(this, RecordListActivity.class);
        startActivity(intent);

        finish();
    }
}
