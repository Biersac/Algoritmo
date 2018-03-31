package com.example.robert.algoritmo;

import android.support.v7.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.EditText;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

import android.view.View;
import android.util.Log;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String stringUUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private static UUID myUUID;

    private ThreadConnect threadConnect;
    private BluetoothAdapter bluetoothAdapter;
    private EditText editTextBluetoothAddress;


    public void buttonConnectOnClick(View view) {
        String address = editTextBluetoothAddress.getText().toString();
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        threadConnect = new ThreadConnect(bluetoothDevice);
        threadConnect.start();
    }

    private class ThreadConnect extends Thread {
        private final BluetoothDevice bluetoothDevice;
        private BluetoothSocket bluetoothSocket;

        private ThreadConnect(BluetoothDevice device) {
            this.bluetoothSocket = null;
            this.bluetoothDevice = device;
            try {
                this.bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            boolean success = false;
            try {
                this.bluetoothSocket.connect();
            } catch (IOException e1) {
                Log.e(TAG, "Error e1: " + e1.getMessage());
                try {

                    this.bluetoothSocket = (BluetoothSocket) this.bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{Integer.TYPE}).invoke(this.bluetoothDevice, new Object[]{Integer.valueOf(1)});
                    this.bluetoothSocket.connect();
                    success = true;
                } catch (Exception e2) {
                    Log.e(TAG, "Couldn't establish Bluetooth connection!");
                    try {
                        this.bluetoothSocket.close();
                    } catch (IOException e3) {
                        e1.printStackTrace();
                    }
                }
            }
            if (success) {
                Log.i(TAG, "Connected to: " + this.bluetoothDevice.getName());
                startThreadConnected(this.bluetoothSocket);
            }
        }


        void cancel() {
            Log.i(TAG, "Socket closed!!!");
            try {
                this.bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ThreadConnected extends Thread {
        private final InputStream connectedInputStream;

        ThreadConnected(BluetoothSocket socket) {
            InputStream in = null;
            try {
                in = socket.getInputStream();
            } catch (IOException e) {
                Log.i(TAG, "Error");
            }
            this.connectedInputStream = in;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    final String msgReceived = "Ricevuto: " + new String(buffer, 0, this.connectedInputStream.read(buffer));
                    Log.i(TAG, msgReceived);
                } catch (IOException e) {
                    Log.i(TAG, "Error");
                    break;
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.editTextBluetoothAddress = findViewById(R.id.editTextBluetoothAddress);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (this.bluetoothAdapter != null) {
            myUUID = UUID.fromString(stringUUID);
        }
    }

    protected void onStart() {
        super.onStart();
        if (!this.bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (threadConnect != null) {
            threadConnect.cancel();
        }
    }

    private void startThreadConnected(BluetoothSocket socket) {
        ThreadConnected threadConnected = new ThreadConnected(socket);
        threadConnected.start();
    }
}
