package uniandes.disc.imagine.android_hri.mobile_interaction.interfaces;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.NodeMainExecutorService;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.nio.ByteBuffer;

import sensor_msgs.CompressedImage;
import uniandes.disc.imagine.android_hri.mobile_interaction.MainActivity;
import uniandes.disc.imagine.android_hri.mobile_interaction.R;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.BooleanTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.Int32Topic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.TwistTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.AndroidNode;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.MjpegInputStream;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.MjpegView;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.UDPComm;
import uniandes.disc.imagine.android_hri.mobile_interaction.widget.CustomVirtualJoystickView;

public class ScreenJoystickInterface extends RosActivity {

    private static final String TAG = "ScreenJoystickInterface";
    private static final String NODE_NAME="/android_"+TAG.toLowerCase();

    private NodeMainExecutorService nodeMain;
    private CustomVirtualJoystickView joystickPositionNodeMain;
    private CustomVirtualJoystickView joystickRotationNodeMain;
    private RosImageView<CompressedImage> imageStreamNodeMain;

    private ToggleButton toggleCamera1;
    private ToggleButton toggleCamera2;
    private ToggleButton toggleCamera3;
    private ToggleButton toggleCamera4;
    private ToggleButton toggleAllControl;
    private ToggleButton toggleSimulatorControl;
    private ToggleButton toggleP3DX1Control;
    private ToggleButton toggleP3DX2Control;

    private AndroidNode androidNode;
    private BooleanTopic emergencyTopic;
    private Int32Topic interfaceNumberTopic;
    private Int32Topic cameraNumberTopic;
    private Int32Topic cameraPTZTopic;
    private Int32Topic p3dxNumberTopic;
    private TwistTopic velocityTopic;

    private ImageView targetImage;
    private UDPComm udpCommCommand;
    private MjpegView mjpegView;

    private boolean running=true;
    private float maxTargetSpeed;

    public ScreenJoystickInterface() {
        super(TAG, TAG, URI.create(MainActivity.ROS_MASTER_URI));;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        setContentView(R.layout.interface_screenjoystick);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        maxTargetSpeed=TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Float.parseFloat(getString(R.string.max_target_speed)), getResources().getDisplayMetrics());
        udpCommCommand = new UDPComm(MainActivity.ROS_MASTER, 58528);

        mjpegView = (MjpegView) findViewById(R.id.mjpegView);
        mjpegView.setDisplayMode(MjpegView.SIZE_BEST_FIT);
        mjpegView.showFps(true);

        joystickPositionNodeMain = (CustomVirtualJoystickView) findViewById(R.id.virtual_joystick_pos);
        joystickRotationNodeMain = (CustomVirtualJoystickView) findViewById(R.id.virtual_joystick_rot);
        joystickPositionNodeMain.setHolonomic(true);
        joystickRotationNodeMain.setHolonomic(true);

        imageStreamNodeMain = (RosImageView<CompressedImage>) findViewById(R.id.streamingView);

        velocityTopic =  new TwistTopic();
        velocityTopic.publishTo(getString(R.string.topic_rosariavel), false, 10);
        velocityTopic.setPublishingFreq(100);

        interfaceNumberTopic = new Int32Topic();
        interfaceNumberTopic.publishTo(getString(R.string.topic_interfacenumber), true, 0);
        interfaceNumberTopic.setPublishingFreq(100);
        interfaceNumberTopic.setPublisher_int(2);

        cameraNumberTopic = new Int32Topic();
        cameraNumberTopic.publishTo(getString(R.string.topic_camera_number), false, 10);
        cameraNumberTopic.setPublishingFreq(10);
        cameraNumberTopic.setPublisher_int(0);

        cameraPTZTopic = new Int32Topic();
        cameraPTZTopic.publishTo(getString(R.string.topic_camera_ptz), false, 10);
        cameraPTZTopic.setPublishingFreq(10);
        cameraPTZTopic.setPublisher_int(-1);

        p3dxNumberTopic = new Int32Topic();
        p3dxNumberTopic.publishTo(getString(R.string.topic_p3dx_number), false, 10);
        p3dxNumberTopic.setPublishingFreq(10);
        p3dxNumberTopic.setPublisher_int(-1);

        emergencyTopic = new BooleanTopic();
        emergencyTopic.publishTo(getString(R.string.topic_emergencystop), true, 0);
        emergencyTopic.setPublishingFreq(100);
        emergencyTopic.setPublisher_bool(true);

        androidNode = new AndroidNode(NODE_NAME);
        androidNode.addTopics(emergencyTopic, interfaceNumberTopic, cameraNumberTopic, p3dxNumberTopic); //velocityTopic ,cameraPTZTopic
        //androidNode.addNodeMain(imageStreamNodeMain);

        //In case you still need to publish the virtualjoysticks values, set the topicname and then add the joysticks to the AndroidNode
        //joystickPositionNodeMain.setTopicName(JOYPOS_TOPIC);
        //joystickRotationNodeMain.setTopicName(JOYROT_TOPIC);
        //androidNode.addNodeMains(joystickPositionNodeMain,joystickRotationNodeMain);

        toggleCamera1 = (ToggleButton)findViewById(R.id.toggleCamera1);
        toggleCamera2 = (ToggleButton)findViewById(R.id.toggleCamera2);
        toggleCamera3 = (ToggleButton)findViewById(R.id.toggleCamera3);
        toggleCamera4 = (ToggleButton)findViewById(R.id.toggleCamera4);
        toggleAllControl = (ToggleButton)findViewById(R.id.toggleAllControl);
        toggleSimulatorControl = (ToggleButton)findViewById(R.id.toggleSimulatorControl);
        toggleP3DX1Control = (ToggleButton)findViewById(R.id.toggleP3DX1Control);
        toggleP3DX2Control = (ToggleButton)findViewById(R.id.toggleP3DX2Control);

        targetImage = (ImageView) findViewById(R.id.targetView);

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



        toggleCamera1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Camera: Simulation", Toast.LENGTH_SHORT).show();
                    toggleCamera1.setChecked(true);
                    toggleCamera2.setChecked(false);
                    toggleCamera3.setChecked(false);
                    toggleCamera4.setChecked(false);
                    cameraNumberTopic.setPublisher_int(0);
                    cameraNumberTopic.publishNow();
                }
            }
        });

        toggleCamera2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Camera: Top-Down", Toast.LENGTH_SHORT).show();
                    toggleCamera1.setChecked(false);
                    toggleCamera2.setChecked(true);
                    toggleCamera3.setChecked(false);
                    toggleCamera4.setChecked(false);
                    cameraNumberTopic.setPublisher_int(1);
                    cameraNumberTopic.publishNow();
                }
            }
        });

        toggleCamera3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), "Camera: First Person", Toast.LENGTH_SHORT).show();
                    toggleCamera1.setChecked(false);
                    toggleCamera2.setChecked(false);
                    toggleCamera3.setChecked(true);
                    toggleCamera4.setChecked(false);
                    cameraNumberTopic.setPublisher_int(2);
                    cameraNumberTopic.publishNow();
                }
            }
        });

        toggleCamera4.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getApplicationContext(), "Camera: Web Cam", Toast.LENGTH_SHORT).show();
                    toggleCamera1.setChecked(false);
                    toggleCamera2.setChecked(false);
                    toggleCamera3.setChecked(false);
                    toggleCamera4.setChecked(true);
                    cameraNumberTopic.setPublisher_int(3);
                    cameraNumberTopic.publishNow();
                }
            }
        });

        toggleAllControl.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getApplicationContext(), "Control: ALL", Toast.LENGTH_SHORT).show();
                    toggleAllControl.setChecked(true);
                    toggleSimulatorControl.setChecked(false);
                    toggleP3DX1Control.setChecked(false);
                    toggleP3DX2Control.setChecked(false);
                    p3dxNumberTopic.setPublisher_int(-1);
                    p3dxNumberTopic.publishNow();
                }
            }
        });

        toggleSimulatorControl.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getApplicationContext(), "Control: Simulation", Toast.LENGTH_SHORT).show();
                    toggleAllControl.setChecked(false);
                    toggleSimulatorControl.setChecked(true);
                    toggleP3DX1Control.setChecked(false);
                    toggleP3DX2Control.setChecked(false);
                    p3dxNumberTopic.setPublisher_int(0);
                    p3dxNumberTopic.publishNow();
                }
            }
        });

        toggleP3DX1Control.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getApplicationContext(), "Control: P3DX1", Toast.LENGTH_SHORT).show();
                    toggleAllControl.setChecked(false);
                    toggleSimulatorControl.setChecked(false);
                    toggleP3DX1Control.setChecked(true);
                    toggleP3DX2Control.setChecked(false);
                    p3dxNumberTopic.setPublisher_int(1);
                    p3dxNumberTopic.publishNow();
                }
            }
        });

        toggleP3DX2Control.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getApplicationContext(), "Control: P3DX2", Toast.LENGTH_SHORT).show();
                    toggleAllControl.setChecked(false);
                    toggleSimulatorControl.setChecked(false);
                    toggleP3DX1Control.setChecked(false);
                    toggleP3DX2Control.setChecked(true);
                    p3dxNumberTopic.setPublisher_int(2);
                    p3dxNumberTopic.publishNow();
                }
            }
        });

        Thread threadMjpeg = new Thread(){
            public void run(){
                mjpegView.setSource(MjpegInputStream.read(MainActivity.ROS_STREAM_URL));
                while(running){
                    try {
                        Thread.sleep(100);
                        updateVelocity();
                    } catch (InterruptedException e) {
                        e.getStackTrace();
                    }
                }
            }
        };
        threadMjpeg.start();

    }

    private void onPostLayout(){
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
        udpCommCommand.destroy();
        running=false;
        super.onDestroy();
    }

    private void updateVelocity(){

        float steer=joystickPositionNodeMain.getAxisY();
        float acceleration=joystickPositionNodeMain.getAxisX()/2;

        float cameraControlHorizontal=joystickRotationNodeMain.getAxisY();
        float cameraControlVertical=joystickRotationNodeMain.getAxisX();

        if(Math.abs(steer) < 0.1f)
            steer=0.f;
        if(Math.abs(acceleration) < 0.1f)
            acceleration=0.f;

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
        if(cameraNumberTopic.getPublisher_int()==2 && ptz!=-1)
            data+=";ptz;"+ptz;
        udpCommCommand.sendData(data.getBytes());

        /*
        velocityTopic.setPublisher_linear(new float[]{acceleration, 0, 0});
        velocityTopic.setPublisher_angular(new float[]{0, 0, steer});
        velocityTopic.publishNow();

        if(cameraNumberTopic.getPublisher_int()==2 && ptz!=-1){
            cameraPTZTopic.setPublisher_int(ptz);
            cameraPTZTopic.publishNow();
        }*/
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        nodeMain=(NodeMainExecutorService)nodeMainExecutor;
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(MainActivity.ROS_HOSTNAME, getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }
}
