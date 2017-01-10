package com.kssivakumar.search2go;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.vision.text.TextBlock;

import java.io.IOException;

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

        Intent intent = getIntent();
        Uri croppedPictureUri = intent.getData();
        Bitmap croppedPictureBitmap = null;
        try {
            croppedPictureBitmap =
                    MediaStore.Images.Media.getBitmap(getContentResolver(), croppedPictureUri);
        } catch (IOException e) {
            Log.e(TAG, "Could not load bitmap from file.");
        }
        imageView.setImageBitmap(croppedPictureBitmap);
        PictureStorage.deleteFile(croppedPictureUri);

        Bundle extras = intent.getExtras();
        if (extras == null)
            return;

        int activityID = extras.getInt(CropActivity.EXTRA_ID);
        SparseArray<TextBlock> textBlocks = CropActivity.getDetectedTextBlocks(activityID);
        if (textBlocks == null)
            return;

        Log.d(TAG, "Length of textBlocks: " + String.valueOf(textBlocks.size()));

        boolean textSet = false;
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.valueAt(i);
            if (textBlock != null && textBlock.getValue() != null) {
                if (!textSet) {
                    searchText.setText(textBlock.getValue());
                    textSet = true;
                }
                Log.d(TAG, textBlock.getValue());
            }
        }
    }

    // Listeners
    private View.OnClickListener searchButton_onClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            String query = searchText.getText().toString();
            String dialogText = getResources().getString(R.string.search_app_chooser_text);
            Intent searchIntent = SearchIntentCreator.createSearchIntent(
                    query,
                    dialogText,
                    getApplicationContext(),
                    getPackageManager()
            );
            startActivity(searchIntent);
        }
    };
}
