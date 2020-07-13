package com.application.winkwinkapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BluetoothListFragment extends Fragment implements View.OnClickListener {

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

        getContext().registerReceiver(bdr, broadcastFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContext().unregisterReceiver(bdr);
    }

    public void appendAdapterDataset(BluetoothDevice dev) {
        adapterDataset.add(dev);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.discover_button) {

            if(bta != null) {
                bta.startDiscovery();
                v.setEnabled(false);
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
            // each data item is just a string in this case
            public TextView textView;
            public MyViewHolder(TextView v) {
                super(v);
                textView = v;
            }
        }

        public BluetoothRecycleAdapter(ArrayList<BluetoothDevice> fragmentDataSet) {
            mDataset = fragmentDataSet;
            mDataset.addAll(bta.getBondedDevices());
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public BluetoothRecycleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                       int viewType) {
            // create a new view
            TextView v = new TextView(getContext());

            return new MyViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            holder.textView.setText(mDataset.get(position).getName());

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