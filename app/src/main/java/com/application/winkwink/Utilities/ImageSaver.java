package com.application.winkwink.Utilities;

/* Code adapted from Google's Camera2BasicFragment example */

import android.graphics.Bitmap;
import android.media.Image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageSaver implements Runnable {

    private final byte[] imageBuffer;
    private final File destFile;

    public ImageSaver(Image image, File file) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        destFile = file;

        imageBuffer = new byte[buffer.remaining()];
        buffer.get(imageBuffer);
        image.close();
    }

    public ImageSaver(byte[] image, File file) {
        imageBuffer = image;
        destFile = file;
    }

    @Override
    public void run() {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destFile);
            output.write(imageBuffer);
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
    }

}