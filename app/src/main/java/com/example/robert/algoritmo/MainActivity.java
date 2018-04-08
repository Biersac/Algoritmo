package com.example.robert.algoritmo;

import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Button;
import android.widget.EditText;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

import android.view.View;
import android.util.Log;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    private static final String stringUUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final double ANGLE_PER_UNIT = 5.628;
    private static final double ANGLE_MAX = 70.35;
    private static final double ANGLE_MIN = -70.35;
    private static final int NUMBER_OF_UNITS = 25;
    private static final double ANGLE_OFFSET = - 70.35;

    private static UUID myUUID;

    private ThreadConnect threadConnect;
    private BluetoothAdapter bluetoothAdapter;

    private EditText editTextBluetoothAddress;
    private ImageView imageViewPointerROLL;
    private ImageView imageViewPointerPITCH;
    private ImageView imageViewIndicator;
    private ImageView imageViewIndicatorPointer;
    private Button buttonSwitchActivity;

    Pattern pattern_string;
    Pattern pattern_values;




    public class ThreadConnect extends Thread {
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
            ArrayList<String> arrayListString = new ArrayList<>();
            while (true) {
                try {
                    final String msgReceived = new String(buffer, 0, this.connectedInputStream.read(buffer));
                    Matcher matcher = pattern_string.matcher(msgReceived);

                    Log.i(TAG, "Messaggio ricevuto: " + msgReceived);
                    while (matcher.find()) {
                        String group = matcher.group();
                        Log.i(TAG, "Messaggio dopo il regex: " + group);

                        String[] values = group.split(";");
                        final float ROLL = Float.parseFloat(values[10]);
                        final float PITCH = Float.parseFloat(values[11]);
                        final int AOA = Integer.parseInt(values[17]);
                        Log.i(TAG, "ROLL: " + ROLL + ", PITCH: " + PITCH + ", Angle of Attack: " + AOA);


                        float RESULT = (float) ((AOA + 5) * ANGLE_PER_UNIT + ANGLE_OFFSET);
                        Log.i(TAG, "Result: " + RESULT);
                        imageViewIndicatorPointer.setRotation(-RESULT);
                            // -RESULT perché è in senso orario.
                            // pitch e roll hanno valori in gradi, che vanno da -90 a +90.
                            // gruppi 10 e 11 per ROLL e PITCH
                        imageViewPointerROLL.setRotation(ROLL);
                        imageViewPointerPITCH.setRotation(PITCH);
                    }

                } catch (IOException e) {
                    Log.i(TAG, "Error");
                    break;
                }
            }
        }
    }


    private void startThreadConnected(BluetoothSocket socket) {
        ThreadConnected threadConnected = new ThreadConnected(socket);
        threadConnected.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.editTextBluetoothAddress = findViewById(R.id.editTextBluetoothAddress);
        this.imageViewPointerPITCH = findViewById(R.id.imageViewPointerPITCH);
        this.imageViewPointerROLL = findViewById(R.id.imageViewPointerROLL);
        this.imageViewIndicator = findViewById(R.id.imageViewIndicator);
        this.imageViewIndicatorPointer = findViewById(R.id.imageViewIndicatorPointer);
        this.buttonSwitchActivity = findViewById(R.id.buttonSwitchActivity);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (this.bluetoothAdapter != null) {
            myUUID = UUID.fromString(stringUUID);
        }
    }

    protected void onStart() {
        this.pattern_string = Pattern.compile("FDL[^FDL\\s]*");
        this.pattern_values = Pattern.compile("\\w+;([0-9\\-.];?)+");

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

    public void buttonConnectOnClick(View view) {
        String address = editTextBluetoothAddress.getText().toString();
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        threadConnect = new ThreadConnect(bluetoothDevice);
        threadConnect.start();
    }

    public void buttonSwitchActivityOnClick(View view) {
        startActivity(new Intent(getApplicationContext(), SecondActivity.class));
    }
}
