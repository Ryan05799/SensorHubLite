package com.example.sensorhublite;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.example.sensorhublite.BleDevice.connStatus;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 2;
	private static final long SCAN_PERIOD = 1000; 
	public enum GattOp {READ_CHARACTERISTIC, WRITE_CHARACTERISTIC, CONNECT};

	public static final String [] BLE_DEVICE_NAME = {"MPU_Sensor", "MPU_Sensor2" ,"MPU_Sensor3"};
	
	//MpuService
	public static final UUID MPU_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_MEASUREMENT_CHAR = UUID.fromString("6e400101-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_COMMAND_CHAR = UUID.fromString("6e400202-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_MODE_CHAR = UUID.fromString("6e400201-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_EXTRAINFO_CHAR = UUID.fromString("6e400302-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID MPU_TIME_CHAR = UUID.fromString("6e400301-b5a3-f393-e0a9-e50e24dcca9e");
	//BAS Service
	public static final UUID BAS_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
	public static final UUID BATTERY_LEVEL_CHAR_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
	
	ListView deviceList;
	public static Button scanBtn;
	public static Button startBtn;
	public static Button discBtn;

	public static ProgressBar scanProgressBar;

	private static Handler mHandler;
	private static BluetoothAdapter mBluetoothAdapter;
	private static DeviceAdapter mDeviceAdapter;
	
	public static BleDevice [] mBleDevice;
	private static BleService mBleService;
	
	private CommandExecThread cmdExeThrd;
	private static ConcurrentLinkedQueue <GattCommand> mCommandQueue;
	
	private boolean isWaiting = false;
	private boolean isCEThreadRunning = false;
	private boolean isConnecting = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		initViews();
		initialize();
		
	}

	private void initViews()
	{
		deviceList = (ListView) this.findViewById(R.id.deviceList);
		scanBtn = (Button) this.findViewById(R.id.btnScan);
		startBtn = (Button) this.findViewById(R.id.btnStart);
		discBtn = (Button) this.findViewById(R.id.btnDisc);
		scanProgressBar = (ProgressBar) this.findViewById(R.id.progressBar1);
		scanProgressBar.setVisibility(android.view.View.INVISIBLE);
		
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		
		
		
		scanBtn.setWidth(width/4);
		startBtn.setWidth(width/4);
		discBtn.setWidth(width/3);
		scanBtn.setText("Scan");
		startBtn.setText("Start");
		discBtn.setText("Disconnect");
	}
	
	private void initialize(){
		
		mDeviceAdapter = new DeviceAdapter(this);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		deviceList.setAdapter(mDeviceAdapter);
		mHandler = new Handler();
		mCommandQueue = new ConcurrentLinkedQueue<GattCommand>();

		
		
		 //initialize BleService
		 if(BleService_init())
			 Log.e("Service", "BleService initialized");
		
		mBleDevice = new BleDevice[DeviceAdapter.DEVICE_NUMBER];
		for(int i = 0 ; i < DeviceAdapter.DEVICE_NUMBER ; i++)
		{
			mBleDevice[i] = new BleDevice(BLE_DEVICE_NAME[i]);
		}
		
		
		if(!mBluetoothAdapter.isDiscovering())
		{
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}		
		
		scanBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				startBtn.setEnabled(false);
				scanBtn.setEnabled(false);
				scanProgressBar.setVisibility(android.view.View.VISIBLE);
				scanBleDevice(true);
			}
			
		});
		
		startBtn.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				startBtn.setEnabled(false);
				scanBtn.setEnabled(false);
				discBtn.setEnabled(false);
				scanProgressBar.setVisibility(android.view.View.VISIBLE);
				for(int index =0 ; index < DeviceAdapter.DEVICE_NUMBER ; index++)
				{
					mBleDevice[index].disconnect = 0;
					mBleDevice[index].tag = 0;
				}
				startCommandExecThread(isCEThreadRunning);
			}
			
		});
		
		discBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				mBleService.disconnect(-1);
				for(int i = 0 ; i<DeviceAdapter.DEVICE_NUMBER ; i++){
					mBleDevice[i].status = connStatus.DISCONNECTED;
					mBleDevice[i].ready = false;
				}
				refreshDeviceList();
			}
			
		});
	}
	
	//Start scanning procedure
	public void scanBleDevice(boolean enable){
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {					
					//End scanning
					mBluetoothAdapter.stopLeScan(mBleScanCallback);
					refreshDeviceList();
					connectAll();
				}
	            }, SCAN_PERIOD);
			mBluetoothAdapter.startLeScan(mBleScanCallback);
		}
		else {
			mBluetoothAdapter.stopLeScan(mBleScanCallback);
		}
	}
	
	//The callback defines the actions taken when a ble device is found
	private static BluetoothAdapter.LeScanCallback mBleScanCallback = new BluetoothAdapter.LeScanCallback(){
		
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			UUID getUUID = findUUID(scanRecord);
			if(getUUID.equals(MPU_SERVICE)){
				for(int i = 0 ; i < DeviceAdapter.DEVICE_NUMBER ; i++)
				{
					if(device.getName().equals(BLE_DEVICE_NAME[i])){
						mBleDevice[i].device = device;
						mBleDevice[i].status = connStatus.DISCONNECTED;
						
						mCommandQueue.add(new GattCommand(GattOp.CONNECT, null, i, null, 0)) ;
						
						break;
					}
				}
			}
			
			else{
				return;
			}
				
		}		
	};
	
	//Find the index of the UUID in the scanRecord
	private static UUID findUUID(byte [] advdata){
		byte type = advdata[1];
		int length = (int) advdata[0];
		int pointer = 0;
		
		while(type != 0x07){
			pointer = pointer + length+1;
			length = (int) advdata[pointer];
			type = advdata[pointer+1];
		}
		
		String UUIDString = "";
		for(int i = length ; i > 1 ; i--){
			UUIDString = UUIDString + String.format("%02x", advdata[pointer+i] & 0xff);
			if(i == 8 || i==10 || i == 12 || i == 14)
				UUIDString = UUIDString +"-";
		}
		return UUID.fromString(UUIDString);
	}
	
	private void connectAll(){

		isConnecting = true;
		cmdExeThrd = new CommandExecThread();
		cmdExeThrd.start();
		
		startBtn.setEnabled(true);
		scanBtn.setEnabled(true);
		scanProgressBar.setVisibility(android.view.View.INVISIBLE);
		refreshDeviceList();
		
	}
	
	private void startCommandExecThread(boolean thrdRunning){

		if(!thrdRunning){
			
			startBtn.setText("Stop");
			isCEThreadRunning = true;
			
			for(int index =0 ; index < DeviceAdapter.DEVICE_NUMBER ; index++)
			{
				byte [] mode = {0x02};
				mCommandQueue.add(new GattCommand(GattOp.WRITE_CHARACTERISTIC, MPU_MODE_CHAR, index, mode,0));
				mCommandQueue.add(new GattCommand(GattOp.READ_CHARACTERISTIC, BATTERY_LEVEL_CHAR_UUID, index, null,0));
			}
			
			cmdExeThrd = new CommandExecThread();
			cmdExeThrd.start();
		}
		else
		{
			
			startBtn.setText("Start");
			isCEThreadRunning = false;
			cmdExeThrd = null;
			while(!mCommandQueue.isEmpty()){
				//Clear command queue
				mCommandQueue.poll();
			}
			discBtn.setEnabled(true);
		}
		
		startBtn.setEnabled(true);
		scanBtn.setEnabled(true);
		scanProgressBar.setVisibility(android.view.View.INVISIBLE);
	}
	
	private static void refreshDeviceList()
	{
		mDeviceAdapter.notifyDataSetChanged();
	}
	
	
	private class CommandExecThread extends Thread{
		
		@Override
		public void run(){
			
			if(isConnecting){
				
				while(!mCommandQueue.isEmpty()){
					GattCommand command = mCommandQueue.poll();
					if(command.op == GattOp.CONNECT){
						mBleService.connect(command.devIndex, mBleDevice[command.devIndex].device);
						waitGattCallback(100);
						
						if(mBleDevice[command.devIndex].status == connStatus.DISCONNECTED && !mBleDevice[command.devIndex].ready)
						{
							mBleService.disconnect(command.devIndex);
							mCommandQueue.add(command);
							
						}
						
					}
					
				}
				
				isConnecting = false;
				this.interrupt();
				
			}

			while(isCEThreadRunning){
				
				//Check command queue
				if(!mCommandQueue.isEmpty()){
					GattCommand command = mCommandQueue.poll();
					if(command != null){
						if(mBleDevice[command.devIndex].ready){
							Log.i("Get cmd " + command.op, "From device " + command.devIndex);
							switch(command.op){
							case READ_CHARACTERISTIC:
								if(command.Target.equals(MPU_MEASUREMENT_CHAR))
								{
									mBleService.readCharateristic(command.devIndex, MPU_SERVICE, MPU_MEASUREMENT_CHAR);
									Log.i("Read MEAS", "Read MEAS from device " + command.devIndex);
									if(!waitGattCallback(100)){
										waitGattCallback(200);
										
									}
									mCommandQueue.add(new GattCommand(GattOp.READ_CHARACTERISTIC, MPU_MEASUREMENT_CHAR, command.devIndex, null,0));

									
								}
								else if(command.Target.equals(BATTERY_LEVEL_CHAR_UUID))
								{
									mBleService.readCharateristic(command.devIndex, BAS_UUID, BATTERY_LEVEL_CHAR_UUID);	
									waitGattCallback(20);
									Log.i("Read BAS", "Read BAS from device " + command.devIndex);
								}
								
								break;
								
							case WRITE_CHARACTERISTIC:
								if(command.Target.equals(MPU_MODE_CHAR)){
									mBleService.writeCharacteristic(command.devIndex, MPU_SERVICE, command.Target, command.value);	
									waitGattCallback(20);
								}
								break;
								
							case CONNECT:
								Log.i("Re-connect", "device " + command.devIndex);
								mBleService.connect(command.devIndex, mBleDevice[command.devIndex].device);
								waitGattCallback(100);
								break;
								
								
							}
						}
					}
				}	
				
			}
			if(!isCEThreadRunning){
				this.interrupt();
			}
		}
		
		private boolean waitGattCallback(int timeout){
			isWaiting = true;
			while(isWaiting && timeout > 0){
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				timeout--;
			}
			
			isWaiting = false;
			if(timeout == 0){
				Log.i("Thread waiting", "Timeout");
				return false;
			}
			else{
				try {
					Log.i("Thread waiting", "onRead");
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return true;
		}
	}
	

		
	
	/**
	 * The following are the functions and instance relative to BleService.
	 * service_init(), unbindBleService(), makeGattUpdateIntentFilter()
	 * mServiceConnection, BleServiceStatusChangeRevceiver
	 */
	//BleService related functions and instances
	public boolean BleService_init(){
		Intent bindIntent = new Intent(this, BleService.class);
		if(this.bindService(bindIntent, mServiceConnection,  Context.BIND_AUTO_CREATE))
		{
			LocalBroadcastManager.getInstance(this).registerReceiver(BleServiceStatusChangeRevceiver, makeGattUpdateIntentFilter());
		}else
		{
			Log.e("Err", "bind fail");
			return false;
		}
		return true;
	}
	
	public void unbindBleService(){
		try{
			LocalBroadcastManager.getInstance(this).unregisterReceiver(BleServiceStatusChangeRevceiver);
		}catch(Exception ignore){
			Log.e("Err", ignore.toString());
		}
		this.unbindService(mServiceConnection);
	}
	
	private ServiceConnection mServiceConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName className, IBinder rawBinder) {
			
			mBleService = ((BleService.LocalBinder) rawBinder).getService();
			if(!mBleService.initialize(DeviceAdapter.DEVICE_NUMBER))
			{				
				Log.e("Service Err", "Fail to initialize BleService");
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			Log.e("Service Err", "BleService disconnected");
			mBleService = null;
		}
		
	};
	
	private static IntentFilter makeGattUpdateIntentFilter(){
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_NOTIFY);
		intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_READ);
		intentFilter.addAction(BleService.ACTION_CHARACTERISTIC_WRITE);
		intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BleService.ACTION_CCCD_ENABLED);		
		intentFilter.addAction(BleService.ACTION_GATT_ERROR);
		return intentFilter;
	}
	
	private final BroadcastReceiver BleServiceStatusChangeRevceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String callDev = intent.getStringExtra(BleService.DEVICE_NAME);
			int index = -1;
			
			for(int i=0 ; i< DeviceAdapter.DEVICE_NUMBER ; i++){
				if(mBleDevice[i].devName.equals(callDev)){
					index = i;
					break;
				}
			}
			if(index == -1){
				//Invalid device name
				Log.i("Service", "Invalid device name : " + intent.getStringExtra(BleService.DEVICE_NAME));
				return;
			}
			
			//On connected
			if(action.equals(BleService.ACTION_GATT_CONNECTED)){
				//find device index by device name 
				Log.i("Connect", "Connect to device: " + intent.getStringExtra(BleService.DEVICE_NAME));
					//Update device info when connected
					mBleDevice[index].status = connStatus.CONNECTED;
					refreshDeviceList();
				
			}
			
			//On disconnected
			if(action.equals(BleService.ACTION_GATT_DISCONNECTED)){

				mBleDevice[index].status = connStatus.DISCONNECTED;
				mBleDevice[index].ready = false;
				mBleDevice[index].disconnect++;
				refreshDeviceList();
				
				if(isCEThreadRunning)
				{//Disconnect while measuring, reconnect
					startCommandExecThread(true);
					mBleService.connect(index, mBleDevice[index].device);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					startCommandExecThread(false);
				}

			}
			
			//On service discovered
			if(action.equals(BleService.ACTION_GATT_SERVICES_DISCOVERED)){
				Log.i("Service Discovery", "device" + index);
				//Enable notification
				mBleService.enableNotification(index, MPU_SERVICE, MPU_MEASUREMENT_CHAR);
				
			}
			
			//On CCCD set
			if(action.equals(BleService.ACTION_CCCD_ENABLED)){
				//Notification enabled , device is ready
				Log.i("READY", "Device" + index +"ready.");
				mBleDevice[index].ready = true;
				refreshDeviceList();
							
			}
			
			//On characteristic notify
			if(action.equals(BleService.ACTION_CHARACTERISTIC_NOTIFY)){
				
				String charUUID = intent.getStringExtra(BleService.CHAR_UUID);
				final byte[] value = intent.getByteArrayExtra(BleService.CHAR_VALUE);
				if(index != -1 && charUUID != null){
					
					
				}
			}
			//On characteristic write
			if(action.equals(BleService.ACTION_CHARACTERISTIC_WRITE)){
				String charUUID = intent.getStringExtra(BleService.CHAR_UUID);
				
				Log.i("On write", "Device" + index );
				if(charUUID == null)
					return;
				if(charUUID.equals(MPU_MODE_CHAR.toString()))
				{
						Log.i("Mode changed", "Device" + index );						
						mCommandQueue.add(new GattCommand(GattOp.READ_CHARACTERISTIC, MPU_MEASUREMENT_CHAR, index, null, 0));
				}
				
			}
			
			//On characteristic read
			if(action.equals(BleService.ACTION_CHARACTERISTIC_READ)){
				String charUUID = intent.getStringExtra(BleService.CHAR_UUID);
				final byte[] value = intent.getByteArrayExtra(BleService.CHAR_VALUE);
				if(charUUID != null && value != null){
					
					if(charUUID.equals(MPU_MEASUREMENT_CHAR.toString())){
						mBleDevice[index].tag++;
						mBleDevice[index].updateMeasurements(value);
						isWaiting = false;
						refreshDeviceList();
					}
					
					if(charUUID.equals(BATTERY_LEVEL_CHAR_UUID.toString())){
						mBleDevice[index].batteryLevel = (int)((float)(value[0] & 0x000000ff)/255 *100);
						Log.i("Read BAS", "level = " + mBleDevice[index].batteryLevel);
						refreshDeviceList();
					}
				}
			}
			//Error
			if(action.equals(BleService.ACTION_GATT_ERROR)){
				
			}
		}
		
	};
	
	private void close(){
		//Procedure to terminate BleService
		if(	cmdExeThrd != null)
		{
			isCEThreadRunning = false;
			cmdExeThrd.interrupt();
		}
		
		unbindBleService();
		mBleService.stopSelf();
		mBleService = null;
		Log.i("Close", "BLE service closed");

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	  public boolean onKeyDown(int keyCode, KeyEvent event) {
	        if ((keyCode == KeyEvent.KEYCODE_BACK)) {   //確定按下退出鍵
	            ConfirmExit(); //呼叫ConfirmExit()函數
	            return true;  
	     }  
	        return super.onKeyDown(keyCode, event);  
	  }

	   public void ConfirmExit(){

	        AlertDialog.Builder ad=new AlertDialog.Builder(MainActivity.this); //創建訊息方塊

	        ad.setTitle("離開");
	        ad.setMessage("確定要離開?");
	        ad.setPositiveButton("是", new DialogInterface.OnClickListener() { //按"是",則退出應用程式
	            public void onClick(DialogInterface dialog, int i) {
	              close();
	              MainActivity.this.finish();//關閉activity
	       }
	     });

	        ad.setNegativeButton("否",new DialogInterface.OnClickListener() { //按"否",則不執行任何操作
	            public void onClick(DialogInterface dialog, int i) {
	       }
	     });

	        ad.show();//顯示訊息視窗

	  }
	   
	   public void toastMessage(String msg){
		   Toast.makeText(this, msg, Toast.LENGTH_SHORT);
	   }
}
