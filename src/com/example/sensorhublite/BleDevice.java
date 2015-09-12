package com.example.sensorhublite;


import android.bluetooth.BluetoothDevice;

public class BleDevice{
	
	public static enum connStatus{CONNECTED, DISCONNECTED, OFFLINE};

	
	String devName;
	public BluetoothDevice device;
	public boolean ready; 
	public int tag;
	public int disconnect;
	public int batteryLevel; 
	public connStatus status;
	
	
	Measurement[] MpuMeas = new Measurement[2];//Array for ACC, GYRO, measurements
	
	
	//Constructor
	public BleDevice(String devName){
		this.devName = devName;
		status = connStatus.OFFLINE;
		ready = false;
		tag = 0;
		disconnect = 0;
		batteryLevel = 0;
		
		MpuMeas[0] = new Measurement(0, true);
		MpuMeas[1] = new Measurement(1, true);
	}
	
	public void updateMeasurements(byte [] value){
		int ptr = 0;
		byte [] meas_buf = new byte[6];
		
		if(value != null){
			if(value.length == 12){
				for(int j = 0 ; j < 6 ; j++){
					meas_buf[j] = value[ptr + j];
				}
				MpuMeas[0].updateMeasurements(0, meas_buf);
				
				ptr+=6;
				
				for(int j = 0 ; j < 6 ; j++){
					meas_buf[j] = value[ptr + j];
				}
				MpuMeas[1].updateMeasurements(1, meas_buf);
				ptr+=6;
			}

		}

	}
}
//The inner class for representing features of a measurement


