package uniandes.disc.imagine.android_hri.mobile_interaction;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

import uniandes.disc.imagine.android_hri.mobile_interaction.topic.CompressedImageTopic;
import uniandes.disc.imagine.android_hri.mobile_interaction.utils.AndroidNode;
import uniandes.disc.imagine.android_hri.mobile_interaction.widget.CustomCameraView;

public class CameraActivity extends RosActivity {

	private static final String TAG = "CameraActivity";
    private static final String NODE_NAME="/android_"+TAG.toLowerCase();

    private int cameraId;
    private CustomCameraView cameraView;

    private final int DISABLED = Color.RED;
    private final int ENABLED = Color.GREEN;
    private final int TRANSITION = Color.rgb(255,195,77); //orange

    private NodeMainExecutorService nodeMain;
    private AndroidNode androidNode;
    private CompressedImageTopic cameraTopic;

    private static final boolean debug = true;

    private boolean running = true;
    private boolean firstRun=true;

    public CameraActivity() {
        super(TAG, TAG, URI.create(MainActivity.ROS_MASTER));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

    	Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        cameraView = (CustomCameraView) findViewById(R.id.custom_camera_view);
        cameraView.setResolution(320, 180);

        cameraTopic = new CompressedImageTopic();
        cameraTopic.publishTo(getString(R.string.topic_streaming), false, 1);
        cameraTopic.setPublishingFreq(33);

        androidNode = new AndroidNode(NODE_NAME);
        androidNode.addTopics(cameraTopic);

        Thread threadTarget = new Thread(){
            public void run(){
                try {
                    while(running){
                        while(!cameraView.hasImageChanged()){
                            Thread.sleep(1);
                        }
                        cameraTopic.setPublisher_image(cameraView.getImage());
                        cameraTopic.publishNow();
                        Thread.sleep(33);
                    }
                } catch (InterruptedException e) {
                    e.getStackTrace();
                }

            }
        };
        threadTarget.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        running=true;
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onDestroy() {
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            int numberOfCameras = Camera.getNumberOfCameras();
            final Toast toast;
            if (numberOfCameras > 1) {
                cameraId = (cameraId + 1) % numberOfCameras;
                cameraView.releaseCamera();
                cameraView.setCamera(Camera.open(cameraId));
                toast = Toast.makeText(this, "Switching cameras.", Toast.LENGTH_SHORT);
            } else {
                toast = Toast.makeText(this, "No alternative cameras to switch to.", Toast.LENGTH_SHORT);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast.show();
                }
            });
        }
        return true;
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        cameraId = 0;
        cameraView.setCamera(Camera.open(cameraId));
        nodeMain=(NodeMainExecutorService)nodeMainExecutor;
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }

}
