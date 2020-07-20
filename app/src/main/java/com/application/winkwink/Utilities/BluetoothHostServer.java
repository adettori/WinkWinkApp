package com.application.winkwink.Utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.application.winkwink.R;
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
    private WeakReference<TextView> textView;
    private WeakReference<FaceSharer> faceSharer;
    private WeakReference<Context> context;

    private ExecutorService saverExecutor;

    private File saveLoc;

    byte[] lenUserName;
    byte[] userName;
    byte[] imgRotation;
    byte[] lenImage;
    byte[] image;

    String curUserName;

    public BluetoothHostServer(File dirFile, ImageView imgV, Button btn, TextView text,
                               FaceSharer faceS, Context cntx) {

        saveLoc = dirFile;
        goButton = new WeakReference<>(btn);
        imgView = new WeakReference<>(imgV);
        textView = new WeakReference<>(text);
        faceSharer = new WeakReference<>(faceS);
        context = new WeakReference<>(cntx);

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

                Context c = context.get();

                if(c != null)
                    Toast.makeText(c, R.string.receiving_data, Toast.LENGTH_SHORT).show();

                BluetoothProtocolPayload protObj = handleConnection(bs);
                bs.close();

                if(protObj == null)
                    continue;

                imgRotation = protObj.getImgRot();
                username = protObj.getUsername();
                bitmapBuffer = protObj.getImgData();

                saverExecutor.submit(new ImageSaver(bitmapBuffer, saveLoc));

                Bitmap bmp = BitmapFactory.decodeByteArray(bitmapBuffer, 0,
                        bitmapBuffer.length);;

                ImageView view = imgView.get();
                assert view != null;
                assert bmp != null;

                //Load the image inside the ViewImage
                BitmapLoader bml = new BitmapLoader(view, bmp);
                bml.run();

                InputImage toDetect = InputImage.fromBitmap(bmp, imgRotation);

                curUserName = username;

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
            Log.e("BluetoothServer", "Received: " + totBytes);
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
        TextView txt = textView.get();
        Face target = null;

        //TODO
        // Quite ugly, needs refactoring

        if(txt != null) {

            if(faces.size() == 0) {
                txt.post(() -> {
                    txt.setText(R.string.no_players);
                });
                return;
            } else if(faces.size() > 1) {
                txt.post(() -> {
                    txt.setText(R.string.too_many_players);
                });
                return;
            }
        }

        target = faces.get(0);

        if(btn != null && txt != null) {

            Face finalTarget = target;
            String challenger = "Challenger: " + curUserName;

            btn.post(() -> {
                    btn.setVisibility(View.VISIBLE);

                    txt.setText(challenger);

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
