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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

/**
 * Protocol:
 * 2 byte: length n of the username
 * n byte: username
 * 4 byte: image rotation
 * 4 byte: size m of the image
 * m byte: image
 */

public class BluetoothHostServer implements Runnable, OnSuccessListener<List<Face>>,
        OnFailureListener {

    private BitmapLoader bml;

    private WeakReference<Button> goButton;
    private WeakReference<ImageView> imgView;
    private WeakReference<FaceSharer> faceSharer;

    private File saveLoc;

    private static final String myName = "it.application.winkwink bluetoothServer";
    private static final UUID myId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    public BluetoothHostServer(File dirFile, ImageView imgV, Button btn, FaceSharer faceS) {

        saveLoc = dirFile;
        bml = new BitmapLoader(imgV, dirFile);
        goButton = new WeakReference<>(btn);
        imgView = new WeakReference<>(imgV);
        faceSharer = new WeakReference<>(faceS);
    }

    @Override
    public void run() {

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        if(bta == null)
            return;

        try (BluetoothServerSocket bss = bta.listenUsingRfcommWithServiceRecord(myName,myId)) {

            while(!Thread.interrupted()) {

                BluetoothSocket bs = bss.accept();
                handleConnection(bs);
                bs.close();

                bml.run();

                FaceDetectorOptions faceOpt = new FaceDetectorOptions.Builder()
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();

                FaceDetector faceDet = FaceDetection.getClient(faceOpt);

                ImageView view = imgView.get();

                assert view != null;

                //Set by bitmap loader above
                BitmapDrawable drawable = (BitmapDrawable) view.getDrawable();
                Bitmap bmp = drawable.getBitmap();

                //TODO
                // Rotation
                InputImage toDetect = InputImage.fromBitmap(bmp, 0);

                faceDet.process(toDetect)
                        .addOnSuccessListener(this)
                        .addOnFailureListener(this);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleConnection(BluetoothSocket bSocket) {

        byte[] lenMsg = new byte[4];
        byte[] command = new byte[1];
        byte[] tmpBuffer = new byte[16384];

        int numBytes;
        int dataSize;
        int totBytes = 0;

        try {
            InputStream is = bSocket.getInputStream();

            numBytes = is.read(lenMsg);
            numBytes = is.read(command);

            dataSize = ByteBuffer.wrap(lenMsg).getInt();
            ByteBuffer buffer = ByteBuffer.allocate(dataSize);

            //TODO
            // Show some kind of loading to the user

            Log.e("BluetoothServer", "Total: "+buffer.array().length);

            /* The bluetooth socket gets sometimes closed right before finishing, ignore */
            try {
                while ((numBytes = is.read(tmpBuffer)) != -1) {

                    totBytes += numBytes;
                    buffer.put(tmpBuffer, 0, numBytes);

                    if (totBytes >= dataSize)
                        break;
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            Log.e("BluetoothServer", "Received: " + totBytes);

            FileOutputStream output = null;

            try {

                output = new FileOutputStream(saveLoc);
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
            Log.e("test", ""+totBytes);
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {

    }

    @Override
    public void onSuccess(List<Face> faces) {

        FaceSharer faceSh = faceSharer.get();
        Button btn = goButton.get();
        Face target = faces.get(0);

        if(btn != null)
            //TODO
            // Refactor urgently!
            btn.post(() -> {
                    btn.setVisibility(View.VISIBLE);

                    if(faceSh != null && faces.size() > 0)
                        faceSh.setFace(target);
            });
    }
}
