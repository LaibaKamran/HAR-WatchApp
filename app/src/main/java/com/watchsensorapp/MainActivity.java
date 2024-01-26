package com.watchsensorapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout sensorsContainer;
    private String serverIP;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorsContainer = findViewById(R.id.sensorsContainer);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor : sensorList) {
            addSensorCheckbox(sensor.getName(), sensor.getType());
        }

        showInputDialog("Enter User ID", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The 'which' parameter contains the user's input
                userId = String.valueOf(which);

                // Continue with the server IP dialog
                showServerIpDialog();
            }
        });
    }

    private void addSensorCheckbox(String sensorName, int sensorType) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(sensorName);
        checkBox.setTag(sensorType);
        checkBox.setChecked(true);
        sensorsContainer.addView(checkBox);
    }

    private void showInputDialog(String title, DialogInterface.OnClickListener positiveClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                positiveClickListener.onClick(dialog, which);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void showServerIpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Server IP Address");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                serverIP = input.getText().toString();

                // Save the server IP address and user ID in SharedPreferences
                SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
                editor.putString("serverIP", serverIP);
                editor.putString("userId", userId);
                editor.apply();

                // Start SensorDisplayActivity when the server IP is obtained
                startSensorDisplayActivity();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void startSensorDisplayActivity() {
        // Start SensorDisplayActivity with the selected sensors and server IP
        Intent intent = new Intent(this, SensorDisplayActivity.class);
        intent.putIntegerArrayListExtra("selectedSensorTypes", getSelectedSensorTypes());
        intent.putExtra("serverIP", serverIP);
        startActivity(intent);
    }

    private ArrayList<Integer> getSelectedSensorTypes() {
        ArrayList<Integer> selectedSensorTypes = new ArrayList<>();
        for (int i = 0; i < sensorsContainer.getChildCount(); i++) {
            View childView = sensorsContainer.getChildAt(i);

            if (childView instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) childView;

                if (checkBox.isChecked()) {
                    selectedSensorTypes.add((Integer) checkBox.getTag());
                }
            }
        }
        return selectedSensorTypes;
    }

    public void showSelectedSensors(View view) {
        // Send selected sensors to the server
        String url = "http://" + serverIP + ":5000/sensor-data";
        JSONObject jsonBody = new JSONObject();


        try {
            jsonBody.put("source", "android_app");
            jsonBody.put("user_id", userId);
            jsonBody.put("timestamp", System.currentTimeMillis());

            JSONArray sensorTypesArray = new JSONArray(getSelectedSensorTypes());
            jsonBody.put("selected_sensor_types", sensorTypesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonBody, response -> {
                    // Handle the server response
                    try {
                        String message = response.getString("message");
                        // You can display the message to the user or perform any other actions
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {
                    // Handle errors
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        // You can handle different status codes here

                        // Example: Handling a 404 error
                        if (statusCode == 404) {
                            // Handle 404 error
                        }
                    }

                    error.printStackTrace();
                });

        // Add the request to the RequestQueue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }
}
