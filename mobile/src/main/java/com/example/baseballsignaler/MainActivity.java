package com.example.baseballsignaler;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;

import java.util.UUID;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private TextView resultTextView;
    private UUID uuid = null;

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestBluetoothPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                resultTextView.setText(R.string.qr_scan_cancelled);
            } else if(!isValidUUID(result.getContents())){
                resultTextView.setText(R.string.invalid_qr);
            } else {
                resultTextView.setText(String.format(getString(R.string.scan_result), result.getContents()));
                uuid = UUID.fromString(result.getContents());
                /* a fun little easter egg */
                resultTextView.setOnClickListener(new View.OnClickListener() {
                    private int tapCount = 0;
                    @Override
                    public void onClick(View view) {
                        tapCount++;
                        if(tapCount == 7){
                            Toast.makeText(getApplicationContext(),
                                    "The numbers, Mason! What do they mean?",
                                    Toast.LENGTH_SHORT).show();
                            tapCount = 0;
                        }
                    }
                });
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private boolean isValidUUID(String contents) {
        try {
            UUID.fromString(contents);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @SuppressLint({"MissingInflatedId", "MissingPermission"}) // delete this eventually
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Bluetooth stuffs

        /* Create buttons and textViews to be used with following code */
        Button buzzButton = findViewById(R.id.buzz_button);
        Button sendButton = findViewById(R.id.send_button);
        Button scanButton = findViewById(R.id.scan_button);
        Button pairBTButton = findViewById(R.id.bt_button);
        resultTextView = findViewById(R.id.result_textview);

        /* Create Vibration stuff*/
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        VibrationEffect haptic = VibrationEffect.createOneShot(50,          // short
                VibrationEffect.DEFAULT_AMPLITUDE);
        VibrationEffect buzz = VibrationEffect.createOneShot(500,           // long
                VibrationEffect.DEFAULT_AMPLITUDE);

        /* Buzz Button Functionality */
        buzzButton.setOnClickListener(v -> {
            vibrator.vibrate(haptic);
            vibrator.vibrate(buzz); // TODO: this buzz should happen on watch side instead

            // TODO: only send this toast if successful buzz sent, other toast "Buzz failed"
            Toast.makeText(this, "Buzz Sent", Toast.LENGTH_SHORT).show();
        });

        /* Send Button functionality */
        sendButton.setOnClickListener(v -> {
            vibrator.vibrate(haptic);
//            TODO: check if there is a paired device and if not send a toast alerting user
//              - they need to pair a device first with the Pair Button
        });

        /* Scan Button Functionality */
        scanButton.setOnClickListener(v -> {
            vibrator.vibrate(haptic);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestCameraPermission();
            }

            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan a QR code");
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setCaptureActivity(CaptureActivityPortrait.class);
            integrator.initiateScan();
        });

        /* Pair Bluetooth Button Functionality */
        pairBTButton.setOnClickListener(v -> {
            vibrator.vibrate(haptic);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestBluetoothPermission();
            }
        });
    }
}