package com.application.winkwink.Utilities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Protocol:
 * 4 byte: length n of the message
 * 1 byte: command id
 * n byte: message
 */

public class BluetoothClientTask implements Runnable {

    private static final int PROTOCOL_LEN = 4;
    private static final int PROTOCOL_COMMAND = 1;

    UUID myId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    BluetoothDevice btd;
    byte[] toSend = null;
    Uri dataUri = null;

    public BluetoothClientTask(BluetoothDevice bDevice, byte[] data) {

        btd = bDevice;
        toSend = data;
    }

    public BluetoothClientTask(BluetoothDevice bDevice, Uri dataLoc) {

        btd = bDevice;
        dataUri = dataLoc;
    }

    @Override
    public void run() {

        if(toSend == null && dataUri != null) {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap data = retrieveBitmap(dataUri);

            data.compress(Bitmap.CompressFormat.PNG, 100, stream);

            toSend = stream.toByteArray();
        }

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
            byte[] msg_len;

            ByteBuffer b = ByteBuffer.allocate(toSend.length + PROTOCOL_LEN + PROTOCOL_COMMAND);
            b.putInt(toSend.length);
            b.put((byte)0);
            b.put(toSend);

            os.write(b.array());
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap retrieveBitmap(Uri loc) {

        Bitmap result = null;

        try {
            FileInputStream fileStream = new FileInputStream(loc.getPath());

             result = BitmapFactory.decodeStream(fileStream);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return result;
    }
}

