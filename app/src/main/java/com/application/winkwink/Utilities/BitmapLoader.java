package com.application.winkwink.Utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class BitmapLoader implements Runnable {

    private WeakReference<ImageView> imgView;

    private File saveLoc;

    BitmapLoader(ImageView view, File dirFile) {

        imgView = new WeakReference<>(view);
        saveLoc = dirFile;
    }

    @Override
    public void run() {

        final ImageView tmp = imgView.get();
        final Bitmap result;

        try {
            FileInputStream is = new FileInputStream(saveLoc);
            result = BitmapFactory.decodeStream(is);

            if(tmp != null)

                tmp.post(new Runnable() {
                    @Override
                    public void run() {
                        tmp.setImageBitmap(result);
                    }
                });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
