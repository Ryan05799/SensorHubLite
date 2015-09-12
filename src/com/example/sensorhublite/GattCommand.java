package com.example.sensorhublite;

import java.util.UUID;
import com.example.sensorhublite.MainActivity.GattOp;

	
	public class GattCommand{
		UUID Target;
		int devIndex;
		int wait;
		GattOp op;
		byte [] value;
		
		public GattCommand(GattOp op, UUID characteristic, int index, byte [] value, int wait){
			this.Target = characteristic;
			this.devIndex = index;
			this.op  = op;
			this.value = value;
			this.wait = wait;
		}
	}