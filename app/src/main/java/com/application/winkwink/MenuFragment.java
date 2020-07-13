package com.application.winkwink;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MenuFragment extends Fragment {

    private Button findButton;
    private Button cameraButton;

    public MenuFragment() {
        // Required empty public constructor
    }

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

        findButton = activity.findViewById((R.id.players_button));
        findButton.setOnClickListener((View.OnClickListener) getActivity());

        cameraButton = activity.findViewById((R.id.camera_button));
        cameraButton.setOnClickListener((View.OnClickListener) getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

}