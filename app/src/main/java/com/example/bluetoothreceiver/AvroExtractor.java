package com.example.bluetoothreceiver;

import android.util.Log;
import android.util.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableFileInput;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AvroExtractor {
    public static void extractDataFromAvroFile(String saveFolder, ArrayList<File> avroFileList, long firstTimestamp, long lastTimestamp) {
        try {
            for(int i = 0 ; i < avroFileList.size(); i++) {
                File avroFile = avroFileList.get(i);
                SeekableFileInput input = new SeekableFileInput(avroFile);

                Schema schema = new DataFileReader<>(input, new GenericDatumReader<>()).getSchema();
                DatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);
                DataFileReader<GenericRecord> dataFileReader = new DataFileReader<GenericRecord>(avroFile, datumReader);

                GenericRecord user = null;
                while (dataFileReader.hasNext()) {
                    user = dataFileReader.next(user);
                    GenericRecord rawData = (GenericRecord) user.get("rawData");


                    // each timestamp is computed with the timestampStart and the samplingFrequency
                    ObjectMapper mapper = new ObjectMapper();
                    //ArrayNode jsonArray = mapper.createArrayNode();
                    ArrayList<ObjectNode> objectNodeList = new ArrayList<>();
                    extractInfo(mapper, objectNodeList, saveFolder, rawData, "bvp", new String[]{"values"}, firstTimestamp, lastTimestamp, Float.class, i != 0);
                    //extractInfo(saveFolder, rawData, "gyroscope", new String[]{"x", "y", "z"}, firstTimestamp, lastTimestamp, Integer.class);
                    extractInfo(mapper, objectNodeList, saveFolder, rawData, "accelerometer", new String[]{"x", "y", "z"}, firstTimestamp, lastTimestamp, Integer.class, false);

                    //for(int i = 0; i < objectNodeList.size(); i++) {
                    //    jsonArray.add(objectNodeList.get(i));
                    //}

                    //File jsonFile = new File(saveFolder, "output.json");
                    //mapper.writeValue(jsonFile, jsonArray);

                    avroFile.delete();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static <T> ArrayList<ObjectNode> extractInfo(ObjectMapper mapper, ArrayList<ObjectNode> objectNodeList, String saveFolder, GenericRecord rawData, String dataName, String[] attribute, long firstTimestamp, long lastTimestamp, Class<T> dataType, boolean append) {

        GenericRecord data = (GenericRecord) rawData.get(dataName);

        Long timestampStart = Long.parseLong(data.get("timestampStart").toString());
        Float samplingFrequency = Float.parseFloat(data.get("samplingFrequency").toString());
        long interval = (long)(1000 / samplingFrequency);

        ArrayList<GenericData.Array<T>> dataList = new ArrayList<>();
        for(int i = 0; i < attribute.length; i++) {
            dataList.add((GenericData.Array<T>) data.get(attribute[i]));
        }
        File file = new File(saveFolder, "raw" + dataName.substring(0, 1).toUpperCase() + dataName.substring(1) + ".csv");

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file, append));
            for (int i = 0; i < dataList.get(0).size(); i++) {
                Long timestamp = (long)(i * 1000 / samplingFrequency) + (timestampStart / 1000);
                String str = timestamp + ",";
                /*if(create) {
                    ObjectNode objectNode = mapper.createObjectNode();
                    objectNode.put("timestamp", timestamp);
                    objectNodeList.add(objectNode);
                }*/

                //ObjectNode objectNode = mapper.createObjectNode();
                for(int j = 0; j < dataList.size(); j++) {
                    //objectNode.put(attribute[j], dataList.get(j).get(i).toString());
                    str += dataList.get(j).get(i);
                    if(j < dataList.size() - 1) str += ",";
                    else str += "\n";
                }
                //objectNodeList.get(i).set(dataName, objectNode);*/
                if (firstTimestamp - interval <= timestamp && timestamp <= lastTimestamp + interval) {
                    bw.write(str);
                } else if(lastTimestamp + interval < timestamp) {
                    break;
                }
            }
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return objectNodeList;
    }
}
