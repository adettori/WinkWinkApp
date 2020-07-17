package com.application.winkwink;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.fragment.app.Fragment;

import com.application.winkwink.Utilities.BluetoothHostServer;

import java.io.File;

public class LobbyFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_DISCOVERABLE_ID = 30;
    private static final int DISCOVERY_DURATION_REQUEST = 120; //Seconds
    private static final String saveName = "LastGuestFace.jpeg";

    private Switch btSwitch;

    private BluetoothAdapter bta;
    private BluetoothToggleReceiver br;
    private IntentFilter broadcastFilter;

    private BluetoothHostServer lbs;
    private Thread serverT;

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
    public void onResume() {
        super.onResume();

        Activity activity = getActivity();

        serverT = new Thread(lbs);
        serverT.start();

        //Forces the synchronisation of the toggle
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
        serverT.interrupt();
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

        File saveFile = new File(activity.getExternalFilesDir(null), saveName);

        ImageView imgView = view.findViewById(R.id.face_view);
        Button btn = view.findViewById(R.id.go_button);

        lbs = new BluetoothHostServer(saveFile, imgView, btn);

        btSwitch = view.findViewById(R.id.bt_switch);
        btSwitch.setOnCheckedChangeListener(this);

        br = new BluetoothToggleReceiver(btSwitch);

    }

    public void onActivityResult(int code, int res, Intent data) {
        super.onActivityResult(code, res, data);

        if (code == REQUEST_DISCOVERABLE_ID) {
            if (res == Activity.RESULT_CANCELED) {
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
}