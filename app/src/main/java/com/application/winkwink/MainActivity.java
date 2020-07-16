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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
        OnSuccessListener<List<Face>>, OnFailureListener {

    private static final int REQUEST_ACCESS_COARSE_LOCATION_ID = 1;

    private FaceDetector faceDet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_menu);

        FaceDetectorOptions faceOpt = new FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build();

        faceDet = FaceDetection.getClient(faceOpt);
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

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_ACCESS_COARSE_LOCATION_ID) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, BluetoothListFragment.newInstance(this))
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
                    .replace(R.id.fragment_container, BluetoothListFragment.newInstance(this))
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
        InputImage toDetect = InputImage.fromMediaImage(input, imageRotation);

        //input.close();

        detector.process(toDetect)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);*/
    }

    @Override
    public void onFailure(@NonNull Exception e) {

    }
}
