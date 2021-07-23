package com.shoaibsaikat.ble;

import java.io.UnsupportedEncodingException;

import android.util.Log;

public class BluetoothUtility {
	public static final String TAG = "BLE";

	public static final String SERVICE_UUID_1 = "00001802-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_UUID_1 = "afdf39cc-e249-4397-b342-295408ac33bc";

	public final static int SCAN_PERIOD = 2000;

	public static String byteArrayToString(byte[] byteArray) {
		try {
			return new String(byteArray, "UTF-8");
		} catch (Exception e) {
			Log.e(TAG,"byteArrayToString failed: " + e.getMessage());
			e.printStackTrace();
		}
		return "";
	}
	
	public static byte[] stringToByte(String value) {
		try {
			if (value != null && value.length() > 0)
				return value.getBytes("UTF-8");
		} catch (UnsupportedEncodingException ex) {
			Log.e(TAG,"stringToByte UnsupportedEncodingException.");
		} catch (Exception e) {
			Log.e(TAG,"stringToByte exception: " + e.getMessage());
		}
		return null;
	}
}
