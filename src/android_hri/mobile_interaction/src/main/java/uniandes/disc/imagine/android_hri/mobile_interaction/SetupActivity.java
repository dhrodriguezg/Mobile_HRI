package uniandes.disc.imagine.android_hri.mobile_interaction;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.ros.address.InetAddressFactory;
import org.ros.android.BitmapFromCompressedImage;
import org.ros.android.NodeMainExecutorService;
import org.ros.android.RosActivity;
import org.ros.android.view.RosImageView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

import sensor_msgs.CompressedImage;
import uniandes.disc.imagine.android_hri.mobile_interaction.service.TestService;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.BooleanTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.Int32Topic;
import uniandes.disc.imagine.android_hri.mobile_interaction.topic.PointTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.touchscreen.MultiTouchArea;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.AndroidNode;


public class SetupActivity extends RosActivity {

	private static final String TAG = "SetupActivity";
    private static final String NODE_NAME="/android_"+TAG.toLowerCase();

    private final int DISABLED = Color.RED;
    private final int ENABLED = Color.GREEN;
    private final int TRANSITION = Color.rgb(255,195,77); //orange

    private NodeMainExecutorService nodeMain;

    private static final boolean debug = true;
    private MultiTouchArea dragHandler = null;

    private RosImageView<CompressedImage> imageStreamNodeMain;
    private boolean running = true;
    private boolean updateCenter=false;

    private float correctionX =0.f;
    private float correctionY =0.f;
    private float scaleCorrection=1.f;
    private float scaleTemp=1.f;
    private float traslationTempX=0.f;
    private float traslationTempY=0.f;

    private ToggleButton buttonON;
    private TextView statusPos1_ON;
    private TextView statusPos2_ON;
    private TextView statusSpread_ON;
    private TextView statusGrasp_ON;

    private ToggleButton buttonOFF;
    private TextView statusPos1_OFF;
    private TextView statusPos2_OFF;
    private TextView statusSpread_OFF;
    private TextView statusGrasp_OFF;

    private ToggleButton buttonQuick;

    private AndroidNode androidNode;
    private BooleanTopic emergencyTopic;
    private Int32Topic interfaceNumberTopic;
    private BooleanTopic setupONTopic;
    private BooleanTopic setupOFFTopic;
    private BooleanTopic setupQuickTopic;
    private Int32Topic pos1StateTopic;
    private Int32Topic pos2StateTopic;
    private Int32Topic graspStateTopic;
    private Int32Topic spreadStateTopic;
    private PointTopic trackerPointTopic;
    private PointTopic targetPointTopic;
    private TestService testService;

    private boolean firstRun=true;
    private static final int MAX_TRACKERS=2;
    private int trackerNumber=0;

    public SetupActivity() {
        super(TAG, TAG, URI.create(MainActivity.ROS_MASTER));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_setup);

        statusPos1_ON = (TextView) findViewById(R.id.statusPos1_ON);
        statusPos2_ON = (TextView) findViewById(R.id.statusPos2_ON);
        statusSpread_ON = (TextView) findViewById(R.id.statusSpread_ON);
        statusGrasp_ON = (TextView) findViewById(R.id.statusGrasp_ON);
        statusPos1_OFF = (TextView) findViewById(R.id.statusPos1_OFF);
        statusPos2_OFF = (TextView) findViewById(R.id.statusPos2_OFF);
        statusSpread_OFF = (TextView) findViewById(R.id.statusSpread_OFF);
        statusGrasp_OFF = (TextView) findViewById(R.id.statusGrasp_OFF);

        imageStreamNodeMain = (RosImageView<CompressedImage>) findViewById(R.id.streamingView);
        imageStreamNodeMain.setTopicName(getString(R.string.topic_streaming));
        imageStreamNodeMain.setMessageType(getString(R.string.topic_streaming_msg));
        imageStreamNodeMain.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageStreamNodeMain.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                onPostLayout();
            }
        });

        setupONTopic = new BooleanTopic();
        setupONTopic.publishTo(getString(R.string.topic_setup_on), false, 100);

        setupOFFTopic = new BooleanTopic();
        setupOFFTopic.publishTo(getString(R.string.topic_setup_off), false, 100);

        setupQuickTopic = new BooleanTopic();
        setupQuickTopic.publishTo(getString(R.string.topic_setup_quick), false, 100);

        pos1StateTopic = new Int32Topic();
        pos1StateTopic.subscribeTo(getString(R.string.topic_setup_pos1state));
        pos2StateTopic = new Int32Topic();
        pos2StateTopic.subscribeTo(getString(R.string.topic_setup_pos2state));
        graspStateTopic = new Int32Topic();
        graspStateTopic.subscribeTo(getString(R.string.topic_setup_grasp));
        spreadStateTopic = new Int32Topic();
        spreadStateTopic.subscribeTo(getString(R.string.topic_setup_spread));

        trackerPointTopic = new PointTopic();
        trackerPointTopic.publishTo(getString(R.string.topic_trackerpoint), false, 10);

        targetPointTopic = new PointTopic();
        targetPointTopic.publishTo(getString(R.string.topic_targetpoint), false, 10);

        interfaceNumberTopic = new Int32Topic();
        interfaceNumberTopic.publishTo(getString(R.string.topic_interfacenumber), true, 0);
        interfaceNumberTopic.setPublishingFreq(100);
        interfaceNumberTopic.setPublisher_int(-1);

        emergencyTopic = new BooleanTopic();
        emergencyTopic.publishTo(getString(R.string.topic_emergencystop), true, 0);
        emergencyTopic.setPublishingFreq(100);
        emergencyTopic.setPublisher_bool(true);

        testService = new TestService();
        testService.clientOf(getString(R.string.service_test));
        testService.serverOf(getString(R.string.service_test));

        imageStreamNodeMain.setMessageToBitmapCallable(new BitmapFromCompressedImage());
        imageStreamNodeMain.setScaleType(ImageView.ScaleType.MATRIX);

        androidNode = new AndroidNode(NODE_NAME);
        androidNode.addTopics(pos1StateTopic, pos2StateTopic, graspStateTopic, spreadStateTopic, trackerPointTopic, targetPointTopic, emergencyTopic, setupONTopic, setupOFFTopic, setupQuickTopic, interfaceNumberTopic);
        androidNode.addNodeMain(imageStreamNodeMain);
        androidNode.addService(testService);

        buttonON = (ToggleButton)findViewById(R.id.buttonON) ;
        buttonON.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), getString(R.string.task_position_msg), Toast.LENGTH_LONG).show();
                    setupONTopic.setPublisher_bool(true);
                    setupONTopic.publishNow();
                    buttonOFF.setEnabled(false);
                    statusPos1_ON.setBackgroundColor(DISABLED);
                    statusPos2_ON.setBackgroundColor(DISABLED);
                    statusGrasp_ON.setBackgroundColor(DISABLED);
                    statusSpread_ON.setBackgroundColor(DISABLED);
                } else {
                    setupONTopic.setPublisher_bool(false);
                    setupONTopic.publishNow();
                    buttonOFF.setEnabled(true);
                }
            }
        });

        buttonOFF = (ToggleButton)findViewById(R.id.buttonOFF);
        buttonOFF.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), getString(R.string.initial_position_msg), Toast.LENGTH_LONG).show();
                    setupOFFTopic.setPublisher_bool(true);
                    setupOFFTopic.publishNow();
                    buttonON.setEnabled(false);
                    statusPos1_OFF.setBackgroundColor(DISABLED);
                    statusPos2_OFF.setBackgroundColor(DISABLED);
                    statusGrasp_OFF.setBackgroundColor(DISABLED);
                    statusSpread_OFF.setBackgroundColor(DISABLED);
                } else {
                    setupOFFTopic.setPublisher_bool(false);
                    setupOFFTopic.publishNow();
                    buttonON.setEnabled(true);
                }
            }
        });

        buttonQuick = (ToggleButton)findViewById(R.id.buttonQuick);
        buttonQuick.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(getApplicationContext(), getString(R.string.quick_position_msg), Toast.LENGTH_LONG).show();
                    setupQuickTopic.setPublisher_bool(true);
                    setupQuickTopic.publishNow();
                    buttonON.setEnabled(false);
                    buttonOFF.setEnabled(false);
                } else {
                    setupQuickTopic.setPublisher_bool(false);
                    setupQuickTopic.publishNow();
                    buttonON.setEnabled(true);
                    buttonOFF.setEnabled(true);
                }
            }
        });

        ToggleButton emergencyStop = (ToggleButton)findViewById(R.id.emergencyButton) ;
        emergencyStop.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(isChecked){
                    Toast.makeText(getApplicationContext(), getString(R.string.emergency_on_msg), Toast.LENGTH_LONG).show();
                    imageStreamNodeMain.setBackgroundColor(Color.RED);
                    emergencyTopic.setPublisher_bool(false);

                    testService.setA(5);
                    testService.setB(3);
                    testService.callService();
                }else{
                    Toast.makeText(getApplicationContext(), getString(R.string.emergency_off_msg), Toast.LENGTH_LONG).show();
                    imageStreamNodeMain.setBackgroundColor(Color.TRANSPARENT);
                    emergencyTopic.setPublisher_bool(true);
                }
            }
        });

        dragHandler = new MultiTouchArea(this, imageStreamNodeMain);
        dragHandler.enableScaling();
        dragHandler.enableOneFingerGestures();
        dragHandler.enableScroll();

        Thread threadTarget = new Thread(){
            public void run(){
                while(running){
                    try {
                        updateTarget();
                        updateStatuses();
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.getStackTrace();
                    }
                }
            }
        };
        threadTarget.start();
    }

    private void onPostLayout(){

    }

    @Override
    public void onResume() {
        super.onResume();
        emergencyTopic.setPublisher_bool(true);
        running=true;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
        emergencyTopic.setPublisher_bool(false);
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

    public void updateStatuses() {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (buttonON.isChecked()) {
                    if (pos1StateTopic.hasReceivedMsg()) {
                        pos1StateTopic.setHasReceivedMsg(false);
                        switch (pos1StateTopic.getSubcriber_int()) {
                            case -1:
                                statusPos1_ON.setBackgroundColor(DISABLED);
                                break;
                            case 0:
                                statusPos1_ON.setBackgroundColor(TRANSITION);
                                break;
                            case 1:
                                statusPos1_ON.setBackgroundColor(ENABLED);
                                break;
                            default:

                                break;
                        }
                    }

                    if (pos2StateTopic.hasReceivedMsg()) {
                        pos2StateTopic.setHasReceivedMsg(false);
                        switch (pos2StateTopic.getSubcriber_int()) {
                            case -1:
                                statusPos2_ON.setBackgroundColor(DISABLED);
                                break;
                            case 0:
                                statusPos2_ON.setBackgroundColor(TRANSITION);
                                break;
                            case 1:
                                statusPos2_ON.setBackgroundColor(ENABLED);
                                break;
                            default:

                                break;
                        }
                    }

                    if (spreadStateTopic.hasReceivedMsg()) {
                        spreadStateTopic.setHasReceivedMsg(false);
                        switch (spreadStateTopic.getSubcriber_int()) {
                            case -1:
                                statusSpread_ON.setBackgroundColor(DISABLED);
                                break;
                            case 0:
                                statusSpread_ON.setBackgroundColor(TRANSITION);
                                break;
                            case 1:
                                statusSpread_ON.setBackgroundColor(ENABLED);
                                break;
                            default:

                                break;
                        }
                    }

                    if (graspStateTopic.hasReceivedMsg()) {
                        graspStateTopic.setHasReceivedMsg(false);
                        switch (graspStateTopic.getSubcriber_int()) {
                            case -1:
                                statusGrasp_ON.setBackgroundColor(ENABLED);
                                break;
                            case 0:
                                statusGrasp_ON.setBackgroundColor(TRANSITION);
                                break;
                            case 1:
                                statusGrasp_ON.setBackgroundColor(DISABLED);
                                break;
                            default:
                                break;
                        }
                    }

                } else if (buttonOFF.isChecked()) {
                    //logic for OFF
                    if (pos1StateTopic.hasReceivedMsg()) {
                        pos1StateTopic.setHasReceivedMsg(false);
                        switch (pos1StateTopic.getSubcriber_int()) {
                            case -1:
                                statusPos1_OFF.setBackgroundColor(DISABLED);
                                break;
                            case 0:
                                statusPos1_OFF.setBackgroundColor(TRANSITION);
                                break;
                            case 1:
                                statusPos1_OFF.setBackgroundColor(ENABLED);
                                break;
                            default:

                                break;
                        }
                    }

                    if (pos2StateTopic.hasReceivedMsg()) {
                        pos2StateTopic.setHasReceivedMsg(false);
                        switch (pos2StateTopic.getSubcriber_int()) {
                            case -1:
                                statusPos2_OFF.setBackgroundColor(DISABLED);
                                break;
                            case 0:
                                statusPos2_OFF.setBackgroundColor(TRANSITION);
                                break;
                            case 1:
                                statusPos2_OFF.setBackgroundColor(ENABLED);
                                break;
                            default:

                                break;
                        }
                    }

                    if (spreadStateTopic.hasReceivedMsg()) {
                        spreadStateTopic.setHasReceivedMsg(false);
                        switch (spreadStateTopic.getSubcriber_int()) {
                            case -1:
                                statusSpread_OFF.setBackgroundColor(ENABLED);
                                break;
                            case 0:
                                statusSpread_OFF.setBackgroundColor(TRANSITION);
                                break;
                            case 1:
                                statusSpread_OFF.setBackgroundColor(DISABLED);
                                break;
                            default:
                                break;
                        }
                    }

                    if (graspStateTopic.hasReceivedMsg()) {
                        graspStateTopic.setHasReceivedMsg(false);
                        switch (graspStateTopic.getSubcriber_int()) {
                            case -1:
                                statusGrasp_OFF.setBackgroundColor(DISABLED);
                                break;
                            case 0:
                                statusGrasp_OFF.setBackgroundColor(TRANSITION);
                                break;
                            case 1:
                                statusGrasp_OFF.setBackgroundColor(ENABLED);
                                break;
                            default:

                                break;
                        }
                    }
                }
            }
        });

    }

    public void updateTarget() {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                float viewWidth= imageStreamNodeMain.getWidth();
                float viewHeight= imageStreamNodeMain.getHeight();
                float streamWidth= imageStreamNodeMain.getDrawable().getIntrinsicWidth();
                float streamHeight= imageStreamNodeMain.getDrawable().getIntrinsicHeight();

                float focusX=viewWidth / 2;
                float focusY=viewHeight / 2;

                if (firstRun) {
                    float scaleX = viewWidth / streamWidth;
                    float scaleY = viewHeight / streamHeight;
                    dragHandler.setScaleFocusX(focusX);
                    dragHandler.setScaleFocusY(focusY);
                    scaleCorrection = Math.min(scaleX, scaleY);
                    scaleTemp = scaleCorrection;
                    if (dragHandler.getScale() != 1.f || dragHandler.getSingleDragX()!=0)
                        firstRun = false;
                }

                float scale = dragHandler.getScale()*scaleCorrection;

                float imageScaledWidth = scale*streamWidth;
                float imageScaledWHeight = scale*streamHeight;

                float imageScaledCenteredX = (viewWidth - imageScaledWidth)/ 2;
                float imageScaledCenteredY = (viewHeight - imageScaledWHeight)/ 2;

                float imageTraslationX = dragHandler.getSingleDragX()-dragHandler.getSingleIDragX();
                float imageTraslationY = dragHandler.getSingleDragY()-dragHandler.getSingleIDragY();

                float finalScaledCenteredX=imageScaledCenteredX + imageTraslationX + correctionX;
                float finalScaledCenteredY=imageScaledCenteredY + imageTraslationY + correctionY;

                if (scaleTemp==scale) {
                    traslationTempX=finalScaledCenteredX;
                    traslationTempY=finalScaledCenteredY;

                }

                float[] targetPoint = new float[2];
                float[] targetPixel = new float[2];
                float[] errorPoint = new float[2];
                targetPoint[0] = focusX;
                targetPoint[1] = focusY;

                Matrix previowsTMatrix = new Matrix();
                Matrix previowsInverseMatrix = new Matrix();
                previowsTMatrix.setScale(scaleTemp, scaleTemp);
                previowsTMatrix.postTranslate(traslationTempX, traslationTempY);
                previowsTMatrix.invert(previowsInverseMatrix);

                previowsInverseMatrix.mapPoints(targetPixel, targetPoint);

                Matrix postTMatrix = new Matrix();
                postTMatrix.setScale(scale, scale);
                postTMatrix.postTranslate(finalScaledCenteredX, finalScaledCenteredY);
                postTMatrix.mapPoints(errorPoint, targetPixel);
                float scaleCorrectionX = focusX - errorPoint[0];
                float scaleCorrectionY = focusY - errorPoint[1];

                Matrix finalTMatrix = new Matrix();
                finalTMatrix.setScale(scale, scale);
                finalTMatrix.postTranslate(finalScaledCenteredX + scaleCorrectionX, finalScaledCenteredY + scaleCorrectionY);

                imageStreamNodeMain.setImageMatrix(finalTMatrix);

                if(dragHandler.isDetectingOneFingerGesture()){
                    updateCenter=false;
                }else{
                    if(!updateCenter){
                        correctionX += imageTraslationX + scaleCorrectionX;
                        correctionY += imageTraslationY + scaleCorrectionY;
                        dragHandler.resetValuesOnRelease();
                        scaleTemp=scale;
                    }
                    updateCenter=true;
                }

            }
        });
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        nodeMain=(NodeMainExecutorService)nodeMainExecutor;
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }

}
