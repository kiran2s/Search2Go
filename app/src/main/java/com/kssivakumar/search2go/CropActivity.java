package com.kssivakumar.search2go;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.Hashtable;

public class CropActivity extends AppCompatActivity
{
    private static final String TAG = "CropActivity";

    protected static final String EXTRA_ID = "ID";
    private static int numInstances = 0;
    private static Hashtable<Integer, SparseArray<TextBlock>> textDetectionsHashtable =
            new Hashtable<>(1);
    protected static SparseArray<TextBlock> getDetectedTextBlocks(int ID) {
        return textDetectionsHashtable.get(ID);
    }
    private final int ID;

    private TextRecognizer textRecognizer;
    private ImageView imageView;
    private Bitmap imageViewBitmap;
    private CropView cropView;
    private TextView textView;
    private SparseArray<TextBlock> textBlockDetections = null;

    protected CropActivity() {
        ID = numInstances;
        numInstances++;
    }

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

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        cropView = (CropView)findViewById(R.id.cropView);
        cropView.setOnCropperAdjustedListener(cropView_OnCropperAdjustedListener);

        textView = (TextView)findViewById(R.id.textView);

        Button searchButton = (Button)findViewById(R.id.searchButton);
        searchButton.setOnClickListener(searchButton_OnClickListener);
        Button advancedSearchButton = (Button)findViewById(R.id.advancedSearchButton);
        advancedSearchButton.setOnClickListener(advancedSearchButton_OnClickListener);
    }

    @Override
    protected void onDestroy() {
        textDetectionsHashtable.remove(ID);
        super.onDestroy();
    }

    private Bitmap cropPicture() {
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

        return Bitmap.createBitmap(
                imageViewBitmap,
                croppedBitmapX,
                croppedBitmapY,
                croppedBitmapWidth,
                croppedBitmapHeight
        );
    }

    private SparseArray<TextBlock> detectTextInCropRegion() {
        Bitmap croppedBitmap = cropPicture();
        Frame croppedFrame = new Frame.Builder().setBitmap(croppedBitmap).build();
        return textRecognizer.detect(croppedFrame);
    }

    private View.OnClickListener searchButton_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String query = textView.getText().toString();
            Resources resources = getResources();
            String dialogText = resources.getString(R.string.search_app_chooser_text);
            Intent searchIntent = SearchIntentCreator.createSearchIntent(
                    query,
                    dialogText,
                    getApplicationContext(),
                    resources,
                    getPackageManager()
            );
            if (searchIntent == null)
                return;
            startActivity(searchIntent);
        }
    };

    private View.OnClickListener advancedSearchButton_OnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (textBlockDetections == null)
                textBlockDetections = detectTextInCropRegion();
            textDetectionsHashtable.put(ID, textBlockDetections);

            Context context = getApplicationContext();
            Bitmap croppedBitmap = cropPicture();
            Uri croppedPictureUri = PictureStorage.saveToTemporaryStorage(croppedBitmap, context);

            Intent searchActivityIntent = new Intent(context, SearchActivity.class);
            searchActivityIntent.setData(croppedPictureUri);
            searchActivityIntent.putExtra(EXTRA_ID, ID);
            startActivity(searchActivityIntent);
        }
    };

    private class DetectTextInCropRegionTask
            extends AsyncTask<Void, Void, SparseArray<TextBlock>> {
        Bitmap croppedBitmap;

        @Override
        protected void onPreExecute() {
            croppedBitmap = cropPicture();
        }

        @Override
        protected SparseArray<TextBlock> doInBackground(Void... params) {
            Frame croppedFrame = new Frame.Builder().setBitmap(croppedBitmap).build();
            return textRecognizer.detect(croppedFrame);
        }

        @Override
        protected void onPostExecute(SparseArray<TextBlock> result) {
            textBlockDetections = result;
            if (textBlockDetections.size() > 0)
                textView.setText(textBlockDetections.valueAt(0).getValue());
            else
                textView.setText("");
        }
    }

    private CropView.OnCropperAdjustedListener cropView_OnCropperAdjustedListener =
            new CropView.OnCropperAdjustedListener() {
                @Override
                public void onCropperAdjusted() {
                    new DetectTextInCropRegionTask().execute();
                }
            };
}
