package com.example.sensorhublite;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ClockTimer {
	
	private Timer mTimer;
	public int secTotal;
	public int sec;
	public int min;
	public int hour;
	public Handler mHandler;
	
	public ClockTimer(Handler mHandler){
		secTotal = 0;
		sec = 0;
		min = 0;
		hour = 0;
		this.mHandler = mHandler;
	}
	
	public void switchClock(boolean on){
		if(on){
			Log.i("Time", "start!");
			
			mTimer = new Timer(); 
			mTimer.schedule(new TimerTask(){

				@Override
				public void run() {
					secTotal++;
					hour = secTotal/3600;
					min = (secTotal%3600)/60;
					sec = secTotal%60;
					
					Message msg = new Message();
					msg.obj = "Tick";
					mHandler.sendMessage(msg);
				}
				
			}, 1000, 1000);
		}
		
		else{
			mTimer.cancel();
			secTotal = 0;
			sec = 0;
			min = 0;
			hour = 0;
			
			mTimer = null;
		}

	}
	
	public String getFormatTimer(){
		String time = hour+"hr "+min+"min "+sec+"sec";
		return time;
	}
	
	public void terminate(){
		if(mTimer != null){
			mTimer.cancel();
			mTimer = null;
		}
	}
	
}
