package com.application.winkwink;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MenuFragment extends Fragment implements View.OnClickListener {

    private static final int REQUEST_ACCESS_COARSE_LOCATION_ID = 1;


    public MenuFragment() {}

    public static MenuFragment newInstance() { return new MenuFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        Activity activity = getActivity();

        assert activity != null;

        Button findButton = activity.findViewById((R.id.find_button));
        findButton.setOnClickListener(this);

        Button hostButton = activity.findViewById(R.id.host_button);
        hostButton.setOnClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.find_button) {

            handleFragmentLocationPermission();
        } else if(view.getId() == R.id.host_button) {

            getParentFragmentManager().beginTransaction()
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
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, BluetoothListFragment.newInstance())
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

    private void handleFragmentLocationPermission () {

        Context context = getContext();

        assert context != null;

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, BluetoothListFragment.newInstance())
                    .addToBackStack("BLUETOOTH_LIST_TRANSITION")
                    .commit();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COARSE_LOCATION_ID);
        }
    }
}