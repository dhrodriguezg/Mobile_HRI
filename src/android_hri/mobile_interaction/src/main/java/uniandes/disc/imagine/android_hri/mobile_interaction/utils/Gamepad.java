package uniandes.disc.imagine.android_hri.mobile_interaction.utils;

import android.app.Activity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dhrodriguezg on 9/3/15.
 */
public class Gamepad { //well...not really a listener.

    private static final String TAG = "Gamepad";
    private static final int AXISES[] = {MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER};
    private static final int BUTTONS[] = {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_BUTTON_1, KeyEvent.KEYCODE_BUTTON_2, KeyEvent.KEYCODE_BUTTON_3, KeyEvent.KEYCODE_BUTTON_4, KeyEvent.KEYCODE_BUTTON_5, KeyEvent.KEYCODE_BUTTON_6};

    private boolean isAttached = false;
    private Activity activity;
    private HashMap<Integer,Float> mAxis;
    private HashMap<Integer,Integer> mButton;

    public Gamepad(Activity activity){
        isAttached=isGamepadConnected();
        this.activity=activity;
        setupMaps();
    }

    public void setupMaps(){
        mAxis = new HashMap<Integer,Float>();
        for (int axis : AXISES)
            mAxis.put(axis,0f);
        mButton = new HashMap<Integer,Integer>();
        for (int button : BUTTONS)
            mButton.put(button,0);
    }

    public boolean isGamepadConnected() {

        ArrayList gameControllerDeviceIds = new ArrayList();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();
            // Verify that the device has gamepad buttons, control sticks, or both.
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
                // This device is a game controller. Store its device ID.
                if (!gameControllerDeviceIds.contains(deviceId)) {
                    gameControllerDeviceIds.add(deviceId);
                }
                return true;
            }
        }
        return false;
    }

    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent){

        for (int axis : AXISES)
            mAxis.put(axis,motionEvent.getAxisValue(axis));

        //some genius got the idea to put the Dpad values in the Axis events too...
        float x=motionEvent.getAxisValue(MotionEvent.AXIS_HAT_X);
        if(Math.abs(x)<0.5f){
            mButton.put(KeyEvent.KEYCODE_DPAD_LEFT, 0);
            mButton.put(KeyEvent.KEYCODE_DPAD_RIGHT, 0);
        }else if (x>0f){
            mButton.put(KeyEvent.KEYCODE_DPAD_LEFT, 0);
            mButton.put(KeyEvent.KEYCODE_DPAD_RIGHT, 1);
        }else{
            mButton.put(KeyEvent.KEYCODE_DPAD_LEFT, 1);
            mButton.put(KeyEvent.KEYCODE_DPAD_RIGHT, 0);
        }
        float y=motionEvent.getAxisValue(MotionEvent.AXIS_HAT_Y);
        if(Math.abs(y)<0.5f){
            mButton.put(KeyEvent.KEYCODE_DPAD_UP, 0);
            mButton.put(KeyEvent.KEYCODE_DPAD_DOWN, 0);
        }else if (y>0f){
            mButton.put(KeyEvent.KEYCODE_DPAD_UP, 0);
            mButton.put(KeyEvent.KEYCODE_DPAD_DOWN, 1);
        }else{
            mButton.put(KeyEvent.KEYCODE_DPAD_UP, 1);
            mButton.put(KeyEvent.KEYCODE_DPAD_DOWN, 0);
        }
        return true;
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent){

        int value =-1;
        if(keyEvent.getAction()==KeyEvent.ACTION_DOWN)
            value=1;
        else if(keyEvent.getAction()==KeyEvent.ACTION_UP)
            value=0;

        //mapping standar ABXY gamepad to generic Gamepad
        int key=keyEvent.getKeyCode();
        if(key==keyEvent.KEYCODE_BUTTON_A)
            key=keyEvent.KEYCODE_BUTTON_1;
        else if(key==keyEvent.KEYCODE_BUTTON_B)
            key=keyEvent.KEYCODE_BUTTON_2;
        else if(key==keyEvent.KEYCODE_BUTTON_X)
            key=keyEvent.KEYCODE_BUTTON_3;
        else if(key==keyEvent.KEYCODE_BUTTON_Y)
            key=keyEvent.KEYCODE_BUTTON_4;
        else if(key==keyEvent.KEYCODE_BUTTON_L1)
            key=keyEvent.KEYCODE_BUTTON_5;
        else if(key==keyEvent.KEYCODE_BUTTON_R1)
            key=keyEvent.KEYCODE_BUTTON_6;
        if(mButton.containsKey(key)){
            mButton.put(key, value);
        }
        return true;
    }

    public boolean isAttached() {
        return isAttached;
    }

    public void setIsGamepad(boolean isGamepad) {
        this.isAttached = isGamepad;
    }

    public HashMap<Integer, Float> getmAxis() {
        return mAxis;
    }

    public void setmAxis(HashMap<Integer, Float> mAxis) {
        this.mAxis = mAxis;
    }

    public HashMap<Integer, Integer> getmButton() {
        return mButton;
    }

    public void setmButton(HashMap<Integer, Integer> mButton) {
        this.mButton = mButton;
    }

    public float getAxisValue(int axis){
        return mAxis.get(axis);
    }

    public int getButtonValue(int button){
        return mButton.get(button);
    }
}
