package com.kssivakumar.search2go;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class MainActivity
        extends AppCompatActivity
        implements CameraTextRecognizerProcessor.Callback
{
    private static final String TAG = "MainActivity";

    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private static final int RC_IMAGE_CROP = 1;

    private boolean cameraDispatchRequested;
    private boolean surfaceAvailable;

    private CameraSource cameraSource;
    private SurfaceView surfaceView;
    private TextView textDetectedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Exit app if phone does not have camera
        if (!phoneHasCamera()) {
            Log.e(TAG, "Device does not have camera");
            finish();
        }

        cameraDispatchRequested = false;
        surfaceAvailable = false;

        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        textDetectedTextView = (TextView)findViewById(R.id.textDetectedTextView);

        surfaceView.getHolder().addCallback(surfaceView_Callback);

        requestCameraPermissionIfNecessary();
        createCamera();

        final ImageButton takePictureButton = (ImageButton)findViewById(R.id.takePictureButton);
        takePictureButton.setOnClickListener(takePictureButton_OnClickListener);
        final ImageButton savedPicturesButton = (ImageButton)findViewById(R.id.savedPicturesButton);
        savedPicturesButton.setOnClickListener(savedPicturesButton_OnClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraDispatchRequested = true;
        dispatchCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraSource.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.release();
        cameraSource = null;
    }

    private void requestCameraPermissionIfNecessary() {
        int resultCode = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (resultCode == PackageManager.PERMISSION_GRANTED)
            return;

        Log.w(TAG, "Permission to handle camera not yet granted. Permission requested.");
        final String[] permissions = new String[] { Manifest.permission.CAMERA };
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
        )) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission to handle camera granted. Initializing camera.");
            createCamera();
        }
        else {
            int resultsLength = grantResults.length;
            Log.e(  TAG, "Permission to handle camera not granted. Results length = " +
                    resultsLength + ". Results code = " +
                    (resultsLength > 0 ? grantResults[0] : "(empty)"));
            finish();
        }
    }

    private void createCamera() {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        textRecognizer.setProcessor(new CameraTextRecognizerProcessor<>(this));

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Text recognition dependencies are not yet available.");
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean deviceHasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (deviceHasLowStorage) {
                Log.w(TAG, "Device has low storage.");
                finish();
            }
        }

        cameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setAutoFocusEnabled(true)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedFps(2.0f)
                        //.setRequestedPreviewSize(1280, 1024)
                        .build();
    }

    private void dispatchCamera() throws SecurityException {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play services unavailable.");
            GoogleApiAvailability.getInstance().getErrorDialog(
                    this,
                    resultCode,
                    RC_HANDLE_GMS
            ).show();
        }

        if (cameraDispatchRequested && surfaceAvailable) {
            try {
                cameraSource.start(surfaceView.getHolder());
            } catch (IOException e) {
                finish();
            }
            cameraDispatchRequested = false;
        }
    }

    private void takePicture() {}

    private void dispatchSavedPicturesViewer() {}

    private void dispatchCropper(Uri imageUri) {
        Intent cropImageIntent = new Intent("com.android.camera.action.CROP");
        cropImageIntent.setDataAndType(imageUri, "image/*");
        cropImageIntent.putExtra("crop", "true");
        cropImageIntent.putExtra("return-data", true);
        if (cropImageIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(cropImageIntent, RC_IMAGE_CROP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == RC_IMAGE_CROP) {}
        }
    }

    @Override
    public void textDetectionUpdated(boolean textDetected) {
        if (textDetected)
            textDetectedTextView.setText(R.string.text_detected_text);
        else
            textDetectedTextView.setText("");
    }

    private boolean phoneHasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    /*** Listeners and Callbacks ***/
    private View.OnClickListener takePictureButton_OnClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            takePicture();
        }
    };

    private View.OnClickListener savedPicturesButton_OnClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            dispatchSavedPicturesViewer();
        }
    };

    private SurfaceHolder.Callback surfaceView_Callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            surfaceAvailable = true;
            dispatchCamera();
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            surfaceAvailable = false;
        }
    };
}
