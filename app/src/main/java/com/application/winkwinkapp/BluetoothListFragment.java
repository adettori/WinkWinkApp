package com.application.winkwinkapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BluetoothListFragment extends Fragment {

    private RecyclerView recyclerView;
    private BluetoothRecycleAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private BluetoothAdapter bta;

    private ArrayList<BluetoothDevice> adapterDataset;

    public BluetoothListFragment() {
        // Required empty public constructor
    }

    public static BluetoothListFragment newInstance() {
        return new BluetoothListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapterDataset = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bluetooth_list, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        bta = BluetoothAdapter.getDefaultAdapter();

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_1);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new BluetoothRecycleAdapter(bta, adapterDataset);
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() { super.onResume(); }

    @Override
    public void onPause() { super.onPause(); }

    public void appendAdapterDataset(BluetoothDevice dev) {
        adapterDataset.add(dev);
        mAdapter.notifyDataSetChanged();
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

        public BluetoothRecycleAdapter(BluetoothAdapter bAdapter,
                                       ArrayList<BluetoothDevice> fragmentDataSet) {
            mDataset = fragmentDataSet;

            mDataset.addAll(bAdapter.getBondedDevices());
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public BluetoothRecycleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                       int viewType) {
            // create a new view
            TextView v = (TextView) new TextView(getContext());

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

}