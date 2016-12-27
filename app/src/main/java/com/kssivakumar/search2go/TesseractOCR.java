package com.kssivakumar.search2go;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class TesseractOCR
{
    private TesseractOCR() {}

    private static AssetManager assetManager;
    private static TessBaseAPI mTess;
    private static String deviceTessPath;
    private static String deviceTessDataFilePath;
    private static String assetPath;

    public static class API {
        public API(AssetManager am, File filesDir) {
            assetManager = am;

            // Initialize important paths
            deviceTessPath = filesDir + "/tesseract/";
            assetPath = "tessdata/eng.traineddata";
            deviceTessDataFilePath = deviceTessPath + assetPath;

            createTessFilesIfNecessary();

            // Initialize Tesseract API
            mTess = new TessBaseAPI();
            mTess.init(deviceTessPath, "eng");
        }

        public String performOCR(Bitmap imageBitmap) {
            mTess.setImage(imageBitmap);
            return mTess.getUTF8Text();
        }
    }

    private static void createTessFilesIfNecessary() {
        File dir = new File(deviceTessPath + "tessdata/");
        if (!dir.exists() && dir.mkdirs())
            copyTessFile();
        else if (dir.exists()) {
            File tessDataFile = new File(deviceTessDataFilePath);
            if (!tessDataFile.exists())
                copyTessFile();
        }
    }

    private static void copyTessFile() {
        try {
            InputStream inStream = assetManager.open(assetPath);
            OutputStream outStream = new FileOutputStream(deviceTessDataFilePath);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inStream.read(buffer)) >= 0) {
                outStream.write(buffer, 0, len);
            }
            outStream.flush();
            outStream.close();
            inStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

