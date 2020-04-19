package com.example.pdfreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

@SuppressLint("AppCompatCustomView")
public class PDFImage extends ImageView {

    Model model = Model.getInstance();

    // image to display
    Bitmap bitmap;

    private float mPositionX;
    private float mPositionY;
    private float mLastTouchX;
    private float mLastTouchY;

    private GestureDetector mGestureDetector;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.0f;
    private final static float mMinZoom = 1.0f;
    private final static float mMaxZoom = 5.0f;



    public PDFImage(Context context) {
        super(context);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (model.getTool().equals(Tool.Hand)) {
                if (detector.getScaleFactor() * mScaleFactor >= mMaxZoom) return true;
                if (detector.getScaleFactor() * mScaleFactor <= mMinZoom) return true;
                mPositionX = (mPositionX - detector.getFocusX()) * detector.getScaleFactor() + detector.getFocusX();
                mPositionY = (mPositionY - detector.getFocusY()) * detector.getScaleFactor() + detector.getFocusY();
                mScaleFactor *= detector.getScaleFactor();
                mScaleFactor = Math.max(mMinZoom, Math.min(mMaxZoom, mScaleFactor));
                return true;
            }
            return false;
        }
    }
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (model.getTool().equals(Tool.Hand)) {
                mPositionX = 0;
                mPositionY = 0;
                mScaleFactor = 1;
                return true;
            }
            return false;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {

        boolean result1 = mScaleDetector.onTouchEvent(event);
        boolean result2 = mGestureDetector.onTouchEvent(event);

        boolean result3 = false;

        Tool curTool = model.getTool();
        switch (curTool) {
            case Draw:
            case Highlight:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        model.initPath((event.getX() - mPositionX) / mScaleFactor, (event.getY() - mPositionY) / mScaleFactor);
                        result3 = true;
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        model.updatePath((event.getX() - mPositionX) / mScaleFactor, (event.getY() - mPositionY) / mScaleFactor);
                        result3 = true;
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        model.cleanupPath((event.getX() - mPositionX) / mScaleFactor, (event.getY() - mPositionY) / mScaleFactor);
                        result3 = true;
                        break;
                    }
                }
                break;
            case Erase:
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    model.erase((event.getX() - mPositionX) / mScaleFactor, (event.getY() - mPositionY) / mScaleFactor);
                    result3 = true;
                }
                break;
            case Hand:
                if (!mScaleDetector.isInProgress()) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            final float x = event.getX();
                            final float y = event.getY();
                            mLastTouchX = x;
                            mLastTouchY = y;
                            result3 = true;
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            final float x = event.getX();
                            final float y = event.getY();
                            final float distanceX = x - mLastTouchX;
                            final float distanceY = y - mLastTouchY;

                            mPositionX += distanceX;
                            mPositionY += distanceY;
                            mPositionX = Math.min(0, Math.max(getWidth() - getWidth() * mScaleFactor, mPositionX));
                            mPositionY = Math.min(0, Math.max(getHeight() - getHeight() * mScaleFactor, mPositionY));
                            mLastTouchX = x;
                            mLastTouchY = y;
                            result3 = true;
                            break;
                        }
                    }
                    break;
                }

        }
        return result1 || result2 || result3;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        bitmap = model.getCurrentPage();
        canvas.translate(mPositionX, mPositionY);
        canvas.scale(mScaleFactor, mScaleFactor);
        super.onDraw(canvas);
        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }
        // draw lines over it
        for (Pair<ArrayList<Pair<Float, Float>>, Paint> edit : model.getCurrentPoints()) {
            Path path = null;
            for (Pair<Float, Float> coordinate : edit.first) {
                if (path == null) {
                    path = new Path();
                    path.moveTo(coordinate.first,coordinate.second);
                }
                else {
                    path.lineTo(coordinate.first,coordinate.second);
                }
            }
            canvas.drawPath(path, edit.second);
        }
    }
}
