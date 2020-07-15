package com.application.winkwink.BluetoothUtilities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BluetoothClientTask implements Runnable {

    UUID myId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    BluetoothDevice btd;
    byte[] toSend;

    public BluetoothClientTask(BluetoothDevice bDevice, byte[] data) {

        btd = bDevice;
        toSend = data;
    }

    public BluetoothClientTask(BluetoothDevice bDevice, Bitmap data) {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        data.compress(Bitmap.CompressFormat.PNG, 100, stream);

        btd = bDevice;
        toSend = stream.toByteArray();
    }

    @Override
    public void run() {

        try (BluetoothSocket bs = btd.createRfcommSocketToServiceRecord(myId)) {

            bs.connect();
            handleConnection(bs);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(BluetoothSocket bs) {

        try {
            OutputStream os = bs.getOutputStream();

            os.write(toSend);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
