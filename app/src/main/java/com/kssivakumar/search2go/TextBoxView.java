package com.kssivakumar.search2go;

import android.content.ClipData;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TextBoxView extends View
{
    private final static String TAG = "TextBoxView";

    private boolean isInitialDraw = true;
    private boolean isBeingDragged = false;

    private Rect box;
    private int boxBorderWidth = 6;
    private int boxColor = Color.CYAN;
    private Paint boxPaint;

    private String boxText;
    private int textColor = Color.GREEN;
    private Paint textPaint;

    public TextBoxView(Context context) {
        super(context);
        init(null, 0);
    }

    public TextBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public TextBoxView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray styledAttributes = getContext().obtainStyledAttributes(
                attrs, R.styleable.TextBoxView, defStyle, 0);
        boxBorderWidth =
                styledAttributes.getInteger(R.styleable.TextBoxView_boxBorderWidth, boxBorderWidth);
        boxColor = styledAttributes.getColor(R.styleable.TextBoxView_boxColor, boxColor);
        boxText = styledAttributes.getString(R.styleable.TextBoxView_boxText);
        textColor = styledAttributes.getColor(R.styleable.TextBoxView_textColor, textColor);
        styledAttributes.recycle();

        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(boxBorderWidth);
        boxPaint.setColor(boxColor);
        box = new Rect();

        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(50.0f);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInitialDraw) {
            box.set(0, 0, getWidth(), getHeight());
            isInitialDraw = false;
        }
        canvas.drawRect(box, boxPaint);

        if (isBeingDragged)
            canvas.drawText(boxText, box.left, box.bottom, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                isBeingDragged = true;
                ClipData clipData = ClipData.newPlainText("text", boxText);
                DragShadowBuilder shadowBuilder = new DragShadowBuilder(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    startDragAndDrop(clipData, shadowBuilder, null, 0);
                else
                    startDrag(clipData, shadowBuilder, null, 0);

                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                isBeingDragged = false;
                break;
            }
        }

        return true;
    }

    public String getText() {
        return boxText;
    }

    public void setText(String text) {
        boxText = text;
    }
}