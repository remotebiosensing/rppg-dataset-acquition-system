package com.example.bluetoothreceiver;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.os.HandlerCompat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Bluetooth {
    private static final String TAG = Bluetooth.class.getSimpleName();
    public static final int INTENT_REQUEST_BLUETOOTH_ENABLE = 0x0701;

    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    //private final List<BluetoothGatt> gattList = new ArrayList<>();
    BluetoothGatt gatt = null;
    private final HashMap<String, BluetoothDevice> hashDeviceMap = new HashMap<>();
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    private boolean scanning = false;
    Context context;
    String selectedDeviceAddress;

    Boolean connected = false;
    BluetoothLeScanner scanner;

    ArrayList<BluetoothGattCharacteristic> readCharacteristic = new ArrayList<>();
    BluetoothDevice selectedBluetoothDevice;


    private ArrayList<BluetoothGattCharacteristic> characteristicList = new ArrayList<>();
    private int currentCharacteristicIndex = 0;

    Bluetooth(Context context, String selectedDeviceAddress, BluetoothDevice selectedBluetoothDevice) {
        this.context = context;
        this.selectedDeviceAddress = selectedDeviceAddress;
        this.selectedBluetoothDevice = selectedBluetoothDevice;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        connGATT(selectedBluetoothDevice);
        //this.selectedBluetoothDevice.connectGatt(context, true, gattCallback);
        while(true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //Log.e("TEST", String.valueOf(readCharacteristic.size()));
            for(int i = 0; i < readCharacteristic.size(); i++) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                gatt.readCharacteristic(readCharacteristic.get(i));
            }
            readCharacteristic.clear();
            gatt.discoverServices();
        }
        //Log.e("TEST", selectedDeviceAddress);
        //this.date = date;
    }

    /**
     * System Bluetooth On Check
     */
    public boolean isOn() {
        return adapter.isEnabled();
    }

    /**
     * System Bluetooth On
     */
    public void on(AppCompatActivity activity) {
        if (!adapter.isEnabled()) {
        }
    }

    /**
     * System Bluetooth On Result
     */
    public boolean onActivityResult(int requestCode, int resultCode) {
        return requestCode == Bluetooth.INTENT_REQUEST_BLUETOOTH_ENABLE
                && Activity.RESULT_OK == resultCode;
    }


    /**
     * System Bluetooth Off
     */
    public void off() {
        if (adapter.isEnabled())
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        adapter.disable();
    }

    /**
     * Check model for ScanRecodeData
     */
    private final ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (connected == false && result.getDevice().getAddress().compareTo(selectedDeviceAddress) == 0) {
                //Log.e("TEST", "find");
                if (connGATT(result.getDevice()) == true) {
                    Log.e(TAG, "connected");
                    connected = true;
                } else {

                }
            }
        }
    };

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void scanDevices() {
        scanning = true;
        if (!adapter.isEnabled()) return;
        if (!scanning) return;
        scanner = adapter.getBluetoothLeScanner();
        mainThreadHandler.postDelayed(() -> {
            scanning = false;
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            scanner.stopScan(callback);
        }, 2 * 60 * 1000);

        scanning = true;
        scanner.startScan(callback);
    }

    public boolean connGATT(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        gatt = device.connectGatt(context, false, gattCallback);
        //Log.e("TEST", gatt.toString());
        if (gatt != null) return true;
        else return false;
    }

    public void disconnectGATT() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        gatt.disconnect();
        gatt.close();
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                gatt.disconnect();
                gatt.close();
                hashDeviceMap.remove(gatt.getDevice().getAddress());
                return;
            }
            if (status == 133) // Unknown Error
            {
                gatt.disconnect();
                gatt.close();
                hashDeviceMap.remove(gatt.getDevice().getAddress());
                return;
            }
            if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "connected");
                gatt.discoverServices();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            readCharacteristic.add(characteristic);
                            //gatt.readCharacteristic(characteristic);
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] dataArray = characteristic.getValue();
                String data = null;
                try {
                    data = new String(dataArray, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                Log.e(TAG, "data : " + data);
            }
/*
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if (characteristic.getProperties() == BluetoothGattCharacteristic.PROPERTY_READ) {
                byte[] dataArray = characteristic.getValue();
                String data = dataArray.toString();
                Log.e("TEST", data);
            }
        }*/
        }

        ;

        public boolean hasProperty(BluetoothGattCharacteristic characteristic, int property) {
            int prop = characteristic.getProperties() & property;
            return prop == property;
        }
    };
}