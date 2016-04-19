package uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen.gesture_detector.TwoFingerGestureDetector;

/**
 * Created by DarkNeoBahamut on 23/07/2015.
 */
public class MultiTouchArea extends TouchArea implements TwoFingerGestureDetector.OnTwoFingerGestureListener {

    private static final String TAG = "MultiTouchArea";

    private TwoFingerGestureDetector mTwoFingerGestureDetector;
    protected float angle = 0.f;
    protected float scale = 1.f;
    protected float graspScale = 1.f;
    protected float scaleFocusX = 0.f;
    protected float scaleFocusY = 0.f;
    protected float doubleDragX = 0.f;
    protected float doubleDragY = 0.f;
    protected float doubleNormalizedDragX = 0.f;
    protected float doubleNormalizedDragY = 0.f;
    protected boolean detectingTwoFingerGesture = false;
    protected boolean enableTwoFingerGestures = true;

    public MultiTouchArea(Activity activity, ImageView view) {
        super(activity, view);
    }

    @Override
    protected void setupListener() {
        mTwoFingerGestureDetector = new TwoFingerGestureDetector(activity, this);
        enableOneFingerGestures = false;
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                addOneFingerListener(view, event);
                addTwoFingerListener(view, event);
                return true;
            }
        });
    }

    protected void addTwoFingerListener(View view, MotionEvent event) {
        if(enableTwoFingerGestures)
            mTwoFingerGestureDetector.onTouchEvent(view, event);
    }



    @Override
    public void OnRotation(float mAngle) {
        angle = mAngle;
    }

    @Override
    public void OnDoubleDrag(float mX, float mY, float normalizedX, float normalizedY) {
        doubleDragX = mX;
        doubleDragY = mY;
        doubleNormalizedDragX = normalizedX;
        doubleNormalizedDragY = normalizedY;
    }

    @Override
    public void OnScale1(float mScale, float mScaleFocusX, float mScaleFocusY) {
        scale=mScale;
        scaleFocusX=mScaleFocusX;
        scaleFocusY=mScaleFocusY;
    }

    @Override
    public void OnScale2(float grasp) {
        graspScale=grasp;
    }

    @Override
    public void onTwoFingerGestureState(boolean detectingGesture) {
        detectingTwoFingerGesture =detectingGesture;
    }

    public float getGraspScale() {
        return graspScale;
    }

    public void setGraspScale(float graspScale) {
        this.graspScale = graspScale;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public float getDoubleDragX() {
        return doubleDragX;
    }

    public void setDoubleDragX(float doubleDragX) {
        this.doubleDragX = doubleDragX;
    }

    public float getDoubleDragY() {
        return doubleDragY;
    }

    public void setDoubleDragY(float doubleDragY) {
        this.doubleDragY = doubleDragY;
    }

    public float getDoubleNormalizedDragX() {
        return doubleNormalizedDragX;
    }

    public void setDoubleNormalizedDragX(float doubleNormalizedDragX) {
        this.doubleNormalizedDragX = doubleNormalizedDragX;
    }

    public float getDoubleNormalizedDragY() {
        return doubleNormalizedDragY;
    }

    public void setDoubleNormalizedDragY(float doubleNormalizedDragY) {
        this.doubleNormalizedDragY = doubleNormalizedDragY;
    }

    public boolean isDetectingTwoFingerGesture() {
        return detectingTwoFingerGesture;
    }

    public void setDetectingTwoFingerGesture(boolean detectingTwoFingerGesture) {
        this.detectingTwoFingerGesture = detectingTwoFingerGesture;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getScaleFocusX() {
        return scaleFocusX;
    }

    public void setScaleFocusX(float scaleFocusX) {
        this.scaleFocusX = scaleFocusX;
    }

    public float getScaleFocusY() {
        return scaleFocusY;
    }

    public void setScaleFocusY(float scaleFocusY) {
        this.scaleFocusY = scaleFocusY;
    }

    public void disableScaling(){
        mTwoFingerGestureDetector.disableScaling();
    }

    public void disableRotating(){
        mTwoFingerGestureDetector.disableRotating();
    }

    public void disableDragging(){
        mTwoFingerGestureDetector.disableDragging();
    }

    public void enableScaling(){
        mTwoFingerGestureDetector.enableScaling();
    }

    public void enableRotating(){
        mTwoFingerGestureDetector.enableRotating();
    }

    public void enableDragging(){
        mTwoFingerGestureDetector.enableDragging();
    }

    public void enableTwoFingerGestures(){
        enableTwoFingerGestures=true;
    }

    public void disableTwoFingerGestures(){
        enableTwoFingerGestures=false;
    }
}
