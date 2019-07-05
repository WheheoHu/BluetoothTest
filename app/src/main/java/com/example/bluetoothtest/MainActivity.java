package com.example.bluetoothtest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE = 1;
    private static final String BLUETOOTH_NAME = "Pocket Cinema Camera 4K A:53A";
    private static final String TAG = "Main AC";


    private Button button;
    private BluetoothAdapter mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice targrtDevice;
    private BluetoothSocket mbluetoothSocket;
    IntentFilter filter = new IntentFilter();
    private TextView textView;

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                String NAME = "PCC4k";
                tmp = mbluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, UUID.fromString("2A29"));
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    // manageConnectedSocket(socket);

                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevices;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevices = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("291D567A-6D75-11E6-8B77-86F30CA893D3"));
            } catch (IOException e) {

            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            mbluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {

                }
                return;
            }


            //TODO: manageConnectedSocket...
            ConnectedThread connectedThread=new ConnectedThread(mmSocket);
            connectedThread.start();


        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private Handler mHandler;

        public ConnectedThread(BluetoothSocket Socket) {
            mmSocket = Socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mHandler = new Handler();
            try {
                tmpIn = Socket.getInputStream();
                tmpOut = Socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    int MESSAGE_READ = 1;
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    private BroadcastReceiver mblueReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.d(TAG, "onReceive: mBlueReceiver action =" + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null) {


                    Log.d(TAG, "onReceive: name= " + device.getName() + " address= " + device.getAddress());
                    textView.append("\n" + device.getName());
                }
                String bluedec_name = device.getName();
                if (bluedec_name != null && bluedec_name.equals(BLUETOOTH_NAME)) {
                    mbluetoothAdapter.cancelDiscovery();
                    targrtDevice = device;
                    Toast.makeText(context, "Find Camera!", Toast.LENGTH_SHORT).show();
                }
            }

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mblueReceiver);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        // mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mbluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!mbluetoothAdapter.isEnabled()) {

            Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabler, REQUEST_ENABLE);

        }
        String bluetoothac_name = mbluetoothAdapter.getName();

        Log.d(TAG, "onCreate: BLname:" + bluetoothac_name + " BLAddress " + mbluetoothAdapter.getAddress());
        Set<BluetoothDevice> devices = mbluetoothAdapter.getBondedDevices();
        Log.d(TAG, "onCreate: bonded device size =" + devices.size());
        for (BluetoothDevice bluetoothDevice : devices) {
            Log.d(TAG, "onCreate: bonded device name = " + bluetoothDevice.getName() + " address = " + bluetoothDevice.getAddress());
            textView.append("\n bonded device name =" + bluetoothDevice.getName() + " address = " + bluetoothDevice.getAddress());
        }
//        if (mbluetoothAdapter.startDiscovery()) {
//            Log.d(TAG, "onCreate: discoverying");
//        }
        mbluetoothAdapter.startDiscovery();
        // mbluetoothAdapter.cancelDiscovery();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mblueReceiver, filter);

        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "aaaa", Toast.LENGTH_SHORT).show();
                ConnectThread connectThread = new ConnectThread(targrtDevice);
                connectThread.start();
                connectThread.cancel();
            }
        });
    }
}
