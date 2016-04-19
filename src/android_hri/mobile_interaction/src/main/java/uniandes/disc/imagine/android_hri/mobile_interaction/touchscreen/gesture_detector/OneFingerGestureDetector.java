package uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen.gesture_detector;

import android.app.Activity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Diego Rodriguez on 25/07/2015.
 */
public class OneFingerGestureDetector extends GestureDetector.SimpleOnGestureListener {

    private static final String TAG = "OneFingerGesture";

    private OnOneFingerGestureListener mListener;
    private GestureDetector mGestureDetector;
    private Activity mActivity;
    private View mView;
    private boolean detectingGesture =false;

    //onSingleTapUp -> one single update
    private float stX;
    private float stY;
    private float normalizedSTX;
    private float normalizedSTY;
    private boolean enableSingleTapUp=false;

    //onDoubleTap -> one single update
    private float dtX;
    private float dtY;
    private float normalizedDTX;
    private float normalizedDTY;
    private boolean enableDoubleTap=false;

    //onScroll -> constant updating, relative distance
    private float sX;
    private float sY;
    private float sdX;
    private float sdY;
    private float dX, dY;
    private float normalizedDX;
    private float normalizedDY;
    private boolean enableScroll=false;

    //onFling -> one single update, absolute velocity
    private float fX;
    private float fY;
    private float fvX;
    private float fvY;
    private boolean enableFling=false;

    //onLongPress -> one single update.
    private float lpX;
    private float lpY;
    private float normalizedLPX;
    private float normalizedLPY;
    private boolean enableLongPress=false;

    private boolean  detectingMultiGesture=false;

    public OneFingerGestureDetector(Activity activity, OnOneFingerGestureListener listener){
        mGestureDetector = new GestureDetector(activity, this);
        mActivity = activity;
        mListener = listener;
    }

    /** Low-level events **/
    public boolean onTouchEvent(View view, MotionEvent event){
        mView = view;
        checkGestureState(event);
        return mGestureDetector.onTouchEvent(event);
    }

    private void checkGestureState(MotionEvent event){
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                detectingGesture = true;
                detectingMultiGesture=false;
                mListener.onOneFingerGestureState(detectingGesture);
                Log.d(TAG, String.format("Detecting [ %b ]", detectingGesture));
                break;
            case MotionEvent.ACTION_UP:
                detectingGesture = false;
                mListener.onOneFingerGestureState(detectingGesture);
                Log.d(TAG, String.format("Detecting [ %b ]", detectingGesture));
                break;
            case MotionEvent.ACTION_POINTER_DOWN: //second finger detected, stop
                detectingMultiGesture=true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                //detectingMultiGesture=false;
                break;
        }
    }

    @Override
    public boolean onDown(MotionEvent event){
        return super.onDown(event);
    }

    @Override
    public void onShowPress(MotionEvent event){
        super.onShowPress(event);
    }


    /** Gesture events **/
    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        if(!enableSingleTapUp)
            return true;
        stX =event.getX(); stY =event.getY();
        float[] normalizedXY = normalizedValues(event,mView);
        normalizedSTX = normalizedXY[0];
        normalizedSTY = normalizedXY[1];
        mListener.onSingleTap(stX, stY, normalizedSTX, normalizedSTY);
        Log.d(TAG, String.format("SingleTap [ %.1f , %.1f , %.4f , %.4f ]", stX, stY, normalizedSTX, normalizedSTY));
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if(!enableDoubleTap)
            return true;
        if(detectingMultiGesture)
            return true;
        dtX =event.getX(); dtY =event.getY();
        float[] normalizedXY = normalizedValues(event,mView);
        normalizedDTX = normalizedXY[0];
        normalizedDTY = normalizedXY[1];
        mListener.onDoubleTap(dtX, dtY, normalizedDTX, normalizedDTY);
        Log.d(TAG, String.format("DoubleTap [ %.1f , %.1f , %.4f , %.4f ]", dtX, dtY, normalizedDTX, normalizedDTY));
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent initial_event, MotionEvent current_event, float distanceX, float distanceY) {
        if(!enableScroll)
            return true;
        if(detectingMultiGesture)
            return true;
        sX =initial_event.getX(); sY =initial_event.getY();
        sdX = distanceX; sdY=distanceY;

        dX=current_event.getX(); dY=current_event.getY();
        float[] normalizedXY = normalizedValues(current_event,mView);
        normalizedDX = normalizedXY[0];
        normalizedDY = normalizedXY[1];

        mListener.onScrollDrag(sX, sY, sdX, sdY, dX, dY, normalizedDX, normalizedDY);
        Log.d(TAG, String.format("Scroll [ %.1f , %.1f , %.4f , %.4f / %.1f , %.1f , %.4f , %.4f ]", sX, sY, sdX, sdY, dX, dY, normalizedDX, normalizedDY));
        return true;
    }

    @Override
    public boolean onFling(MotionEvent initial_event, MotionEvent current_event, float velocityX, float velocityY) {
        if(!enableFling)
            return true;
        if(detectingMultiGesture)
            return true;
        fX = initial_event.getX();
        fY = initial_event.getY();
        fvX = velocityX; fvY = velocityY;
        mListener.onFling(fX, fY, fvX, fvY);
        Log.d(TAG, String.format("Fling [ %.4f , %.4f , %.4f , %.4f ]", fX, fY, fvX, fvY));
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if(!enableLongPress)
            return;
        if(detectingMultiGesture)
            return;

        lpX =event.getX(); lpY =event.getY();
        float[] normalizedXY = normalizedValues(event,mView);
        normalizedLPX = normalizedXY[0];
        normalizedLPY = normalizedXY[1];
        mListener.onLongPress(lpX, lpY, normalizedLPX,normalizedLPY);
        Log.d(TAG, String.format("LongPress [ %.1f , %.1f , %.4f , %.4f ]", lpX, lpY, normalizedLPX, normalizedLPY));
    }

    /** Gesture interfaces **/

    public interface OnOneFingerGestureListener {
        void onSingleTap(float stX, float stY, float normalizedSTX, float normalizedSTY);
        void onDoubleTap(float dtX, float dtY, float normalizedDTX, float normalizedDTY);
        void onScrollDrag(float sX, float sY, float sdX, float sdY, float dX, float dY, float normalizedDX, float normalizedDY);
        void onFling(float fX, float fY, float fvX, float fvY);
        void onLongPress(float lpX, float lpY, float normalizedLPX, float normalizedLPY);
        void onOneFingerGestureState(boolean detectingGesture);
    }

    /** Normalized values **/

    private float[] normalizedValues(MotionEvent event, View view){
        float[] touch = new float[]{event.getX(), event.getY()}; //relative to the view. Right,Down increase
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

    public void disableSingleTapUp(){
        enableSingleTapUp=false;
    }

    public void disableDoubleTap(){
        enableDoubleTap=false;
    }

    public void disableScroll(){
        enableScroll=false;
    }

    public void disableFling(){
        enableFling=false;
    }

    public void disableLongPress(){
        enableLongPress=false;
    }

    public void enableSingleTapUp(){
        enableSingleTapUp=true;
    }

    public void enableDoubleTap(){
        enableDoubleTap=true;
    }

    public void enableScroll(){
        enableScroll=true;
    }

    public void enableFling(){
        enableFling=true;
    }

    public void enableLongPress(){
        enableLongPress=true;
    }











}
