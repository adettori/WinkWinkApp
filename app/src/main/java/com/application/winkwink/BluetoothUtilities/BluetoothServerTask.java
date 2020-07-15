package com.application.winkwink.BluetoothUtilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothServerTask implements Runnable{

    String myName = "it.application.winkwink bluetoothServer";
    UUID myId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    BluetoothAdapter bta;

    BluetoothSocket bs;

    public BluetoothServerTask() {

        bta = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void run() {

        try (BluetoothServerSocket bss = bta.listenUsingRfcommWithServiceRecord(myName,myId)) {

            while(!Thread.interrupted()) {

                bs = bss.accept();
                handleConnection(bs);
                bs.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(BluetoothSocket bSocket) {

        try {
            InputStream is = bSocket.getInputStream();

            byte[] buffer = new byte[1024];
            int numBytes;

            //TODO
            // Comunication protocol to define
            numBytes = is.read(buffer);


            String s = new String(buffer);
            Log.e("test " + numBytes, s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
