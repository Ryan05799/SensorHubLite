package com.example.sensorhublite;

import android.util.Log;

public class Measurement{
	
	public static final String [] MEASUREMENT_TYPE = {"Accelerometer", "Gyroscope", "Magnetometer", "Thermometer" };
	public static final int [] SCALES = {2, 4, 8 , 250, 500, 1000, 2000, 12000, 1};
	public final int [] DEFALUT_SCALE = {0, 3, 7, 8} ;
	private final String [] UINT = {"g", "'/s", "uT", "'C"};
	
	boolean enable;
	int scale;
	String unit;
	String type;
	String value;
	int recvDataNum;
	
	//Constructor
	public Measurement(int type, boolean enable){
		this.enable = enable;
		scale = SCALES[DEFALUT_SCALE[type]]; 
		unit = UINT[type];
		value = "";
		recvDataNum = 0;
		this.type = MEASUREMENT_TYPE[type];
	}
	public void updateMeasurements(int type, byte [] value){
		switch(type){
		case 0:
			//update acceleration
			this.value = formatValue(value);
			Log.i("Value:", "Acc"+ this.value);
			break;
		case 1:
			//update angular velocity
			this.value = formatValue(value);
			break;
		case 2:
			//update magnetometer read
			this.value = formatMagValue(value);
			break;
		case 3:
			//update temperature
			this.value = formatTempValue(value);
			
			break;
		}
	}
	private String formatValue(byte [] value){
		float [] dataBuf = new float[3];
		String text;
		dataBuf[0] = (float)((value[0]<<24 & 0xff000000 | value[1]<<16 & 0xff0000)>>16)/32767 * scale;
		dataBuf[1] = (float)((value[2]<<24 & 0xff000000 | value[3]<<16 & 0xff0000)>>16)/32767 * scale;
		dataBuf[2] = (float)((value[4]<<24 & 0xff000000 | value[5]<<16 & 0xff0000)>>16)/32767 * scale;
		text = "X:" + String.format("%.3f", dataBuf[0]) + unit + " ; Y:" 
				+  String.format("%.3f", dataBuf[1]) + unit +" ; Z:" + String.format("%.3f", dataBuf[2]) + unit;		
		return text;
	}
	
	private String formatMagValue(byte [] value){
		float [] dataBuf = new float[3];
		String text;
		dataBuf[0] = (float)((value[1]<<24 & 0xff000000 | value[2]<<16 & 0xff0000)>>16)/32767 * scale;
		dataBuf[1] = (float)((value[3]<<24 & 0xff000000 | value[4]<<16 & 0xff0000)>>16)/32767 * scale;
		dataBuf[2] = (float)((value[5]<<24 & 0xff000000 | value[6]<<16 & 0xff0000)>>16)/32767 * scale;
		text = "X:" + String.format("%.3f", dataBuf[0]) + unit + " ; Y:" 
				+  String.format("%.3f", dataBuf[1]) + unit +" ; Z:" + String.format("%.3f", dataBuf[2]) + unit;		
		return text;
	}
	
	private String formatTempValue(byte [] value)
	{
		float [] dataBuf = new float[3];
		String text;
		dataBuf[0] = (float)((value[0]<<24 & 0xff000000 | value[1]<<16 & 0xff0000)>>16)/32767 * scale;
		text = String.format("%.3f", dataBuf[0]) + unit;		
		return text;
		
	}
}
