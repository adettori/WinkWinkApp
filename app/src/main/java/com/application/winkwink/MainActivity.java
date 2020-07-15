/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.application.winkwink;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, OnSuccessListener<List<Face>>, OnFailureListener {

    private static final int DISCOVERY_DURATION_REQUEST = 120; //Seconds
    private static final int REQUEST_DISCOVERABLE_ID = 1;
    private static final int REQUEST_ACCESS_COARSE_LOCATION_ID = 2;

    private Switch btSwitch;
    private BluetoothAdapter bta;
    private BluetoothToggleReceiver br;
    private IntentFilter broadcastFilter;
    private FaceDetector faceDet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_menu);

        btSwitch = findViewById(R.id.bt_switch);
        btSwitch.setOnCheckedChangeListener(this);

        bta = BluetoothAdapter.getDefaultAdapter();

        br = new BluetoothToggleReceiver(btSwitch);

        broadcastFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        broadcastFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);

        FaceDetectorOptions faceOpt = new FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();

        faceDet = FaceDetection.getClient(faceOpt);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Forces the synchronisation of the toggle
        btSwitch.setChecked(false);
        if(bta != null &&
                bta.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            btSwitch.setChecked(true);


        this.registerReceiver(br, broadcastFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.unregisterReceiver(br);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.find_button) {

            handleLocationPermissionFragment();
        } else if(view.getId() == R.id.host_button) {

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, LobbyFragment.newInstance())
                    .addToBackStack("LOBBY_TRANSITION")
                    .commit();
        }
    }

    protected void onActivityResult(int code, int res, Intent data) {
        super.onActivityResult(code, res, data);

        if (code == REQUEST_DISCOVERABLE_ID) {
            if (res == RESULT_CANCELED) {
                btSwitch.setChecked(false);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if(buttonView == btSwitch) {

            if(isChecked &&
                    bta != null &&
                    bta.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {

                Intent i = new Intent();
                i.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                        DISCOVERY_DURATION_REQUEST);
                startActivityForResult(i, REQUEST_DISCOVERABLE_ID);

            } else if (!isChecked &&
                    bta != null &&
                    bta.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {

                //TODO
                //Show toast to explain that it will stop being discoverable in x seconds
                btSwitch.setChecked(true);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_ACCESS_COARSE_LOCATION_ID) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, BluetoothListFragment.newInstance())
                        .addToBackStack("BLUETOOTH_LIST_TRANSITION")
                        .commit();
            } else {
                //TODO
                // Explain to the user that the feature is unavailable because
                // the features requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void handleLocationPermissionFragment () {

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, BluetoothListFragment.newInstance())
                    .addToBackStack("BLUETOOTH_LIST_TRANSITION")
                    .commit();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COARSE_LOCATION_ID);
        }
    }

    @Override
    public void onSuccess(List<Face> faces) {
        Log.e("SUP", "Faces detected: " + faces.size());

        for (Face face : faces) {
            Log.v(
                    "MLKit",
                    "face left eye open probability: "
                            + face.getLeftEyeOpenProbability());
            Log.v(
                    "MLKit",
                    "face right eye open probability: "
                            + face.getRightEyeOpenProbability());
            Log.v("MLKit", "face smiling probability: "
                    + face.getSmilingProbability());
        }

        /*
        *         InputImage toDetect = InputImage.fromMediaImage(input, imageRotation);

        //input.close();

        detector.process(toDetect)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);*/
    }

    @Override
    public void onFailure(@NonNull Exception e) {

    }

    private static class BluetoothToggleReceiver extends BroadcastReceiver {

        Switch bluetoothSwitch;

        public BluetoothToggleReceiver(Switch switchButton) {
            super();

            bluetoothSwitch = switchButton;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction()) &&
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_OFF) {

                bluetoothSwitch.setChecked(false);
            } else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {

                if(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                    != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)

                    bluetoothSwitch.setChecked(false);
                else if(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                    == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)

                    bluetoothSwitch.setChecked(true);
            }
        }
    }
/*
    private static class ImageLoader implements Runnable {

        private final Uri mUri;

        ImageLoader(Uri uri, ImageView imgV) {
            mUri = uri;
        }

        @Override
        public void run() {

            try {
                FileInputStream fileStream = new FileInputStream(mUri.getPath());

                BitmapFactory.decodeStream(fileStream);



            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Image img = MediaStore.Images.Media.getBitmap(mUri);
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mUri);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
*/
}
