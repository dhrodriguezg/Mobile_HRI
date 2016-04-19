package uniandes.disc.imagine.android_hri.mobile_interaction.utils;

import android.app.Activity;
import android.os.Environment;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Finger;
import com.leapmotion.leap.FingerList;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Listener;
import com.leapmotion.leap.Vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import uniandes.disc.imagine.android_hri.mobile_interaction.R;

public class LeapMotionListener extends Listener {

    private static final String TAG = "LeapMotionListener";
    private static final float MIN_POS_X = -200;
    private static final float MIN_POS_Y = 0;
    private static final float MIN_POS_Z = -200;
    private static final float MAX_POS_X =  200;
    private static final float MAX_POS_Y =  600;
    private static final float MAX_POS_Z =  200;
    private static final float OPEN_THRESHOLD =  0.17f;

    private LeapMotionFrameListener mListener;
    private Activity mActivity;
    private LowPassFilter[] leftHandFilter;
    private LowPassFilter[] rightHandFilter;
    private LowPassFilter rotationFilter;
    private LowPassFilter graspFilter;
    private boolean rightHanded;
    private boolean isAttached;

    private String hands, fingers, rightHand, leftHand, actionHand, taskHand;
    private String palm, wrist, index, middle ,ring, pinky, thumb;
    private String selectTask, moveTask, rotateTask, graspTask, distances;
    private String initialized, connected, disconnected, exited;
    private String task, taskNaN, task5, task6;
    private StringBuffer palmPos = new StringBuffer();

    public LeapMotionListener(Activity activity, LeapMotionFrameListener listener) {
        mActivity = activity;
        mListener = listener;
        rightHanded = true;
        isAttached = false;
        graspFilter = new LowPassFilter();
        rotationFilter = new LowPassFilter();
        leftHandFilter = new LowPassFilter[7];
        rightHandFilter = new LowPassFilter[7];
        for(int n = 0; n < leftHandFilter.length; n++){
            leftHandFilter[n]= new LowPassFilter();
            rightHandFilter[n]= new LowPassFilter();
        }
        initialized=activity.getString(R.string.initialized);
        connected=activity.getString(R.string.connected);
        disconnected=activity.getString(R.string.disconnected);
        exited=activity.getString(R.string.exited);
        hands=activity.getString(R.string.hands);
        fingers=activity.getString(R.string.fingers);
        rightHand=activity.getString(R.string.rightHand);
        leftHand=activity.getString(R.string.leftHand);
        actionHand=activity.getString(R.string.actionHand);
        taskHand=activity.getString(R.string.taskHand);
        selectTask=activity.getString(R.string.selectTask);
        moveTask=activity.getString(R.string.moveTask);
        rotateTask=activity.getString(R.string.rotateTask);
        graspTask=activity.getString(R.string.graspTask);
        distances=activity.getString(R.string.distances);
        palm=activity.getString(R.string.palm);
        wrist=activity.getString(R.string.wrist);
        index=activity.getString(R.string.index);
        middle=activity.getString(R.string.middle);
        ring=activity.getString(R.string.ring);
        pinky=activity.getString(R.string.pinky);
        thumb=activity.getString(R.string.thumb);
        task=activity.getString(R.string.task);
        taskNaN=activity.getString(R.string.taskNaN);
        task5=activity.getString(R.string.task5);
        task6=activity.getString(R.string.task6);
    }

    public void onInit(Controller controller) {
        mListener.onUpdateMsg(initialized);
    }

    public void onConnect(Controller controller) {
        mListener.onUpdateMsg(connected);
    }

    public void onDisconnect(Controller controller) {
        mListener.onUpdateMsg(disconnected);
    }

    public void onExit(Controller controller) {
        mListener.onUpdateMsg(exited);
    }

    public void onFrame(Controller controller) {
        isAttached = true;
        StringBuffer msg = new StringBuffer();
        Frame frame = controller.frame();
        boolean isRight = false;
        boolean isLeft = false;
        msg.append(String.format("FPS: %.2f", frame.currentFramesPerSecond()));
        msg.append(String.format("\n%s: %d, %s: %d", hands, frame.hands().count(), fingers, frame.fingers().count()));
        if (!frame.hands().isEmpty()) {
            mListener.onHands(true);
            for (Hand hand : frame.hands() ){
                if(!hand.isValid())
                    return;
                if(rightHanded){
                    if(hand.isRight()){
                        msg.append("\n   "+actionHand + "(" + rightHand + ")");
                        rightHand(hand, msg);
                        isRight = true;
                    }else if(hand.isLeft()){
                        msg.append("\n   "+taskHand + "(" + leftHand + ")");
                        leftHand(hand, msg);
                        isLeft = true;
                    }
                }else{
                    if(hand.isRight()){
                        msg.append("\n   "+actionHand + "(" + leftHand + ")");
                        leftHand(hand, msg);
                        isLeft = true;
                    } else if(hand.isLeft()){
                        msg.append("\n   "+taskHand + "(" + rightHand + ")");
                        rightHand(hand, msg);
                        isRight = true;
                    }
                }

            }
        }else{
            mListener.onHands(false);
        }
        if(!isRight)
            mListener.onMoveRightHand(null);
        if(!isLeft)
            mListener.onMoveLeftHand(null);
        mListener.onUpdateMsg(msg.toString());
    }

    private void rightHand(Hand hand, StringBuffer msg){

        Vector[] rightPositions = extractPositions(hand,msg);
        if (rightPositions==null)
            return;

        for(int n = 0; n < rightHandFilter.length; n++)
            rightHandFilter[n].applyFilter1(rightPositions[n]);

        Vector handNormal = new Vector(hand.palmNormal());
        handNormal.setX(dir2Radians(handNormal.getX()));
        handNormal.setY(dir2Radians(handNormal.getY()));
        handNormal.setZ(dir2Radians(handNormal.getZ()));
        rotationFilter.applyFilter1(handNormal);

        float grasp = graspFilter.applyFilter1(calculateGrasping(rightPositions));

        msg.append(String.format("\n      %s: (%.2f, %.2f, %.2f)", selectTask, rightPositions[1].getX(), rightPositions[1].getY(), rightPositions[1].getZ()));
        msg.append(String.format("\n      %s: (%.2f, %.2f, %.2f)", moveTask, rightPositions[6].getX(), rightPositions[6].getY(), rightPositions[6].getZ()));
        msg.append(String.format("\n      %s: (%.2f, %.2f, %.2f)", rotateTask, handNormal.getY(), handNormal.getX(), handNormal.getZ()));
        msg.append(String.format("\n      %s: (%.2f)", graspTask, grasp));

        mListener.onSelect(rightPositions[1].getX(), rightPositions[1].getY(), rightPositions[1].getZ());
        mListener.onMove(rightPositions[6].getX(), rightPositions[6].getY(), rightPositions[6].getZ());
        mListener.onRotate(handNormal.getY(), handNormal.getX(), handNormal.getZ());
        mListener.onGrasping(grasp); //values from 0 to 1.
        mListener.onMoveRightHand(rightPositions);
    }

    private void leftHand(Hand hand, StringBuffer msg){

        Vector[] leftPositions = extractPositions(hand, msg);
        if (leftPositions==null)
            return;

        for(int n = 0; n < leftHandFilter.length; n++)
            leftHandFilter[n].applyFilter1(leftPositions[n]);

        mListener.onMoveLeftHand(leftPositions);

        double indexLenght=Math.hypot( leftPositions[0].getX()-leftPositions[1].getX() , leftPositions[0].getY()-leftPositions[1].getY() );
        indexLenght = Math.hypot( indexLenght , leftPositions[0].getZ()-leftPositions[1].getZ() );

        double middleLenght=Math.hypot( leftPositions[0].getX()-leftPositions[2].getX() , leftPositions[0].getY()-leftPositions[2].getY() );
        middleLenght = Math.hypot( middleLenght , leftPositions[0].getZ()-leftPositions[2].getZ() );

        double ringLenght=Math.hypot( leftPositions[0].getX()-leftPositions[3].getX() , leftPositions[0].getY()-leftPositions[3].getY() );
        ringLenght = Math.hypot( ringLenght , leftPositions[0].getZ()-leftPositions[3].getZ() );

        double pinkyLenght=Math.hypot(leftPositions[0].getX() - leftPositions[4].getX(), leftPositions[0].getY() - leftPositions[4].getY());
        pinkyLenght = Math.hypot( pinkyLenght , leftPositions[0].getZ()-leftPositions[4].getZ() );

        double thumbLenght=Math.hypot(leftPositions[0].getX() - leftPositions[5].getX(), leftPositions[0].getY() - leftPositions[5].getY());
        thumbLenght = Math.hypot( thumbLenght , leftPositions[0].getZ()-leftPositions[5].getZ() );

        msg.append(String.format("\n      %s => (%.4f, %.4f, %.4f, %.4f, %.4f)",distances, indexLenght, middleLenght, ringLenght, pinkyLenght, thumbLenght));

        boolean isIndex = indexLenght > OPEN_THRESHOLD;
        boolean isMiddle = middleLenght > OPEN_THRESHOLD;
        boolean isRing = ringLenght > OPEN_THRESHOLD;
        boolean isPinky = pinkyLenght > OPEN_THRESHOLD;
        boolean isThumb = thumbLenght > OPEN_THRESHOLD;

        int numOfFingers=0;

        if(isIndex && isMiddle && isRing && isPinky && isThumb){
            msg.append("\n      "+task5);
            mListener.onTask(5);
            return; //nothing just the hand open...
        }

        if(isIndex)
            numOfFingers++;
        if(isMiddle)
            numOfFingers++;
        if(isRing)
            numOfFingers++;
        if(isPinky)
            numOfFingers++;

        if(!isThumb){
            msg.append("\n      "+task+": "+numOfFingers);
            mListener.onTask(numOfFingers);
        }else if(numOfFingers>0){
            msg.append("\n      "+taskNaN);
            mListener.onTask(-1); //not valid gesture
        }else{
            msg.append("\n      " + task6);
            mListener.onTask(6); //confirm
            //saveFile(palmPos.toString());
        }
    }

    private float calculateGrasping(Vector[] fingerPosition){
        // It's calculated with the mean of the distance between the fingers and the thumb.
        double indexLenght=Math.hypot( fingerPosition[5].getX()-fingerPosition[1].getX() , fingerPosition[5].getY()-fingerPosition[1].getY() );
        indexLenght = Math.hypot(indexLenght, fingerPosition[5].getZ() - fingerPosition[1].getZ());

        double middleLenght=Math.hypot( fingerPosition[5].getX()-fingerPosition[2].getX() , fingerPosition[5].getY()-fingerPosition[2].getY() );
        middleLenght = Math.hypot( middleLenght , fingerPosition[5].getZ()-fingerPosition[2].getZ() );

        double ringLenght=Math.hypot( fingerPosition[5].getX()-fingerPosition[3].getX() , fingerPosition[5].getY()-fingerPosition[3].getY() );
        ringLenght = Math.hypot( ringLenght , fingerPosition[5].getZ()-fingerPosition[3].getZ() );

        double pinkyLenght=Math.hypot(fingerPosition[5].getX() - fingerPosition[4].getX(), fingerPosition[5].getY() - fingerPosition[4].getY());
        pinkyLenght = Math.hypot( pinkyLenght , fingerPosition[5].getZ()-fingerPosition[4].getZ() );

        float meanDistance = (float) (indexLenght + middleLenght + ringLenght + pinkyLenght)/4.f;
        meanDistance = (meanDistance - 0.05f) / (0.30f - 0.05f);

        if(meanDistance > 1f){
            meanDistance = 1f;
        }
        if(meanDistance < 0f){
            meanDistance = 0f;
        }
        return meanDistance;
    }

    private Vector[] extractPositions(Hand hand, StringBuffer msg){
        int numFingers=0;
        Vector[] positions = new Vector[7];

        msg.append("\n      "+palm);
        positions[0] = applyPositionLimits( new Vector(hand.palmPosition()), msg); //palm -> 0;

        palmPos.append(positions[0].getX()+","+positions[0].getY()+","+positions[0].getZ()+"\n");

        FingerList fingers = hand.fingers();
        for (Finger finger : fingers) {
            numFingers++;
            if (finger.type().equals(Finger.Type.TYPE_INDEX)){
                msg.append("\n         "+index);
                positions[1] = applyPositionLimits( new Vector(finger.tipPosition()), msg); //index -> 1;
            } else if(finger.type().equals(Finger.Type.TYPE_MIDDLE)){
                msg.append("\n         "+middle);
                positions[2] = applyPositionLimits( new Vector(finger.tipPosition()), msg); //middle -> 2;
            } else if(finger.type().equals(Finger.Type.TYPE_RING)){
                msg.append("\n         "+ring);
                positions[3] = applyPositionLimits( new Vector(finger.tipPosition()), msg); //ring -> 3;
            } else if(finger.type().equals(Finger.Type.TYPE_PINKY)){
                msg.append("\n         "+pinky);
                positions[4] = applyPositionLimits( new Vector(finger.tipPosition()), msg); //pinky -> 4;
            }else if(finger.type().equals(Finger.Type.TYPE_THUMB)){
                msg.append("\n         "+thumb);
                positions[5] = applyPositionLimits( new Vector(finger.tipPosition()), msg); //thumb -> 5;
            }
        }

        if(numFingers<5)
            return null;

        msg.append("\n         "+wrist);
        positions[6] = applyPositionLimits( new Vector(hand.wristPosition()), msg); //wrist -> 6;
        return positions;
    }

    private Vector applyPositionLimits(Vector position, StringBuffer msg){
        msg.append(String.format("(%.0f, %.0f, %.0f) -> ", position.getX(), position.getY(), position.getZ()));
        if(position.getX() > MAX_POS_X)
            position.setX(MAX_POS_X);
        else if(position.getX() < MIN_POS_X)
            position.setX(MIN_POS_X);
        if(position.getY() > MAX_POS_Y)
            position.setY(MAX_POS_Y);
        else if(position.getY() < MIN_POS_Y)
            position.setY(MIN_POS_Y);
        if(position.getZ() > MAX_POS_Z)
            position.setZ(MAX_POS_Z);
        else if(position.getZ() < MIN_POS_Z)
            position.setZ(MIN_POS_Z);

        Vector result = new Vector( (1 + position.getX() / MAX_POS_X)/2f, position.getY() / MAX_POS_Y, (1 + position.getZ() / MAX_POS_Z)/2f); //values from 0 to 1
        msg.append(String.format("(%.4f, %.4f, %.4f) ", result.getX(), result.getY(), result.getZ()));
        return result;
    }

    private float dir2Radians(float dir){
        return dir*3.1416f/2f;
    }

    public boolean isRightHanded() {
        return rightHanded;
    }

    public void setRightHanded(boolean rightHanded) {
        this.rightHanded = rightHanded;
    }

    public static interface LeapMotionFrameListener {
        public void onHands(boolean hands);
        public void onSelect(float x, float y, float z);
        public void onMove(float x, float y, float z);
        public void onRotate(float x, float y, float z);
        public void onGrasping(float g);

        public void onTask(int task);

        public void onUpdateMsg(String msg);
        public void onMoveLeftHand(Vector[] positions);
        public void onMoveRightHand(Vector[] positions);
    }

    public boolean isAttached() {
        return isAttached;
    }

    public void setIsAttached(boolean isAttached) {
        this.isAttached = isAttached;
    }

    private void saveFile(String text){

        File dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath() + "/rosAndroid/");
        dir.mkdirs();
        File file = new File(dir, "handPos.csv");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(text.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}