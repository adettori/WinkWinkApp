package com.application.winkwink.Utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class BitmapLoader implements Runnable {

    private WeakReference<ImageView> imgView;
    private Uri imgLoc;

    BitmapLoader(ImageView view, Uri loc) {

        imgView = new WeakReference<>(view);
        imgLoc = loc;
    }

    @Override
    public void run() {

        File bitmapFile = new File(imgLoc.getPath());
        final ImageView tmp = imgView.get();
        final Bitmap result;

        try {
            FileInputStream is = new FileInputStream(bitmapFile);
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
