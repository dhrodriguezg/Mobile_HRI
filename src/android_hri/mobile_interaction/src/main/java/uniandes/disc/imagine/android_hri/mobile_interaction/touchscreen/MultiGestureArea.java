package uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by DarkNeoBahamut on 23/07/2015.
 */
public class MultiGestureArea extends GestureDetector.SimpleOnGestureListener{

    private static final String TAG = "MultiGestureArea";

    private Activity mActivity;
    private View mView;
    private GestureDetector mGestureDetector;
    private static final int INVALID_POINTER_ID = -1;

    private boolean detectingMultiGesture = false;
    private boolean detectingGesture = false;
    private boolean isLongPress;
    private boolean isDoubleTap;
    private boolean isFling;

    private float fX, sX, sY;
    private float nsY;
    private int ptrID1, ptrID2;

    private float steer;
    private float throttle;

    private float flingX;
    private float flingY;

    public MultiGestureArea(Activity activity, View view){
        mGestureDetector = new GestureDetector(activity, this);
        ptrID1 = INVALID_POINTER_ID;
        ptrID2 = INVALID_POINTER_ID;
        steer = 0f;
        mActivity = activity;
        mView = view;
        setupListener();
    }

    private void setupListener(){
        mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                calculateGestures(view, event);
                mGestureDetector.onTouchEvent(event);
                return true;
            }
        });
    }

    private void calculateGestures(View view, MotionEvent event) {

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                throttle = 0.f;
                ptrID1 = event.getPointerId(event.getActionIndex());
                sY = event.getY(event.findPointerIndex(ptrID1));
                detectingMultiGesture = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                detectingMultiGesture = true;
                if(ptrID1 == INVALID_POINTER_ID || ptrID2 != INVALID_POINTER_ID) //to ignore 3 or more finger inputs
                    return;
                ptrID2 = event.getPointerId(event.getActionIndex());
                sX = event.getX(event.findPointerIndex(ptrID1));
                fX = event.getX(event.findPointerIndex(ptrID2));
                steer = (sX-fX)/360f;
                break;
            case MotionEvent.ACTION_MOVE:
                if ( ptrID1 != INVALID_POINTER_ID ) {
                    detectingMultiGesture = true;
                    nsY = event.getY(event.findPointerIndex(ptrID1));
                    throttle = nsY-sY;
                    if ( ptrID2 != INVALID_POINTER_ID ) {
                        sX = event.getX(event.findPointerIndex(ptrID1));
                        fX = event.getX(event.findPointerIndex(ptrID2));
                        steer = (sX-fX)/360f;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                int id = event.getPointerId(event.getActionIndex());
                if(id==ptrID1){
                    throttle = 0.f;
                    ptrID1 = INVALID_POINTER_ID;
                    detectingMultiGesture = false;
                }
                if(id==ptrID2){
                    ptrID2 = INVALID_POINTER_ID;
                    steer =0.f;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                ptrID1 = INVALID_POINTER_ID;
                ptrID2 = INVALID_POINTER_ID;
                throttle = 0.f;
                steer =0.f;
                detectingMultiGesture = false;
                break;
        }

    }

    @Override
    public void onLongPress(MotionEvent event) {
        //detectingGesture = true;
        //isLongPress = true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        detectingGesture = true;
        isDoubleTap = true;
        return true;
    }

    @Override
    public boolean onFling(MotionEvent initial_event, MotionEvent current_event, float velocityX, float velocityY) {
        detectingGesture = true;
        isFling = true;
        if( Math.abs(velocityX) > Math.abs(velocityY) ){
            flingX = velocityX > 0f ? 1.f : -1.f;
            flingY = 0.f;
        }else{
            flingX = 0.f;
            flingY = velocityY > 0f ? 1.f : -1.f;
        }
        return true;
    }

    public float getSteer() {
        return steer;
    }

    public float getThrottle() {
        return throttle;
    }

    public void setThrottle(float throttle) {
        this.throttle = throttle;
    }

    public boolean isDetectingMultiGesture() {
        return detectingMultiGesture;
    }

    public void setDetectingMultiGesture(boolean detectingMultiGesture) {
        this.detectingMultiGesture = detectingMultiGesture;
    }

    public boolean isDetectingGesture() {
        return detectingGesture;
    }

    public void setDetectingGesture(boolean detectingGesture) {
        this.detectingGesture = detectingGesture;
    }

    public boolean isFling() {
        return isFling;
    }

    public void setFling(boolean isFling) {
        this.isFling = isFling;
    }

    public boolean isLongPress() {
        return isLongPress;
    }

    public void setLongPress(boolean isLongPress) {
        this.isLongPress = isLongPress;
    }

    public boolean isDoubleTap() {
        return isDoubleTap;
    }

    public void setDoubleTap(boolean isDoubleTap) {
        this.isDoubleTap = isDoubleTap;
    }

    public float getFlingX() {
        return flingX;
    }

    public void setFlingX(float flingX) {
        this.flingX = flingX;
    }

    public float getFlingY() {
        return flingY;
    }

    public void setFlingY(float flingY) {
        this.flingY = flingY;
    }

}
