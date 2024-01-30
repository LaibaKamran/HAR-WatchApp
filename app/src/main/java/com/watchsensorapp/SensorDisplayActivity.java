package com.watchsensorapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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
    private String userId;
    private int serverPort = 12345; // Port number where the server is listening
    private boolean sendingData = false;
    private long startTime = 0;
    private long stopTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_display);

        sensorsContainer = findViewById(R.id.sensorsContainer);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        serverIP = getIntent().getStringExtra("serverIP");
        userId = getIntent().getStringExtra("userId");
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

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSendingData();
            }
        });

        Button stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSendingData();
            }
        });
    }

    private void startSendingData() {
        sendingData = true;
        startTime = System.currentTimeMillis();
    }

    private void stopSendingData() {
        sendingData = false;
        Log.d("SendingData", "sendingData value after change: " + sendingData);
        stopTime = System.currentTimeMillis();
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
        String sensorName = event.sensor.getName(); // Retrieve sensor name
        sensorName=sensorName.replaceAll(" ","_");

        List<Float> sensorValues = sensorDataMap.get(sensorType);

        if (sensorValues.size() < 3) {
            for (int i = 0; i < 3; i++) {
                sensorValues.add(0.0f);
            }
        }

        for (int i = 0; i < Math.min(3, event.values.length); i++) {
            sensorValues.set(i, event.values[i]);
        }

        String sensorData = sensorName + "\n" + // Change to sensorName
                "X:" + sensorValues.get(0) + "\n" +
                "Y:" + sensorValues.get(1) + "\n" +
                "Z:" + sensorValues.get(2);
        String sensorDataToSend = "Sensor Type: "+sensorName + ":" + // Change to sensorName
                sensorValues.get(0) + "," +
                sensorValues.get(1) + "," +
                 sensorValues.get(2)+"\n";

        TextView sensorTextView = sensorTextViewMap.get(sensorType);
        sensorTextView.setText(sensorData);

        // Send data to the server if sendingData is true
        if (sendingData) {
            sendDataToServer(sensorDataToSend, userId, "smartwatch");
        }
    }

    private void sendDataToServer(final String message, final String userId, final String source) {
          final long timestamp = System.currentTimeMillis();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    // Open a socket connection to the server
                    Socket socket = new Socket(serverIP, serverPort);

                    // Create a data output stream to send data to the server
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                    // Construct the message including the timestamp
                    //StringBuilder messageWithTimestamp = new StringBuilder();
                    StringBuilder messageWithTimestamp = new StringBuilder("Source: Smart_Watch").append(", User ID: ").append(userId).append(", Timestamp: ").append(System.currentTimeMillis()).append(",").append(message);

                    // Log the data before sending to the server
                    Log.d("DataToSend", "Data: " + messageWithTimestamp.toString());

                    // Check if sendingData flag is still true before sending the message
                    if (sendingData) {
                        // Write the sensor data along with source, user ID, sensor type, and timestamp to the output stream
                        outputStream.writeUTF(messageWithTimestamp.toString());

                        Log.d("MessageSent", "Message sent successfully");
                    } else {
                        Log.d("MessageSent", "Message sending stopped by user");
                    }
                    // Close the output stream and the socket
                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Exception", "Exception: " + e.getMessage());
                }
                return null;
            }
        }.execute();
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
