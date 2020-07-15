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

    String myName = "application.winkwink bluetoothServer";
    UUID myId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    BluetoothAdapter bta;

    BluetoothServerSocket bss;
    BluetoothSocket bs;

    public BluetoothServerTask() {

        bta = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void run() {

        try {
            bss = bta.listenUsingRfcommWithServiceRecord(myName,myId);

            while(!Thread.interrupted()) {

                bs = bss.accept();
                handleConnection(bs);
                bs.close();
            }

            bss.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(BluetoothSocket bSocket) {

        try {
            InputStream is = bSocket.getInputStream();

            byte[] content = new byte[100];
            int numBytes;

            numBytes = is.read(content);

            String s = new String(content);
            Log.e("test " + numBytes, s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
