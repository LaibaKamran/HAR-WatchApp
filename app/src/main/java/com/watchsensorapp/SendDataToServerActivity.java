package com.watchsensorapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SendDataToServerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        final String serverIP = intent.getStringExtra("serverIP");
        final String message = intent.getStringExtra("message");

        new Thread(new Runnable() {
            @Override
            public void run() {
                sendDataToServer(serverIP, message);
            }
        }).start();

        // Close this activity after sending data
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 500);
    }

    private void sendDataToServer(String serverIP, String message) {
        try {
            Socket socket = new Socket(serverIP, 12345);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            writer.write(message);
            writer.newLine();
            writer.flush();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
