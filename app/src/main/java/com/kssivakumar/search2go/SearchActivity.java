package com.kssivakumar.search2go;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.youtube.player.YouTubeIntents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity
{
    private static final String TAG = "SearchActivity";

    private ImageView imageView;
    private EditText searchText;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        imageView = (ImageView)findViewById(R.id.imageView);
        searchText = (EditText)findViewById(R.id.searchText);
        searchButton = (Button)findViewById(R.id.searchButton);

        searchButton.setOnClickListener(searchButton_onClickListener);

        Uri croppedPictureUri = getIntent().getData();
        Bitmap croppedPictureBitmap = null;
        try {
            croppedPictureBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), croppedPictureUri);
        } catch (IOException e) {
            Log.e(TAG, "Could not load bitmap from file.");
        }
        imageView.setImageBitmap(croppedPictureBitmap);

        Frame croppedPictureFrame = new Frame.Builder().setBitmap(croppedPictureBitmap).build();
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(croppedPictureFrame);

        Log.d(TAG, "Length of textBlocks: " + String.valueOf(textBlocks.size()));

        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.valueAt(i);
            if (textBlock != null && textBlock.getValue() != null) {
                searchText.setText(textBlock.getValue());
                Log.d(TAG, textBlock.getValue());
                break;
            }
        }
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

            SearchAppLabel searchAppLabel = SearchAppLabel.APP_NA;
            if (packageName.contains("sbrowser"))
                searchAppLabel = SearchAppLabel.APP_SBROWSER;
            else if (packageName.contains("chrome"))
                searchAppLabel = SearchAppLabel.APP_CHROME;
            else if (packageName.contains("youtube"))
                searchAppLabel = SearchAppLabel.APP_YOUTUBE;

            if (searchAppLabel != SearchAppLabel.APP_NA) {
                Intent extraSearchIntent = new Intent();
                switch (searchAppLabel) {
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

    // Listeners
    private View.OnClickListener searchButton_onClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            dispatchSearch();
        }
    };
}
