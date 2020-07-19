package com.application.winkwink.Utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Protocol:
 * 4 byte: length n of the username
 * 4 byte: image rotation
 * 4 byte: size m of the image
 * n byte: username
 * m byte: image
 */

public class BluetoothHostServer implements Runnable, OnSuccessListener<List<Face>>,
        OnFailureListener {

    private static final int PROTOCOL_USER_LEN = 4;
    private static final int PROTOCOL_IMAGE_ROT = 4;
    private static final int PROTOCOL_IMAGE_LEN = 4;

    private static final String myName = "it.application.winkwink bluetoothServer";
    private static final UUID myId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private WeakReference<Button> goButton;
    private WeakReference<ImageView> imgView;
    private WeakReference<FaceSharer> faceSharer;

    private ExecutorService saverExecutor;

    private File saveLoc;

    byte[] lenUserName;
    byte[] userName;
    byte[] imgRotation;
    byte[] lenImage;
    byte[] image;

    public BluetoothHostServer(File dirFile, ImageView imgV, Button btn, FaceSharer faceS) {

        saveLoc = dirFile;
        goButton = new WeakReference<>(btn);
        imgView = new WeakReference<>(imgV);
        faceSharer = new WeakReference<>(faceS);
        saverExecutor = Executors.newSingleThreadExecutor();

        lenUserName = new byte[PROTOCOL_USER_LEN];
        imgRotation = new byte[PROTOCOL_IMAGE_ROT];
        lenImage = new byte[PROTOCOL_IMAGE_LEN];
    }

    @Override
    public void run() {

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        FaceDetectorOptions faceOpt = new FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();
        FaceDetector faceDet = FaceDetection.getClient(faceOpt);

        if(bta == null)
            return;

        try (BluetoothServerSocket bss = bta.listenUsingRfcommWithServiceRecord(myName,myId)) {

            while(!Thread.interrupted()) {

                int imgRotation;
                byte[] bitmapBuffer;
                String username;

                //Receive the data via bluetooth
                BluetoothSocket bs = bss.accept();
                BluetoothProtocolPayload protObj = handleConnection(bs);
                bs.close();

                if(protObj == null)
                    continue;

                imgRotation = protObj.getImgRot();
                username = protObj.getUsername();
                bitmapBuffer = protObj.getImgData();

                saverExecutor.submit(new ImageSaver(bitmapBuffer, saveLoc));

                //Load the image inside the ViewImage
                BitmapLoader bml = new BitmapLoader(imgView.get(), bitmapBuffer);
                bml.run();

                ImageView view = imgView.get();

                assert view != null;

                //Set by bitmap loader above
                BitmapDrawable drawable = (BitmapDrawable) view.getDrawable();
                Bitmap bmp = drawable.getBitmap();

                InputImage toDetect = InputImage.fromBitmap(bmp, imgRotation);

                //Find the facial features of the image
                faceDet.process(toDetect)
                        .addOnSuccessListener(this)
                        .addOnFailureListener(this);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        saverExecutor.shutdown();
    }

    private BluetoothProtocolPayload handleConnection(BluetoothSocket bSocket) {

        //TODO
        // Show some kind of loading to the user

        BluetoothProtocolPayload result = null;

        byte[] dataArray;

        int readResult;

        int lenUserNameInt;
        int imgRotationInt;
        int lenImageInt;

        int totPayload = 0;
        int curPos;

        try {
            InputStream is = bSocket.getInputStream();

            readResult = is.read(lenUserName);
            if(readResult == -1) return null;
            lenUserNameInt = ByteBuffer.wrap(lenUserName).getInt();

            readResult = is.read(imgRotation);
            if(readResult == -1) return null;
            imgRotationInt = ByteBuffer.wrap(imgRotation).getInt();

            readResult = is.read(lenImage);
            if(readResult == -1) return null;
            lenImageInt = ByteBuffer.wrap(lenImage).getInt();

            dataArray = new byte[lenUserNameInt + lenImageInt];

            readDataFromStream(dataArray, is);

            curPos = 0;
            userName = Arrays.copyOfRange(dataArray, curPos, curPos + lenUserNameInt);

            /* Read until the end just to avoid OutOfBounds due to bluetooth socket closing */
            curPos += lenUserNameInt;
            image = Arrays.copyOfRange(dataArray, curPos, dataArray.length);

            result = new BluetoothProtocolPayload(imgRotationInt,
                    new String(userName, StandardCharsets.UTF_8), image);

        } catch (IOException e) {
            Log.e("Size: ", "" + totPayload);
            e.printStackTrace();
        }

        return result;
    }

    private void readDataFromStream(byte[] targetBuffer, InputStream is) {

        ByteBuffer buffer = ByteBuffer.wrap(targetBuffer);
        byte[] tmpBuffer = new byte[16384];

        int numBytes;
        int totBytes = 0;

        /* The bluetooth socket gets sometimes closed right before finishing, ignore and read
            the data anyway */
        try {

            Log.e("BluetoothServer", "Total: "+buffer.array().length);

            while ((numBytes = is.read(tmpBuffer)) != -1) {

                totBytes += numBytes;
                buffer.put(tmpBuffer, 0, numBytes);
            }

            Log.e("BluetoothServer", "Received: " + totBytes);

        } catch (IOException e) {
            Log.e("BluetoothServer", "Received" + totBytes);
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {

        e.printStackTrace();
    }

    @Override
    public void onSuccess(List<Face> faces) {

        FaceSharer faceSh = faceSharer.get();
        Button btn = goButton.get();
        Face target = null;

        if(faces.size() > 0)
            target = faces.get(0);

        //TODO
        // Notify the user that there is no face to imitate

        if(btn != null)
            //TODO
            // Refactor urgently!
        {
            Face finalTarget = target;
            btn.post(() -> {
                    btn.setVisibility(View.VISIBLE);

                    if(faceSh != null && faces.size() > 0)
                        faceSh.setFace(finalTarget);
            });
        }
    }

    private static class BluetoothProtocolPayload {

        private int imgRot;
        private String username;
        private byte[] imgData;

        BluetoothProtocolPayload(int rot, String name, byte[] data) {

            imgRot = rot;
            username = name;
            imgData = data;
        }

        public int getImgRot() { return imgRot; }
        public String getUsername() { return username; }
        public byte[] getImgData() { return  imgData; }
    }
}
