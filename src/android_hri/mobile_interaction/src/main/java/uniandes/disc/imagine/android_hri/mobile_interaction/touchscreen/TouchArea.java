package uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen.gesture_detector.OneFingerGestureDetector;

/**
 * Created by Diego Rodriguez on 22/07/2015.
 */
public class TouchArea implements OneFingerGestureDetector.OnOneFingerGestureListener{

    private static final String TAG = "TouchArea";

    protected OneFingerGestureDetector mOneFingerGestureDetector;
    protected Activity activity;
    protected ImageView view;
    protected Vibrator vibrator;

    protected boolean detectingOneFingerGesture = false;
    protected float singleDragX = 0.f;
    protected float singleDragY = 0.f;
    protected float singleIDragX = 0.f;
    protected float singleIDragY = 0.f;
    protected float singleDragNormalizedX = 0.f;
    protected float singleDragNormalizedY = 0.f;
    protected float doubleTapX = -1.f;
    protected float doubleTapY = -1.f;
    protected float doubleTapNormalizedX = -1.f;
    protected float doubleTapNormalizedY = -1.f;
    protected float longClickX = -1.f;
    protected float longClickY = -1.f;
    protected float longClickNormalizedX = -1.f;
    protected float longClickNormalizedY = -1.f;
    protected boolean enableOneFingerGestures = true;

    public TouchArea(Activity activity, ImageView view){
        mOneFingerGestureDetector = new OneFingerGestureDetector(activity,this);
        vibrator = (Vibrator) activity.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        this.activity = activity;
        this.view = view;
        view.setVisibility(View.VISIBLE);
        setupListener();
    }

    protected void setupListener() {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                addOneFingerListener(view, event);
                return true;
            }
        });
    }

    protected void addOneFingerListener(View view, MotionEvent event) {
        if(enableOneFingerGestures)
            mOneFingerGestureDetector.onTouchEvent(view, event);
    }

    @Override
    public void onScrollDrag(float sX, float sY, float sdX, float sdY, float dX, float dY, float normalizedDX, float normalizedDY) {
        singleIDragX=sX;
        singleIDragY=sY;
        singleDragNormalizedX=normalizedDX;
        singleDragNormalizedY=normalizedDY;
        singleDragX=dX;
        singleDragY=dY;
    }

    @Override
    public void onDoubleTap(float dtX, float dtY, float normalizedDTX, float normalizedDTY) {
        doubleTapNormalizedX=normalizedDTX;
        doubleTapNormalizedY=normalizedDTY;
        doubleTapX=dtX;
        doubleTapY=dtY;
    }

    @Override
    public void onLongPress(float lpX, float lpY, float normalizedLPX, float normalizedLPY) {
        longClickNormalizedX=normalizedLPX;
        longClickNormalizedY=normalizedLPY;
        longClickX=lpX;
        longClickY=lpY;
        vibrator.vibrate(200);
    }

    @Override
    public void onSingleTap(float stX, float stY, float normalizedSTX, float normalizedSTY) {
        //unused...for now
    }

    @Override
    public void onFling(float fX, float fY, float fvX, float fvY) {
        //unused...for now
    }

    @Override
    public void onOneFingerGestureState(boolean detectingGesture) {
        detectingOneFingerGesture=detectingGesture;
        //resetValuesOnRelease(); //TODO por alguna razon lo estaba reseteando
    }

    public void resetValuesOnRelease(){
        if(!detectingOneFingerGesture){
            singleDragNormalizedX=0;
            singleDragNormalizedY=0;
            singleDragX=0;
            singleDragY=0;
            singleIDragX=0;
            singleIDragY=0;
        }
    }


    public boolean isDetectingOneFingerGesture(){
        return detectingOneFingerGesture;
    }

    public float getSingleDragX() {
        return singleDragX;
    }

    public void setSingleDragX(float singleDragX) {
        this.singleDragX = singleDragX;
    }

    public float getSingleDragY() {
        return singleDragY;
    }

    public void setSingleDragY(float singleDragY) {
        this.singleDragY = singleDragY;
    }

    public float getSingleDragNormalizedX() {
        return singleDragNormalizedX;
    }

    public void setSingleDragNormalizedX(float singleDragNormalizedX) {
        this.singleDragNormalizedX = singleDragNormalizedX;
    }

    public float getSingleDragNormalizedY() {
        return singleDragNormalizedY;
    }

    public void setSingleDragNormalizedY(float singleDragNormalizedY) {
        this.singleDragNormalizedY = singleDragNormalizedY;
    }

    public float getDoubleTapX() {
        return doubleTapX;
    }

    public void setDoubleTapX(float doubleTapX) {
        this.doubleTapX = doubleTapX;
    }

    public float getDoubleTapY() {
        return doubleTapY;
    }

    public void setDoubleTapY(float doubleTapY) {
        this.doubleTapY = doubleTapY;
    }

    public float getDoubleTapNormalizedX() {
        return doubleTapNormalizedX;
    }

    public void setDoubleTapNormalizedX(float doubleTapNormalizedX) {
        this.doubleTapNormalizedX = doubleTapNormalizedX;
    }

    public float getDoubleTapNormalizedY() {
        return doubleTapNormalizedY;
    }

    public void setDoubleTapNormalizedY(float doubleTapNormalizedY) {
        this.doubleTapNormalizedY = doubleTapNormalizedY;
    }

    public float getLongClickX() {
        return longClickX;
    }

    public void setLongClickX(float longClickX) {
        this.longClickX = longClickX;
    }

    public float getLongClickY() {
        return longClickY;
    }

    public void setLongClickY(float longClickY) {
        this.longClickY = longClickY;
    }

    public float getLongClickNormalizedX() {
        return longClickNormalizedX;
    }

    public void setLongClickNormalizedX(float longClickNormalizedX) {
        this.longClickNormalizedX = longClickNormalizedX;
    }

    public float getLongClickNormalizedY() {
        return longClickNormalizedY;
    }

    public void setLongClickNormalizedY(float longClickNormalizedY) {
        this.longClickNormalizedY = longClickNormalizedY;
    }

    public float getWidth() {
        return view.getWidth();
    }

    public float getHeight() {
        return view.getHeight();
    }

    public float getSingleIDragX() {
        return singleIDragX;
    }

    public void setSingleIDragX(float singleIDragX) {
        this.singleIDragX = singleIDragX;
    }

    public float getSingleIDragY() {
        return singleIDragY;
    }

    public void setSingleIDragY(float singleIDragY) {
        this.singleIDragY = singleIDragY;
    }

    public void enableOneFingerGestures(){
        enableOneFingerGestures=true;
    }

    public void disableOneFingerGestures(){
        enableOneFingerGestures=false;
    }

    public void disableSingleTapUp(){
        mOneFingerGestureDetector.disableSingleTapUp();
    }

    public void disableDoubleTap(){
        mOneFingerGestureDetector.disableDoubleTap();
    }

    public void disableScroll(){
        mOneFingerGestureDetector.disableScroll();
    }

    public void disableFling(){
        mOneFingerGestureDetector.disableFling();
    }

    public void disableLongPress(){
        mOneFingerGestureDetector.disableLongPress();
    }

    public void enableSingleTapUp(){
        mOneFingerGestureDetector.enableSingleTapUp();
    }

    public void enableDoubleTap(){
        mOneFingerGestureDetector.enableDoubleTap();
    }

    public void enableScroll(){
        mOneFingerGestureDetector.enableScroll();
    }

    public void enableFling(){
        mOneFingerGestureDetector.enableFling();
    }

    public void enableLongPress(){
        mOneFingerGestureDetector.enableLongPress();
    }
}
