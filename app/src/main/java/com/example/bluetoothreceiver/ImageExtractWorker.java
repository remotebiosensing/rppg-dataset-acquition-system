package com.example.bluetoothreceiver;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ImageExtractWorker extends Worker {

    String recordDirectory;

    public ImageExtractWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        recordDirectory = getApplicationContext().getFilesDir().getPath() + "/record";
    }

    @NonNull
    @Override
    public Result doWork() {
        String startTimestamp = getInputData().getString("timestamp");

        ArrayList<Long> frameTimestamps = readTimestamp(startTimestamp);

        extractImageFromVideo(startTimestamp, frameTimestamps);

        return Result.success();
    }

    private ArrayList<Long> readTimestamp(String startTimestamp) {
        File timestampFile = new File(recordDirectory + "/" + startTimestamp, "imageTimestamp.csv");
        ArrayList<Long> frameTimestamps = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(timestampFile));
            String line;

            while((line = br.readLine()) != null) {
                Long timestamp = Long.parseLong(line);
                frameTimestamps.add(timestamp);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return frameTimestamps;
    }

    private void extractImageFromVideo(String startTimestamp, ArrayList<Long> frameTimestamps){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(recordDirectory + "/" + startTimestamp + "/" + startTimestamp + ".mp4");
        int frameCount = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT));

        for(int i = 0; i < frameTimestamps.size(); i++) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                if(imageExists(startTimestamp,frameTimestamps.get(frameTimestamps.size() - 1 - i)))
                    continue;
                Bitmap bitmap = mediaMetadataRetriever.getFrameAtIndex(frameCount - 1 - i);
                saveImage(startTimestamp, bitmap, frameTimestamps.get(frameTimestamps.size() - 1 - i));
            }
        }
    }

    private boolean imageExists(String startTimestamp, long timestamp) {
        String fileName = timestamp + ".jpg";
        File imageFile = new File(recordDirectory + "/" + startTimestamp + "/image", fileName);
        if(imageFile.exists()) return true;
        else return false;
    }

    private void saveImage(String startTimestamp, Bitmap bitmap, long timestamp) {
        String fileName = timestamp + ".jpg";
        File imageFile = new File(recordDirectory + "/" + startTimestamp + "/image", fileName);
        if(imageFile.exists()) return ;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            bitmap = Bitmap.createScaledBitmap(bitmap, 360, 640, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}