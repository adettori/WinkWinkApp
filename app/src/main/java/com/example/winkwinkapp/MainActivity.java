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

package com.example.winkwinkapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private Button findButton;
    private Button cameraButton;
    private static Switch btSwitch;
    private BluetoothAdapter bta;
    private BluetoothReceiver br;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_menu);

        findButton = (Button) findViewById((R.id.players_button));
        findButton.setOnClickListener(this);

        cameraButton = (Button) findViewById((R.id.camera_button));
        cameraButton.setOnClickListener(this);

        btSwitch = (Switch) findViewById(R.id.bt_switch);
        btSwitch.setOnCheckedChangeListener(this);

        bta = BluetoothAdapter.getDefaultAdapter();

        br = new BluetoothReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        this.registerReceiver(br, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.unregisterReceiver(br);
    }

    @Override
    public void onClick(View view) {
        if(view == findButton) {

            if(bta.isDiscovering()) {

                String myname = "it.unipi.di.sam.bttest server";
                UUID myid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
                BluetoothServerSocket bss;
                BluetoothSocket bs = null;

                try {
                    bss = bta.listenUsingRfcommWithServiceRecord(myname,myid);
                    bs = bss.accept();
                    bss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //servi(bs);
            }
        } else if(view == cameraButton) {

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.menu_container, Camera2BasicFragment.newInstance())
                    .commit();
        }
    }

    protected void onActivityResult(int code, int res, Intent data) {
        super.onActivityResult(code, res, data);
        if (code == 1 || code == 2) {
            if (res == RESULT_CANCELED) { btSwitch.setChecked(false); }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if(buttonView == btSwitch) {

            Intent i = new Intent();

            if(isChecked) {

                i.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                startActivityForResult(i, 1);

            } else {

                bta.cancelDiscovery();
            }
        }
    }

    private static class BluetoothReceiver extends BroadcastReceiver {


        @Override
        public void onReceive(Context context, Intent intent) {

            if((intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED) &&
                intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) ==
                        BluetoothAdapter.STATE_OFF)
                ||
                intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {

                btSwitch.setChecked(false);
            } else if(intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {

                btSwitch.setChecked(true);
            }
        }
    }

}