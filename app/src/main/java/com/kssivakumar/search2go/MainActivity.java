package com.kssivakumar.search2go;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int IMAGE_CROP = 2;

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!phoneHasCamera()) {
            finish();
            System.exit(0);
        }

        imageView = (ImageView)findViewById(R.id.imageView);

        dispatchCamera();
    }

    private void dispatchCamera() {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (imageCaptureIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(imageCaptureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Uri imageUri = data.getData();
                Intent cropImageIntent = new Intent("com.android.camera.action.CROP");
                cropImageIntent.setDataAndType(imageUri, "image/*");
                cropImageIntent.putExtra("crop", "true");
                cropImageIntent.putExtra("return-data", true);
                startActivityForResult(cropImageIntent, IMAGE_CROP);
            }
            else if (requestCode == IMAGE_CROP) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = extras.getParcelable("data");
                imageView.setImageBitmap(imageBitmap);
            }
        }
    }

    private boolean phoneHasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }
}
