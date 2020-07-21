package com.application.winkwink;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.application.winkwink.Utilities.BluetoothHostServer;
import com.application.winkwink.Utilities.LobbySharer;
import com.google.mlkit.vision.face.Face;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LobbyFragment extends Fragment
        implements CompoundButton.OnCheckedChangeListener, View.OnClickListener, LobbySharer {

    private static final int REQUEST_DISCOVERABLE_ID = 30;
    private static final int REQUEST_CAMERA2_FRAGMENT_ID = 31;
    private static final int REQUEST_ACCESS_CAMERA_ID = 32;
    private static final int DISCOVERY_DURATION_REQUEST = 120; //Seconds
    private static final String saveName = "LastGuestFace.jpeg";

    private Switch btSwitch;
    private Button goButton;
    private ImageView faceView;
    private TextView descText;

    private BluetoothAdapter bta;
    private BluetoothToggleReceiver br;
    private IntentFilter broadcastFilter;

    private BluetoothServerSocket bss;
    private ExecutorService bluetoothExecutor;
    private File saveFile;

    private Face curFaceImageView;

    public LobbyFragment() {}

    public static LobbyFragment newInstance() {
        return new LobbyFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        broadcastFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        broadcastFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);

        bta = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        Activity activity = getActivity();

        assert activity != null;

        saveFile = new File(activity.getFilesDir(), saveName);

        descText = view.findViewById(R.id.desc_text);

        faceView = view.findViewById(R.id.face_view);

        goButton = view.findViewById(R.id.go_button);
        goButton.setOnClickListener(this);

        btSwitch = view.findViewById(R.id.bt_switch);
        btSwitch.setOnCheckedChangeListener(this);

        br = new BluetoothToggleReceiver(btSwitch);
    }

    @Override
    public void onResume() {
        super.onResume();

        Activity activity = getActivity();

        try {
            bss = bta.listenUsingRfcommWithServiceRecord(
                    BluetoothHostServer.myName, BluetoothHostServer.myId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BluetoothHostServer lbs =
                new BluetoothHostServer(bss, saveFile, faceView, goButton, descText, this);

        bluetoothExecutor = Executors.newSingleThreadExecutor();
        bluetoothExecutor.submit(lbs);

        //Forces the synchronisation of the toggle button
        btSwitch.setChecked(false);
        if(bta != null &&
                bta.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            btSwitch.setChecked(true);

        assert activity != null;
        activity.registerReceiver(br, broadcastFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        Activity activity = getActivity();

        assert activity != null;
        activity.unregisterReceiver(br);

        try {
            if(bss != null)
                bss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        bluetoothExecutor.shutdown();
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

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.go_button) {

            handleFragmentCameraPermission();
        }
    }

    public void onActivityResult(int code, int res, Intent data) {
        super.onActivityResult(code, res, data);

        if (code == REQUEST_DISCOVERABLE_ID) {
            if (res == Activity.RESULT_CANCELED) {
                btSwitch.setChecked(false);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_ACCESS_CAMERA_ID) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                coordinateCameraAccess();
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

    private void handleFragmentCameraPermission () {

        Context context = getContext();

        assert context != null;

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {

            coordinateCameraAccess();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_ACCESS_CAMERA_ID);
        }
    }

    private void coordinateCameraAccess() {

        CameraXFragment cameraFragment = CameraXFragment.newInstance();
        Bundle args = new Bundle();
        float[] featuresArray = null;

        if(curFaceImageView != null) {
            featuresArray = new float[3];

            featuresArray[0] = curFaceImageView.getLeftEyeOpenProbability();
            featuresArray[1] = curFaceImageView.getRightEyeOpenProbability();
            featuresArray[2] = curFaceImageView.getSmilingProbability();
        }

        // Deprecated... but the alternative is still in alpha... great!
        cameraFragment.setTargetFragment(this, REQUEST_CAMERA2_FRAGMENT_ID);
        args.putInt(getString(R.string.BUNDLE_CAMERA_MODE), CameraXFragment.CAMERA_MODE_COMPARE);
        args.putFloatArray(getString(R.string.BUNDLE_FACIAL_FEATURES_ARRAY), featuresArray);
        cameraFragment.setArguments(args);

        //TODO
        // Find a better way to pass the drawable
        cameraFragment.setReminderDrawable(faceView.getDrawable());

        getParentFragmentManager().beginTransaction()
                .replace(R.id.menu_container, cameraFragment)
                .addToBackStack("CAMERA_TRANSITION")
                .commit();
    }

    @Override
    public void setFace(Face face) {
        curFaceImageView = face;
    }

    @Override
    public void setChallengerUsername(String name) {

        Activity activity = getActivity();

        assert activity != null;
        activity.getPreferences(Context.MODE_PRIVATE).edit().
                putString(getString(R.string.PREFERENCES_GAME_RESULT_CHALLENGER), name).apply();
    }

    private static class BluetoothToggleReceiver extends BroadcastReceiver {

        private Switch bluetoothSwitch;

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
}