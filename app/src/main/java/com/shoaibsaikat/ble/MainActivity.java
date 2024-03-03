package com.shoaibsaikat.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity implements OnClickListener {
  
	private static final int REQUEST_ENABLE_BT = 0;
	Button mBtnServer, mBtnClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (getSupportActionBar() != null)
			getSupportActionBar().hide();

		setContentView(R.layout.activity_main);

		mBtnServer = (Button) findViewById(R.id.buttonServer);
		mBtnClient = (Button) findViewById(R.id.buttonClient);

		mBtnClient.setOnClickListener(this);
		mBtnServer.setOnClickListener(this);

		BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

		if (bluetoothAdapter == null) {
			finish();
		} else {
//			Permission is only checked once, can be checked in other activities for better experience
			if (!BluetoothUtility.isBluetoothPermitted(this)) {
				Toast.makeText(this, "Bluetooth not permitted", Toast.LENGTH_SHORT).show();
				finish();
			}

//			Check if bluetooth is enabled, if not, then request enable
			if (!bluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED) {
			finish();
		}
	}

	@SuppressLint("NonConstantResourceId")
	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.buttonServer:
			intent = new Intent(getApplicationContext(), ServerActivity.class);
			startActivity(intent);
			break;
		case R.id.buttonClient:
			intent = new Intent(getApplicationContext(), ClientActivity.class);
			startActivity(intent);
			break;
		default:
			break;
		}
	}
}
