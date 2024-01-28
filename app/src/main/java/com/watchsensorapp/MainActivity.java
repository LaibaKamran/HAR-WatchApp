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

        // Show input dialog to get user ID
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter User ID");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userId = input.getText().toString();
                showServerIpDialog(); // Show dialog to get server IP
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

    // Function to add sensor checkboxes
    private void addSensorCheckbox(String sensorName, int sensorType) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(sensorName);
        checkBox.setTag(sensorType);
        checkBox.setChecked(true);
        sensorsContainer.addView(checkBox);
    }

    // Function to show dialog to input server IP
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
                saveServerIPAndUserID(serverIP, userId); // Save server IP and user ID
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

    // Function to save server IP and user ID using SharedPreferences
    private void saveServerIPAndUserID(String serverIP, String userId) {
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString("serverIP", serverIP);
        editor.putString("userId", userId);
        editor.apply();
    }

    // Function to start SensorDisplayActivity
    private void startSensorDisplayActivity() {
        Intent intent = new Intent(this, SensorDisplayActivity.class);
        intent.putExtra("serverIP", serverIP);
        intent.putExtra("userId", userId);
        intent.putExtra("selectedSensorTypes", getSelectedSensorTypes());
        startActivity(intent);
    }

    // Function to get selected sensor types
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

    // Function to handle button click to show selected sensors
    public void showSelectedSensors(View view) {
        startSensorDisplayActivity();
    }

}
