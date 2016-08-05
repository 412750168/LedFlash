package zzl.bestidear.ledflash;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.Toast;

public class BootCompleteReceive extends BroadcastReceiver {

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
	private final int CMD_LED_OPEN = 1; //
	private final int CMD_LED_CLOSE = 0; //

	private final int LED_GREEN_TYPE = 1;
	private final int LED_RED_TYPE = 2;
	private final int LED_YELLOW_TYPE = 3;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub

		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

			if (queryMessage(context) > 0) {
				String strCMD = String.valueOf(LED_GREEN_TYPE);
				writeSysfs(LED_COLOR_TYPE, strCMD);
				strCMD = String.valueOf(CMD_LED_OPEN);
				writeSysfs(LED_GREEN_FLASH, strCMD);
			} else {
				String strCMD = String.valueOf(CMD_LED_CLOSE);
				writeSysfs(LED_GREEN_FLASH, strCMD);

				strCMD = String.valueOf(CMD_LED_OPEN);
				writeSysfs(LED_GREEN, strCMD);
			}

			Intent startServiceIntent = new Intent(context,
					KeyTouchServerListener.class);
			startServiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(startServiceIntent);

			Toast.makeText(context, "test-led-flash", Toast.LENGTH_LONG).show();

		}
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

}
