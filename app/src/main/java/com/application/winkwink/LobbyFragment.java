package com.application.winkwink;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.application.winkwink.Utilities.BluetoothServerTask;

public class LobbyFragment extends Fragment {

    BluetoothServerTask bst;
    Thread serverT;

    public LobbyFragment() {}

    public static LobbyFragment newInstance() {
        return new LobbyFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        serverT = new Thread(bst);
        serverT.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        serverT.interrupt();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lobby, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        bst = new BluetoothServerTask(getActivity());
    }
}