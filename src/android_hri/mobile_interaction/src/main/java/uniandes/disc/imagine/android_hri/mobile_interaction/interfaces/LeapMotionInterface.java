package uniandes.disc.imagine.android_hri.mobile_interaction.interfaces;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Vector;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.NodeMainExecutorService;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

import sensor_msgs.CompressedImage;
import uniandes.disc.imagine.android_hri.mobile_interaction.MainActivity;
import uniandes.disc.imagine.android_hri.mobile_interaction.R;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.BooleanTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.Float32Topic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.Int32Topic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.PointTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.StringTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.AndroidNode;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.LeapMotionListener;

public class LeapMotionInterface extends RosActivity implements LeapMotionListener.LeapMotionFrameListener{

	private static final String TAG = "LeapMotionInterface";
    private static final String NODE_NAME="/android_"+TAG.toLowerCase();

    private final int DISABLED = Color.RED;
    private final int ENABLED = Color.GREEN;
    private final int TRANSITION = Color.rgb(255,195,77); //orange
    private final int MAX_TASK_COUNTER = 30;

    private int confirmCounter;
    private boolean isConfirm=false;
    private boolean isEnable=true;
    private boolean isReset=true;

    private NodeMainExecutorService nodeMain;
    private Controller mController;
    private LeapMotionListener mLeapMotionListener;

    private ToggleButton emergencyStop;
    private RosImageView<CompressedImage> imageStreamNodeMain;
    private TextView msgText;
    private TextView statusText;
    private TextView trackingText;

    private AndroidNode androidNode;
    private BooleanTopic emergencyTopic;
    private Int32Topic interfaceNumberTopic;
    private PointTopic positionTopic;
    private PointTopic rotationTopic;
    private Float32Topic graspTopic;
    private StringTopic logTopic;

    private Switch rightHanded;
    private CheckBox showLog;
    private CheckBox showHands;
    private ImageView targetImage;
    private ImageView positionImage;

    //left hand
    private ImageView leftHand;
    private ImageView leftIndex;
    private ImageView leftMiddle;
    private ImageView leftRing;
    private ImageView leftPinky;
    private ImageView leftThumb;

    //right hand
    private ImageView rightHand;
    private ImageView rightIndex;
    private ImageView rightMiddle;
    private ImageView rightRing;
    private ImageView rightPinky;
    private ImageView rightThumb;

    private String status_ok="";
    private String status_fail="";
    private float[] lastPosition;
    private float maxTargetSpeed;

    public LeapMotionInterface() {
        super(TAG, TAG, URI.create(MainActivity.PREFERENCES.getProperty( "ROS_MASTER_URI" )));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.interface_leapmotion);

        status_ok=getString(R.string.status_ok);
        status_fail=getString(R.string.status_fail);

        maxTargetSpeed = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Float.parseFloat(getString(R.string.max_target_speed)), getResources().getDisplayMetrics())*3.f;
        msgText = (TextView) findViewById(R.id.msgTextView);

        targetImage = (ImageView) findViewById(R.id.targetView);
        positionImage = (ImageView) findViewById(R.id.positionView);
        leftHand = (ImageView) findViewById(R.id.leftHand);
        leftIndex = (ImageView) findViewById(R.id.leftIndexFinger);
        leftMiddle = (ImageView) findViewById(R.id.leftMiddleFinger);
        leftRing = (ImageView) findViewById(R.id.leftRingFinger);
        leftPinky = (ImageView) findViewById(R.id.leftPinkyFinger);
        leftThumb = (ImageView) findViewById(R.id.leftThumbFinger);
        rightHand = (ImageView) findViewById(R.id.rightHand);
        rightIndex = (ImageView) findViewById(R.id.rightIndexFinger);
        rightMiddle = (ImageView) findViewById(R.id.rightMiddleFinger);
        rightRing = (ImageView) findViewById(R.id.rightRingFinger);
        rightPinky = (ImageView) findViewById(R.id.rightPinkyFinger);
        rightThumb = (ImageView) findViewById(R.id.rightThumbFinger);

        showLog = (CheckBox) findViewById(R.id.showLog);

        imageStreamNodeMain = (RosImageView<CompressedImage>) findViewById(R.id.streamingView);
        imageStreamNodeMain.setTopicName(getString(R.string.topic_streaming));
        imageStreamNodeMain.setMessageType(getString(R.string.topic_streaming_msg));
        imageStreamNodeMain.setMessageToBitmapCallable(new BitmapFromCompressedImage());
        imageStreamNodeMain.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageStreamNodeMain.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageStreamNodeMain.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                onPostLayout();
            }
        });

        positionTopic = new PointTopic();
        positionTopic.publishTo(getString(R.string.topic_positionabs), false, 10);

        rotationTopic = new PointTopic();
        rotationTopic.publishTo(getString(R.string.topic_rotationabs), false, 10);

        graspTopic = new Float32Topic();
        graspTopic.setPublishingFreq(500);
        graspTopic.publishTo(getString(R.string.topic_graspingabs), true, 0);

        interfaceNumberTopic = new Int32Topic();
        interfaceNumberTopic.publishTo(getString(R.string.topic_interfacenumber), true, 0);
        interfaceNumberTopic.setPublishingFreq(100);
        interfaceNumberTopic.setPublisher_int(5);

        emergencyTopic = new BooleanTopic();
        emergencyTopic.publishTo(getString(R.string.topic_emergencystop), true, 0);
        emergencyTopic.setPublishingFreq(100);
        emergencyTopic.setPublisher_bool(true);

        logTopic = new StringTopic();
        logTopic.publishTo(getString(R.string.topic_lmlog), false, 10);
        logTopic.setPublishingFreq(100);

        androidNode = new AndroidNode(NODE_NAME);
        androidNode.addTopics(positionTopic, graspTopic, rotationTopic, emergencyTopic, logTopic, interfaceNumberTopic);
        androidNode.addNodeMain(imageStreamNodeMain);

        mController = new Controller();
        mLeapMotionListener = new LeapMotionListener(this, this);
        mController.addListener(mLeapMotionListener);

        statusText = (TextView)findViewById(R.id.statusView);
        trackingText = (TextView) findViewById(R.id.statusTracking);

        emergencyStop = (ToggleButton)findViewById(R.id.emergencyButton);
        emergencyStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), getString(R.string.emergency_on_msg), Toast.LENGTH_LONG).show();
                    imageStreamNodeMain.setBackgroundColor(Color.RED);
                    emergencyTopic.setPublisher_bool(false);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.emergency_off_msg), Toast.LENGTH_LONG).show();
                    imageStreamNodeMain.setBackgroundColor(Color.TRANSPARENT);
                    emergencyTopic.setPublisher_bool(true);
                }
            }
        });

        showHands = (CheckBox) findViewById(R.id.showHands);
        showHands.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(!isChecked){
                    hideLeftHand();
                    hideRightHand();
                }
            }
        });

        rightHanded = (Switch) findViewById(R.id.rightHanded);
        rightHanded.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                mLeapMotionListener.setRightHanded(isChecked);
            }
        });

    }

    private void onPostLayout(){
        lastPosition = new float[]{targetImage.getX()+ targetImage.getWidth()/2, targetImage.getY()+ targetImage.getHeight()/2};
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()); //convert pid to pixel
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)targetImage.getLayoutParams();
        params.rightMargin=px;
    }

    @Override
    public void onResume() {
        super.onResume();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()); //convert pid to pixel
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)targetImage.getLayoutParams();
        params.rightMargin=px;
        emergencyTopic.setPublisher_bool(true);
    }
    
    @Override
    protected void onPause()
    {
        emergencyTopic.setPublisher_bool(false);
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
        emergencyTopic.setPublisher_bool(false);
        nodeMain.forceShutdown();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onHands(final boolean hands) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (hands) {
                    statusText.setText(status_ok);
                    statusText.setBackgroundColor(ENABLED);
                } else {
                    statusText.setText(status_fail);
                    statusText.setBackgroundColor(DISABLED);
                }
            }
        });
    }

    @Override
    public void onSelect(float x, float y, float z) {
        if(!isConfirm)
            return;
    }

    private void smoothMovement(float[] currPos){
        float dx = currPos[0]-lastPosition[0];
        float dy = currPos[1]-lastPosition[1];
        float max = Math.max(Math.abs(dx), Math.abs(dy));

        if(max > maxTargetSpeed){
            dx=maxTargetSpeed*dx/max;
            dy=maxTargetSpeed*dy/max;
        }

        currPos[0]=lastPosition[0]+dx;
        currPos[1]=lastPosition[1]+dy;
    }

    @Override
    public void onMove(float x, float y, float z) {
        if(!isConfirm)
            return;

        float[] wristPoint = calculatePoint(imageStreamNodeMain.getDrawable().getIntrinsicWidth()*x, imageStreamNodeMain.getDrawable().getIntrinsicHeight()*z);
        final float wristx = wristPoint[0] - positionImage.getWidth()/2;
        final float wristy = wristPoint[1] - positionImage.getHeight()/2;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                positionImage.setX(wristx);
                positionImage.setY(wristy);
            }
        });

        float[] smoothedPos = new float[]{x*(float)imageStreamNodeMain.getWidth(),z*(float)imageStreamNodeMain.getHeight()};
        smoothMovement(smoothedPos);
        lastPosition=smoothedPos;
        x=smoothedPos[0]/(float)imageStreamNodeMain.getWidth();
        z=smoothedPos[1]/(float)imageStreamNodeMain.getHeight();

        //positionTopic.getPublisher_point()[0] = MainActivity.WORKSPACE_Y_OFFSET - z*MainActivity.WORKSPACE_HEIGHT;
        //positionTopic.getPublisher_point()[1] = MainActivity.WORKSPACE_X_OFFSET - x*MainActivity.WORKSPACE_WIDTH;
        positionTopic.publishNow();

        float[] smoothedPoint = calculatePoint(imageStreamNodeMain.getDrawable().getIntrinsicWidth()*x, imageStreamNodeMain.getDrawable().getIntrinsicHeight()*z);
        final float smoothedx = smoothedPoint[0] - targetImage.getWidth()/2;
        final float smoothedy = smoothedPoint[1] - targetImage.getHeight()/2;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                targetImage.setX(smoothedx);
                targetImage.setY(smoothedy);
            }
        });
    }

    @Override
    public void onRotate(float x, float y, float z) {
        if(!isConfirm)
            return;
        rotationTopic.getPublisher_point()[0]=y+1.57f;
        rotationTopic.getPublisher_point()[1]=z;
        rotationTopic.publishNow();
    }

    @Override
    public void onGrasping(float g) {
        if(!isConfirm)
            return;
        float grasp = (1f-g)*1.75f;
        graspTopic.setPublisher_float(grasp);
        graspTopic.publishNow();
    }

    @Override
    public void onTask(final int task) {
        if(!isEnable)
            return;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (task) {
                    case -1: // nonvalid gesture (reset): unused
                    case 0: // all fingers closed (reset): unused
                    case 1: //move selected: unused
                    case 2: //rotate selected: unused
                    case 3: //grasp selected: unused
                    case 4: //ALL selected: unused
                    case 5: // all fingers opened (no task): unused
                        confirmCounter = 0;
                        isReset = true;
                        break;
                    case 6: //confirm
                        if(!isReset)
                            return;
                        if (confirmCounter > MAX_TASK_COUNTER) {
                            isReset=false;
                            confirmCounter = 0;
                            isConfirm = !isConfirm;
                            if (isConfirm) {
                                trackingText.setBackgroundColor(ENABLED);
                                Toast.makeText(getApplicationContext(), getString(R.string.tracking_activated), Toast.LENGTH_LONG).show();
                            } else {
                                trackingText.setBackgroundColor(DISABLED);
                                Toast.makeText(getApplicationContext(), getString(R.string.tracking_deactivated), Toast.LENGTH_LONG).show();
                            }
                            return;
                        }
                        confirmCounter++;
                        break;
                    default:
                        break;
                }
            }
        });

    }

    private void resetTasks(){
        isConfirm=false;
        imageStreamNodeMain.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void onUpdateMsg(final String msg) {
        logTopic.setPublisher_string(msg);
        logTopic.publishNow();

        runOnUiThread(new Runnable() {
            public void run() {
                if (!showLog.isChecked()) {
                    msgText.setAlpha(0.0f);
                    return;
                }
                msgText.setText(msg + "\n");
                msgText.setAlpha(0.5f);
            }
        });
    }

    private void hideLeftHand(){
        leftHand.setAlpha(0.0f);
        leftIndex.setAlpha(0.0f);
        leftMiddle.setAlpha(0.0f);
        leftRing.setAlpha(0.0f);
        leftPinky.setAlpha(0.0f);
        leftThumb.setAlpha(0.0f);
    }

    private void hideRightHand(){
        rightHand.setAlpha(0.0f);
        rightIndex.setAlpha(0.0f);
        rightMiddle.setAlpha(0.0f);
        rightRing.setAlpha(0.0f);
        rightPinky.setAlpha(0.0f);
        rightThumb.setAlpha(0.0f);
    }

    @Override
    public void onMoveLeftHand(final Vector[] positions) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(positions!=null && positions[0].getY()>0.8f){
                    isEnable=false;
                    resetTasks();
                }else{
                    isEnable=true;
                }
                if(!showHands.isChecked() || positions==null){
                    hideLeftHand();
                    return;
                }
                int counter=0;
                for(Vector position : positions){
                    float[] point = calculatePoint(imageStreamNodeMain.getDrawable().getIntrinsicWidth() * position.getX() , imageStreamNodeMain.getDrawable().getIntrinsicHeight() * position.getZ() );
                    switch (counter){
                        case 0: //palm
                            leftHand.setX(point[0] - leftHand.getWidth() / 2);
                            leftHand.setY(point[1] - leftHand.getHeight() / 2);
                            leftHand.setAlpha(0.4f);
                            break;
                        case 1: //index
                            leftIndex.setX(point[0] - leftIndex.getWidth()/2);
                            leftIndex.setY(point[1] - leftIndex.getHeight()/2);
                            leftIndex.setAlpha(0.4f);
                            break;
                        case 2: //middle
                            leftMiddle.setX(point[0] - leftMiddle.getWidth()/2);
                            leftMiddle.setY(point[1] - leftMiddle.getHeight()/2);
                            leftMiddle.setAlpha(0.4f);
                            break;
                        case 3: //ring
                            leftRing.setX(point[0] - leftRing.getWidth()/2);
                            leftRing.setY(point[1] - leftRing.getHeight()/2);
                            leftRing.setAlpha(0.4f);
                            break;
                        case 4: //pinky
                            leftPinky.setX(point[0] - leftPinky.getWidth()/2);
                            leftPinky.setY(point[1] - leftPinky.getHeight()/2);
                            leftPinky.setAlpha(0.4f);
                            break;
                        case 5: //thumb
                            leftThumb.setX(point[0] - leftThumb.getWidth()/2);
                            leftThumb.setY(point[1] - leftThumb.getHeight()/2);
                            leftThumb.setAlpha(0.4f);
                            break;
                        default:
                            break;
                    }
                    counter++;
                }
            }
        });
    }

    @Override
    public void onMoveRightHand(final Vector[] positions) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!showHands.isChecked()  || positions==null){
                    hideRightHand();
                    return;
                }
                int counter=0;
                for(Vector position : positions){
                    float[] point = calculatePoint(imageStreamNodeMain.getDrawable().getIntrinsicWidth() * position.getX() , imageStreamNodeMain.getDrawable().getIntrinsicHeight() * position.getZ() );
                    switch (counter){
                        case 0: //palm
                            rightHand.setX(point[0] - rightHand.getWidth()/2);
                            rightHand.setY(point[1] - rightHand.getHeight()/2);
                            //rightHand.setAlpha(0.4f);
                            break;
                        case 1: //index
                            rightIndex.setX(point[0] - rightIndex.getWidth()/2);
                            rightIndex.setY(point[1] - rightIndex.getHeight()/2);
                            rightIndex.setAlpha(0.4f);
                            break;
                        case 2: //middle
                            rightMiddle.setX(point[0] - rightMiddle.getWidth()/2);
                            rightMiddle.setY(point[1] - rightMiddle.getHeight()/2);
                            rightMiddle.setAlpha(0.4f);
                            break;
                        case 3: //ring
                            rightRing.setX(point[0] - rightRing.getWidth()/2);
                            rightRing.setY(point[1] - rightRing.getHeight()/2);
                            rightRing.setAlpha(0.4f);
                            break;
                        case 4: //pinky
                            rightPinky.setX(point[0] - rightPinky.getWidth()/2);
                            rightPinky.setY(point[1] - rightPinky.getHeight()/2);
                            rightPinky.setAlpha(0.4f);
                            break;
                        case 5: //thumb
                            rightThumb.setX(point[0] - rightThumb.getWidth()/2);
                            rightThumb.setY(point[1] - rightThumb.getHeight()/2);
                            rightThumb.setAlpha(0.4f);
                            break;
                        default:
                            break;
                    }
                    counter++;
                }
            }
        });

    }

    private float[] calculatePoint(float x, float y){
        float[] positionPixel = new float[]{x,y};
        float[] positionPoint = new float[2];
        Matrix streamMatrix = imageStreamNodeMain.getImageMatrix();
        streamMatrix.mapPoints(positionPoint, positionPixel);
        return positionPoint;
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        nodeMain=(NodeMainExecutorService)nodeMainExecutor;
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic( MainActivity.PREFERENCES.getProperty( getString(R.string.HOSTNAME) ), getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }
}