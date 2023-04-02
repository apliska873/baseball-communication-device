package com.example.baseballsignaler;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.zxing.integration.android.IntentIntegrator;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String connectionName = "baseball";    //todo useful? yes -rach
    private static final int SELECT_DEVICE_REQUEST_CODE = 1;
    private UUID uuid = null;

    private BluetoothServerSocket mmServerSocket;
    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;
    CompanionDeviceManager deviceManager;

    BTConnectionService bluetoothConnection;

    BluetoothDevice myBTDevice;

    Button buzzButton;
    Button sendButton;
    Button pairButton;

    @SuppressLint({"MissingInflatedId", "MissingPermission"}) // delete this eventually
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermission();
        }

        // TODO: Bluetooth stuffs
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported");
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        assert bluetoothAdapter != null;
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
            }
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        /* Create buttons and textViews to be used with following code */
        buzzButton = findViewById(R.id.buzz_button);
        sendButton = findViewById(R.id.send_button);
        pairButton = findViewById(R.id.pair_button);

        /* Create Vibration stuff*/
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        VibrationEffect haptic = VibrationEffect.createOneShot(50,          // short
                VibrationEffect.DEFAULT_AMPLITUDE);
        VibrationEffect buzz = VibrationEffect.createOneShot(500,           // long
                VibrationEffect.DEFAULT_AMPLITUDE);

        /* Buzz Button Functionality */
        buzzButton.setOnClickListener(v -> {
            Log.d(TAG, "Buzz button pressed.");
            vibrator.vibrate(haptic);
            vibrator.vibrate(buzz); // TODO: this buzz should happen on watch side instead

            // TODO: only send this toast if successful buzz sent, other toast "Buzz failed"
            Toast.makeText(this, "Buzz Sent", Toast.LENGTH_SHORT).show();
        });

        /* Send Button functionality */
        sendButton.setOnClickListener(v -> {
            Log.d(TAG, "Send button pressed");
            vibrator.vibrate(haptic);
//            TODO: check if there is a paired device and if not send a toast alerting user
//              - they need to pair a device first with the Pair Button
            // TODO: how to access the textbox??
            //          - byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
            //          - bluetoothConnection.write(bytes)
        });

        /* Pair Button Functionality */
        pairButton.setOnClickListener(v -> {
            Log.d(TAG, "Pair button pressed.");
            vibrator.vibrate(haptic);

            //TODO: is this the right place for this?
            // - want to create a bond w/ myBTdevice and start the connection service
            // - pt.4: 7:30
            // - not sure if this right, just want to doc this so i can trace back
            startConnection();
            myBTDevice.createBond();
            bluetoothConnection = new BTConnectionService(MainActivity.this);

            // bluetooth pairing stuff
            /* Device Manager for pairing stuff */
            // Set device filter (isn't filtering anything)
            BluetoothDeviceFilter deviceFilter = new BluetoothDeviceFilter.Builder()
                    // Match only Bluetooth devices whose name matches the pattern.
                    .setNamePattern(Pattern.compile("Watch"))
                    .build();
            Log.d(TAG, "Device Filter Generated");
            // Associates the filter with the pair request
            AssociationRequest pairingRequest = new AssociationRequest.Builder()
                    .addDeviceFilter(deviceFilter)
                    .setSingleDevice(false) // allows the user to find more than one device
                    .build();
            Log.d(TAG, "Association Request Generated");
            deviceManager =
                    (CompanionDeviceManager) getSystemService(Context.COMPANION_DEVICE_SERVICE);
            deviceManager.associate(pairingRequest, new CompanionDeviceManager.Callback() {
                // Called when a device is found. Launch the IntentSender so the user can
                // select the device they want to pair with.
                @Override
                public void onDeviceFound(IntentSender chooserLauncher) {
                    try {
                        startIntentSenderForResult(
                                chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0
                        );
                        Log.d(TAG, "Pair UI sent");
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "Failed to send intent");
                    }
                }

                @Override
                public void onFailure(CharSequence error) {
                    // Handle the failure.
                }
            }, null);

            // old qr code pairing stuff
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestCameraPermission();
            }

            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan a QR code");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setCaptureActivity(CaptureActivityPortrait.class);
            integrator.initiateScan();*/
        });

    }
    //method for statring connection
    //remember: connection will fail and app will crash if not paired first
    public void startConnection()
    {
        startBTConnection(myBTDevice, uuid);
    }

    // All the bluetooth permission request stuff
    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestBluetoothPermission()
    {
        Log.d(TAG, "Requesting bluetooth permissions");
        if (ActivityCompat.checkSelfPermission(this,
            Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.BLUETOOTH}, REQUEST_BLUETOOTH_PERMISSION);
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG, "startBTConnection: Initializing RFCOM BT Connection");

        //initiates dialogue and starts ConnectThread
        bluetoothConnection.startClient(device, uuid);
    }

    /**
     * getter for bluetoothAdapter
     * @return bluetoothAdapter
     */
    public BluetoothAdapter getBluetoothAdapter(){
        return bluetoothAdapter;
    }

    /**
     * getter for connectionName
     * @return connectionName
     */
    public String getConnectionName()
    {
        return connectionName;
    }

    /* Stuff for paring devices */
    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SELECT_DEVICE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                BluetoothDevice deviceToPair = data.getParcelableExtra(
                        CompanionDeviceManager.EXTRA_DEVICE
                );

                if (deviceToPair != null) {
                    deviceToPair.createBond();
                    // ... Continue interacting with the paired device.
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Camera permission request that currently isn't used
    private void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    // UUID information was used in conjunction with the qr code pairing method which is
    //      currently not in use.
    private boolean isValidUUID(String contents) {
        try {
            UUID.fromString(contents);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    public UUID getUUID() {
        return uuid;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                myBTDevice = device;
            }
        }
    };

    // Closes everything
    @Override
    protected void onDestroy(){
        Log.d(TAG, "Closing");
        super.onDestroy();
        unregisterReceiver(receiver);
    }


}