package com.example.winkwinkapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FacialFeaturesDetector implements OnSuccessListener<List<Face>>, OnFailureListener {

    private static final String TAG = "FaceDetectorProcessor";
    private static final String MANUAL_TESTING_LOG = "LogTagForTest";

    private final FaceDetector detector;

    public FacialFeaturesDetector (Context context) {
        this(
                context,
                new FaceDetectorOptions.Builder()
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build());
    }

    public FacialFeaturesDetector (Context context, FaceDetectorOptions options) {

        detector = FaceDetection.getClient(options);

    }

    /* Careful: the image cannot be too big, otherwise an OOM kill will occur */
    public void detect (Image input, int imageRotation) {

        InputImage toDetect = InputImage.fromMediaImage(input, imageRotation);

        //input.close();

        detector.process(toDetect)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    public void detect (Bitmap input, int imageRotation) {

        InputImage toDetect = InputImage.fromBitmap(input, imageRotation);

        detector.process(toDetect)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    public void onSuccess(List<Face> faces) {

        Log.e("SUP", "Faces detected: " + faces.size());

        for (Face face : faces) {
            Log.v(
                    MANUAL_TESTING_LOG,
                    "face left eye open probability: "
                            + face.getLeftEyeOpenProbability());
            Log.v(
                    MANUAL_TESTING_LOG,
                    "face right eye open probability: "
                            + face.getRightEyeOpenProbability());
            Log.v(MANUAL_TESTING_LOG, "face smiling probability: "
                    + face.getSmilingProbability());
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }

}
