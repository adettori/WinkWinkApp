package com.application.winkwink.Utilities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class BluetoothHostServer implements Runnable {

    private BitmapLoader bml;

    private File saveLoc;

    private String myName = "it.application.winkwink bluetoothServer";
    private UUID myId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    public BluetoothHostServer(File dirFile, ImageView imgV) {

        saveLoc = dirFile;
        bml = new BitmapLoader(imgV, dirFile);
    }

    @Override
    public void run() {

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        try (BluetoothServerSocket bss = bta.listenUsingRfcommWithServiceRecord(myName,myId)) {

            while(!Thread.interrupted()) {

                BluetoothSocket bs = bss.accept();
                handleConnection(bs);
                bs.close();

                bml.run();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void handleConnection(BluetoothSocket bSocket) {

        try {
            InputStream is = bSocket.getInputStream();

            byte[] lenMsg = new byte[4];
            byte[] command = new byte[1];
            byte[] tmpBuffer = new byte[8192];

            int numBytes;
            int dataSize;
            int totBytes = 0;

            is.read(lenMsg);
            is.read(command);

            dataSize = ByteBuffer.wrap(lenMsg).getInt();
            ByteBuffer buffer = ByteBuffer.allocate(dataSize);

            //TODO
            // Show some kind of loading to the user

            while((numBytes = is.read(tmpBuffer)) != -1) {

                totBytes += numBytes;
                buffer.put(tmpBuffer, 0, numBytes);

                if(totBytes >= dataSize)
                    break;
            }

            Log.e("test", ""+buffer.array().length);

            FileOutputStream output = null;

            try {

                output = new FileOutputStream(saveLoc);
                output.write(buffer.array());
            } catch (IOException e) {

                e.printStackTrace();
            } finally {

                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
