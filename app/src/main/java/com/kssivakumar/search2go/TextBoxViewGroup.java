package com.kssivakumar.search2go;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;

import com.google.android.gms.vision.text.Text;

import java.util.ArrayList;

public class TextBoxViewGroup extends ViewGroup {
    private static String TAG = "TextBoxViewGroup";

    private Context context;
    private AttributeSet attrs;
    private int defStyleAttr;

    private boolean isInitialLayout = true;
    private float width;
    private float height;
    private float bitmapWidth;
    private float bitmapHeight;
    private ArrayList<TextBox> textBoxes = new ArrayList<>();

    private class TextBox {
        private TextBoxView textBoxView;
        private Rect rect;

        public TextBox(String text, Rect rect) {
            textBoxView = new TextBoxView(context, attrs, defStyleAttr);
            textBoxView.setText(text);
            this.rect = rect;
        }

        public void addToViewGroup() {
            addView(textBoxView);
        }

        public void layout(float scaleFactor, boolean fillWidth) {
            scale(scaleFactor);
            translate(scaleFactor, fillWidth);
            layout();
        }

        public void layout() {
            textBoxView.layout(rect.left, rect.top, rect.right, rect.bottom);
        }

        private void scale(float scaleFactor) {
            rect.set(
                    Math.round(rect.left * scaleFactor),
                    Math.round(rect.top * scaleFactor),
                    Math.round(rect.right * scaleFactor),
                    Math.round(rect.bottom * scaleFactor)
            );
        }

        private void translate(float scaleFactor, boolean fillWidth) {
            int translationX = 0;
            int translationY = 0;
            if (fillWidth) {
                float pictureHeight = bitmapHeight * scaleFactor;
                translationY = Math.round((height - pictureHeight) / 2);
            }
            else {
                float pictureWidth = bitmapWidth * scaleFactor;
                translationX = Math.round((width - pictureWidth) / 2);
            }

            rect.set(
                    rect.left + translationX,
                    rect.top + translationY,
                    rect.right + translationX,
                    rect.bottom + translationY
            );
        }
    }

    public class TextBoxAdder {
        public TextBoxAdder(float bitmapWidth, float bitmapHeight) {
            TextBoxViewGroup.this.bitmapWidth = bitmapWidth;
            TextBoxViewGroup.this.bitmapHeight = bitmapHeight;
        }

        public void addTextBox(Text text) {
            TextBox textBox =
                    new TextBox(
                            text.getValue(),
                            text.getBoundingBox()
                    );
            textBox.addToViewGroup();
            textBoxes.add(textBox);
        }
    }

    public TextBoxViewGroup(Context context) {
        super(context);
        init(context, null, 0);
    }

    public TextBoxViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public TextBoxViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        this.context = context;
        this.attrs = attrs;
        this.defStyleAttr = defStyleAttr;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (isInitialLayout) {
            isInitialLayout = false;

            width = getWidth();
            height = getHeight();
            float widthRatio = width / bitmapWidth;
            float heightRatio = height / bitmapHeight;
            boolean fillWidth = widthRatio < heightRatio;
            float scaleFactor = fillWidth ? widthRatio : heightRatio;

            for (TextBox textBox : textBoxes) {
                textBox.layout(scaleFactor, fillWidth);
            }
        }
        else {
            for (TextBox textBox : textBoxes) {
                textBox.layout();
            }
        }
    }
}
