package uniandes.disc.imagine.android_hri.mobile_interaction.interfaces;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
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
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.Int32Topic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.TwistTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.AndroidNode;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.Gamepad;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.MjpegInputStream;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.MjpegView;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.UDPComm;

public class GamepadInterface extends RosActivity {

    private static final String TAG = "GamepadInterface";
    private static final String NODE_NAME="/android_"+TAG.toLowerCase();

    private NodeMainExecutorService nodeMain;
    private RosImageView<CompressedImage> imageStreamNodeMain;
    private ImageView targetImage;

    private AndroidNode androidNode;
    private BooleanTopic emergencyTopic;
    private Int32Topic interfaceNumberTopic;
    private Int32Topic cameraNumberTopic;
    private Int32Topic cameraPTZTopic;
    private Int32Topic p3dxNumberTopic;
    private TwistTopic velocityTopic;

    private Gamepad gamepad;
    private boolean running=true;
    private boolean changingCamera=false;
    private boolean changingP3DX=false;

    private ImageView joystickRotationNodeMain;
    private TextView textPTZ;
    private UDPComm udpCommCommand;
    private MjpegView mjpegView;

    private int currentCamera = 0;
    private int currentP3DX = -1;

    public GamepadInterface() {
        super(TAG, TAG, URI.create(MainActivity.PREFERENCES.getProperty( "ROS_MASTER_URI" )));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        setContentView(R.layout.interface_gamepad);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageStreamNodeMain = (RosImageView<CompressedImage>) findViewById(R.id.streamingView);

        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.udp))) )
            udpCommCommand = new UDPComm( MainActivity.PREFERENCES.getProperty( getString(R.string.MASTER) ) , Integer.parseInt(getString(R.string.udp_port)) );

        mjpegView = (MjpegView) findViewById(R.id.mjpegView);
        mjpegView.setDisplayMode(MjpegView.SIZE_BEST_FIT);
        mjpegView.showFps(true);

        textPTZ = (TextView) findViewById(R.id.rotationTextView);
        joystickRotationNodeMain = (ImageView) findViewById(R.id.virtual_joystick_rot);
        targetImage = (ImageView) findViewById(R.id.targetView);
        velocityTopic = new TwistTopic();
        velocityTopic.publishTo(getString(R.string.topic_rosariavel), false, 10);
        velocityTopic.setPublishingFreq(100);

        interfaceNumberTopic = new Int32Topic();
        interfaceNumberTopic.publishTo(getString(R.string.topic_interfacenumber), true, 0);
        interfaceNumberTopic.setPublishingFreq(100);
        interfaceNumberTopic.setPublisher_int(4);

        cameraNumberTopic = new Int32Topic();
        cameraNumberTopic.publishTo(getString(R.string.topic_camera_number), false, 10);
        cameraNumberTopic.setPublishingFreq(10);
        cameraNumberTopic.setPublisher_int(currentCamera);

        cameraPTZTopic = new Int32Topic();
        cameraPTZTopic.publishTo(getString(R.string.topic_camera_ptz), false, 10);
        cameraPTZTopic.setPublishingFreq(10);
        cameraPTZTopic.setPublisher_int(-1);

        p3dxNumberTopic = new Int32Topic();
        p3dxNumberTopic.publishTo(getString(R.string.topic_p3dx_number), false, 10);
        p3dxNumberTopic.setPublishingFreq(10);
        p3dxNumberTopic.setPublisher_int(currentP3DX);

        emergencyTopic = new BooleanTopic();
        emergencyTopic.publishTo(getString(R.string.topic_emergencystop), true, 0);
        emergencyTopic.setPublishingFreq(100);
        emergencyTopic.setPublisher_bool(true);

        androidNode = new AndroidNode(NODE_NAME);
        androidNode.addTopics(emergencyTopic, interfaceNumberTopic, cameraNumberTopic, p3dxNumberTopic);
        //androidNode.addNodeMain(imageStreamNodeMain);

        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.tcp))) )
            androidNode.addTopics(velocityTopic ,cameraPTZTopic);
        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.ros_cimage))) )
            androidNode.addNodeMain(imageStreamNodeMain);
        else
            imageStreamNodeMain.setVisibility( View.GONE );
        if ( !MainActivity.PREFERENCES.containsKey((getString(R.string.mjpeg))) )
            mjpegView.setVisibility(View.GONE);

        gamepad = new Gamepad(this);

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

        if(gamepad.isAttached()){
            Toast.makeText(getApplicationContext(), getString(R.string.gamepad_on_msg), Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(), getString(R.string.gamepad_off_msg), Toast.LENGTH_LONG).show();
            Thread exitActivity = new Thread(){
                public void run(){
                    try {
                        Thread.sleep(3000);
                        finish();
                    } catch (InterruptedException e) {
                        e.getStackTrace();
                    }
                }
            };
            exitActivity.start();
        }

        Thread threadGamepad = new Thread(){
            public void run(){
                if ( MainActivity.PREFERENCES.containsKey((getString(R.string.mjpeg))) )
                    mjpegView.setSource(MjpegInputStream.read(MainActivity.PREFERENCES.getProperty(getString(R.string.STREAM_URL), "")));
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
        threadGamepad.start();
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
        if (udpCommCommand!= null)
            udpCommCommand.destroy();
        running=false;
        super.onDestroy();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent){
        super.dispatchGenericMotionEvent(motionEvent);
        return gamepad.dispatchGenericMotionEvent(motionEvent);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent){
        super.dispatchKeyEvent(keyEvent);
        return gamepad.dispatchKeyEvent(keyEvent);
    }

    private void updateVelocity(){
        if(!gamepad.isAttached())
            return;
        float steer=-gamepad.getAxisValue(MotionEvent.AXIS_X);
        float acceleration=-gamepad.getAxisValue(MotionEvent.AXIS_Y)/2f;
        acceleration += (gamepad.getAxisValue(MotionEvent.AXIS_RTRIGGER) - gamepad.getAxisValue(MotionEvent.AXIS_LTRIGGER))/2f;

        float cameraControlHorizontal= gamepad.getAxisValue(MotionEvent.AXIS_Z);
        float cameraControlVertical=-gamepad.getAxisValue(MotionEvent.AXIS_RZ);
        //float acceleration=( gamepad.getAxisValue(MotionEvent.AXIS_RTRIGGER) - gamepad.getAxisValue(MotionEvent.AXIS_LTRIGGER) )/2f;

        if(Math.abs(steer) < 0.1f)
            steer=0.f;
        if(Math.abs(acceleration) < 0.1f)
            acceleration=0.f;

        int ptz = -1;
        if(cameraControlHorizontal < -0.5f)
            ptz=3;
        else if(cameraControlHorizontal > 0.5f)
            ptz=4;

        if(cameraControlVertical < -0.5f)
            ptz=2;
        else if(cameraControlVertical > 0.5f)
            ptz=1;

        String data="velocity;"+acceleration+";"+steer;
        if(cameraNumberTopic.getPublisher_int()==2 && ptz!=-1){
            cameraPTZTopic.setPublisher_int(ptz);
            data+=";ptz;"+ptz;
        }

        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.udp))) )
            udpCommCommand.sendData(data.getBytes());

        if ( MainActivity.PREFERENCES.containsKey((getString(R.string.tcp))) ){
            velocityTopic.setPublisher_linear(new float[]{acceleration, 0, 0});
            velocityTopic.setPublisher_angular(new float[]{0, 0, steer});
            velocityTopic.publishNow();
            cameraPTZTopic.publishNow();
        }

        if( gamepad.getButtonValue(KeyEvent.KEYCODE_DPAD_LEFT) == 1)
            changeCamera(0);
        else if( gamepad.getButtonValue(KeyEvent.KEYCODE_DPAD_UP) == 1)
            changeCamera(1);
        else if( gamepad.getButtonValue(KeyEvent.KEYCODE_DPAD_RIGHT) == 1)
            changeCamera(2);

        if( gamepad.getButtonValue(KeyEvent.KEYCODE_BUTTON_A) == 1)
            changeP3DX(0);
        else if( gamepad.getButtonValue(KeyEvent.KEYCODE_BUTTON_B) == 1)
            changeP3DX(1);
    }

    private void changeCamera(final int camera){
        if (changingCamera)
            return;

        Thread threadCamera = new Thread(){
            public void run(){
                changingCamera=true;
                currentCamera++;
                if(currentCamera > 2)
                    currentCamera = 0;
                cameraNumberTopic.setPublisher_int(camera);
                cameraNumberTopic.publishNow();

                String msg = "Camera: ";
                if(currentCamera==0) {
                    msg += "Simulation";
                    changeVisibility(joystickRotationNodeMain, View.INVISIBLE);
                    changeVisibility(textPTZ , View.INVISIBLE);
                }else if(currentCamera==1){
                    msg+= "Top-Down";
                    changeVisibility(joystickRotationNodeMain, View.INVISIBLE);
                    changeVisibility(textPTZ , View.INVISIBLE);
                }else if(currentCamera==2){
                    msg+= "First Person";
                    changeVisibility(joystickRotationNodeMain, View.VISIBLE);
                    changeVisibility(textPTZ , View.VISIBLE);
                }else if(currentCamera==3) {
                    msg += "Web Cam";
                    changeVisibility(joystickRotationNodeMain, View.INVISIBLE);
                    changeVisibility(textPTZ, View.INVISIBLE);
                }
                showToast(msg);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.getStackTrace();
                }
                changingCamera=false;
            }
        };
        threadCamera.start();
    }

    private void changeP3DX(final int p3dx){
        if (changingP3DX)
            return;
        Thread threadCamera = new Thread(){
            public void run(){
                changingP3DX=true;
                currentP3DX++;
                if(currentP3DX > 2)
                    currentP3DX=0;
                p3dxNumberTopic.setPublisher_int(p3dx);
                p3dxNumberTopic.publishNow();

                String msg = "Control: ";
                if(currentP3DX==0)
                    msg+= "Simulation";
                else if(currentP3DX==1)
                    msg+= "P3DX1";
                else if(currentP3DX==2)
                    msg+= "P3DX2";
                else if(currentP3DX==-1)
                    msg+= "ALL";
                showToast(msg);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.getStackTrace();
                }
                changingP3DX=false;
            }
        };
        threadCamera.start();
    }

    public void changeVisibility(final View view, final int visibility){
        runOnUiThread(new Runnable() {
            public void run() {
                view.setVisibility( visibility );
            }
        });
    }

    public void showToast(final String msg) {

        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        nodeMain=(NodeMainExecutorService)nodeMainExecutor;
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic( MainActivity.PREFERENCES.getProperty( getString(R.string.HOSTNAME) ), getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }
}
