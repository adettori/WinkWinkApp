package com.application.winkwink.Utilities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Protocol:
 * 4 byte: length n of the message
 * 1 byte: command id
 * n byte: message
 */


public class BluetoothServerTask implements Runnable{

    Context execContext;

    String myName = "it.application.winkwink bluetoothServer";
    UUID myId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    BluetoothAdapter bta;
    BluetoothSocket bs;

    public BluetoothServerTask(Context context) {

        bta = BluetoothAdapter.getDefaultAdapter();
        execContext = context;
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

            byte[] lenMsg = new byte[4];
            byte[] command = new byte[1];
            byte[] tmpBuffer = new byte[8192];

            int numBytes;
            int dataSize;
            int totBytes = 0;

            numBytes = is.read(lenMsg);
            numBytes = is.read(command);

            dataSize = ByteBuffer.wrap(lenMsg).getInt();
            ByteBuffer buffer = ByteBuffer.allocate(dataSize);

            while((numBytes = is.read(tmpBuffer)) != -1) {

                totBytes += numBytes;
                buffer.put(tmpBuffer, 0, numBytes);
                if(totBytes >= dataSize)
                    break;
            }

            Log.e("test", ""+buffer.array().length);

            File mFile = new File(execContext.getExternalFilesDir(null), "pic.jpg");

            FileOutputStream output = null;

            try {
                output = new FileOutputStream(mFile);
                output.write(buffer.array());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
