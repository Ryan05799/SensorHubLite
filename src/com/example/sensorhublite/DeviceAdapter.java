package com.example.sensorhublite;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DeviceAdapter  extends BaseAdapter{
	
	static public final int DEVICE_NUMBER = 3;
	
	
	LayoutInflater mLayoutInflater;
	
	public DeviceAdapter(Context context){
		 mLayoutInflater = LayoutInflater.from(context);
	}
	
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return DEVICE_NUMBER;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewGroup vg;
		if(convertView != null){
			vg = (ViewGroup) convertView;
		}else{
			vg = (ViewGroup) mLayoutInflater.inflate(R.layout.device_item, null);
		}
		
		TextView devNameTxt = (TextView) vg.findViewById(R.id.devName);
		TextView statusTxt = (TextView) vg.findViewById(R.id.connStatus);
		TextView batLvlTxt = (TextView) vg.findViewById(R.id.batteryLevel);
		TextView disconnTxt = (TextView) vg.findViewById(R.id.disconnCount);
		TextView readyTxt = (TextView) vg.findViewById(R.id.devReady);
		TextView tagTxt = (TextView) vg.findViewById(R.id.dataNum);
		TextView accMeasTxt = (TextView) vg.findViewById(R.id.measAcc);
		TextView gyroMeasTxt = (TextView) vg.findViewById(R.id.measGyro);
		
		BleDevice device = MainActivity.mBleDevice[position];
		
		devNameTxt.setText(device.devName);

		switch(device.status){
		case CONNECTED:
			statusTxt.setText("Connected");
			break;
			
		case DISCONNECTED:
			statusTxt.setText("Disonnected");
			break;
			
		case OFFLINE:
			statusTxt.setText("Offline");
			break;
		}
		
		batLvlTxt.setText(" : " + device.batteryLevel +"%");
		
		disconnTxt.setText(" " + device.disconnect);
		
		if(device.ready)
			readyTxt.setText("Ready");
		else
			readyTxt.setText("Not ready");
		
		tagTxt.setText(" " + device.tag);
		accMeasTxt.setText(device.MpuMeas[0].value);
		gyroMeasTxt.setText(device.MpuMeas[1].value);

		
		
		return vg;
	}

	
}
