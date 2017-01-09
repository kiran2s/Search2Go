package com.kssivakumar.search2go;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Hashtable;

public class CropView extends View
{
    private final static String TAG = "CropView";

    public interface OnCropperAdjustedListener {
        void onCropperAdjusted();
    }

    private ArrayList<OnCropperAdjustedListener> listeners = new ArrayList<>();

    private boolean isInitialDraw = true;

    private int rectX = 0;
    private int rectY = 0;
    private int rectWidth = 100;
    private int rectHeight = 100;
    private int rectBorderWidth = 4;
    private int rectColor;

    private Paint rectPaint;
    private Rect rect;

    private int rectFrameAlpha = 100;
    private RectFrame rectFrame;

    private int circleRadius = 25;
    private final float CIRCLE_TOUCH_FACTOR = 2.5f;
    private int circleColor;

    private Paint circlePaint;
    private PointF circle1Pos;
    private PointF circle2Pos;

    private Hashtable<Integer, SingleTouchData> multiTouchData;

    private class RectFrame {
        private final int NUM_RECTS = 4;
        private final int ABOVE_RECT = 0;
        private final int BELOW_RECT = 1;
        private final int LEFT_RECT = 2;
        private final int RIGHT_RECT = 3;
        private Paint paint;
        private Rect[] frame = new Rect[NUM_RECTS];
        private boolean isSet = false;

        public RectFrame(Paint paint) {
            this.paint = paint;
            for (int i = 0; i < NUM_RECTS; i++)
                frame[i] = new Rect();
        }

        public void set(int rectX, int rectY, int rectWidth, int rectHeight) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            int rectBottom = rectY + rectHeight;
            frame[ABOVE_RECT].set(0, 0, viewWidth, rectY);
            frame[BELOW_RECT].set(0, rectBottom, viewWidth, viewHeight);
            frame[LEFT_RECT].set(0, rectY, rectX, rectBottom);
            frame[RIGHT_RECT].set(rectX + rectWidth, rectY, viewWidth, rectBottom);
            isSet = true;
        }

        public boolean draw(Canvas canvas) {
            if (!isSet)
                return false;
            for (Rect frameRect : frame)
                canvas.drawRect(frameRect, paint);
            return true;
        }
    }

    private enum TouchClassification {
        IN_CIRCLE1, IN_CIRCLE2, IN_RECT, OUTSIDE_CROPPER
    }

    private class SingleTouchData {
        private PointF touchPoint;
        private TouchClassification touchClassification;

        public SingleTouchData(PointF touchPoint, TouchClassification touchClassification) {
            this.touchPoint = touchPoint;
            this.touchClassification = touchClassification;
        }

        public PointF getTouchPoint() {
            return touchPoint;
        }

        public TouchClassification getTouchClassification() {
            return touchClassification;
        }
    }

    public CropView(Context context) {
        super(context);
        init(null, 0);
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        rectColor = ResourcesCompat.getColor(getResources(), R.color.colorCropRect2, null);
        circleColor = ResourcesCompat.getColor(getResources(), R.color.colorCropRect, null);

        final TypedArray styledAttributes = getContext().obtainStyledAttributes(
                attrs, R.styleable.CropView, defStyle, 0);

        rectX = styledAttributes.getInteger(R.styleable.CropView_rectX, rectX);
        rectY = styledAttributes.getInteger(R.styleable.CropView_rectY, rectY);
        rectWidth = styledAttributes.getInteger(R.styleable.CropView_rectWidth, rectWidth);
        rectHeight = styledAttributes.getInteger(R.styleable.CropView_rectHeight, rectHeight);
        rectBorderWidth =
                styledAttributes.getInteger(R.styleable.CropView_rectBorderWidth, rectBorderWidth);
        rectColor = styledAttributes.getColor(R.styleable.CropView_rectColor, rectColor);
        rectFrameAlpha =
                styledAttributes.getInteger(R.styleable.CropView_rectFrameAlpha, rectFrameAlpha);
        circleRadius = styledAttributes.getInteger(R.styleable.CropView_circleRadius, circleRadius);
        circleColor = styledAttributes.getColor(R.styleable.CropView_circleColor, circleColor);
        styledAttributes.recycle();

        rectPaint = new Paint();
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(rectBorderWidth);
        rectPaint.setColor(rectColor);
        rect = new Rect();

        Paint rectFramePaint = new Paint();
        rectFramePaint.setColor(Color.argb(rectFrameAlpha, 0, 0, 0));
        rectFrame = new RectFrame(rectFramePaint);

        circlePaint = new Paint();
        circlePaint.setColor(circleColor);
        circle1Pos = new PointF();
        circle2Pos = new PointF();

        multiTouchData = new Hashtable<>(3);
    }

    private void initRectDimensions() {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        rectX = viewWidth / 4;
        rectWidth = rectHeight = viewWidth / 2;
        rectY = (viewHeight - rectHeight) / 2;
    }

    private void invalidateCropper() {
        rect.set(rectX, rectY, rectX + rectWidth, rectY + rectHeight);
        rectFrame.set(rectX, rectY, rectWidth, rectHeight);
        circle1Pos.set(rectX, rectY + rectHeight);
        circle2Pos.set(rectX + rectWidth, rectY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInitialDraw) {
            initRectDimensions();
            invalidateCropper();
            isInitialDraw = false;
        }
        rectFrame.draw(canvas);
        canvas.drawRect(rect, rectPaint);
        canvas.drawCircle(circle1Pos.x, circle1Pos.y, circleRadius, circlePaint);
        canvas.drawCircle(circle2Pos.x, circle2Pos.y, circleRadius, circlePaint);
    }

    public void setOnCropperAdjustedListener(OnCropperAdjustedListener listener) {
        listeners.add(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        int pointerID = event.getPointerId(pointerIndex);
        int maskedAction = event.getActionMasked();

        PointF touchPoint = new PointF(event.getX(pointerIndex), event.getY(pointerIndex));

        switch (maskedAction) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                TouchClassification touchClassification = determineTouchClassification(touchPoint);
                multiTouchData.put(pointerID, new SingleTouchData(touchPoint, touchClassification));
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (pointerIndex = 0; pointerIndex < event.getPointerCount(); pointerIndex++) {
                    pointerID = event.getPointerId(pointerIndex);
                    touchPoint = new PointF(event.getX(pointerIndex), event.getY(pointerIndex));
                    SingleTouchData prevTouch = multiTouchData.get(pointerID);
                    TouchClassification touchClassification = prevTouch.getTouchClassification();
                    if (touchClassification != TouchClassification.OUTSIDE_CROPPER) {
                        PointF prevTouchPoint = prevTouch.getTouchPoint();
                        PointF differenceVector = calculateDifferenceVector(touchPoint, prevTouchPoint);
                        updateRect(differenceVector, touchClassification);
                    }
                    multiTouchData.put(pointerID, new SingleTouchData(touchPoint, touchClassification));
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                multiTouchData.remove(pointerID);
                break;
            }
            case MotionEvent.ACTION_UP: {
                for (OnCropperAdjustedListener listener : listeners)
                    listener.onCropperAdjusted();
                multiTouchData.clear();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                multiTouchData.clear();
                break;
            }
        }

        return true;
    }

    private void updateRect(PointF differenceVector, TouchClassification touchClassification) {
        switch (touchClassification) {
            case IN_CIRCLE1:
            case IN_CIRCLE2:
            {
                int prevRectX = rectX;
                int prevRectY = rectY;
                int prevRectWidth = rectWidth;
                int prevRectHeight = rectHeight;
                int newCircleX;
                int newCircleY;

                if (touchClassification == TouchClassification.IN_CIRCLE1) {
                    PointF newCirclePos = translatePoint(circle1Pos, differenceVector);
                    if (isPointInView(newCirclePos)) {
                        newCircleX = Math.round(newCirclePos.x);
                        newCircleY = Math.round(newCirclePos.y);

                        rectX = newCircleX;
                        rectWidth += prevRectX - newCircleX;
                        rectHeight = newCircleY - prevRectY;
                    }
                }
                else {
                    PointF newCirclePos = translatePoint(circle2Pos, differenceVector);
                    if (isPointInView(newCirclePos)) {
                        newCircleX = Math.round(newCirclePos.x);
                        newCircleY = Math.round(newCirclePos.y);

                        rectY = newCircleY;
                        rectHeight += prevRectY - newCircleY;
                        rectWidth = newCircleX - prevRectX;
                    }
                }

                if (rectWidth < circleRadius) {
                    rectWidth = circleRadius;
                    rectX = prevRectX + prevRectWidth - rectWidth;
                }
                if (rectHeight < circleRadius) {
                    rectHeight = circleRadius;
                    rectY = prevRectY + prevRectHeight - rectHeight;
                }

                break;
            }
            case IN_RECT: {
                PointF newRectPos = translatePoint(new PointF(rectX, rectY), differenceVector);
                rectX = Math.round(newRectPos.x);
                rectY = Math.round(newRectPos.y);
                break;
            }
        }

        performCollisionDetection(touchClassification);
        invalidateCropper();
        invalidate();
    }

    private TouchClassification determineTouchClassification(PointF touchPoint) {
        if (isPointInCircle(touchPoint, circle1Pos, CIRCLE_TOUCH_FACTOR * circleRadius))
            return TouchClassification.IN_CIRCLE1;
        else if (isPointInCircle(touchPoint, circle2Pos, CIRCLE_TOUCH_FACTOR * circleRadius))
            return TouchClassification.IN_CIRCLE2;
        else if (isPointInRect(touchPoint))
            return TouchClassification.IN_RECT;
        else
            return TouchClassification.OUTSIDE_CROPPER;
    }

    private boolean isPointInCircle(PointF point, PointF circlePos, float radius) {
        return calculateDifferenceVector(point, circlePos).length() < radius;
    }

    private boolean isPointInRect(PointF point) {
        float px = point.x;
        float py = point.y;
        return px > rectX && px < rectX + rectWidth && py > rectY && py < rectY + rectHeight;
    }

    private boolean isPointInView(PointF point) {
        return point.x >= 0 && point.x < getWidth() && point.y >= 0 && point.y < getHeight();
    }

    private PointF calculateDifferenceVector(PointF p1, PointF p2) {
        return new PointF(p1.x - p2.x, p1.y - p2.y);
    }

    private PointF translatePoint(PointF point, PointF vector) {
        return new PointF(point.x + vector.x, point.y + vector.y);
    }

    private void performCollisionDetection(TouchClassification touchClassification) {
        if (rectX < 0)
            rectX = 0;
        if (rectY < 0)
            rectY = 0;

        int viewWidth = getWidth();
        int viewHeight = getHeight();
        switch (touchClassification) {
            case IN_CIRCLE1:
            case IN_CIRCLE2: {
                if (rectX + rectWidth > viewWidth)
                    rectWidth = viewWidth - rectX;
                if (rectY + rectHeight > viewHeight)
                    rectHeight = viewHeight - rectY;
                break;
            }
            case IN_RECT: {
                if (rectX + rectWidth > viewWidth)
                    rectX = viewWidth - rectWidth;
                if (rectY + rectHeight > viewHeight)
                    rectY = viewHeight - rectHeight;
                break;
            }
        }
    }

    /*** Getters and Setters ***/
    public int getRectX() {
        return rectX;
    }

    public int getRectY() {
        return rectY;
    }

    public int getRectWidth() {
        return rectWidth;
    }


    public int getRectHeight() {
        return rectHeight;
    }
}
