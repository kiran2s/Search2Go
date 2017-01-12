package com.kssivakumar.search2go;

import android.content.Intent;
import android.content.res.Resources;
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

import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

import java.io.IOException;
import java.util.List;

public class SearchActivity extends AppCompatActivity
{
    private static final String TAG = "SearchActivity";

    private ImageView imageView;
    private Bitmap croppedPictureBitmap;
    private TextBoxViewGroup textBoxViewGroup;
    private SparseArray<TextBlock> textBlocks;
    private EditText searchText;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        imageView = (ImageView)findViewById(R.id.imageView);
        textBoxViewGroup = (TextBoxViewGroup)findViewById(R.id.textBoxViewGroup);
        searchText = (EditText)findViewById(R.id.searchText);
        searchButton = (Button)findViewById(R.id.searchButton);

        searchButton.setOnClickListener(searchButton_onClickListener);

        Intent intent = getIntent();
        Uri croppedPictureUri = intent.getData();
        croppedPictureBitmap = null;
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
        textBlocks = CropActivity.getDetectedTextBlocks(activityID);
        if (textBlocks == null)
            return;
        Log.d(TAG, "Length of textBlocks: " + String.valueOf(textBlocks.size()));

        // Does not work because imageView has not been laid out yet
        // Read SearchActivity and TextBoxViewGroup
        // Take imageView layout and textBoxViewGroup layout into account
        Log.d(TAG, String.valueOf(croppedPictureBitmap.getWidth()));
        Log.d(TAG, String.valueOf(croppedPictureBitmap.getHeight()));
        Log.d(TAG, String.valueOf(imageView.getWidth()));
        Log.d(TAG, String.valueOf(imageView.getHeight()));

        if (textBlocks == null)
            return;

        TextBoxViewGroup.TextBoxAdder textBoxAdder =
                textBoxViewGroup.new TextBoxAdder(
                        croppedPictureBitmap.getWidth(),
                        croppedPictureBitmap.getHeight(),
                        imageView.getWidth(),
                        imageView.getHeight()
                );

        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.valueAt(i);
            if (textBlock != null) {
                List<? extends Text> textLines = textBlock.getComponents();
                if (textLines != null) {
                    for (Text textLine : textLines) {
                        textBoxAdder.addTextBox(textLine);
                    }
                }
            }
        }
    }

    // Listeners
    private View.OnClickListener searchButton_onClickListener = new View.OnClickListener() {
        public void onClick(View view) {
            String query = searchText.getText().toString();
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
}
