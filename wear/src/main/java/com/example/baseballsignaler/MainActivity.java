package com.example.baseballsignaler;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.widget.TextView;

import com.example.baseballsignaler.databinding.ActivityMainBinding;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends Activity {

    //private TextView numberTextView;
    //private ActivityMainBinding binding;
    private static final UUID UUID = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private BluetoothServerSocket serverSocket;
    private BluetoothSocket socket;
    private InputStream inputStream;

    private void acceptConnection(){
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try{
            serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("BaseballCodeSender", UUID);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private String recieveNumberFromPhone(){
        byte[] buffer = new byte[1024];
        int bytes;
        try{
            bytes = inputStream.read(buffer);
            return new String(buffer, 0, bytes);
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //binding = ActivityMainBinding.inflate(getLayoutInflater());
        //setContentView(binding.getRoot());
        //numberTextView = binding.numTextView; // numTextView is the id of the element

        acceptConnection();
        TextView numberDisplay = findViewById(R.id.numTextView);

        String receivedNumber = recieveNumberFromPhone();
        numberDisplay.setText(receivedNumber);
        //int firstCodeNumber = 11; // this should be a passed in var instead of hard code
        //String formatted = getString(R.string.numToDisplay, firstCodeNumber);
        //numberTextView.setText(getString(R.string.numToDisplay, firstCodeNumber));

    }
}