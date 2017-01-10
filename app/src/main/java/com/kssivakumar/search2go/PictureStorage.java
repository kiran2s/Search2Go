package com.kssivakumar.search2go;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PictureStorage
{
    private static String TAG = "PictureStorage";

    private PictureStorage() {}

    private interface FileWritable
    {
        Uri writeToFile(File file);
    }

    private static class JpegDataWritable implements FileWritable
    {
        private byte[] jpegData;

        public JpegDataWritable(byte[] jpegData) {
            this.jpegData = jpegData;
        }

        @Override
        public Uri writeToFile(File file) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(jpegData);
                fileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Problem writing JPEG data to file.");
            }
            return Uri.fromFile(file);
        }
    }

    private static class BitmapWritable implements FileWritable
    {
        private Bitmap bitmap;

        public BitmapWritable(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public Uri writeToFile(File file) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                fileOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Problem writing bitmap to file.");
            }
            return Uri.fromFile(file);
        }
    }

    public static Uri saveToInternalStorage(byte[] jpegData, Context context) {
        return saveToInternalStorage(new JpegDataWritable(jpegData), context);
    }

    public static Uri saveToInternalStorage(Bitmap bitmap, Context context) {
        return saveToInternalStorage(new BitmapWritable(bitmap), context);
    }

    private static Uri saveToInternalStorage(FileWritable fileWritable, Context context) {
        File pictureFile = new File(context.getFilesDir(), getNewFilename());
        return fileWritable.writeToFile(pictureFile);
    }

    public static Uri saveToTemporaryStorage(byte[] jpegData, Context context) {
        return saveToTemporaryStorage(new JpegDataWritable(jpegData), context);
    }

    public static Uri saveToTemporaryStorage(Bitmap bitmap, Context context) {
        return saveToTemporaryStorage(new BitmapWritable(bitmap), context);
    }

    private static Uri saveToTemporaryStorage(FileWritable fileWritable, Context context) {
        try {
            File pictureFile = File.createTempFile(getNewFilename(), "", context.getCacheDir());
            //pictureFile.deleteOnExit();

            Uri uri = fileWritable.writeToFile(pictureFile);
            Log.d(TAG, uri.toString());
            for (File file : context.getCacheDir().listFiles()) {
                Log.d(TAG, file.getAbsolutePath());
            }

            return uri;
        }
        catch (IOException e) {
            Log.e(TAG, "Unable to create temporary file.");
            return null;
        }
    }

    public static Uri saveToExternalPublicStorage(byte[] jpegData) {
        return saveToExternalPublicStorage(new JpegDataWritable(jpegData));
    }

    public static Uri saveToExternalPublicStorage(Bitmap bitmap) {
        return saveToExternalPublicStorage(new BitmapWritable(bitmap));
    }

    private static Uri saveToExternalPublicStorage(FileWritable fileWritable) {
        if (!isExternalStorageWritable())
            Log.e(TAG, "External storage is not writable.");

        File picturesDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Search2Go"
        );
        return savePictureInDir(fileWritable, picturesDir);
    }

    public static Uri saveToExternalPrivateStorage(byte[] jpegData, Context context) {
        return saveToExternalPrivateStorage(new JpegDataWritable(jpegData), context);
    }

    public static Uri saveToExternalPrivateStorage(Bitmap bitmap, Context context) {
        return saveToExternalPrivateStorage(new BitmapWritable(bitmap), context);
    }

    private static Uri saveToExternalPrivateStorage(FileWritable fileWritable, Context context) {
        if (!isExternalStorageWritable())
            Log.e(TAG, "External storage is not writable.");

        File picturesDir = new File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Search2Go"
        );
        return savePictureInDir(fileWritable, picturesDir);
    }

    public static void deleteFile(Uri uri) {
        File file = new File(uri.getPath());
        if (file.delete())
            Log.d(TAG, "Temp file deleted.");
        else
            Log.e(TAG, "Unable to delete temp file.");
    }

    private static Uri savePictureInDir(FileWritable fileWritable, File dir) {
        createDirIfDoesNotExist(dir);
        File pictureFile = new File(dir, getNewFilename());
        return fileWritable.writeToFile(pictureFile);
    }

    private static void createDirIfDoesNotExist(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs())
                Log.e(TAG, "Cannot create pictures directory.");
        }
    }

    private static String getNewFilename() {
        return new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS", Locale.US).format(new Date())
                .concat(".jpg");
    }

    private static boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}
