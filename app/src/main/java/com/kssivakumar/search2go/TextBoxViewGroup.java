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

        public void addTextBoxView() {
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

    public void addTextBox(Text text) {
        Rect textBoundingBox = text.getBoundingBox();
        TextBox textBox =
                new TextBox(text.getValue(),
                        textBoundingBox.left,
                        textBoundingBox.top,
                        textBoundingBox.right,
                        textBoundingBox.bottom
                );
        textBox.addTextBoxView();
        textBoxes.add(textBox);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (TextBox textBox : textBoxes)
            textBox.layout();
    }
}
