package uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.TypedValue;
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
    private static final int INVALID = -1;
    private static final int FAST_GRASPING_TIME_THREASHOLD = 300;//ms
    private static final int FAST_GRASPING_DISTANCE_THREASHOLD = 100;//dpi
    private static final float REF_DISTANCE_DIP = 600;
    private Vibrator vibrator;

    private static final float MAX_GRASP = 2.f;
    private static final float MIN_GRASP = .1f;

    private boolean detectingMultiGesture = false;
    private boolean detectingGesture = false;
    private float fX, fY, sX, sY;
    private float nfX, nfY, nsX, nsY;
    private int ptrID1, ptrID2;

    private float initDistance;
    private float initAngle;
    private float initPosX;
    private float initPosY;
    private float initGrasp;
    private float currDistance;
    private float currAngle;
    private float currPosX;
    private float currPosY;
    private float currGrasp;

    private float rotation;
    private float grasp;
    private float posX;
    private float posY;
    private float targetX;
    private float targetY;

    private float flingX;
    private float flingY;

    private long initTime=0;
    private long endTime=0;
    private boolean synced;
    private boolean isTarget;

    private boolean isLongPress;
    private boolean isDoubleTap;
    private boolean isFling;

    public MultiGestureArea(Activity activity, View view){
        vibrator = (Vibrator) activity.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        mGestureDetector = new GestureDetector(activity, this);
        ptrID1 = INVALID_POINTER_ID;
        ptrID2 = INVALID_POINTER_ID;
        currPosX = INVALID;
        currPosY = INVALID;
        grasp = MAX_GRASP;
        isTarget = false;
        synced = false;
        rotation = 0f;
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
                ptrID1 = event.getPointerId(event.getActionIndex());
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                detectingMultiGesture = true;
                if(ptrID1 == INVALID_POINTER_ID || ptrID2 != INVALID_POINTER_ID) //to ignore 3 or more finger inputs
                    return;
                ptrID2 = event.getPointerId(event.getActionIndex());
                sX = event.getX(event.findPointerIndex(ptrID1));
                sY = event.getY(event.findPointerIndex(ptrID1));
                fX = event.getX(event.findPointerIndex(ptrID2));
                fY = event.getY(event.findPointerIndex(ptrID2));

                initTime =  event.getEventTime();
                initPosX = getPosX();
                initPosY = getPosY();
                initAngle = getRotation();
                initDistance = (float) Math.sqrt(Math.pow(sX - fX, 2) + Math.pow(sY - fY, 2));
                initDistance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, initDistance, mActivity.getResources().getDisplayMetrics());
                break;
            case MotionEvent.ACTION_MOVE:
                if (ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID) {
                    detectingMultiGesture = true;
                    nsX = event.getX(event.findPointerIndex(ptrID1));
                    nsY = event.getY(event.findPointerIndex(ptrID1));
                    nfX = event.getX(event.findPointerIndex(ptrID2));
                    nfY = event.getY(event.findPointerIndex(ptrID2));

                    //Dragging
                    currPosX = (nsX-sX+nfX-fX)/2f;
                    currPosY = (nsY-sY+nfY-fY)/2f;
                    //Rotating
                    currAngle = getAngleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY);

                    setPosX(currPosX); setPosY(currPosY);
                    setRotation(currAngle);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                int id = event.getPointerId(event.getActionIndex());
                endTime =  event.getEventTime();
                if(id==ptrID1)
                    ptrID1 = INVALID_POINTER_ID;
                if(id==ptrID2)
                    ptrID2 = INVALID_POINTER_ID;

                if(ptrID1 == INVALID_POINTER_ID || ptrID2 == INVALID_POINTER_ID){
                    detectingMultiGesture = false;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                ptrID1 = INVALID_POINTER_ID;
                ptrID2 = INVALID_POINTER_ID;
                detectingMultiGesture = false;
                break;
        }

    }

    @Override
    public void onLongPress(MotionEvent event) {
        detectingGesture = true;
        isLongPress = true;
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


    private float getAngleBetweenLines (float fX, float fY, float sX, float sY, float nfX, float nfY, float nsX, float nsY) {
        float angle1 = (float) Math.atan2((fY - sY), (fX - sX));
        float angle2 = (float) Math.atan2((nfY - nsY), (nfX - nsX));

        float angle = ((float) Math.toDegrees(angle1 - angle2)) % 360;
        if (angle < -180.f) angle += 360.0f;
        if (angle > 180.f) angle -= 360.0f;
        return angle;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
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
