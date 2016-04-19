package uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by DarkNeoBahamut on 23/07/2015.
 */
public class MultiGestureArea extends GestureDetector.SimpleOnGestureListener{

    private static final String TAG = "MultiGestureArea";

    private Activity mActivity;
    private ImageView mView;
    private GestureDetector mGestureDetector;
    private static final int INVALID_POINTER_ID = -1;
    private static final int INVALID = -1;
    private static final int FAST_GRASPING_TIME_THREASHOLD = 300;//ms
    private static final int FAST_GRASPING_DISTANCE_THREASHOLD = 100;//dpi
    private static final float REF_DISTANCE_DIP = 600;
    private Vibrator vibrator;

    private static final float MAX_GRASP = 2.f;
    private static final float MIN_GRASP = .1f;

    private boolean detectingGesture =false;
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

    private long initTime=0;
    private long endTime=0;
    private boolean synced;
    private boolean isTarget;
    private float syncX;
    private float syncY;

    public MultiGestureArea(Activity activity, ImageView view){
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
                detectingGesture = true;
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
                initGrasp = getGrasp();
                initDistance = (float) Math.sqrt(Math.pow(sX - fX, 2) + Math.pow(sY - fY, 2));
                initDistance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, initDistance, mActivity.getResources().getDisplayMetrics());
                if(synced){
                    synced =false;
                    initPosX = syncX;
                    initPosY = syncY;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID) {
                    detectingGesture = true;
                    nsX = event.getX(event.findPointerIndex(ptrID1));
                    nsY = event.getY(event.findPointerIndex(ptrID1));
                    nfX = event.getX(event.findPointerIndex(ptrID2));
                    nfY = event.getY(event.findPointerIndex(ptrID2));

                    //Dragging
                    currPosX = initPosX + (nsX-sX+nfX-fX)/2f;
                    currPosY = initPosY + (nsY-sY+nfY-fY)/2f;
                    //Rotating
                    currAngle = initAngle + getAngleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY);

                    //Grasping
                    currDistance = (float) Math.sqrt(Math.pow(nsX - nfX, 2) + Math.pow(nsY - nfY, 2));
                    currDistance = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, currDistance, mActivity.getResources().getDisplayMetrics());
                    float normalizedDistance = (currDistance-initDistance)/REF_DISTANCE_DIP;
                    currGrasp = Math.max(MIN_GRASP,Math.min(initGrasp - normalizedDistance,MAX_GRASP));

                    setPosX(currPosX); setPosY(currPosY);
                    setRotation(currAngle);
                    setGrasp(currGrasp);
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
                    detectingGesture = false;
                    if( endTime-initTime < FAST_GRASPING_TIME_THREASHOLD){
                        if(currDistance-initDistance > FAST_GRASPING_DISTANCE_THREASHOLD)
                            currGrasp = MIN_GRASP;
                        else if(initDistance-currDistance > FAST_GRASPING_DISTANCE_THREASHOLD)
                            currGrasp = MAX_GRASP;
                        setGrasp(currGrasp);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                ptrID1 = INVALID_POINTER_ID;
                ptrID2 = INVALID_POINTER_ID;
                detectingGesture = false;
                break;
        }

    }

    @Override
    public void onLongPress(MotionEvent event) {
        targetX =event.getX();
        targetY =event.getY();
        vibrator.vibrate(200);
        isTarget = true;
    }

    public void resetTarget(){
        isTarget = false;
    }

    public boolean isTargetSelected(){
        return isTarget;
    }

    private float getAngleBetweenLines (float fX, float fY, float sX, float sY, float nfX, float nfY, float nsX, float nsY) {
        float angle1 = (float) Math.atan2((fY - sY), (fX - sX));
        float angle2 = (float) Math.atan2((nfY - nsY), (nfX - nsX));

        float angle = ((float) Math.toDegrees(angle1 - angle2)) % 360;
        if (angle < -180.f) angle += 360.0f;
        if (angle > 180.f) angle -= 360.0f;
        return angle;
    }

    public void syncPos(float x, float y){
        synced = true;
        syncX =x;
        syncY =y;
    }

    public float getTargetX() {
        return targetX;
    }

    public void setTargetX(float targetX) {
        this.targetX = targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public void setTargetY(float targetY) {
        this.targetY = targetY;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public void setIsTarget(boolean isTarget) {
        this.isTarget = isTarget;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public float getGrasp() {
        return grasp;
    }

    public void setGrasp(float grasp) {
        this.grasp = grasp;
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

    public boolean isDetectingGesture() {
        return detectingGesture;
    }

    public void setDetectingGesture(boolean detectingGesture) {
        this.detectingGesture = detectingGesture;
    }

}
