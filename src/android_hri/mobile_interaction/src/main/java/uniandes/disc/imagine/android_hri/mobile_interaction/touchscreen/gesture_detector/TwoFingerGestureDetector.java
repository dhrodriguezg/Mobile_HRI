package uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen.gesture_detector;

import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by Diego Rodriguez on 25/07/2015.
 */
public class TwoFingerGestureDetector extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    private static final String TAG = "TwoFingerGesture";

    private OnTwoFingerGestureListener mListener;
    private ScaleGestureDetector mGestureDetector;
    private Activity mActivity;
    private View mView;

    private static final int INVALID_POINTER_ID = -1;
    private static final int DRAGGING_THREASHOLD = 20;
    private static final int ROTATING_THREASHOLD = 10;
    private static final int FAST_SCALING_THREASHOLD = 200;
    public static final float MAX_DRAGGING_DISTANCE=0.2f;
    public static float MAX_RESOLUTION=0;

    public static final float MIN_SCALE = .2f;
    public static final float MAX_SCALE = 5.f;
    public static final float MIN_GRASP = 10f;
    public static final float MAX_GRASP = 50f;

    private boolean detectingGesture =false;
    private float fX, fY, sX, sY;
    private int ptrID1, ptrID2;

    private float mAngle;
    private float mX, mY;
    private float normalizedX;
    private float normalizedY;
    private float mScale = 1.f;
    private float mScaleFocusX = 0.f;
    private float mScaleFocusY = 0.f;

    private float initScale = mScale;
    private float endScale = mScale;
    private float grasp = mScale;
    private long initTime=0;
    private long endTime=0;

    private boolean enableRotating=false;
    private boolean enableDragging=false;
    private boolean enableScaling=false;

    private boolean rotating;
    private boolean dragging;
    private boolean scaling;


    public TwoFingerGestureDetector(Activity activity, OnTwoFingerGestureListener listener){
        mGestureDetector = new ScaleGestureDetector(activity, this);
        mActivity = activity;
        mListener = listener;
        ptrID1 = INVALID_POINTER_ID;
        ptrID2 = INVALID_POINTER_ID;
    }

    /** Low-level events **/

    public boolean onTouchEvent(View view, MotionEvent event){
        mView = view;
        calculateGestures(view,event);
        return mGestureDetector.onTouchEvent(event);
    }

    private boolean calculateGestures(View view, MotionEvent event) {

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                ptrID1 = event.getPointerId(event.getActionIndex());
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                scaling=enableScaling;
                rotating=enableRotating;
                dragging=enableDragging;
                detectingGesture = true;
                ptrID2 = event.getPointerId(event.getActionIndex());
                sX = event.getX(event.findPointerIndex(ptrID1));
                sY = event.getY(event.findPointerIndex(ptrID1));
                fX = event.getX(event.findPointerIndex(ptrID2));
                fY = event.getY(event.findPointerIndex(ptrID2));

                double distance = Math.sqrt(Math.pow(sX - fX,2) + Math.pow(sY - fY, 2));
                if (distance > MAX_DRAGGING_DISTANCE*MAX_RESOLUTION)
                    dragging=false;

                mListener.onTwoFingerGestureState(detectingGesture);
                Log.d(TAG, String.format("Detecting [ %b ]", detectingGesture));
                break;
            case MotionEvent.ACTION_MOVE:
                if (ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID) {
                    detectingGesture = true;
                    float nfX, nfY, nsX, nsY, nX, nY;
                    nsX = event.getX(event.findPointerIndex(ptrID1));
                    nsY = event.getY(event.findPointerIndex(ptrID1));
                    nfX = event.getX(event.findPointerIndex(ptrID2));
                    nfY = event.getY(event.findPointerIndex(ptrID2));

                    if(dragging){
                        mX=(nsX+nfX)/2; mY=(nsY+nfY)/2;

                        float[] normalizedXY = normalizedValues(mX,mY,mView);
                        normalizedX = normalizedXY[0];
                        normalizedY = normalizedXY[1];

                        nX=(sX+fX)/2; nY=(sY+fY)/2;
                        if(Math.hypot(mX-nX,mY-nY) > DRAGGING_THREASHOLD){
                            rotating=false;
                            scaling=false;
                            mListener.OnDoubleDrag(mX, mY, normalizedX, normalizedY);
                            Log.d(TAG, String.format("Drag [ %.4f , %.4f ]", mX,mY));
                        }
                    }

                    if(rotating){
                        mAngle = angleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY);
                        if(Math.abs(mAngle) > ROTATING_THREASHOLD){
                            scaling=false;
                            dragging=false;
                            mListener.OnRotation(mAngle);
                            Log.d(TAG, String.format("Angle [ %.4f ]", mAngle));
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                ptrID1 = INVALID_POINTER_ID;
                detectingGesture = false;
                scaling=enableScaling;
                rotating=enableRotating;
                dragging=enableDragging;
                mListener.onTwoFingerGestureState(detectingGesture);
                Log.d(TAG, String.format("Detecting [ %b ]", detectingGesture));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                ptrID2 = INVALID_POINTER_ID;
                detectingGesture = false;
                scaling=enableScaling;
                rotating=enableRotating;
                dragging=enableDragging;
                mListener.onTwoFingerGestureState(detectingGesture);
                Log.d(TAG, String.format("Detecting [ %b ]", detectingGesture));
                break;
            case MotionEvent.ACTION_CANCEL:
                detectingGesture = false;
                scaling=enableScaling;
                rotating=enableRotating;
                dragging=enableDragging;
                ptrID1 = INVALID_POINTER_ID;
                ptrID2 = INVALID_POINTER_ID;
                mListener.onTwoFingerGestureState(detectingGesture);
                Log.d(TAG, String.format("Detecting [ %b ]", detectingGesture));
                break;
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector){
        if(scaling) {
            initTime = detector.getEventTime();
            initScale = mScale;
            rotating=false;
            dragging=false;
        }
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector){
        if(scaling) {
            mScaleFocusX=detector.getFocusX();
            mScaleFocusY=detector.getFocusY();
            grasp *= detector.getScaleFactor();
            mScale *= detector.getScaleFactor();
            grasp = Math.max(MIN_GRASP, Math.min(grasp, MAX_GRASP));
            mScale = Math.max(MIN_SCALE, Math.min(mScale, MAX_SCALE));
            mListener.OnScale1(mScale, mScaleFocusX, mScaleFocusY);
            mListener.OnScale2(grasp);
            Log.d(TAG, String.format("Scale [ %.4f %.4f %.4f]", mScale,mScaleFocusX,mScaleFocusY));
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector){
        if(scaling) {
            endTime=detector.getEventTime();
            endScale = mScale;
            if( endTime-initTime < FAST_SCALING_THREASHOLD){
                //Fast Pinch
                if(endScale > initScale){
                    mScale = MAX_SCALE;
                    grasp = MAX_GRASP;
                }else if(endScale < initScale ){
                    mScale = MIN_SCALE;
                    grasp = MIN_GRASP;
                }
                mScaleFocusX=detector.getFocusX();
                mScaleFocusY=detector.getFocusY();
                mListener.OnScale1(mScale,mScaleFocusX,mScaleFocusY);
                mListener.OnScale2(grasp);
                Log.d(TAG, String.format("Scale [ %.4f %.4f %.4f]", mScale,mScaleFocusX,mScaleFocusY));
            }
        }
    }

    /** Gesture interfaces **/

    public interface OnTwoFingerGestureListener {
        void OnDoubleDrag(float mX, float mY, float normalizedX, float normalizedY);
        void OnRotation(float mAngle);
        void OnScale1(float mScale, float mScaleFocusX, float mScaleFocusY);
        void OnScale2(float grasp);
        void onTwoFingerGestureState(boolean detectingGesture);
    }

    /** Normalized values **/

    private float[] normalizedValues(float mX, float mY, View view){
        float[] touch = new float[]{mX,mY}; //relative to the view. Right,Down increase
        float[] viewCenter = new float[]{view.getWidth() / 2, view.getHeight() / 2};
        float[] touchVector = new float[] {touch[0] - viewCenter[0], touch[1] - viewCenter[1]};
        touchVector[0]/=  viewCenter[0];
        touchVector[1]/= -viewCenter[1];
        if(touchVector[0] > 1 || touchVector[0] < -1 || touchVector[1] > 1 || touchVector[1] < -1){
            touchVector[0] = 0;
            touchVector[1] = 0;
        }
        return touchVector;
    }

    private float angleBetweenLines (float fX, float fY, float sX, float sY, float nfX, float nfY, float nsX, float nsY) {
        float angle1 = (float) Math.atan2((fY - sY), (fX - sX));
        float angle2 = (float) Math.atan2((nfY - nsY), (nfX - nsX));

        float angle = ((float) Math.toDegrees(angle1 - angle2)) % 360;
        if (angle < -180.f) angle += 360.0f;
        if (angle > 180.f) angle -= 360.0f;
        return angle;
    }

    public void disableScaling(){
        enableScaling=false;
    }

    public void disableRotating(){
        enableRotating=false;
    }

    public void disableDragging(){
        enableDragging=false;
    }

    public void enableScaling(){
        enableScaling=true;
    }

    public void enableRotating(){
        enableRotating=true;
    }

    public void enableDragging(){
        enableDragging=true;
    }

}
