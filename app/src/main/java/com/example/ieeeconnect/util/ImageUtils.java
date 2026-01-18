package com.example.ieeeconnect.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(in);
        }
    }

    public static Bitmap scaleDownBitmap(Bitmap realImage, int maxImageSize) {
        if (realImage == null) return null;
        float ratio = Math.min((float) maxImageSize / realImage.getWidth(), (float) maxImageSize / realImage.getHeight());
        if (ratio >= 1.0f) return realImage;
        int width = Math.max(1, Math.round(ratio * realImage.getWidth()));
        int height = Math.max(1, Math.round(ratio * realImage.getHeight()));
        return Bitmap.createScaledBitmap(realImage, width, height, true);
    }

    public static Uri writeBitmapToCacheAndGetUri(Context context, Bitmap bmp, String filename) throws IOException {
        File cacheDir = context.getCacheDir();
        File outFile = new File(cacheDir, filename);
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
        }
        return Uri.fromFile(outFile);
    }
}
