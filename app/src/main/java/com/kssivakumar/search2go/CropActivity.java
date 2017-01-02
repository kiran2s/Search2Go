package com.kssivakumar.search2go;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;

public class CropActivity extends AppCompatActivity
{
    private static final String TAG = "CropActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        ImageView imageView = (ImageView)findViewById(R.id.imageView);

        Uri pictureUri = getIntent().getData();
        Bitmap pictureBitmap = null;
        try {
            pictureBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), pictureUri);
        } catch (IOException e) {
            Log.e(TAG, "Could not load bitmap from file.");
        }

        float srcWidth = pictureBitmap.getWidth();
        float srcHeight = pictureBitmap.getHeight();

        float dstWidth;
        float dstHeight;
        if (srcWidth < srcHeight) {
            dstHeight = 4096.0f;
            dstWidth = srcWidth * (dstHeight / srcHeight);
        }
        else {
            dstWidth = 4096.0f;
            dstHeight = srcHeight * (dstWidth / srcWidth);
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                pictureBitmap,
                Math.round(dstWidth),
                Math.round(dstHeight),
                true
        );
        imageView.setImageBitmap(pictureBitmap);
    }
}
