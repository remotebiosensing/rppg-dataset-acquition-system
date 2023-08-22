package com.example.bluetoothreceiver;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.telecom.VideoProfile;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VideoRecordingActivity extends AppCompatActivity {
    private AmazonS3 amazonS3;
    private String accessKey = "empatica-us-east-1-prod-data/v2/394/";
    private String secretKey = "AKIAWWZYTIF5VZGAPPEV";
    private Regions clientRegion = Regions.AP_NORTHEAST_2;
    private String bucket = "";
    private String recordDirectory;
    private ImageReader imageReader;
    private TextureView textureView;

    private CameraDevice cameraDevice;
    private Size previewSize;
    private CameraCaptureSession cameraSession;
    private CaptureRequest.Builder captureRequestBuilder;

    private MediaRecorder mediaRecorder;

    private TextView recordTextView;
    private HandlerThread backgroundHandlerThread;
    private Handler backgroundHandler;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices;
    String selectedDeviceAddress;
    BluetoothDevice selectedBluetoothDevice;

    boolean recording = false;

    CameraCharacteristics cameraCharacter;

    ArrayList<Long> frameTimestamps;

    Context context;

    SensorReceiver sensorReceiver;
    static final Map<Integer, Pair<String, String[]>> sensorInfoMap = new HashMap<Integer, Pair<String, String[]>>(){{
        put(Sensor.TYPE_GYROSCOPE, new Pair<>("gyro", new String[]{"x","y","z"}));
        put(Sensor.TYPE_LIGHT, new Pair<>("light", new String[]{"value"}));
    }};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_recording);

        permission();
        initTextureView();
        recordDirectory = getApplicationContext().getFilesDir().getPath() + "/record";
        context = getApplicationContext();
        //selectBluetoothDevice();
        //SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
        //selectedDeviceAddress = connectedWearable.getString("address", null);
        //if(selectedDeviceAddress == null) selectBluetoothDevice();
        //PairingBluetoothListState();
        //Bluetooth bluetoothReceiver = new Bluetooth(getApplicationContext(), selectedDeviceAddress, selectedBluetoothDevice);
        //bluetoothReceiver.scanDevices();

        recordTextView = findViewById(R.id.recordTextView);
        recordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recording == false) {
                    sensorReceiver = new SensorReceiver(getApplicationContext());
                    frameTimestamps = new ArrayList<>();
                    startBackgroundThread();
                    new File(recordDirectory, "temp").mkdir();
                    new File(recordDirectory + "/temp", "image").mkdir();
                    //imageReader = ImageReader.newInstance(640, 360, ImageFormat.JPEG, 60);
                    setupMediaRecorder(1280, 720);
                    startRecording();
                    recording = true;
                    recordTextView.setText("중지");

                } else if (recording == true) {
                    stopRecording();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    private void stopRecording() {
        mediaRecorder.stop();
        sensorReceiver.exitSensorReceive();
        cameraDevice.close();
        stopBackgroundThread();
        recording = false;
        recordTextView.setText("시작");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                File dir = new File(recordDirectory + "/temp", "image");
                File files[] = dir.listFiles();
                Arrays.sort(files);
                String startTimestamp = frameTimestamps.get(1) + "";

                fileRename(recordDirectory, "temp", startTimestamp);
                fileRename(recordDirectory + "/" + startTimestamp, "temp.mp4", startTimestamp + ".mp4");

                writeTimestamp(startTimestamp);
                writeSensor(startTimestamp);

                Data inputData = new Data.Builder()
                        .putString("timestamp", startTimestamp)
                        .build();
                OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(ImageExtractWorker.class)
                        .setInputData(inputData)
                        .addTag("extractImage")
                        .build();
                WorkManager.getInstance(getApplicationContext()).enqueue(oneTimeWorkRequest);
            }
        });
        thread.start();

        openCamera();
    }

    private void writeTimestamp(String startTimestamp) {
        File timestampFile = new File(recordDirectory + "/" + startTimestamp, "imageTimestamp.csv");
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(timestampFile));
            for(int i = 1; i < frameTimestamps.size(); i++) {
                bw.write(frameTimestamps.get(i) + "\n");
            }
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeSensor(String startTimestamp) {
        Map<Integer, ArrayList<Pair<Long, float[]>>> sensorRecord = sensorReceiver.getSensorValue();
        Set<Integer> keySet = sensorRecord.keySet();
        JSONObject rootObject = new JSONObject();
        for(int key : keySet) {
            try {
                String[] sensorValueName = sensorInfoMap.get(key).second;
                JSONArray recordsArray = new JSONArray();
                ArrayList<Pair<Long, float[]>> value = sensorRecord.get(key);
                for (Pair<Long, float[]> record : value) {
                    JSONObject recordObject = new JSONObject();
                    recordObject.put("timestamp", record.first);
                    for (int i = 0; i < record.second.length; i++) {
                        recordObject.put(sensorValueName[i], record.second[i]);
                    }
                    recordsArray.put(recordObject);
                }
                rootObject.put(sensorInfoMap.get(key).first, recordsArray);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void fileRename(String filePath, String oldName, String newName) {
        File oldFile = new File(filePath, oldName);
        File newFile = new File(filePath, newName);
        oldFile.renameTo(newFile);
    }

    private void initTextureView() {
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdArray = manager.getCameraIdList();

            String cameraId = cameraIdArray[1];

            cameraCharacter = manager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = cameraCharacter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizesForStream = map.getOutputSizes(SurfaceTexture.class);

            previewSize = sizesForStream[14];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice openedCameraDevice) {
                    cameraDevice = openedCameraDevice;
                    showCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int errorCode) {
                    Log.e("TEST", "MMM errorCode = " + errorCode);
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e("TEST", "MMM openCamera ", e);
        }
    }
    private void showCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface textureViewSurface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(textureViewSurface);
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            cameraDevice.createCaptureSession(Arrays.asList(textureViewSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    cameraSession = cameraCaptureSession;
                    try { // preview update
                        cameraSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {

                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException e) {

        }
    }

    private void startBackgroundThread() {
        backgroundHandlerThread = new HandlerThread("CameraVideoThread");
        backgroundHandlerThread.start();
        backgroundHandler = new Handler(backgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundHandlerThread.quitSafely();
        try {
            backgroundHandlerThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setupMediaRecorder(int width, int height) {
        mediaRecorder = new MediaRecorder();

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setOrientationHint(270);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setOutputFile(recordDirectory + "/temp/temp.mp4");
        //mediaRecorder.setVideoProfile(VideoProfile.QUALITY_HIGH);
        //mediaRecorder.setVideoProfile(new VideoProfile());
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startRecording() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        Surface recordingSurface = mediaRecorder.getSurface();

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
        captureRequestBuilder.addTarget(previewSurface);
        captureRequestBuilder.addTarget(recordingSurface);
        //captureRequestBuilder.addTarget(imageSurface);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation = getOrientation(rotation, cameraCharacter);
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);

        try {
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordingSurface), captureStateVideoCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private int getOrientation(int rotation,CameraCharacteristics characteristics) {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        final SparseIntArray ORIENTATIONS = new SparseIntArray();
            ORIENTATIONS.append(Surface.ROTATION_0, 0);
            ORIENTATIONS.append(Surface.ROTATION_90, 90);
            ORIENTATIONS.append(Surface.ROTATION_180, 180);
            ORIENTATIONS.append(Surface.ROTATION_270, 270);

        int deviceOrientation = ORIENTATIONS.get(rotation);
        int displayOrientation = (sensorOrientation - deviceOrientation + 360) % 360;

        return displayOrientation;
    }

    CameraCaptureSession.StateCallback captureStateVideoCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {

            try {
                session.setRepeatingRequest(captureRequestBuilder.build(), captureVideoCallback, backgroundHandler);
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
            mediaRecorder.start();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }

        @Override
        public void onActive(@NonNull CameraCaptureSession session) {
            super.onActive(session);
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
        }
    };

    CameraCaptureSession.CaptureCallback captureVideoCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);

                long timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) / 1000000L;
                timestamp = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime() + timestamp;
                frameTimestamps.add(timestamp);

                //Image image = imageReader.acquireLatestImage();
                //image.close();
                /*if(image != null) {
                    Log.e("TEST", "save");

                    //saveImage(image, timestamp);
                    image.close();
                }*/
            }
    };



    private void saveImage(Image image, long timestamp) {
        String fileName = timestamp + ".jpg";
        File imageFile = new File(recordDirectory + "/temp/image", fileName);

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();

            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            fos.write(bytes);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void permission() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(VideoRecordingActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(VideoRecordingActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }


        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS)
                .check();
    }

    /*public boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            boolean connected = (boolean) m.invoke(device, (Object[]) null);
            return connected;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void PairingBluetoothListState() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return ;
            }
            Set<BluetoothDevice> bluetoothDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                if (isConnected(bluetoothDevice) && bluetoothDevice.getAddress().compareTo(selectedDeviceAddress) == 0) {
                    Log.e("TEST", bluetoothDevice.getName() + " connected");
                    selectedBluetoothDevice = bluetoothDevice;
                    //TODO : 연결중인상태
                }else{
                    Log.e("TEST", bluetoothDevice.getName() + " not connected");
                    //TODO : 연결중이 아닌상태
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void selectBluetoothDevice() {

        if (ActivityCompat.checkSelfPermission(VideoRecordingActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        devices = bluetoothAdapter.getBondedDevices();

        int pairedDeviceCount = devices.size();
        if (pairedDeviceCount == 0) {
            Toast.makeText(VideoRecordingActivity.this, "페어링 되어있는 디바이스가 없습니다", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("페어링 블루투스 디바이스 목록");

            List<String> bluetoothDeviceNameList = new ArrayList<>();
            List<String> bluetoothDeviceAddressList = new ArrayList<>();

            for (BluetoothDevice bluetoothDevice : devices) {
                bluetoothDeviceNameList.add(bluetoothDevice.getName());
                bluetoothDeviceAddressList.add(bluetoothDevice.getAddress());
            }
            bluetoothDeviceNameList.add("취소");

            final CharSequence[] charSequences = bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);
            bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);

            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which < bluetoothDeviceNameList.size() - 1) {
                        CharSequence cs = bluetoothDeviceAddressList.get(which);
                        SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = connectedWearable.edit();
                        editor.putString("address", cs.toString());
                        editor.apply();
                    }
                }
            });
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }, 0);
        }
        //}
    }
*/
}
