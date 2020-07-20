package com.application.winkwink.Utilities;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Protocol:
 * 4 byte: length n of the username
 * 4 byte: image rotation
 * 4 byte: size m of the image
 * n byte: username
 * m byte: image
 */

public class BluetoothGuestClient implements Runnable {

    private static final int PROTOCOL_USER_LEN = 4;
    private static final int PROTOCOL_IMAGE_ROT = 4;
    private static final int PROTOCOL_IMAGE_LEN = 4;

    private BluetoothDevice btd;

    private File dataFile = null;
    private Bitmap imgToSend = null;

    byte[] lenUserName;
    byte[] imgRotation;
    byte[] lenImage;
    byte[] userName;
    byte[] toSend = null;

    public BluetoothGuestClient(BluetoothDevice bDevice, String name, int rotation, byte[] data) {

        this(bDevice, name, rotation);
        toSend = data;
    }

    public BluetoothGuestClient(BluetoothDevice bDevice, String name, int rotation, Bitmap img) {

        this(bDevice, name, rotation);
        imgToSend = img;
    }

    public BluetoothGuestClient(BluetoothDevice bDevice, String name, int rotation, File savedFile) {

        this(bDevice, name, rotation);
        dataFile = savedFile;
    }

    BluetoothGuestClient(BluetoothDevice bDevice, String name, int rotation) {

        //TODO
        // Remove risk of overflow
        btd = bDevice;
        userName = name.getBytes(StandardCharsets.UTF_8);
        //TODO
        // This conversion method isn't exactly architecture independent, to fix
        lenUserName = ByteBuffer.allocate(PROTOCOL_USER_LEN).putInt(userName.length).array();
        imgRotation = ByteBuffer.allocate(PROTOCOL_IMAGE_ROT).putInt(rotation).array();
    }

    @Override
    public void run() {

        if(toSend == null && dataFile != null) {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Bitmap data = retrieveBitmap(dataFile);

            assert data != null;

            data.compress(Bitmap.CompressFormat.PNG, 100, stream);

            toSend = stream.toByteArray();
        } else if(toSend == null && imgToSend != null) {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            imgToSend.compress(Bitmap.CompressFormat.PNG, 100, stream);

            toSend = stream.toByteArray();
        }

        lenImage = ByteBuffer.allocate(PROTOCOL_IMAGE_LEN).putInt(toSend.length).array();

        try (BluetoothSocket bs = btd.createRfcommSocketToServiceRecord(BluetoothHostServer.myId)) {

            bs.connect();
            handleConnection(bs);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(BluetoothSocket bs) throws IOException {

        OutputStream os = bs.getOutputStream();

        int payloadSize = PROTOCOL_USER_LEN + userName.length +
                PROTOCOL_IMAGE_ROT + PROTOCOL_IMAGE_LEN + toSend.length;

        ByteBuffer b = ByteBuffer.allocate(payloadSize);

        b.put(lenUserName);
        b.put(imgRotation);
        b.put(lenImage);
        b.put(userName);
        b.put(toSend);

        if(!Thread.interrupted()) {

            os.write(b.array());
            os.flush();
        }
    }

    private Bitmap retrieveBitmap(File loc) {

        Bitmap result = null;

        try {
            FileInputStream fileStream = new FileInputStream(loc);

             result = BitmapFactory.decodeStream(fileStream);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return result;
    }
}

