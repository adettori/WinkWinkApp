package com.application.winkwink;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BluetoothListFragment extends Fragment
        implements View.OnClickListener {

    private static final int REQUEST_ACCESS_COARSE_LOCATION_ID = 1;

    private RecyclerView recyclerView;
    private BluetoothRecycleAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private BluetoothAdapter bta;
    private BluetoothDeviceReceiver bdr;
    private IntentFilter broadcastFilter;
    private Button discoverButton;

    private ArrayList<BluetoothDevice> adapterDataset;

    public BluetoothListFragment() {}

    public static BluetoothListFragment newInstance() {
        return new BluetoothListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapterDataset = new ArrayList<>();

        bdr = new BluetoothDeviceReceiver();

        broadcastFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        broadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bluetooth_list, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        bta = BluetoothAdapter.getDefaultAdapter();

        recyclerView = view.findViewById(R.id.recycler_view_1);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new BluetoothRecycleAdapter(adapterDataset);
        recyclerView.setAdapter(mAdapter);

        discoverButton = view.findViewById(R.id.discover_button);
        discoverButton.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        //TODO
        //Handle context == null
        requireContext().registerReceiver(bdr, broadcastFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        requireContext().unregisterReceiver(bdr);
    }

    public void appendAdapterDataset(BluetoothDevice dev) {

        if(!adapterDataset.contains(dev)) {
            adapterDataset.add(dev);
            mAdapter.notifyDataSetChanged();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.discover_button) {

            if (ContextCompat.checkSelfPermission(
                    getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {

                coordinateDeviceDiscovery(bta);
            } else {
                requestPermissions(
                        new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                        REQUEST_ACCESS_COARSE_LOCATION_ID);
            }

        }
    }

    private void coordinateDeviceDiscovery(BluetoothAdapter ba) {

        if(ba != null) {
            ba.startDiscovery();
            adapterDataset.clear();
            mAdapter.notifyDataSetChanged();
            discoverButton.setEnabled(false);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION_ID:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    coordinateDeviceDiscovery(bta);
                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                }
        }
    }


    private class BluetoothRecycleAdapter
            extends RecyclerView.Adapter<BluetoothRecycleAdapter.MyViewHolder> {
        private ArrayList<BluetoothDevice> mDataset;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class MyViewHolder extends RecyclerView.ViewHolder {

            private CardView cv;
            private TextView bluetoothName;
            private TextView bluetoothAddress;
            private ImageView bluetoothIcon;

            public MyViewHolder(View v) {
                super(v);

                cv = v.findViewById(R.id.cv);
                bluetoothName = v.findViewById(R.id.cv_main_line);
                bluetoothAddress = v.findViewById(R.id.cv_secondary_line);
                bluetoothIcon = v.findViewById(R.id.cv_icon);
            }
        }

        public BluetoothRecycleAdapter(ArrayList<BluetoothDevice> fragmentDataSet) {
            mDataset = fragmentDataSet;
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public BluetoothRecycleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                       int viewType) {
            // create a new view
            View v = getLayoutInflater()
                    .inflate(R.layout.card_view_item, parent, false);

            return new MyViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.bluetoothName.setText(mDataset.get(position).getName());
            holder.bluetoothAddress.setText(mDataset.get(position).getAddress());

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

    }

    private class BluetoothDeviceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {

                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                appendAdapterDataset(dev);

            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {

                if(discoverButton != null)
                    discoverButton.setEnabled(true);
            }
        }
    }

}