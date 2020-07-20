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

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_USERNAME_ID = 100;

    private String preferredUsername;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_layout);

        pref = getPreferences(MODE_PRIVATE);

        preferredUsername = pref.getString("preferredUsername", null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(preferredUsername == null) {

            Intent i = AccountManager.newChooseAccountIntent(null, null,
                    new String[]{"com.google"}, null,
                    null, null,
                    null);

            startActivityForResult(i, REQUEST_USERNAME_ID);
        }
    }

    public void onActivityResult(int code, int res, Intent data) {

        super.onActivityResult(code, res, data);

        if(code == REQUEST_USERNAME_ID) {

            if(Activity.RESULT_OK == res) {

                preferredUsername = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                SharedPreferences.Editor editor = pref.edit();
                editor.putString("preferredUsername", preferredUsername);
                editor.apply();
            } else {

                preferredUsername = android.os.Build.MODEL;
            }

        }
    }

    protected String getUsername(){ return preferredUsername; }

}
