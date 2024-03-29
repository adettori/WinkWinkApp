package com.application.winkwink;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.winkwink.Utilities.BluetoothGuestClient;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class BluetoothListFragment extends Fragment
        implements View.OnClickListener {

    private static final int REQUEST_DISCOVER_BLUETOOTH_ENABLE_ID = 10;
    private static final int REQUEST_BOND_BLUETOOTH_ENABLE_ID = 11;
    private static final int REQUEST_CAMERA2_FRAGMENT_ID = 12;
    private static final int REQUEST_ACCESS_CAMERA_ID = 13;

    private BluetoothRecycleAdapter mAdapter;

    private BluetoothAdapter bta;
    private BluetoothDiscoveryReceiver bdr;
    private IntentFilter broadcastFilter;
    private Button discoverButton;

    private ArrayList<BluetoothDevice> adapterDataset;

    private BluetoothDevice lastRefDev;
    private Thread btSenderThread;

    public BluetoothListFragment() {}

    public static BluetoothListFragment newInstance() {
        return new BluetoothListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapterDataset = new ArrayList<>();

        bdr = new BluetoothDiscoveryReceiver();

        broadcastFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        broadcastFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_bluetooth_list, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        bta = BluetoothAdapter.getDefaultAdapter();

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_1);

        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new BluetoothRecycleAdapter(adapterDataset, this, getActivity());
        recyclerView.setAdapter(mAdapter);

        discoverButton = view.findViewById(R.id.discover_button);
        discoverButton.setOnClickListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();

        //TODO
        // Handle context == null
        requireContext().registerReceiver(bdr, broadcastFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(btSenderThread != null && btSenderThread.isAlive())
            btSenderThread.interrupt();

        requireContext().unregisterReceiver(bdr);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.discover_button) {

            if(bta != null && !bta.isEnabled()) {
                Intent i = new Intent();
                i.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(i, REQUEST_DISCOVER_BLUETOOTH_ENABLE_ID);
            } else {

                coordinateDeviceDiscovery(bta);
            }

        } else if(v.getId() == R.id.cv) {

            BluetoothDevice btDevice = (BluetoothDevice) v.getTag();
            lastRefDev = btDevice;

            //Bluetooth adapter is already initialised if a cardview is presented
            if(!bta.isEnabled()) {

                Intent i = new Intent();
                i.setAction(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(i, REQUEST_BOND_BLUETOOTH_ENABLE_ID);

            } else if(btDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    btDevice.createBond();
            }

            if(btDevice.getBondState() == BluetoothDevice.BOND_BONDED) {

                handleFragmentCameraPermission();
            }

        }

    }

    public void onActivityResult(int code, int res, Intent data) {

        if(code == REQUEST_DISCOVER_BLUETOOTH_ENABLE_ID) {

            if (res == Activity.RESULT_CANCELED) {
                //TODO
                // Show toast to notify the user that bluetooth is needed
            } else {
                 coordinateDeviceDiscovery(bta);
            }
        } else if(code == REQUEST_BOND_BLUETOOTH_ENABLE_ID) {

            if (res == Activity.RESULT_CANCELED) {
                //TODO
                // Show toast to notify the user that bluetooth is needed
            } else {

                if(lastRefDev.getBondState() == BluetoothDevice.BOND_NONE)
                    lastRefDev.createBond();

                lastRefDev = null;
            }
        } else if(code == REQUEST_CAMERA2_FRAGMENT_ID && data != null) {

            assert lastRefDev != null;

            Uri faceURI = data.getParcelableExtra(getString(R.string.BUNDLE_GUEST_FACE_URI));
            int faceRotation = data.getIntExtra(getString(R.string.BUNDLE_GUEST_FACE_ROTATION),
                    Surface.ROTATION_0);

            assert faceURI != null;

            String username = getActivity().getPreferences(Context.MODE_PRIVATE)
                    .getString(getString(R.string.PREFERENCES_PREFERRED_USERNAME),
                            android.os.Build.MODEL);

            BluetoothGuestClient sendTask =
                    new BluetoothGuestClient(lastRefDev, username, faceRotation,
                            new File(faceURI.getPath()));

            if(btSenderThread == null || !btSenderThread.isAlive()) {
                btSenderThread = new Thread(sendTask);
                btSenderThread.start();
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_ACCESS_CAMERA_ID) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                coordinateCameraAccess();
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

    public void appendAdapterDataset(BluetoothDevice dev) {

        if(!adapterDataset.contains(dev) && dev.getName() != null) {
            adapterDataset.add(dev);
            mAdapter.notifyDataSetChanged();
        }
    }

    private void handleFragmentCameraPermission () {

        Context context = getContext();

        assert context != null;

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {

            coordinateCameraAccess();
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_ACCESS_CAMERA_ID);
        }
    }

    private void coordinateCameraAccess() {

        Fragment cameraFragment = CameraXFragment.newInstance();
        Bundle args = new Bundle();

        bta.cancelDiscovery();

        // Deprecated... but the alternative is still in alpha... great!
        cameraFragment.setTargetFragment(this, REQUEST_CAMERA2_FRAGMENT_ID);
        args.putParcelable(getString(R.string.BUNDLE_TARGET_DEVICE), lastRefDev);
        args.putInt(getString(R.string.BUNDLE_CAMERA_MODE), CameraXFragment.CAMERA_MODE_PHOTO);
        cameraFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.menu_container, cameraFragment)
                .addToBackStack("CAMERA_TRANSITION")
                .commit();
    }

    private void coordinateDeviceDiscovery(BluetoothAdapter ba) {

        if(ba != null) {
            ba.startDiscovery();
            adapterDataset.clear();
            mAdapter.notifyDataSetChanged();
            discoverButton.setEnabled(false);
        }
    }

    private static class BluetoothRecycleAdapter
            extends RecyclerView.Adapter<BluetoothRecycleAdapter.MyViewHolder> {

        private ArrayList<BluetoothDevice> mDataset;
        private View.OnClickListener externalListener;

        private WeakReference<Activity> activityWeakReference;

        public static class MyViewHolder extends RecyclerView.ViewHolder {

            private CardView cv;
            private TextView bluetoothName;
            private TextView bluetoothAddress;
            private ImageView bluetoothIcon;
            //TODO
            // Add icon depending on device class

            public MyViewHolder(View v) {
                super(v);

                cv = v.findViewById(R.id.cv);
                bluetoothName = v.findViewById(R.id.cv_main_line);
                bluetoothAddress = v.findViewById(R.id.cv_secondary_line);
                bluetoothIcon = v.findViewById(R.id.cv_icon);
            }
        }

        public BluetoothRecycleAdapter(ArrayList<BluetoothDevice> fragmentDataSet,
                                       View.OnClickListener listener, Activity a) {
            mDataset = fragmentDataSet;
            externalListener = listener;
            activityWeakReference = new WeakReference<>(a);
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public BluetoothRecycleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                       int viewType) {

            Activity a = activityWeakReference.get();

            if(a == null)
                throw new NullPointerException();

            // create a new view
            View v = a.getLayoutInflater()
                    .inflate(R.layout.card_view_1_item, parent, false);

            return new MyViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {

            holder.bluetoothName.setText(mDataset.get(position).getName());
            holder.bluetoothAddress.setText(mDataset.get(position).getAddress());
            holder.cv.setTag(mDataset.get(position));

            holder.cv.setOnClickListener(externalListener);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

    }

    private class BluetoothDiscoveryReceiver extends BroadcastReceiver {

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