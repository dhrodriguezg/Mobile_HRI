package uniandes.disc.imagine.android_hri.mobile_interaction;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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
    private Spinner spinnerResolution;

    private static final boolean debug = true;

    private boolean running = true;
    private Switch cameraSwitch;
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

        cameraSwitch = (Switch) findViewById(R.id.cameraSwitch);
        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if(isChecked)
                    cameraView.selectCamera(0);
                else
                    cameraView.selectCamera(1);
            }
        });

        spinnerResolution = (Spinner) findViewById(R.id.resolutionSpinner);
        spinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] resolution = ((String)adapterView.getItemAtPosition(i)).split("x");
                cameraView.setResolution(Integer.parseInt(resolution[0]), Integer.parseInt(resolution[1]) );
                cameraView.updateCamera();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        cameraView = (CustomCameraView) findViewById(R.id.custom_camera_view);
        cameraView.setResolution(640, 360);

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
                        System.out.println("sending...");
                        cameraTopic.setPublisher_image(cameraView.getImage());
                        cameraView.setImageChanged(false);
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


    private void updateResolutions(List<Camera.Size> resolutions){
        int index = 0;
        Camera.Size customSize = cameraView.getPreviewSize();
        final List<String> resolutionList = new ArrayList<String>();

        for(int n=0; n < resolutions.size(); n++){
            Camera.Size resolution = resolutions.get(n);
            resolutionList.add(resolution.width+"x"+resolution.height);
            if( resolution.equals(customSize))
                index = n;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, resolutionList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final int pos = index;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spinnerResolution.setAdapter(adapter);
                spinnerResolution.setSelection(pos);
            }
        });
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        updateResolutions(cameraView.selectCamera(0));
        nodeMain=(NodeMainExecutorService)nodeMainExecutor;
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress(), getMasterUri());
        nodeMainExecutor.execute(androidNode, nodeConfiguration.setNodeName(androidNode.getName()));
    }

}
