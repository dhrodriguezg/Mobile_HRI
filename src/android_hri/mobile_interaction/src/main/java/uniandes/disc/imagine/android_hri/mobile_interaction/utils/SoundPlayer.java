package uniandes.disc.imagine.android_hri.mobile_interaction.utils;

import android.content.Context;
import android.media.MediaPlayer;

public class SoundPlayer {

	private static final String TAG = "SoundPlayer";
	
	private MediaPlayer mediaControllerOffline = null;
	private MediaPlayer mediaControllerOnline = null;
	private MediaPlayer mediaTargetLost = null;
	private MediaPlayer mediaTargetFound = null;
	private MediaPlayer mediaTaskFinished = null;
	
	public SoundPlayer(Context context){
		/*
		mediaControllerOffline = MediaPlayer.create(context , R.raw.controller_offline);
		mediaControllerOnline = MediaPlayer.create(context , R.raw.controller_online);
		mediaTargetLost = MediaPlayer.create(context , R.raw.target_lost);
		mediaTargetFound = MediaPlayer.create(context , R.raw.target_found);
		mediaTaskFinished = MediaPlayer.create(context , R.raw.task_finished);*/
	}
	
	public void controllerOnline(){
		mediaControllerOnline.start();
	}
	
	public void controllerOffline(){
		mediaControllerOffline.start();
	}
	
	public void targetLost(){
		mediaTargetLost.start();
	}
	
	public void targetFound(){
		mediaTargetFound.start();
	}
	
	public void taskFinished(){
		mediaTaskFinished.start();
	}
}
