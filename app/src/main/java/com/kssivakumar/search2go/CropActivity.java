package com.kssivakumar.search2go;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;

public class CropActivity extends AppCompatActivity
{
    private static final String TAG = "CropActivity";

    private ImageView imageView;
    private Bitmap imageViewBitmap;
    private CropView cropView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        Uri pictureUri = getIntent().getData();
        Bitmap pictureBitmap = null;
        try {
            pictureBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), pictureUri);
        } catch (IOException e) {
            Log.e(TAG, "Could not load bitmap from file.");
        }

        Matrix rotation = new Matrix();
        try {
            ExifInterface exif = new ExifInterface(pictureUri.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case 6:
                    rotation.postRotate(90);
                    break;
                case 3:
                    rotation.postRotate(180);
                    break;
                case 8:
                    rotation.postRotate(270);
                    break;
                default:
                    rotation.postRotate(0);
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not get Exif data.");
        }

        float srcWidth = pictureBitmap.getWidth();
        float srcHeight = pictureBitmap.getHeight();
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                pictureBitmap,
                0,
                0,
                Math.round(srcWidth),
                Math.round(srcHeight),
                rotation,
                true
        );

        srcWidth = rotatedBitmap.getWidth();
        srcHeight = rotatedBitmap.getHeight();
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

        imageViewBitmap = Bitmap.createScaledBitmap(
                rotatedBitmap,
                Math.round(dstWidth),
                Math.round(dstHeight),
                true
        );

        imageView.setImageBitmap(imageViewBitmap);

        Button doneButton = (Button)findViewById(R.id.doneButton);
        doneButton.setOnClickListener(doneButton_OnClickListener);

        cropView = (CropView)findViewById(R.id.cropView);
    }

    private View.OnClickListener doneButton_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int bitmapWidth = imageViewBitmap.getWidth();
            int bitmapHeight = imageViewBitmap.getHeight();
            int cropViewWidth = cropView.getWidth();
            int cropViewHeight = cropView.getHeight();
            float cropX = cropView.getRectX();
            float cropY = cropView.getRectY();
            float cropWidth = cropView.getRectWidth();
            float cropHeight = cropView.getRectHeight();

            int croppedBitmapX = Math.round((cropX / cropViewWidth) * bitmapWidth);
            int croppedBitmapY = Math.round((cropY / cropViewHeight) * bitmapHeight);
            int croppedBitmapWidth = Math.round((cropWidth / cropViewWidth) * bitmapWidth);
            int croppedBitmapHeight = Math.round((cropHeight / cropViewHeight) * bitmapHeight);

            Bitmap croppedBitmap =
                    Bitmap.createBitmap(
                            imageViewBitmap,
                            croppedBitmapX,
                            croppedBitmapY,
                            croppedBitmapWidth,
                            croppedBitmapHeight
                    );

            Uri croppedPictureUri = PictureStorage.saveToExternalPublicStorage(croppedBitmap);

            Intent searchActivityIntent = new Intent(
                    getApplicationContext(),
                    SearchActivity.class
            );
            searchActivityIntent.setData(croppedPictureUri);
            startActivity(searchActivityIntent);
        }
    };
}
