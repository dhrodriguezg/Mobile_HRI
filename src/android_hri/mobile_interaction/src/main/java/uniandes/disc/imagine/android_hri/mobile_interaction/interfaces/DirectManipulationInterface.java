package uniandes.disc.imagine.android_hri.mobile_interaction.interfaces;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
import uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen.MultiGestureArea;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.AndroidNode;


public class DirectManipulationInterface extends RosActivity implements SensorEventListener {
	
	private static final String TAG = "DirectManipulationInterface";
    private static final String NODE_NAME="/android_"+TAG.toLowerCase();

    private NodeMainExecutorService nodeMain;
    private MultiGestureArea statelessGestureHandler = null;

    private RosImageView<CompressedImage> imageStreamNodeMain;
    private ImageView targetImage;
    private ImageView positionImage;
    private TextView msgText;

    private AndroidNode androidNode;
    private BooleanTopic emergencyTopic;
    private Int32Topic interfaceNumberTopic;
    private PointTopic positionTopic;
    private PointTopic rotationTopic;
    private Float32Topic graspTopic;

    private StringBuffer msg;
    private String moveMsg="";
    private String rotateMsg="";
    private String graspMsg="";
    private boolean running = true;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float[] lastPosition;
    private float maxTargetSpeed;

    public DirectManipulationInterface() {
        super(TAG, TAG, URI.create(MainActivity.ROS_MASTER_URI));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.interface_directmanipulation);

        maxTargetSpeed =TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Float.parseFloat(getString(R.string.max_target_speed)), getResources().getDisplayMetrics());
        moveMsg=getString(R.string.move_msg) + " (%.4f , %.4f)";
        rotateMsg=getString(R.string.rotate_msg) + " += %.2f";
        graspMsg=getString(R.string.grasp_msg) + " = %.2f";
        targetImage = (ImageView) findViewById(R.id.targetView);
        positionImage = (ImageView) findViewById(R.id.positionView);
        msgText = (TextView) findViewById(R.id.msgTextView);

        imageStreamNodeMain = (RosImageView<CompressedImage>) findViewById(R.id.streamingView);
        statelessGestureHandler = new MultiGestureArea(this, imageStreamNodeMain);

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
        interfaceNumberTopic.setPublisher_int(3);

        emergencyTopic = new BooleanTopic();
        emergencyTopic.publishTo(getString(R.string.topic_emergencystop), true, 0);
        emergencyTopic.setPublishingFreq(100);
        emergencyTopic.setPublisher_bool(true);

        androidNode = new AndroidNode(NODE_NAME);
        androidNode.addTopics(positionTopic, graspTopic, rotationTopic, emergencyTopic, interfaceNumberTopic);
        androidNode.addNodeMain(imageStreamNodeMain);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        ToggleButton emergencyStop = (ToggleButton)findViewById(R.id.emergencyButton) ;
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

        msg = new StringBuffer();
        Thread threadGestures = new Thread(){
            public void run(){
                while(running){
                    try {
                        Thread.sleep(10);
                        updatePosition();
                        updateRotation();
                        updateGrasping();
                        updateText();
                    } catch (InterruptedException e) {
                        e.getStackTrace();
                    }
                }
            }
        };
        threadGestures.start();
    }

    private void onPostLayout(){
        lastPosition = new float[]{targetImage.getX()+targetImage.getWidth()/2, targetImage.getY()+targetImage.getHeight()/2};
        statelessGestureHandler.syncPos(lastPosition[0], lastPosition[1]); //This way the tracker begins at the target position
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics()); //convert pid to pixel
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)targetImage.getLayoutParams();
        params.rightMargin=px;
    }

    @Override
    public void onResume() {
        super.onResume();
        emergencyTopic.setPublisher_bool(true);
        running=true;
    }
    
    @Override
    protected void onPause() {
        emergencyTopic.setPublisher_bool(false);
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
        emergencyTopic.setPublisher_bool(false);
        nodeMain.forceShutdown();
        running=false;
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
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

    private void updatePosition() {
        final float x,y;
        if(statelessGestureHandler.isDetectingGesture()) {
            //positionPoint = new float[]{statelessGestureHandler.getPosX(), statelessGestureHandler.getPosY()};
            x=statelessGestureHandler.getPosX();
            y=statelessGestureHandler.getPosY();
            statelessGestureHandler.resetTarget();
        } else if(statelessGestureHandler.isTargetSelected()) {
            x=statelessGestureHandler.getTargetX();
            y=statelessGestureHandler.getTargetY();
        } else {
            return;
        }

        float[] positionPoint = new float[]{x,y};
        float[] positionPixel = new float[2];
        smoothMovement(positionPoint);

        Matrix streamMatrix = new Matrix();
        imageStreamNodeMain.getImageMatrix().invert(streamMatrix);
        streamMatrix.mapPoints(positionPixel, positionPoint);

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                positionImage.setX(x - positionImage.getWidth() / 2);
                positionImage.setY(y - positionImage.getHeight() / 2);
            }
        });

        if(!validTarget(positionPixel[0],positionPixel[1]))
            return;

        lastPosition=positionPoint;
        statelessGestureHandler.syncPos(lastPosition[0], lastPosition[1]);

        positionTopic.getPublisher_point()[0] = MainActivity.WORKSPACE_Y_OFFSET - positionPixel[1]*MainActivity.WORKSPACE_HEIGHT/(float) imageStreamNodeMain.getDrawable().getIntrinsicHeight();
        positionTopic.getPublisher_point()[1] = MainActivity.WORKSPACE_X_OFFSET - positionPixel[0]*MainActivity.WORKSPACE_WIDTH/(float) imageStreamNodeMain.getDrawable().getIntrinsicWidth();
        positionTopic.getPublisher_point()[2] = 0;
        positionTopic.publishNow();
        msg.append(String.format(moveMsg + " | ", positionTopic.getPublisher_point()[0], positionTopic.getPublisher_point()[1]));
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                targetImage.setX(lastPosition[0] - targetImage.getWidth() / 2);
                targetImage.setY(lastPosition[1] - targetImage.getHeight() / 2);
                if(Math.hypot(positionImage.getX()-targetImage.getX(),positionImage.getY()-targetImage.getY())<2)
                    statelessGestureHandler.resetTarget();
            }
        });
    }

    private void updateRotation() {
        if(!statelessGestureHandler.isDetectingGesture())
            return;
        float angle = 0.5f*statelessGestureHandler.getRotation()*3.1416f/180f;
        msg.append(String.format(rotateMsg + " | ",angle));
        rotationTopic.getPublisher_point()[0]=0;
        rotationTopic.getPublisher_point()[1]=angle;
        rotationTopic.publishNow();
    }

    private void updateGrasping() {
        float grasp = statelessGestureHandler.getGrasp();
        graspTopic.setPublisher_float(grasp);
        msg.append(String.format(graspMsg, grasp));
    }

    private void updateText() {
        final String message = msg.toString();
        msg.delete(0,msg.length());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(message.length()!=0)
                    msgText.setText(message);
            }
        });
    }

    private boolean validTarget(float x, float y) {
        if (x < 0 || y < 0)
            return false;
        if (x > imageStreamNodeMain.getDrawable().getIntrinsicWidth() ||  y > imageStreamNodeMain.getDrawable().getIntrinsicHeight())
            return false;
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /*
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            z=y-1; //for testing with shield
            y=x-1;

            if(Math.abs(y)>ROLL_THREASHOLD){
                rollText.setAlpha(1.f);
                jawText.setAlpha(.1f);
            }
            if(Math.abs(z)>JAW_THREASHOLD) {
                rollText.setAlpha(.1f);
                jawText.setAlpha(1.f);
            }
        }
        */
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        nodeMain=(NodeMainExecutorService)nodeMainExecutor;
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(MainActivity.ROS_HOSTNAME, getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }

}