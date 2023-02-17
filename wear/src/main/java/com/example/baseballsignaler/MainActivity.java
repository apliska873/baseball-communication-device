package com.example.baseballsignaler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.baseballsignaler.databinding.ActivityMainBinding;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends Activity {
    private ImageView qrCodeImageView;
    private TextView code;
    private final UUID uuid = UUID.fromString("d76f80f2-ae6b-11ed-afa1-0242ac120002");

    @SuppressLint({"MissingInflatedId", "MissingPermission"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ToggleButton toggleButton = findViewById(R.id.generate_qr_code_button);
        qrCodeImageView = findViewById(R.id.qr_code_image_view);
        code = findViewById(R.id.code_display);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Show the QR code
                    ImageView qrCodeImageView = findViewById(R.id.qr_code_image_view);
                    qrCodeImageView.setVisibility(View.VISIBLE);
                    code.setVisibility(View.GONE);
                    qrCodeImageView.setImageBitmap(generateQrCodeBitmap(String.valueOf(uuid)));
                } else {
                    // Hide the QR code
                    ImageView qrCodeImageView = findViewById(R.id.qr_code_image_view);
                    qrCodeImageView.setVisibility(View.GONE);
                    code.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private Bitmap generateQrCodeBitmap(String data) {
        int size = getResources().getDimensionPixelSize(R.dimen.qr_code_size);
        BitMatrix bitMatrix = null;
        try {
            bitMatrix = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size + 50, size + 50);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        assert bitMatrix != null;
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap qrCodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        qrCodeBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return qrCodeBitmap;
    }
}


