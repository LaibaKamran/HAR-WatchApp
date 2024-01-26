package com.watchsensorapp;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorDisplayActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private LinearLayout sensorsContainer;
    private Map<Integer, TextView> sensorTextViewMap = new HashMap<>();
    private Map<Integer, List<Float>> sensorDataMap = new HashMap<>();
    private String serverIP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_display);

        sensorsContainer = findViewById(R.id.sensorsContainer);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        serverIP = getIntent().getStringExtra("serverIP");
        ArrayList<Integer> selectedSensorTypes = getIntent().getIntegerArrayListExtra("selectedSensorTypes");

        if (selectedSensorTypes != null) {
            for (int sensorType : selectedSensorTypes) {
                Sensor sensor = sensorManager.getDefaultSensor(sensorType);
                if (sensor != null) {
                    registerSensor(sensorType);
                    createSensorLayout(sensor.getName(), sensorType);
                } else {
                    setNoSensorAvailableText(sensorType);
                }
            }
        } else {
            setNoSelectedSensorsText();
        }
    }

    private void createSensorLayout(String sensorName, int sensorType) {
        LinearLayout sensorLayout = new LinearLayout(this);
        sensorLayout.setOrientation(LinearLayout.VERTICAL);

        TextView sensorNameTextView = createSensorTextView(sensorName);
        sensorLayout.addView(sensorNameTextView);

        TextView sensorDataTextView = createSensorTextView("");
        sensorLayout.addView(sensorDataTextView);

        sensorsContainer.addView(sensorLayout);

        sensorTextViewMap.put(sensorType, sensorDataTextView);
        sensorDataMap.put(sensorType, new ArrayList<>());
    }

    private TextView createSensorTextView(String sensorName) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        textView.setText(sensorName);
        return textView;
    }

    private void registerSensor(int sensorType) {
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            setNoSensorAvailableText(sensorType);
        }
    }

    private void setNoSelectedSensorsText() {
        TextView noSensorsText = findViewById(R.id.textSensorData);
        noSensorsText.setText("No selected sensors");
    }

    private void setNoSensorAvailableText(int sensorType) {
        TextView sensorTextView = sensorTextViewMap.get(sensorType);
        sensorTextView.setText("No sensor available");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        List<Float> sensorValues = sensorDataMap.get(sensorType);

        if (sensorValues.size() < 3) {
            for (int i = 0; i < 3; i++) {
                sensorValues.add(0.0f);
            }
        }

        for (int i = 0; i < Math.min(3, event.values.length); i++) {
            sensorValues.set(i, event.values[i]);
        }

        String sensorData = sensorType + "\n" +
                "X:" + sensorValues.get(0) + "\n" +
                "Y:" + sensorValues.get(1) + "\n" +
                "Z:" + sensorValues.get(2);

        TextView sensorTextView = sensorTextViewMap.get(sensorType);
        sensorTextView.setText(sensorData);

        Intent intent = getIntent();
        String userId = intent.getStringExtra("userId");

        // Send data to the server
        sendDataToServer(sensorData, userId);
    }

    private void sendDataToServer(final String message, final String userId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Construct the URL
                    String urlString = "http://" + serverIP + ":5000/sensor-data";
                    URL url = new URL(urlString);

                    // Open the connection
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    // Construct the JSON body
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("source", "android_app");
                    jsonBody.put("user_id", userId);
                    jsonBody.put("timestamp", System.currentTimeMillis());
                    // Assuming 'message' is the data you want to send
                    jsonBody.put("message", message);

                    // Write the JSON body to the output stream
                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = jsonBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    } catch(IOException e){
                        e.printStackTrace();
                        System.out.println("Exception: " + e.getMessage());

                    }

                    // Get the response from the server
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        // You can handle the response from the server here
                        System.out.println("Server Response: " + response.toString());
                    }

                    // Close the connection
                    connection.disconnect();
                    System.out.println("Message sent successfully");
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    System.out.println("Exception: " + e.getMessage());
                }
            }
        }).start();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        for (int sensorType : sensorTextViewMap.keySet()) {
            Sensor sensor = sensorManager.getDefaultSensor(sensorType);
            if (sensor != null) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
