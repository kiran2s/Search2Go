package com.kssivakumar.search2go;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.google.android.gms.vision.text.Text;

import java.util.ArrayList;

public class TextBoxViewGroup extends ViewGroup {
    private Context context;
    private AttributeSet attrs;
    private int defStyleAttr;

    private ArrayList<TextBox> textBoxes = new ArrayList<>();

    private class TextBox {
        private TextBoxView textBoxView;
        private String text;
        private final int LEFT;
        private final int TOP;
        private final int RIGHT;
        private final int BOTTOM;

        public TextBox(String text, int left, int top, int right, int bottom) {
            this.text = text;
            this.LEFT = left;
            this.TOP = top;
            this.RIGHT = right;
            this.BOTTOM = bottom;
        }

        public void addToViewGroup() {
            textBoxView = new TextBoxView(context, attrs, defStyleAttr);
            textBoxView.setText(text);
            addView(textBoxView);
        }

        public void layout() {
            textBoxView.layout(LEFT, TOP, RIGHT, BOTTOM);
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

    public class TextBoxAdder {
        private float srcWidth;
        private float srcHeight;
        private float dstWidth;
        private float dstHeight;
        private float scaleFactor;
        private boolean fillWidth;

        public TextBoxAdder(float srcWidth, float srcHeight, float dstWidth, float dstHeight) {
            this.srcWidth = srcWidth;
            this.srcHeight = srcHeight;
            this.dstWidth = dstWidth;
            this.dstHeight = dstHeight;

            float widthRatio = dstWidth / srcWidth;
            float heightRatio = dstHeight / srcHeight;
            fillWidth = widthRatio < heightRatio;
            scaleFactor = fillWidth ? widthRatio : heightRatio;
        }

        public void addTextBox(Text text) {
            Rect textBoundingBox = translate(scale(text.getBoundingBox()));
            //Rect textBoundingBox = text.getBoundingBox();
            TextBox textBox =
                    new TextBox(text.getValue(),
                            textBoundingBox.left,
                            textBoundingBox.top,
                            textBoundingBox.right,
                            textBoundingBox.bottom
                    );
            textBox.addToViewGroup();
            textBoxes.add(textBox);
        }

        private Rect scale(Rect rect) {
            rect.set(
                    Math.round(rect.left * scaleFactor),
                    Math.round(rect.top * scaleFactor),
                    Math.round(rect.right * scaleFactor),
                    Math.round(rect.bottom * scaleFactor)
            );
            return rect;
        }

        private Rect translate(Rect rect) {
            if (fillWidth) {
                float srcHeightInDst = srcHeight * scaleFactor;
                int translationY = Math.round((dstHeight - srcHeightInDst) / 2);
                rect.set(
                        rect.left,
                        rect.top + translationY,
                        rect.right,
                        rect.bottom + translationY
                );
            }
            else {
                float srcWidthInDst = srcWidth * scaleFactor;
                int translationX = Math.round((dstWidth - srcWidthInDst) / 2);
                rect.set(
                        rect.left + translationX,
                        rect.top,
                        rect.right + translationX,
                        rect.bottom
                );
            }
            return rect;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (TextBox textBox : textBoxes)
            textBox.layout();
    }
}
