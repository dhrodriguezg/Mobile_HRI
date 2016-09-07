package uniandes.disc.imagine.android_hri.mobile_interaction.interfaces;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
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
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.TwistTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen.MultiGestureArea;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.AndroidNode;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.MjpegInputStream;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.MjpegView;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.UDPComm;


public class DirectManipulationInterface extends RosActivity implements SensorEventListener {
	
	private static final String TAG = "DirectManipulationInterface";
    private static final String NODE_NAME="/android_"+TAG.toLowerCase();

    private NodeMainExecutorService nodeMain;
    private MultiGestureArea statelessGestureHandler = null;

    private RosImageView<CompressedImage> imageStreamNodeMain;
    private ImageView targetImage;
    private ImageView positionImage;
    private TextView msgText;
    private RadioButton deviceMsim;
    private RadioButton deviceP3DX1;
    private RadioButton cameraSim;
    private RadioButton cameraTopDown;
    private RadioButton cameraFirstPerson;

    private AndroidNode androidNode;
    private BooleanTopic emergencyTopic;
    private Int32Topic interfaceNumberTopic;
    private PointTopic positionTopic;
    private PointTopic rotationTopic;
    private Float32Topic graspTopic;
    private Int32Topic cameraNumberTopic;
    private Int32Topic cameraPTZTopic;
    private Int32Topic p3dxNumberTopic;
    private TwistTopic velocityTopic;

    private StringBuffer msg;
    private String moveMsg="";
    private String rotateMsg="";
    private String graspMsg="";

    private UDPComm udpCommCommand;
    private MjpegView mjpegView;
    private boolean running = true;

    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float[] lastPosition;
    private float maxTargetSpeed;

    public DirectManipulationInterface() {
        super(TAG, TAG, URI.create(MainActivity.PREFERENCES.getProperty( "ROS_MASTER_URI" )));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.interface_directmanipulation);

        deviceMsim = (RadioButton) findViewById(R.id.radioMSIM);
        deviceP3DX1 = (RadioButton) findViewById(R.id.radioP3DX1);
        cameraSim = (RadioButton) findViewById(R.id.radioSimulator);
        cameraTopDown = (RadioButton) findViewById(R.id.radioTopDown);
        cameraFirstPerson = (RadioButton) findViewById(R.id.radioFirstPerson);

        maxTargetSpeed =TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Float.parseFloat(getString(R.string.max_target_speed)), getResources().getDisplayMetrics());
        moveMsg=getString(R.string.move_msg) + " (%.4f , %.4f)";
        rotateMsg=getString(R.string.rotate_msg) + " += %.2f";
        graspMsg=getString(R.string.grasp_msg) + " = %.2f";
        targetImage = (ImageView) findViewById(R.id.targetView);
        positionImage = (ImageView) findViewById(R.id.positionView);
        msgText = (TextView) findViewById(R.id.msgTextView);

        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.udp))) )
            udpCommCommand = new UDPComm( MainActivity.PREFERENCES.getProperty( getString(R.string.MASTER) ) , Integer.parseInt(getString(R.string.udp_port)));

        mjpegView = (MjpegView) findViewById(R.id.mjpegView);
        mjpegView.setDisplayMode(MjpegView.SIZE_BEST_FIT);
        mjpegView.showFps(true);

        imageStreamNodeMain = (RosImageView<CompressedImage>) findViewById(R.id.streamingView);

        velocityTopic =  new TwistTopic();
        velocityTopic.publishTo(getString(R.string.topic_rosariavel), false, 10);
        velocityTopic.setPublishingFreq(100);

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

        cameraNumberTopic = new Int32Topic();
        cameraNumberTopic.publishTo(getString(R.string.topic_camera_number), false, 100);
        cameraNumberTopic.setPublishingFreq(10);
        cameraNumberTopic.setPublisher_int(0);
        cameraNumberTopic.publishNow();

        cameraPTZTopic = new Int32Topic();
        cameraPTZTopic.publishTo(getString(R.string.topic_camera_ptz), false, 10);
        cameraPTZTopic.setPublishingFreq(10);
        cameraPTZTopic.setPublisher_int(-1);

        p3dxNumberTopic = new Int32Topic();
        p3dxNumberTopic.publishTo(getString(R.string.topic_p3dx_number), false, 100);
        p3dxNumberTopic.setPublishingFreq(10);
        p3dxNumberTopic.setPublisher_int(0);
        p3dxNumberTopic.publishNow();

        emergencyTopic = new BooleanTopic();
        emergencyTopic.publishTo(getString(R.string.topic_emergencystop), true, 0);
        emergencyTopic.setPublishingFreq(100);
        emergencyTopic.setPublisher_bool(true);

        androidNode = new AndroidNode(NODE_NAME);
        androidNode.addTopics(emergencyTopic, p3dxNumberTopic, cameraNumberTopic, interfaceNumberTopic); //positionTopic, graspTopic, rotationTopic,
        androidNode.addNodeMain(imageStreamNodeMain);

        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.tcp))) )
            androidNode.addTopics(velocityTopic, cameraPTZTopic );
        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.ros_cimage))) ) {
            androidNode.addNodeMain(imageStreamNodeMain);
            statelessGestureHandler = new MultiGestureArea(this, imageStreamNodeMain);
        }else
            imageStreamNodeMain.setVisibility( View.GONE );
        if ( !MainActivity.PREFERENCES.containsKey((getString(R.string.mjpeg))) )
            mjpegView.setVisibility(View.GONE);
        else {
            statelessGestureHandler = new MultiGestureArea(this, mjpegView);
        }
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
                if ( MainActivity.PREFERENCES.containsKey((getString(R.string.mjpeg))) )
                    mjpegView.setSource(MjpegInputStream.read(MainActivity.PREFERENCES.getProperty(getString(R.string.STREAM_URL), "")));
                while(running){
                    try {
                        Thread.sleep(100);
                        updateVelocity();
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
        mjpegView.stopPlayback();
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
        emergencyTopic.setPublisher_bool(false);
        nodeMain.forceShutdown();
        if (udpCommCommand!= null)
            udpCommCommand.destroy();
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

    private void updateVelocity() {
        float y = 0.f;
        float rot = 0.f;

        float cameraControlHorizontal = 0.f;
        float cameraControlVertical = 0.f;

        if(statelessGestureHandler.isDetectingMultiGesture()) {
            y = -statelessGestureHandler.getPosY();
            rot = statelessGestureHandler.getRotation();
        }

        if(statelessGestureHandler.isDetectingGesture()) {

            if( statelessGestureHandler.isLongPress() ){

                statelessGestureHandler.setLongPress(false);
                if ( p3dxNumberTopic.getPublisher_int() == 0 ){
                    p3dxNumberTopic.setPublisher_int( 1 );
                }else{
                    p3dxNumberTopic.setPublisher_int(0);
                }
                p3dxNumberTopic.publishNow();

                runOnUiThread(new Runnable() {
                    public void run() {
                        if (p3dxNumberTopic.getPublisher_int() == 0) {
                            deviceMsim.setChecked(true);
                            deviceP3DX1.setChecked(false);
                            cameraSim.setChecked(true);
                            cameraSim.setAlpha(1.f);
                            cameraTopDown.setChecked(false);
                            cameraTopDown.setAlpha(.3f);
                            cameraFirstPerson.setChecked(false);
                            cameraFirstPerson.setAlpha(.3f);
                            cameraNumberTopic.setPublisher_int(0);
                            cameraNumberTopic.publishNow();
                        } else {
                            deviceMsim.setChecked(false);
                            deviceP3DX1.setChecked(true);
                            cameraSim.setChecked(false);
                            cameraSim.setAlpha(.3f);
                            cameraTopDown.setChecked(true);
                            cameraTopDown.setAlpha(1.f);
                            cameraFirstPerson.setChecked(false);
                            cameraFirstPerson.setAlpha(1.f);
                            cameraNumberTopic.setPublisher_int(1);
                            cameraNumberTopic.publishNow();
                        }
                    }
                });

            }

            if( statelessGestureHandler.isDoubleTap() ){

                statelessGestureHandler.setDoubleTap(false);
                int device = p3dxNumberTopic.getPublisher_int();
                if( device == 0 ){
                    cameraNumberTopic.setPublisher_int(0);
                    cameraNumberTopic.publishNow();
                }else{
                    if ( cameraNumberTopic.getPublisher_int() == 1 ){
                        cameraNumberTopic.setPublisher_int( 2 );
                    }else{
                        cameraNumberTopic.setPublisher_int( 1 );
                    }
                    cameraNumberTopic.publishNow();
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        String msg = "Camera: ";
                        if (cameraNumberTopic.getPublisher_int() == 0 ) {
                            msg += "Simulation";
                            cameraSim.setChecked(true);
                            cameraTopDown.setChecked(false);
                            cameraFirstPerson.setChecked(false);
                        } else if (cameraNumberTopic.getPublisher_int() == 1 ) {
                            msg += "Top-Down";
                            cameraSim.setChecked(false);
                            cameraTopDown.setChecked(true);
                            cameraFirstPerson.setChecked(false);
                        } else if (cameraNumberTopic.getPublisher_int() == 2 ) {
                            msg += "First Person";
                            cameraSim.setChecked(false);
                            cameraTopDown.setChecked(false);
                            cameraFirstPerson.setChecked(true);
                        } else if (cameraNumberTopic.getPublisher_int() == 3) {
                            msg += "Web Cam";
                        }
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if( statelessGestureHandler.isFling() ){
                statelessGestureHandler.setFling(false);
                cameraControlHorizontal = -statelessGestureHandler.getFlingX();
                cameraControlVertical = -statelessGestureHandler.getFlingY();
            }
        }

        float steer = rot/180.f;
        float acceleration= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, y, getResources().getDisplayMetrics()) / 1200;

        if(Math.abs(steer) < 0.1f)
            steer=0.f;
        if(Math.abs(acceleration) < 0.1f)
            acceleration=0.f;
        if( acceleration > 0.25f)
            acceleration=0.25f;
        else if( acceleration < -0.25f)
            acceleration=-0.25f;

        int ptz = -1;
        if(cameraControlHorizontal < -0.5f)
            ptz=4;
        else if(cameraControlHorizontal > 0.5f)
            ptz=3;

        if(cameraControlVertical < -0.5f)
            ptz=2;
        else if(cameraControlVertical > 0.5f)
            ptz=1;


        String data="velocity;"+acceleration+";"+steer;
        if(cameraNumberTopic.getPublisher_int()==2 && ptz!=-1){
            data+=";ptz;"+ptz;
        }else{
            ptz=-1;
        }
        cameraPTZTopic.setPublisher_int(ptz);

        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.udp))) )
            udpCommCommand.sendData(data.getBytes());

        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.tcp))) ){
            velocityTopic.setPublisher_linear(new float[]{acceleration, 0, 0});
            velocityTopic.setPublisher_angular(new float[]{0, 0, steer});
            velocityTopic.publishNow();
            cameraPTZTopic.publishNow();
        }

        msg.append(String.format("Steer: %.4f | Throttle: %.4f ", steer, acceleration ));
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

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        nodeMain=(NodeMainExecutorService)nodeMainExecutor;
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic( MainActivity.PREFERENCES.getProperty( getString(R.string.HOSTNAME) ), getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }

}