package com.google.nartkolai.droidadbtools.Utils;

import android.content.Context;
import android.util.Log;

import com.google.nartkolai.droidadbtools.R;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CopyFiles {
    private static String TAG = "CopyFiles";

    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        int length;
        while ((length = input.read(buffer)) != -1) {
            output.write(buffer, 0, length);
            Log.i(TAG, "Stream " + output);
        }
    }

    public static void copyFile(Context inputPath, String outputPath) throws IOException {
            InputStream input = inputPath.getResources().openRawResource(R.raw.adb);
            FileOutputStream output = new FileOutputStream(outputPath);
            Log.i(TAG, "input " + input);
            copyStream(input, output);
    }
}
