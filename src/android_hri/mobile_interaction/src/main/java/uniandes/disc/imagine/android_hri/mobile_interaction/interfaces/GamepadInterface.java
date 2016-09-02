package uniandes.disc.imagine.android_hri.mobile_interaction.interfaces;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
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

import sensor_msgs.CompressedImage;
import uniandes.disc.imagine.android_hri.mobile_interaction.MainActivity;
import uniandes.disc.imagine.android_hri.mobile_interaction.R;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.BooleanTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.Int32Topic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.TwistTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.AndroidNode;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.Gamepad;

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

    private int currentCamera = 0;
    private int currentP3DX = -1;

    public GamepadInterface() {
        super(TAG, TAG, URI.create(MainActivity.ROS_MASTER_URI));;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        setContentView(R.layout.interface_gamepad);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageStreamNodeMain = (RosImageView<CompressedImage>) findViewById(R.id.streamingView);

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
        androidNode.addTopics(emergencyTopic, velocityTopic, interfaceNumberTopic, cameraNumberTopic, cameraPTZTopic, p3dxNumberTopic);
        androidNode.addNodeMain(imageStreamNodeMain);

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
                while(running){
                    try {
                        Thread.sleep(10);
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

        velocityTopic.setPublisher_linear(new float[]{acceleration, 0, 0});
        velocityTopic.setPublisher_angular(new float[]{0, 0, steer});
        velocityTopic.publishNow();

        if(currentCamera==2 && ptz!=-1){
            cameraPTZTopic.setPublisher_int(ptz);
            cameraPTZTopic.publishNow();

        }

        if( gamepad.getButtonValue(KeyEvent.KEYCODE_BUTTON_A) == 1)
            changeCamera();
        if( gamepad.getButtonValue(KeyEvent.KEYCODE_BUTTON_Y) == 1)
            changeP3DX();
    }

    private void changeCamera(){
        if (changingCamera)
            return;

        Thread threadCamera = new Thread(){
            public void run(){
                changingCamera=true;
                currentCamera++;
                if(currentCamera>3)
                    currentCamera=0;
                cameraNumberTopic.setPublisher_int(currentCamera);
                cameraNumberTopic.publishNow();

                String msg = "Camera: ";
                if(currentCamera==0)
                    msg+= "Simulation";
                else if(currentCamera==1)
                    msg+= "Top-Down";
                else if(currentCamera==2)
                    msg+= "First Person";
                else if(currentCamera==3)
                    msg+= "Web Cam";
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

    private void changeP3DX(){
        if (changingP3DX)
            return;
        Thread threadCamera = new Thread(){
            public void run(){
                changingP3DX=true;
                currentP3DX++;
                if(currentP3DX>2)
                    currentP3DX=-1;
                p3dxNumberTopic.setPublisher_int(currentP3DX);
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
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(MainActivity.ROS_HOSTNAME, getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }
}
