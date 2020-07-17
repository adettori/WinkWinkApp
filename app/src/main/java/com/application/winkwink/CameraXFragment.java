package com.application.winkwink;

import android.content.Context;
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
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraXFragment extends Fragment implements View.OnClickListener {

    private PreviewView viewFinder;
    private ImageCapture imageCapture;

    public CameraXFragment() {}

    public static CameraXFragment newInstance() { return new CameraXFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startCamera();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_x, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        viewFinder = view.findViewById(R.id.texture);

        Button btn = view.findViewById(R.id.picture);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.picture) {

            takePhoto();
        }
    }

    private void startCamera() {

        Context context = getActivity();

        assert context != null;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                ProcessCameraProvider cameraProvider = null;
                try {
                    cameraProvider = cameraProviderFuture.get();
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                try {
                    // Unbind use cases before rebinding
                    assert cameraProvider != null;
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    Camera camera = cameraProvider.bindToLifecycle(
                            getActivity(), cameraSelector, preview, imageCapture);

                    preview.setSurfaceProvider(
                            viewFinder.createSurfaceProvider(camera.getCameraInfo()));
                } catch(Exception exc) {
                    Log.e("CameraXTest", "Use case binding failed", exc);
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void takePhoto() {

        // Create timestamped output file to hold the image
        File photoFile = new File(
                getActivity().getExternalFilesDir(null),
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.ITALY
                ).format(System.currentTimeMillis()) + ".jpg");

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // Setup image capture listener which is triggered after photo has
        // been taken
        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback () {

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Log.e("CameraXTest",
                                "Photo capture failed: ${exc.message}", exc);
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        String msg = "Photo capture succeeded: $savedUri";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d("CameraXTest", msg);
                    }
        });
    }
}