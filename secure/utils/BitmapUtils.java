package com.example.achalasecure.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BitmapUtils {

    // Crop the given bitmap with the given rect.
    public static Bitmap cropRectFromBitmap(Bitmap source, RectF rect) {
        int width = (int)rect.width();
        int  height = (int)rect.height();
        if ((rect.left + width) > source.getWidth()) {
            width = source.getWidth() - (int)rect.left;
        }
        if ((rect.top + height) > source.getHeight()) {
            height = source.getHeight() - (int)rect.top;
        }
        return Bitmap.createBitmap(source, (int)rect.left,(int)rect.top, width, height);
    }

    // Get the image as a Bitmap from the given Uri
    public static Bitmap getBitmapFromUri(ContentResolver contentResolver, Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    // Rotate the given `source` by `degrees`.
    public static Bitmap rotateBitmap(Bitmap source, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
    }

    // Flip the given `Bitmap` horizontally.
    public static Bitmap flipBitmap(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.postScale(-1f, 1f, source.getWidth() / 2f, source.getHeight() / 2f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    // Save a Bitmap to the internal storage (app-specific storage) of your device.
    public static void saveBitmap(Context context, Bitmap image, String name) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(new File(context.getFilesDir().getAbsolutePath() + "/" + name + ".png"));
        image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        fileOutputStream.close();
    }

    // Convert android.media.Image to android.graphics.Bitmap
    public static Bitmap imageToBitmap(Image image, int rotationDegrees) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] yuv = out.toByteArray();
        Bitmap output = BitmapFactory.decodeByteArray(yuv, 0, yuv.length);
        output = rotateBitmap(output, rotationDegrees);
        return flipBitmap(output);
    }

    // Convert the given Bitmap to NV21 ByteArray
    public static byte[] bitmapToNV21ByteArray(Bitmap bitmap) {
        int[] argb = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(argb, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] yuv = new byte[height * width + 2 * (int) Math.ceil(height / 2.0) * (int) Math.ceil(width / 2.0)];
        encodeYUV420SP(yuv, argb, width, height);

        return yuv;
    }

    private static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int R, G, B, Y, U, V;
        int index = 0;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
            R = (argb[index] & 0xff0000) >> 16;
            G = (argb[index] & 0xff00) >> 8;
            B = (argb[index] & 0xff);

            Y = (66 * R + 129 * G + 25 * B + 128) >> 8 + 16;
            U = (-38 * R - 74 * G + 112 * B + 128) >> 8 + 128;
            V = (112 * R - 94 * G - 18 * B + 128) >> 8 + 128;

            yuv420sp[yIndex++] = (byte) (Math.max(0, Math.min(Y, 255)));

            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = (byte) (Math.max(0, Math.min(V, 255)));
                yuv420sp[uvIndex++] = (byte) (Math.max(0, Math.min(U, 255)));
            }
            index++;
        }
        }
    }
}
