package uniandes.disc.imagine.android_hri.mobile_interaction;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.ros.address.InetAddressFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.util.Locale;

import uniandes.disc.imagine.android_hri.mobile_interaction.interfaces.DirectManipulationInterface;
import uniandes.disc.imagine.android_hri.mobile_interaction.interfaces.GamepadInterface;
import uniandes.disc.imagine.android_hri.mobile_interaction.interfaces.LeapMotionInterface;
import uniandes.disc.imagine.android_hri.mobile_interaction.interfaces.ScreenJoystickInterface;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    public static String ROS_MASTER = "";
    public static String ROS_HOST = "";

    public static float WORKSPACE_X_OFFSET = 0.2306f;
    public static float WORKSPACE_WIDTH = 0.4889f;
    public static float WORKSPACE_Y_OFFSET = 0.9100f;
    public static float WORKSPACE_HEIGHT = 0.3546f;

    private EditText rosIP;
    private EditText rosPort;
    private ImageView interface_1;
    private ImageView interface_2;
    private ImageView interface_3;
    private ImageView interface_4;
    private MenuItem[] language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadGUI();
    }

    private void loadGUI(){
        setContentView(R.layout.activity_main);
        rosIP = (EditText) findViewById(R.id.editIP);
        rosPort = (EditText) findViewById(R.id.editPort);
        interface_1 = (ImageView) findViewById(R.id.imageViewPreviewController);
        interface_2 = (ImageView) findViewById(R.id.imageViewPreviewDragging);
        interface_3 = (ImageView) findViewById(R.id.imageViewPreviewGamepad);
        interface_4 = (ImageView) findViewById(R.id.imageViewPreviewLeapMotion);
        WORKSPACE_X_OFFSET= Float.parseFloat(getString(R.string.workspace_xoffset));
        WORKSPACE_WIDTH= Float.parseFloat(getString(R.string.workspace_width));
        WORKSPACE_Y_OFFSET= Float.parseFloat(getString(R.string.workspace_yoffset));
        WORKSPACE_HEIGHT= Float.parseFloat(getString(R.string.workspace_height));

        Button pingButton = (Button) findViewById(R.id.pingButton);
        pingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSendingPing();
            }
        });

        Button calibrationButton = (Button) findViewById(R.id.calibrationButton);
        calibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCalibrationActivity();
            }
        });

        Button cameraButton = (Button) findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCameraActivity();
            }
        });

        Button controllerButton = (Button) findViewById(R.id.controllerButton);
        controllerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControllerActivity();
            }
        });
        interface_1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControllerActivity();
            }
        });

        Button draggingButton = (Button) findViewById(R.id.draggingButton);
        draggingButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                startDraggingActivity();
            }
        });
        interface_2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDraggingActivity();
            }
        });

        Button gamepadButton = (Button) findViewById(R.id.gamepadButton);
        gamepadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGamepadActivity();
            }
        });
        interface_3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGamepadActivity();
            }
        });

        Button leapButton = (Button) findViewById(R.id.leapMotionButton);
        leapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLeapMotionActivity();
            }
        });
        interface_4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLeapMotionActivity();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        language = new MenuItem[2]; //for now...
        language[0]= menu.add(1,1, Menu.NONE,getString(R.string.english));
        language[1]= menu.add(1,2, Menu.NONE,getString(R.string.spanish));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int gid = item.getGroupId();
        int iid = item.getItemId();

        if(gid==1 && iid==1){
            //english
            String languageToLoad  = "en";
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
            loadGUI();
            return true;
        }else if(gid==1 && iid==2){
            //spanish
            String languageToLoad  = "es";
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
            loadGUI();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startSendingPing(){
        int exit = pingHost(rosIP.getText().toString(), 10, true);
        if (exit!=0){
            Toast.makeText(getApplicationContext(), rosIP.getText().toString() + " is not reachable!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void startCalibrationActivity(){
        if (isMasterValid()){
            Intent myIntent = new Intent(MainActivity.this, SetupActivity.class);
            MainActivity.this.startActivity(myIntent);
        }
    }

    private void startCameraActivity(){
        if (isMasterValid()){
            Intent myIntent = new Intent(MainActivity.this, CameraActivity.class);
            MainActivity.this.startActivity(myIntent);
        }
    }

    private void startControllerActivity(){
        if (isMasterValid()){
            Intent myIntent = new Intent(MainActivity.this, ScreenJoystickInterface.class);
            MainActivity.this.startActivity(myIntent);
        }
    }

    private void startDraggingActivity(){
        if (isMasterValid()){
            Intent myIntent = new Intent(MainActivity.this, DirectManipulationInterface.class);
            MainActivity.this.startActivity(myIntent);
        }
    }

    private void startGamepadActivity(){
        if (isMasterValid()){
            Intent myIntent = new Intent(MainActivity.this, GamepadInterface.class);
            MainActivity.this.startActivity(myIntent);
        }
    }

    private void startLeapMotionActivity(){
        if (isMasterValid()){
            Intent myIntent = new Intent(MainActivity.this, LeapMotionInterface.class);
            MainActivity.this.startActivity(myIntent);
        }
    }

    private boolean isMasterValid(){
        try {
            ROS_HOST = NetworkInterface.getByName("ppp0").getInterfaceAddresses().get(0).getAddress().getHostAddress();
        } catch (Exception e) {
        }
        if(ROS_HOST.equals(""))
            ROS_HOST = InetAddressFactory.newNonLoopback().getHostAddress();

        ROS_MASTER = "http://" + rosIP.getText().toString() + ":" + rosPort.getText().toString();
        int exit = pingHost(rosIP.getText().toString(), 1, false);
        if (exit!=0){
            Toast.makeText(getApplicationContext(), rosIP.getText().toString() + " is not reachable!!!", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private int pingHost(String host, int tries, boolean showStats){
        int exit = -1;
        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("ping -c " + tries + " " + host);
            proc.waitFor();
            if(showStats){
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                DecimalFormat numberFormat = new DecimalFormat("#.00");
                float minTime= Float.MAX_VALUE;
                float maxTime= Float.MIN_VALUE;
                float avgTime=0;
                float n=0.f;
                String result_line;
                while ((result_line = stdInput.readLine()) != null) {
                    if(!result_line.contains("time="))
                        continue;
                    String s_time = result_line.split("time=")[1].split(" ")[0];
                    float currentTime = Float.parseFloat(s_time);
                    avgTime += currentTime;
                    minTime = minTime < currentTime ? minTime : currentTime;
                    maxTime = maxTime > currentTime ? maxTime : currentTime;
                    n++;
                }
                avgTime/=n;
                Toast.makeText(getApplicationContext(), "Avg: " + numberFormat.format(avgTime) + "ms Min: " + numberFormat.format(minTime) + "ms Max: " + numberFormat.format(maxTime) + "ms", Toast.LENGTH_LONG).show();
            }
            exit = proc.exitValue();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return exit;
    }

}
