package com.shoaibsaikat.ble;

import java.nio.charset.StandardCharsets;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;

public class BluetoothUtility {
	public static final String TAG = "BLE";

	public static final String SERVICE_UUID_1 = "00001802-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_UUID_1 = "afdf39cc-e249-4397-b342-295408ac33bc";
	public static final String CHAR_VALUE = "test value";

	public final static int SCAN_PERIOD = 2000;

	public static String byteArrayToString(byte[] byteArray) {
		try {
			return new String(byteArray, StandardCharsets.UTF_8);
		} catch (Exception e) {
			Log.e(TAG,"byteArrayToString failed: " + e.getMessage());
			e.printStackTrace();
		}
		return "";
	}
	
	public static byte[] stringToByte(String value) {
		try {
			if (value != null && value.length() > 0)
				return value.getBytes(StandardCharsets.UTF_8);
		} catch (Exception e) {
			Log.e(TAG,"stringToByte exception: " + e.getMessage());
		}
		return null;
	}

	public static boolean isBluetoothPermitted(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
		} else {
			return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
		}
	}
}
