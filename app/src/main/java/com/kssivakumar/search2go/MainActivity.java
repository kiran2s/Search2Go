package com.kssivakumar.search2go;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.google.android.youtube.player.YouTubeIntents;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int IMAGE_CROP = 2;

    private enum SearchApp {
        APP_NA, APP_SBROWSER, APP_CHROME, APP_YOUTUBE
    }

    private TessBaseAPI mTess;
    private String deviceTessPath;
    private String deviceTessDataFilePath;
    private String assetPath;
    private EditText searchText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Exit app if phone does not have camera
        if (!phoneHasCamera()) {
            finish();
            System.exit(0);
        }

        // Initialize important paths
        deviceTessPath = getFilesDir() + "/tesseract/";
        assetPath = "tessdata/eng.traineddata";
        deviceTessDataFilePath = deviceTessPath + assetPath;

        createTessFilesIfNecessary();

        // Initialize Tesseract API
        mTess = new TessBaseAPI();
        mTess.init(deviceTessPath, "eng");

        searchText = (EditText)findViewById(R.id.searchText);
        final Button newImageButton = (Button)findViewById(R.id.newImageButton);
        newImageButton.setOnClickListener(newImageButton_OnClickListener);
        final Button savedImageButton = (Button)findViewById(R.id.savedImageButton);
        savedImageButton.setOnClickListener(savedImageButton_OnClickListener);
        final Button searchButton = (Button)findViewById(R.id.searchButton);
        searchButton.setOnClickListener(searchButton_onClickListener);
    }

    private void dispatchCamera() {
        Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (imageCaptureIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(imageCaptureIntent, REQUEST_IMAGE_CAPTURE);
    }

    private void dispatchSavedImagesViewer() {
        return;
    }

    private void dispatchCropper(Uri imageUri) {
        Intent cropImageIntent = new Intent("com.android.camera.action.CROP");
        cropImageIntent.setDataAndType(imageUri, "image/*");
        cropImageIntent.putExtra("crop", "true");
        cropImageIntent.putExtra("return-data", true);
        if (cropImageIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(cropImageIntent, IMAGE_CROP);
    }

    private void dispatchSearch() {
        String query = searchText.getText().toString();
        Intent searchIntent;
        Intent appChooserIntent;
        String searchAppChooserText = getResources().getString(R.string.search_app_chooser_text);

        if (URLUtil.isValidUrl(query)) {
            searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
            appChooserIntent = Intent.createChooser(searchIntent, searchAppChooserText);
            startActivity(appChooserIntent);
            return;
        }

        searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
        searchIntent.putExtra(SearchManager.QUERY, query);
        appChooserIntent = Intent.createChooser(searchIntent, searchAppChooserText);

        PackageManager packageManager = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appInfoList = packageManager.queryIntentActivities(mainIntent, 0);
        List<LabeledIntent> intentList = new ArrayList<>();

        for (int i = 0; i < appInfoList.size(); i++) {
            ResolveInfo appInfo = appInfoList.get(i);
            String appName = appInfo.activityInfo.name;
            String packageName = appInfo.activityInfo.packageName;

            SearchApp searchApp = SearchApp.APP_NA;
            if (packageName.contains("sbrowser"))
                searchApp = SearchApp.APP_SBROWSER;
            else if (packageName.contains("chrome"))
                searchApp = SearchApp.APP_CHROME;
            else if (packageName.contains("youtube"))
                searchApp = SearchApp.APP_YOUTUBE;

            if (searchApp != SearchApp.APP_NA) {
                Intent extraSearchIntent = new Intent();
                switch (searchApp) {
                    case APP_SBROWSER:
                    case APP_CHROME:
                        extraSearchIntent.setAction(Intent.ACTION_VIEW);
                        extraSearchIntent.setData(Uri.parse("https://www.google.com/search?q=" + query));
                        extraSearchIntent.setComponent(new ComponentName(packageName, appName));
                        break;
                    case APP_YOUTUBE:
                        extraSearchIntent = YouTubeIntents.createSearchIntent(
                                getApplicationContext(),
                                query
                        );
                        List<ResolveInfo> youtubeAppInfoList = packageManager.queryIntentActivities(
                                extraSearchIntent, 0
                        );
                        extraSearchIntent.setComponent(
                                new ComponentName(
                                        packageName,
                                        youtubeAppInfoList.get(0).activityInfo.name
                                )
                        );
                        break;
                }

                intentList.add(
                        new LabeledIntent(
                                extraSearchIntent,
                                packageName,
                                appInfo.loadLabel(packageManager),
                                appInfo.icon
                        )
                );
            }
        }

        LabeledIntent[] extraSearchIntents = intentList.toArray(
                new LabeledIntent[intentList.size()]
        );
        appChooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraSearchIntents);
        startActivity(appChooserIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                dispatchCropper(data.getData());
            }
            else if (requestCode == IMAGE_CROP) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = extras.getParcelable("data");
                String query = performOCR(imageBitmap);
                searchText.setText(query);
            }
        }
    }

    private String performOCR(Bitmap imageBitmap) {
        mTess.setImage(imageBitmap);
        return mTess.getUTF8Text();
    }

    private boolean phoneHasCamera() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    private void createTessFilesIfNecessary() {
        File dir = new File(deviceTessPath + "tessdata/");
        if (!dir.exists() && dir.mkdirs())
            copyTessFile();
        else if (dir.exists()) {
            File tessDataFile = new File(deviceTessDataFilePath);
            if (!tessDataFile.exists())
                copyTessFile();
        }
    }

    private void copyTessFile() {
        try {
            AssetManager assetManager = getAssets();
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

    /*** Listeners ***/
    private View.OnClickListener newImageButton_OnClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            dispatchCamera();
        }
    };

    private View.OnClickListener savedImageButton_OnClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            dispatchSavedImagesViewer();
        }
    };

    private View.OnClickListener searchButton_onClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            dispatchSearch();
        }
    };
}
