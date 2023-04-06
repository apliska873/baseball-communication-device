package com.example.baseballsignaler;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BTConnectionService
{
    //debugging tag
    private static final String TAG = "BluetoothConnectionService";

    //need UUID (insecure or secure) should be secure
    private UUID myUUID = null;

    private String connectionName = null;
    private final BluetoothAdapter myBTAdapter;

    private AcceptThread myAcceptThread;

    //all being used in ConnectThread
    private ConnectThread myConnectThread;
    private BluetoothDevice myDevice;
    private UUID deviceUUID;
    MainActivity ma = null;
    ProgressDialog myProgressDialog;
    // end ConnectThread variables
    Context myContext;

    //ConnectedThread objects
    private ConnectedThread myConnectedThread;

    public BTConnectionService(Context c, MainActivity m) {
        myContext = c;
        myBTAdapter = ma.getBluetoothAdapter();
        ma = m;
        myUUID = ma.getUUID();
        connectionName = ma.getConnectionName();
        start();
    }

    /**
     * thread runs while listening for incoming connection
     * behaves like a server-side client, runs until a connection is accepted or canceled
     */
    public class AcceptThread extends Thread {
        // local server socket
        private final BluetoothServerSocket myServersocket;

        @SuppressLint("MissingPermission")
        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;

            //listening server socket
            try {
                tmp = myBTAdapter.listenUsingRfcommWithServiceRecord(connectionName, myUUID);
                Log.d(TAG, "AcceptThread: Setting up server using: " +myUUID);
            } catch (IOException e)
            {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }
            myServersocket = tmp;
        }
        //automatically executes when an AcceptThread object is made
        public void run()
        {
            Log.d(TAG, "Run: AcceptThread Running.");

            BluetoothSocket socket = null;

            Log.d(TAG, "Run: server socket start...");

            //blocking call: will only return on a successful connection or exception
            try
            {
                socket = myServersocket.accept();
                Log.d(TAG, "Run: server socket accepted connection");
            }
            catch (IOException e)
            {
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }
            // todo: look more into connected() and its use; COMEBACK
            if(socket != null)
            {
                connected(socket, myDevice);
            }
            Log.i(TAG, "END myAcceptThread");
        }

        public void cancel()
        {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try
            {
                myServersocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed." + e.getMessage());
            }
        }
    }

    /**
     * Runs thread while attempting to make an outgoing connection with a device.
     * Runs straight through; connection either succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private BluetoothSocket mySocket;

        public ConnectThread(BluetoothDevice device, UUID uuid)
        {
            Log.d(TAG, "ConnectThread: started.");
            myDevice = device;
            deviceUUID = uuid;
        }
        @SuppressLint("MissingPermission")
        //automatically executes when a ConnectThread object is made
        public void run()
        {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN myConnectThread.");

            //get a BTsocket for a connection w/a given BT device
            try
            {
                Log.d(TAG, "ConnectThread: Trying to create RFcomm socket using UUID: " + myUUID);
                tmp = myDevice.createRfcommSocketToServiceRecord(deviceUUID);
            }
            catch (IOException e)
            {
                Log.e(TAG, "ConnectThread: Could not create RFcomm socket " + e.getMessage());
                e.printStackTrace();
            }
            mySocket = tmp;

            //always cancel discovery bc it will slow down a connection
            myBTAdapter.cancelDiscovery();

            try {
                //blocking call: will only return on a successful connection and exception
                mySocket.connect();
                Log.d(TAG, "run: ConnectThread connected");
            } catch (IOException e) {
                //close the socket
                try
                {
                    mySocket.close();
                    Log.d(TAG, "run: ConnectThread closed socket");
                } catch (IOException ex)
                {
                    Log.e(TAG, "myConnectThread: run: unable to close connection in socket " + ex.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + myUUID);
            }
            connected(mySocket, myDevice);
        }

        public void cancel()
        {
            try
            {
                Log.d(TAG, "cancel: Closing client socket.");
                mySocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "cancel: close() of mySocket in ConnectThread failed. " + e.getMessage());
            }
        }
    }

    /**
     * start the connection service
     * start AcceptThread to beging session is listening (server) mode.
     * ^^^^called by MainActivity onResume()^^^ todo: AYO whats this
     */
    public synchronized void start()
    {
        Log.d(TAG, "start");

        //cancel any thread attempting to make a connection
        if (myConnectThread != null)
        {
            myConnectThread.cancel();
            myConnectThread = null;
        }

        if (myAcceptThread == null)
        {
            myAcceptThread = new AcceptThread();
            myAcceptThread.start(); //native method for thread objects
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG, "startClient: Started.");

        //initprogress dialog
        myProgressDialog = ProgressDialog.show(myContext, "Connecting BT", "Please wait...", true);
        myConnectThread = new ConnectThread(device, uuid);
        myConnectThread.start();
    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream myInputStream;
        private final OutputStream myOutputStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //dismiss the progressDialog when connection is established
            try
            {
                myProgressDialog.dismiss();
            } catch (NullPointerException e)
            {
                Log.e(TAG, "ConnectedThread: " + e.getMessage());
            }

            try
            {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            myInputStream = tmpIn;
            myOutputStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[1024]; //buffer store for the stream

            int bytes; //bytes returned from read()

            //keep listening to InputStream until an exception occurs
            while (true)
            {
                //read from the InputStream
                try
                {
                    bytes = myInputStream.read(buffer);
                    String incomingMsg = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMsg);
                } catch (IOException e)
                {
                    Log.e(TAG, "run: Error reading input stream. " + e.getMessage());
                    break;
                }
            }
        }

        //call this from MainActivity to shutdown connection
        public void cancel()
        {
            try
            {
                Log.d(TAG, "cancel: Closing ConnectedThread mmSocket.");
                mmSocket.close();
            } catch (IOException e)
            {
                Log.e(TAG, "cancel: close() of mmSocket in ConnectedThread failed. " + e.getMessage());
            }
        }

        public void write(byte[] bytes)
        {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: " + text);
            try
            {
                myOutputStream.write(bytes);
            } catch (IOException e)
            {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }
    }
    //connects BTsocket and device, starts thread
    private void connected(BluetoothSocket socket, BluetoothDevice device)
    {
        Log.d(TAG, "connected: Starting.");

        //start thread to manage connection and perform transmissions
        myConnectedThread = new ConnectedThread(socket);
        myConnectedThread.start();
    }

    private void write(byte[] out)
    {
        //sync ConnectedThread and perform write
        Log.d(TAG, "write: Write called.");
        myConnectedThread.write(out);
    }

}
