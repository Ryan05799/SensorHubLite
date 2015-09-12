package com.example.sensorhublite;

import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class BleService extends Service{
	
	public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	//Define actions can be broadcasted
	public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_CCCD_ENABLED = "ACTION_CCCD_ENABLED";
	public final static String ACTION_CHARACTERISTIC_WRITE = "ACTION_CHARACTERISTIC_WRITE";
	public final static String ACTION_CHARACTERISTIC_READ = "ACTION_CHARACTERISTIC_READ";
	public final static String ACTION_CHARACTERISTIC_NOTIFY = "ACTION_CHARACTERISTIC_NOTIFY";
	public final static String ACTION_GATT_ERROR = "ACTION_GATT_ERROR";
	//String index for put and get extra data
	public final static String DEVICE_NAME = "Device_name";
	public final static String CHAR_UUID = "Char_UUID";
	public final static String CHAR_VALUE = "Char_value";

	
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt [] mBluetoothGatt;
    
    private String TAG = "BleService";
	
	public class LocalBinder extends Binder{
		//Get this BleService while initializing
		BleService getService(){
			return BleService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	@Override 
	public boolean onUnbind(Intent intent){
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.
		close();
		return super.onUnbind(intent);
	}
	private final IBinder mBinder = new LocalBinder();
	
	public boolean initialize(int conn){
		
		//dispatch a BluetoothGatt instance for each connected device
		mBluetoothGatt = new BluetoothGatt[conn];
        Log.i(TAG, "Set " + mBluetoothGatt.length + " BluetoothGatt instances.");
		
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
		
		return true;
	}
	private void close(){
		//close all BluetoothGatt connection
        Log.w(TAG, "BluetoothGatt closed");
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt closed null");      	
            return;
        }
        disconnect(-1);
	}
	
	//BluetoothGattCallback and broadcaster for each GATT events
	private final class BleGattCallback extends BluetoothGattCallback{
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
	            
	            if (newState == BluetoothProfile.STATE_CONNECTED) {

	                Log.i(TAG, "Connected to GATT server.");
	                broadcastConnStatus( ACTION_GATT_CONNECTED, gatt.getDevice().getName());
	                // Attempts to discover services after successful connection.
	                Log.i(TAG, "Attempting to start service discovery:" +
	                        gatt.discoverServices());

	            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
	            	broadcastConnStatus(ACTION_GATT_DISCONNECTED, gatt.getDevice().getName());
	                Log.i(TAG, "Disconnected from GATT server.");
	                gatt.close();
	                gatt = null;
	               
	                
	            }
		}
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.w(TAG, "Services discovered");
                broadcastConnStatus( ACTION_GATT_SERVICES_DISCOVERED, gatt.getDevice().getName());
			}
			else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}
		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
			if(descriptor.getUuid().equals(CCCD) && status == BluetoothGatt.GATT_SUCCESS){
				Log.i(TAG, "CCCD set");
				broadcastConnStatus(ACTION_CCCD_ENABLED, gatt.getDevice().getName());
			}
		}
		
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
											BluetoothGattCharacteristic characteristic, int status){
			if(status == BluetoothGatt.GATT_SUCCESS){
				broadcastDataUpdate(ACTION_CHARACTERISTIC_READ , gatt.getDevice().getName() , characteristic);
			}else{
				Log.e(TAG, "read characteristic failed - " + status);
			}
		}
		//on characteristic notify
		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
											BluetoothGattCharacteristic characteristic){
			Log.i(TAG, "Receivce characteristic notification");
			broadcastDataUpdate(ACTION_CHARACTERISTIC_NOTIFY , gatt.getDevice().getName() , characteristic);
		}
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, 
											BluetoothGattCharacteristic characteristic, int status){
			
			broadcastDataUpdate(ACTION_CHARACTERISTIC_WRITE, gatt.getDevice().getName(), characteristic);

		}
	}
	
	//broadcast for gatt connect/disconnect
	private void broadcastConnStatus(final String action, String devName){
		final Intent intent = new Intent(action);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        intent.putExtra(DEVICE_NAME, devName);
        Log.i("broadcast", action);
	}
	//broadcast for gatt characteristic read or notification, including device name, charateristic uuid and value
	private void broadcastDataUpdate(final String action, String devName, BluetoothGattCharacteristic characteristic){
		final Intent intent = new Intent(action);
		String UUID = characteristic.getUuid().toString();
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        intent.putExtra(DEVICE_NAME, devName);
        intent.putExtra(CHAR_UUID, UUID);
        intent.putExtra(CHAR_VALUE, characteristic.getValue());
        Log.i("broadcast", action);
        Log.i("broadcast", devName);
        Log.i("broadcast", UUID);

	}

	/**
	 * The following functions perform BLE GATT related operations
	 * Connect, disconnect, Characteristics R/W, descriptors R/W 
	 */
	public boolean connect(int index, BluetoothDevice device){
		if (mBluetoothAdapter == null || device == null) 
		{
			Log.w(TAG, "BluetoothAdapter not initialized or unspecified device.");
			return false;
			}
		/*//reconnect a device
		if(mBluetoothGatt[index] != null){
            Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt[index].connect(); 
		}*/
		
		//Establish new GATT connection
		mBluetoothGatt[index] = device.connectGatt(this,  false, new BleGattCallback());
		if(mBluetoothGatt[index] != null){
			Log.i(TAG, "GATT connected.");
			return true;
		}
		Log.e(TAG, "Fail to connect GATT");
		return false;
	}
	//Disconnect a GATT connection according to the index
	public void disconnect(int index){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        //disconnect & close  all GATT Connection (is used when trying to terminate the BleService)
        if(index == -1)
        {
        	for(int i = 0 ; i < mBluetoothGatt.length ; i++){
        		if(mBluetoothGatt[i] != null){
                    mBluetoothGatt[i].disconnect();
                    mBluetoothGatt[i].close();
                    mBluetoothGatt[i] = null;
        		}
        	}
        	return;
        }
        if(index < 0 || index >= mBluetoothGatt.length){
            Log.w(TAG, "Index out of bound");
            return;
        }
        if(mBluetoothGatt[index] != null){
            Log.w(TAG, "Disconnect BluetoothGatt");
            mBluetoothGatt[index].disconnect();
            mBluetoothGatt[index].close();
            mBluetoothGatt[index] = null;
        }
	}
	
	public void enableNotification(int index, UUID service, UUID characteristic){
		BluetoothGattService Service;
		BluetoothGattCharacteristic Characteristic;
		BluetoothGattDescriptor Descriptor;
		if(index < 0 || index > mBluetoothGatt.length){
			Log.e(TAG, "Read char index out of bound");
			return;
		}
		if(mBluetoothGatt[index] == null){
			Log.e(TAG, "GATT isn't connected");
			return;
		}
		if(service != null && characteristic != null){
			Service = mBluetoothGatt[index].getService(service);
			if(Service == null){
				Log.e(TAG, "Service not found");
				return;
			}
			Characteristic = Service.getCharacteristic(characteristic);
			if(Characteristic == null){
				Log.e(TAG, "Characteristic not found");
				return;
			}
			mBluetoothGatt[index].setCharacteristicNotification(Characteristic, true);
			Descriptor = Characteristic.getDescriptor(CCCD);
			Descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt[index].writeDescriptor(Descriptor);
			Log.i(TAG, "CCCD set");
		}
	}
	
	public void readCharateristic(int index, UUID service, UUID characteristic){
		BluetoothGattService Service;
		BluetoothGattCharacteristic Characteristic;
		if(index < 0 || index > mBluetoothGatt.length){
			Log.e(TAG, "Read char index out of bound");
			return;
		}
		if(mBluetoothGatt[index] == null){
			Log.e(TAG, "GATT isn't connected");
			return;
		}
		if(service != null && characteristic != null){
			Service = mBluetoothGatt[index].getService(service);
			if(Service == null){
				Log.e(TAG, "Service not found");
				return;
			}
			Characteristic = Service.getCharacteristic(characteristic);
			if(Characteristic == null){
				Log.e(TAG, "Characteristic not found");
				return;
			}
			
			if(!mBluetoothGatt[index].readCharacteristic(Characteristic))
				Log.e(TAG, "Read initiation failed");
		}
		
	}
	/**
	 * Function for writing value to characteristic*/
	public void writeCharacteristic(int index, UUID service, UUID characteristic, byte [] value){
		BluetoothGattService Service;
		BluetoothGattCharacteristic Characteristic;
		if(index < 0 || index > mBluetoothGatt.length){
			Log.e(TAG, "Read char index out of bound");
			return;
		}
		if(mBluetoothGatt[index] == null){
			Log.e(TAG, "GATT isn't connected");
			return;
		}
		if(service != null && characteristic != null){
			Service = mBluetoothGatt[index].getService(service);
			if(Service == null){
				Log.e(TAG, "Service not found");
				return;
			}
			Characteristic = Service.getCharacteristic(characteristic);
			if(Characteristic == null){
				Log.e(TAG, "Characteristic not found");
				return;
			}
			Characteristic.setValue(value);
			mBluetoothGatt[index].writeCharacteristic(Characteristic);
		}
	}
}
