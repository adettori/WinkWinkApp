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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private static final int DISCOVERY_DURATION_REQUEST = 120; //Seconds
    private static final int REQUEST_DISCOVERABLE_ID = 1;
    private static final int REQUEST_BLUETOOTH_ENABLE_ID = 2;
    private static final int REQUEST_CAMERA2_ACTIVITY_ID = 4;

    private Switch btSwitch;
    private BluetoothAdapter bta;
    private BluetoothMainReceiver br;
    private IntentFilter broadcastFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_menu);

        btSwitch = findViewById(R.id.bt_switch);
        btSwitch.setOnCheckedChangeListener(this);

        bta = BluetoothAdapter.getDefaultAdapter();

        br = new BluetoothMainReceiver();

        broadcastFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        broadcastFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
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

    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.players_button) {

            if(bta != null && !bta.isEnabled()) {
                Intent i = new Intent();
                i.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(i, REQUEST_BLUETOOTH_ENABLE_ID);
            } else {

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, BluetoothListFragment.newInstance())
                        .addToBackStack("BLUETOOTH_LIST_TRANSITION")
                        .commit();
            }

        } else if(view.getId() == R.id.camera_button) {

            Intent i = new Intent(this, Camera2BasicActivity.class);
            startActivityForResult(i, REQUEST_CAMERA2_ACTIVITY_ID);
        }
    }

    protected void onActivityResult(int code, int res, Intent data) {
        super.onActivityResult(code, res, data);

        switch(code) {
            case REQUEST_DISCOVERABLE_ID:
                if (res == RESULT_CANCELED) { btSwitch.setChecked(false); }
                break;
            case REQUEST_BLUETOOTH_ENABLE_ID:
                if(res == RESULT_OK) {

                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, BluetoothListFragment.newInstance())
                        .addToBackStack("BLUETOOTH_LIST_TRANSITION")
                        .commit();
                }
                break;
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

    private class BluetoothMainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction()) &&
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_OFF) {

                btSwitch.setChecked(false);
            } else if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())) {

                if(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                    != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)

                    btSwitch.setChecked(false);
                else if(intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)
                    == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)

                    btSwitch.setChecked(true);
            }
        }
    }

}