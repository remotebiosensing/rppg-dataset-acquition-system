package com.example.bluetoothreceiver;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SensorReceiver implements SensorEventListener {
    private SensorManager sensorManager;

    private TriggerEventListener triggerEventListener;

    ArrayList<Sensor> sensorList = new ArrayList<>();
    Map<Integer, ArrayList<Pair<Long, float[]>>> sensorRecord = new HashMap<>();
    //ArrayList<Pair<Long, Float[]>> sensor = new ArrayList<>();

    SensorReceiver(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        addSensor(Sensor.TYPE_GYROSCOPE);
        addSensor(Sensor.TYPE_LIGHT);

        registerSensors();
    }

    private void addSensor(int TYPE_SENSOR) {
        Sensor sensor = sensorManager.getDefaultSensor(TYPE_SENSOR);
        sensorList.add(sensor);
        sensorRecord.put(TYPE_SENSOR, new ArrayList<>());
    }

    private void registerSensors(){
        for(int i = 0; i < sensorList.size(); i++) {
            sensorManager.registerListener(this, sensorList.get(i), SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long timestamp = event.timestamp / 1000000;
        int sensorType = event.sensor.getType();
        if(sensorRecord.containsKey(sensorType)) {
            sensorRecord.get(sensorType).add(new Pair<>(timestamp, event.values));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void exitSensorReceive(){
        for(int i = 0; i < sensorList.size(); i++ ){
            sensorManager.unregisterListener(this, sensorList.get(i));
        }
    }

    public Map<Integer, ArrayList<Pair<Long, float[]>>> getSensorValue(){
        return sensorRecord;
    }
}
