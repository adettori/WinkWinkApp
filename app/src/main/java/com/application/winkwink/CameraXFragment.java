package com.application.winkwink;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Original code taken from Google's
 * https://codelabs.developers.google.com/codelabs/camerax-getting-started */

public class CameraXFragment extends Fragment implements View.OnClickListener {

    public final static String TAG = "CameraXTest";
    public final static int CAMERA_MODE_COMPARE = 1;
    public final static int CAMERA_MODE_PHOTO = 2;
    //TODO
    public final static int CAMERA_MODE_EASTEREGG = 3;

    private int ACTIVE_MODE;

    private PreviewView viewFinder;

    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalyzer;
    private ExecutorService cameraExecutor;

    // Left eye, right eye, smile probabilities
    private float[] faceToCompare;

    public CameraXFragment() {}

    public static CameraXFragment newInstance() { return new CameraXFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = this.getArguments();

        if(args != null) {

            int mode = args.getInt("cameraXMode");

            if(mode == CAMERA_MODE_COMPARE || mode == CAMERA_MODE_PHOTO
                    || mode == CAMERA_MODE_EASTEREGG)
                ACTIVE_MODE = mode;
            else
                ACTIVE_MODE = CAMERA_MODE_PHOTO;

            faceToCompare = args.getFloatArray("facialFeaturesArray");
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        startCamera();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_x, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        viewFinder = view.findViewById(R.id.preview);

        Button btn = view.findViewById(R.id.picture);
        btn.setOnClickListener(this);

        if(ACTIVE_MODE == CAMERA_MODE_COMPARE || ACTIVE_MODE == CAMERA_MODE_EASTEREGG)
            view.findViewById(R.id.btn_container).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.picture) {

            takePhoto();
        }
    }

    //TODO
    // Limit resolution to speedup bluetooth transfer
    private void startCamera() {

        Context context = getActivity();

        assert context != null;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            ProcessCameraProvider cameraProvider = null;
            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            Preview preview = new Preview.Builder().build();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();

            try {
                // Unbind use cases before rebinding
                assert cameraProvider != null;
                cameraProvider.unbindAll();

                // Bind use cases to camera
                Camera camera;

                if(ACTIVE_MODE == CAMERA_MODE_PHOTO) {

                    imageCapture = new ImageCapture.Builder().build();
                    camera = cameraProvider.bindToLifecycle(getActivity(), cameraSelector,
                            preview, imageCapture);
                } else {

                    imageAnalyzer = new ImageAnalysis.Builder().build();
                    imageAnalyzer.setAnalyzer(cameraExecutor,
                            new FacialFeaturesAnalyzer(faceToCompare));
                    camera = cameraProvider.bindToLifecycle(getActivity(), cameraSelector,
                            preview, imageAnalyzer);
                }

                preview.setSurfaceProvider(
                        viewFinder.createSurfaceProvider(camera.getCameraInfo()));
            } catch(Exception exc) {
                Log.e("", "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void takePhoto() {

        Context context = getContext();

        assert context != null;

        // Create timestamped output file to hold the image
        File photoFile = new File(
                context.getExternalFilesDir(null),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.ITALY
                ).format(System.currentTimeMillis()) + ".jpg");

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback () {

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.d(TAG, "Photo capture failed", exc);
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        String msg = "Photo capture succeeded";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("guestFaceUri", savedUri);
                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_OK, resultIntent);

                        getParentFragmentManager().popBackStack();
                    }
        });
    }

    private static class FacialFeaturesAnalyzer implements ImageAnalysis.Analyzer,
            OnSuccessListener<List<Face>>, OnFailureListener {

        private FaceDetector faceDet;
        private ImageProxy curImage;
        private Boolean[] faceToCompareFeatures;

        public FacialFeaturesAnalyzer(float[] probabilitiesArray) {

            this();

            if(probabilitiesArray != null)
                faceToCompareFeatures = facialFeaturesSolver(probabilitiesArray);
        }

        public FacialFeaturesAnalyzer() {

            FaceDetectorOptions faceOpt = new FaceDetectorOptions.Builder()
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .build();

            faceDet = FaceDetection.getClient(faceOpt);
        }

        @SuppressLint("UnsafeExperimentalUsageError")
        @Override
        public void analyze(@NonNull ImageProxy image) {

            InputImage toDetect = InputImage.fromMediaImage(image.getImage(),
                    image.getImageInfo().getRotationDegrees());

            curImage = image;

            faceDet.process(toDetect)
                    .addOnSuccessListener(this)
                    .addOnFailureListener(this);
        }

        @Override
        public void onFailure(@NonNull Exception e) {

            Log.e(TAG, "Detect failed");
            curImage.close();
            e.printStackTrace();
        }

        @Override
        public void onSuccess(List<Face> faces) {

            curImage.close();

            Log.d(TAG, "Found faces: " + faces.size());

            if(faces.size() == 0)
                return;

            Face curFace = faces.get(0);

            float[] curFaceFeatures = {curFace.getLeftEyeOpenProbability(),
                    curFace.getRightEyeOpenProbability(), curFace.getSmilingProbability()};

            if(Arrays.equals(facialFeaturesSolver(curFaceFeatures), faceToCompareFeatures))
                Log.e(TAG, "Well done!");
            else
                Log.e(TAG, "Booo!");
        }

        private Boolean[] facialFeaturesSolver(float[] input) {

            Boolean[] result = new Boolean[3];
            double threshold = 0.70;

            for(int i=0; i<input.length;i++) {

                result[i] = input[i] > threshold;
            }

            return result;
        }
    }
}