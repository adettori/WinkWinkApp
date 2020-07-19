package com.application.winkwink.Utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class BitmapLoader implements Runnable {

    private WeakReference<ImageView> imgView;

    private File saveLoc;
    private byte[] bitmapBuffer;

    BitmapLoader(ImageView view, File dirFile) {

        imgView = new WeakReference<>(view);
        saveLoc = dirFile;
    }

    BitmapLoader(ImageView view, byte[] buffer) {

        imgView = new WeakReference<>(view);
        bitmapBuffer = buffer;
    }

    @Override
    public void run() {

        ImageView tmpImgV = imgView.get();
        Bitmap result = null;

        try {
            if(saveLoc != null) {
                FileInputStream is = new FileInputStream(saveLoc);
                result = BitmapFactory.decodeStream(is);
            } else if(bitmapBuffer != null) {

                result = BitmapFactory.decodeByteArray(bitmapBuffer, 0,
                        bitmapBuffer.length);
            } else {

                Log.e("BitmapLoader", "data null");
            }

            if(tmpImgV != null) {
                Bitmap finalResult = result;
                tmpImgV.post(() -> tmpImgV.setImageBitmap(finalResult));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
