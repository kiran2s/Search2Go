package com.kssivakumar.search2go;

import android.app.Activity;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;

public class CameraTextRecognizerProcessor
        <CallbackActivity extends Activity & CameraTextRecognizerProcessor.Callback>
        implements Detector.Processor<TextBlock>
{
    private static final String TAG = "CameraProcessor";
    private CallbackActivity callbackActivity;

    public CameraTextRecognizerProcessor(CallbackActivity callbackActivity) {
        this.callbackActivity = callbackActivity;
    }

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        final SparseArray<TextBlock> textBlockDetections = detections.getDetectedItems();
        boolean textDetected = false;
        for (int i = 0; i < textBlockDetections.size(); i++) {
            TextBlock textBlock = textBlockDetections.valueAt(i);
            if (textBlock != null && textBlock.getValue() != null)
                textDetected = true;
        }
        final boolean detectionResult = textDetected;
        callbackActivity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        callbackActivity.textDetectionUpdated(detectionResult);
                    }
                }
        );
    }

    @Override
    public void release() {}

    public interface Callback {
        void textDetectionUpdated(boolean textDetected);
    }
}
