package zzl.bestidear.ledflash;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.IBinder;
import android.support.v7.internal.app.WindowCallback;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class KeyTouchServerListener extends Service implements android.hardware.SensorEventListener{

	private IntentFilter intentFilter;
	private BatteryReceiver batteryReceiver;

	private static final String AIXIN_UPDATE_BC = "com.qiyi.mediacenter.boardcast.aixin";
	private final String LED_RED = "/sys/class/led/red";
	private final String LED_GREEN = "/sys/class/led/green";
	private final String LED_YELLOW = "/sys/class/led/yellow";
	private final String LED_COLOR_TYPE = "/sys/class/led/color";// 1: Green ; 2
																	// Red 3
																	// yellow
	private final String LED_GREEN_FLASH = "/sys/class/led/green_flash";
	private final String LED_SHUT = "/sys/class/led/shut";

	// comand
	private final int CMD_LED_OPEN = 1; 
	private final int CMD_LED_CLOSE = 0; 

	private final int LED_GREEN_TYPE = 1;
	private final int LED_RED_TYPE = 2;
	private final int LED_YELLOW_TYPE = 3;
	
	private SensorManager mySensorManager;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
		intentFilter.addAction(AIXIN_UPDATE_BC);

		batteryReceiver = new BatteryReceiver();
		registerReceiver(batteryReceiver, intentFilter);
		
		mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); 
		mySensorManager.registerListener( 
	                this, 
	                mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), 
	                SensorManager.SENSOR_DELAY_GAME 
	                ); 
	}

	private class BatteryReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {

				int level = intent.getIntExtra("level", 0);
				int scale = intent.getIntExtra("scale", 100);
				int num = (level * 100) / scale;
				// String battery = num +"%";

				Log.d("zzl:::", "battery ::" + num);
				int statue = intent
						.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

				String battery = num + "%";
				if (num < 18
						&& (statue != BatteryManager.BATTERY_STATUS_CHARGING || statue != BatteryManager.BATTERY_STATUS_FULL)) {
					// 请充电
					String strCMD = String.valueOf(LED_RED_TYPE);
					writeSysfs(LED_COLOR_TYPE, strCMD);
					strCMD = String.valueOf(CMD_LED_OPEN);
					writeSysfs(LED_GREEN_FLASH, strCMD);
				} else {
					
					if(queryMessage(context) > 0){
						
						String strCMD = String.valueOf(LED_GREEN_TYPE);
						writeSysfs(LED_COLOR_TYPE, strCMD);
						strCMD = String.valueOf(CMD_LED_OPEN);
						writeSysfs(LED_GREEN_FLASH, strCMD);
						
					}else{
						String strCMD = String.valueOf(CMD_LED_CLOSE);
						writeSysfs(LED_GREEN_FLASH, strCMD);
	
						strCMD = String.valueOf(CMD_LED_OPEN);
						writeSysfs(LED_GREEN, strCMD);
					}
				}

				if (statue == BatteryManager.BATTERY_STATUS_CHARGING) {
					// 正在充电
					if(queryMessage(context) > 0){
						
						String strCMD = String.valueOf(LED_GREEN_TYPE);
						writeSysfs(LED_COLOR_TYPE, strCMD);
						strCMD = String.valueOf(CMD_LED_OPEN);
						writeSysfs(LED_GREEN_FLASH, strCMD);
						
					}else{
						String strCMD = String.valueOf(LED_RED_TYPE);
						writeSysfs(LED_COLOR_TYPE, strCMD);
						strCMD = String.valueOf(CMD_LED_OPEN);
						writeSysfs(LED_GREEN_FLASH, strCMD);
					}

				}
				if (num == 100
						&& (statue == BatteryManager.BATTERY_STATUS_CHARGING || statue == BatteryManager.BATTERY_STATUS_FULL)) {
					// 充满电
					
					if(queryMessage(context) > 0){
						
						String strCMD = String.valueOf(LED_GREEN_TYPE);
						writeSysfs(LED_COLOR_TYPE, strCMD);
						strCMD = String.valueOf(CMD_LED_OPEN);
						writeSysfs(LED_GREEN_FLASH, strCMD);
						
					}else{
						String strCMD = String.valueOf(CMD_LED_CLOSE);
						writeSysfs(LED_GREEN_FLASH, strCMD);
	
						strCMD = String.valueOf(CMD_LED_OPEN);
						writeSysfs(LED_GREEN, strCMD);
					}
				}
				if (num == 0) {
				}

			} else if (intent.getAction().equals(AIXIN_UPDATE_BC)) {

				int num = intent.getIntExtra("num", 0);
				if (num > 0) {
					// open green flash
					String strCMD = String.valueOf(LED_GREEN_TYPE);
					writeSysfs(LED_COLOR_TYPE, strCMD);
					strCMD = String.valueOf(CMD_LED_OPEN);
					writeSysfs(LED_GREEN_FLASH, strCMD);

				} else {
					// close green flash
					String strCMD = String.valueOf(CMD_LED_CLOSE);
					writeSysfs(LED_GREEN_FLASH, strCMD);

					strCMD = String.valueOf(CMD_LED_OPEN);
					writeSysfs(LED_GREEN, strCMD);
				}
			}
		}

	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		unregisterReceiver(batteryReceiver);
		mySensorManager.unregisterListener(this); 
		super.onDestroy();
	}

	
	private int queryMessage(Context context) {

		int num = 0;
		final Uri URI = Uri
				.parse("content://com.qiyi.minitv.video.messagecenter/itvcloud");
		String select = "is_read=0";

		ContentResolver cr = context.getContentResolver();
		Cursor cursor = cr.query(URI, null, select, null, null);

		if (cursor == null) {
			return num;
		}

		try {
			if (cursor.moveToFirst()) {
				do {
					num = num + 1;
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}

		return num;
	}
	
	private int writeSysfs(String path, String val) {
		if (!new File(path).exists()) {
			return 1;
		}

		try {
			FileWriter fw = new FileWriter(path);
			BufferedWriter writer = new BufferedWriter(fw, 64);
			try {
				writer.write(val);
			} finally {
				writer.close();
				fw.close();
			}
			return 0;

		} catch (IOException e) {
			return 1;
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		
		float[] values = event.values; 
        int sensorType = event.sensor.TYPE_LIGHT; 
        
        int level = 10;
        if (sensorType == Sensor.TYPE_LIGHT) { 
        	Log.d("zzl:::","light::"+values[0]+"::"+values[1]+"::"+values[2]);
        	
        	if(values[0] < 85)
        		level = 10;
        	if( values[0] >= 85 && values[0] < 170)
        		level = 30;
        	
        	if( values[0] >= 170 && values[0] < 255)
        		level = 50;
        	
        	if( values[0] >= 255 && values[0] < 340)
        		level = 70;
        	
        	if( values[0] >= 340 && values[0] < 425)
        		level = 90;
        	
        	if( values[0] >= 425 && values[0] < 510)
        		level = 110;
        	
        	if( values[0] >= 510 && values[0] < 595)
        		level = 130;
        	
        	if( values[0] >= 595 && values[0] < 680)
        		level = 150;
        	
        	if( values[0] >= 680 && values[0] < 765)
        		level = 170;
        	
        	if( values[0] >= 765 && values[0] < 850)
        		level = 190;
        	
        	if( values[0] >= 850 && values[0] < 935)
        		level = 210;
        	
        	if( values[0] >= 935 && values[0] < 1020)
        		level = 230;
        	
        	if( values[0] >= 1020 && values[0] < 1105)
        		level = 255;
         // this is setting light sensor
		     android.provider.Settings.System.putInt(getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    level);
        }
		
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

}
