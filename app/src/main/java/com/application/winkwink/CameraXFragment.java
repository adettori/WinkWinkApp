package com.application.winkwink;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.lang.ref.WeakReference;
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
    private static final String saveName = "LastPhoto.jpeg";
    public final static int CAMERA_MODE_COMPARE = 1;
    public final static int CAMERA_MODE_PHOTO = 2;

    private int ACTIVE_MODE;

    private PreviewView viewFinder;
    private TextView countdownView;

    private ProcessCameraProvider cameraProvider;
    private Preview preview = null;
    private ImageCapture imageCapture = null;
    private ImageAnalysis imageAnalyzer = null;

    private ExecutorService cameraExecutor;
    private ExecutorService counterExecutor;

    // Left eye, right eye, smile probabilities
    private float[] faceToCompare;
    private Drawable imgReminder;

    private CustomCounter countDownTimer;

    public CameraXFragment() {}

    public static CameraXFragment newInstance() { return new CameraXFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = this.getArguments();

        if(args != null) {

            int mode = args.getInt("cameraXMode");

            if(mode == CAMERA_MODE_COMPARE || mode == CAMERA_MODE_PHOTO)
                ACTIVE_MODE = mode;
            else
                ACTIVE_MODE = CAMERA_MODE_PHOTO;

            faceToCompare = args.getFloatArray("facialFeaturesArray");
        }

        //Used to set the correct orientation, needed because of c
        OrientationEventListener orientationEventListener = new OrientationEventListener(getContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation;

                // Monitors orientation values to determine the target rotation value
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }

                if(imageCapture != null)
                    imageCapture.setTargetRotation(rotation);
            }
        };

        cameraExecutor = Executors.newSingleThreadExecutor();
        counterExecutor = Executors.newSingleThreadExecutor();

        orientationEventListener.enable();
        startCamera();
    }

    @Override
    public void onResume() {

        super.onResume();

        if(cameraProvider == null)
            startCamera();
    }

    @Override
    public void onPause() {

        super.onPause();

        //Unbind the UseCases otherwise the app will crash
        if(cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_x, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        viewFinder = view.findViewById(R.id.preview);

        ImageView viewReminder = view.findViewById(R.id.view_reminder);

        countdownView = view.findViewById(R.id.countdown);

        Button btn = view.findViewById(R.id.picture);
        btn.setOnClickListener(this);

        if(ACTIVE_MODE == CAMERA_MODE_COMPARE) {

            view.findViewById(R.id.btn_container).setVisibility(View.INVISIBLE);

            viewReminder.setImageDrawable(imgReminder);
            viewReminder.setVisibility(View.VISIBLE);

            countdownView.setVisibility(View.VISIBLE);

            countDownTimer = new CustomCounter(countdownView, getString(R.string.game_score),
                    10000, (AppCompatActivity) getActivity());

            counterExecutor.submit(countDownTimer);
        }

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

            try {
                cameraProvider = cameraProviderFuture.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build();

            try {
                // Unbind use cases before rebinding
                assert cameraProvider != null;
                cameraProvider.unbindAll();

                preview = new Preview.Builder().build();

                imageAnalyzer = new ImageAnalysis.Builder().build();
                imageAnalyzer.setAnalyzer(cameraExecutor,
                        new FacialFeaturesAnalyzer(faceToCompare, countDownTimer,
                                (AppCompatActivity) getActivity()));

                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(new Size(720, 1280))
                        .build();

                // Bind use cases to camera provider
                Camera camera = null;

                if(ACTIVE_MODE == CAMERA_MODE_PHOTO) {

                    camera = cameraProvider.bindToLifecycle(getActivity(), cameraSelector,
                            preview, imageCapture);
                } else if(ACTIVE_MODE == CAMERA_MODE_COMPARE) {

                    camera = cameraProvider.bindToLifecycle(getActivity(), cameraSelector,
                            preview, imageAnalyzer);
                }

                assert camera != null;
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
                context.getFilesDir(),
                saveName);

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback() {

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

                        int orientation = imageCapture.getTargetRotation();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("guestFaceUri", savedUri);
                        resultIntent.putExtra("guestFaceRotation", orientation);

                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_OK, resultIntent);

                        getParentFragmentManager().popBackStack();
                    }
        });
    }

    //Used by LobbyFragment
    public void setReminderDrawable(Drawable dr) {

        imgReminder = dr;
    }

    private static class FacialFeaturesAnalyzer implements ImageAnalysis.Analyzer,
            OnSuccessListener<List<Face>>, OnFailureListener {

        private WeakReference<AppCompatActivity> activity;
        private WeakReference<CustomCounter> counter;

        private FaceDetector faceDet;
        private ImageProxy curImage;
        private Boolean[] faceToCompareFeatures;

        public FacialFeaturesAnalyzer(float[] probabilitiesArray, CustomCounter c,
                                      AppCompatActivity a) {

            this();

            if(probabilitiesArray != null) {
                faceToCompareFeatures = facialFeaturesSolver(probabilitiesArray);
            }

            activity = new WeakReference<>(a);
            counter = new WeakReference<>(c);
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

            Image tmp = image.getImage();

            assert tmp != null;
            InputImage toDetect = InputImage.fromMediaImage(tmp,
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
        }

        @Override
        public void onSuccess(List<Face> faces) {

            AppCompatActivity a = activity.get();
            Face curFace;

            curImage.close();

            Log.d(TAG, "Found faces: " + faces.size());

            if(faces.size() == 0)
                return;

            curFace = faces.get(0);

            float[] curFaceFeatures = {
                    curFace.getLeftEyeOpenProbability(),
                    curFace.getRightEyeOpenProbability(),
                    curFace.getSmilingProbability()};

            if(Arrays.equals(facialFeaturesSolver(curFaceFeatures), faceToCompareFeatures)) {

                Log.e(TAG, "Well done!");

                if(a != null) {

                    long finalScore = -1;

                    CustomCounter c = counter.get();

                    if(c != null)
                        finalScore = c.cancel();

                    long finalScore1 = finalScore;
                    a.runOnUiThread(() -> {
                        String winFormat = "Well done! %d points";
                        Toast.makeText(a,
                                String.format(Locale.ROOT, winFormat, finalScore1),
                                Toast.LENGTH_SHORT).show();
                        SharedPreferences.Editor editor =
                                a.getPreferences(Context.MODE_PRIVATE).edit();

                        //Clear backstack
                        a.getSupportFragmentManager()
                                .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                        editor.putBoolean("GAME_RESULT_POSITIVE", true);
                        editor.putBoolean("GAME_RESULT_REGISTERED", false);
                        editor.putLong("GAME_RESULT_SCORE", finalScore1);
                        editor.apply();

                        a.getSupportFragmentManager().beginTransaction()
                                .replace(R.id.menu_container, MenuFragment.newInstance())
                                .commit();
                    });
                }
            } else
                Log.e(TAG, "Booo!");
        }

        private Boolean[] facialFeaturesSolver(float[] input) {

            Boolean[] result = new Boolean[3];
            double threshold = 0.70;

            for(int i=0; i<input.length; i++) {

                result[i] = input[i] > threshold;
            }

            return result;
        }
    }

    private static class CustomCounter implements Runnable {

        private WeakReference<AppCompatActivity> activity;

        private CountDownTimer localTimer;

        private long lastTick;

        CustomCounter(TextView text, String template, long initialValue, AppCompatActivity a) {

            activity = new WeakReference<>(a);

            localTimer = new CountDownTimer(initialValue, 1) {
                @Override
                public void onTick(long millisUntilFinished) {

                    lastTick = millisUntilFinished;
                    String result = String.format(template, millisUntilFinished);

                    text.post(() -> text.setText(result));
                }

                //Game over condition
                @Override
                public void onFinish() {

                    AppCompatActivity a = activity.get();

                    String result = String.format(template, 0);

                    text.post(() -> text.setText(result));

                    if(a != null) {

                        a.runOnUiThread(() -> {

                            Toast.makeText(a, R.string.lost, Toast.LENGTH_SHORT).show();

                            //Clear backstack
                            a.getSupportFragmentManager()
                                    .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                            SharedPreferences.Editor editor =
                                    a.getPreferences(Context.MODE_PRIVATE).edit();
                            editor.putBoolean("GAME_RESULT_REGISTERED", false);
                            editor.putLong("GAME_RESULT_SCORE", 0);
                            editor.apply();

                            a.getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.menu_container, MenuFragment.newInstance())
                                    .commit();
                        });
                    }
                }
            };
        }

        @Override
        public void run() {

            localTimer.start();
        }

        public long cancel() {

            localTimer.cancel();

            return lastTick;
        }
    }
}