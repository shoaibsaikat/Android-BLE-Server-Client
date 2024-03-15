package com.shoaibsaikat.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class ClientActivity extends AppCompatActivity {
    private boolean mIsScanning;
    
    private Button mBtnScan;
    private Button mBtnStop;
    private Button mBtnConnect;
    private Button mBtnSend;
    private EditText mEtInput;
    private TextView mTvClient;
    private Spinner mSpnCentralList;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private List<BluetoothDevice> mDeviceList;
    private ArrayList<String> mDeviceNameList;
    private static ArrayAdapter<String> centralAdapter;

    private Handler mHandler;
    
    private BluetoothGatt mBtGatt = null;
    private BluetoothGattService mGattService = null;
    BluetoothGattCharacteristic mGattCharacteristic = null;

    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        setContentView(R.layout.activity_client);
        
        mIsScanning = false;

        BluetoothManager bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mBtnScan = findViewById(R.id.buttonScan);
        mBtnStop = findViewById(R.id.buttonStopScan);
        mBtnConnect = findViewById(R.id.buttonConnect);
        mBtnSend = findViewById(R.id.buttonSendClient);
        mSpnCentralList = findViewById(R.id.spinnerCentralList);
        mEtInput = findViewById(R.id.editTextInputClient);
        mTvClient = findViewById(R.id.textViewClient);

        mBtnConnect.setEnabled(false);
        mBtnSend.setEnabled(false);
        
        mDeviceNameList = new ArrayList<>();
        centralAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mDeviceNameList);
        centralAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpnCentralList.setAdapter(centralAdapter);
        mDeviceList = new ArrayList<>();

        mHandler = new Handler();
    }
    
    @Override
    protected void onDestroy() {
        if (mDeviceNameList != null) {
    		mDeviceNameList.clear();
    	}
        if (mBtGatt != null) {
            mBtGatt.close();
        }
        super.onDestroy();
    }

    public void handleScanStart(View view) {
        runOnUiThread(() -> {
            mBtnConnect.setEnabled(false);
            centralAdapter.clear();
            centralAdapter.notifyDataSetChanged();
        });
        startBleScan();
    }

    public void handleScanStop(View view) {
        stopBleScan();
    }
    
    public void handleConnect(View view) {
    	int choice = mSpnCentralList.getSelectedItemPosition();
    	
    	Log.d(BluetoothUtility.TAG, "chosen: " + choice);
    	Log.d(BluetoothUtility.TAG, "devices size: " + mDeviceList.size());
    	
        if (!mDeviceList.isEmpty()) {
            if (mBtGatt != null)
    			mBtGatt.close();
	    	mBtGatt = mDeviceList.get(choice).connectGatt(getApplicationContext(), false, mGattCallback);
    	} else {
            Log.e(BluetoothUtility.TAG, "no device to connect");
        }
    }
    
    public void handleSend(View view) {
        if (mBtGatt != null && mGattCharacteristic != null) {
			mGattCharacteristic.setValue(BluetoothUtility.stringToByte(mEtInput.getText().toString()));
			if (mBtGatt.writeCharacteristic(mGattCharacteristic))
                Log.d(BluetoothUtility.TAG, "data sent");
			else
                Log.d(BluetoothUtility.TAG, "data not sent");
    	}
    }
    
    //Check if bluetooth is enabled, if not, then request enable
    private void enableBluetooth() {
        if (mBluetoothAdapter == null) {
            Log.e(BluetoothUtility.TAG, "bluetooth not supported");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

	public void startBleScan() {
        if (mIsScanning)
            return;
        enableBluetooth();
        mIsScanning = true;
        
        // Stops scanning after a pre-defined scan period.
		mHandler.postDelayed(ClientActivity.this::stopBleScan, BluetoothUtility.SCAN_PERIOD);
		
        mBluetoothLeScanner.startScan(mBleScanCallback);
        mBtnScan.setEnabled(false);
        mBtnStop.setEnabled(true);
    }

	public void stopBleScan() {
        if (!mIsScanning)
            return;
        mIsScanning = false;
        
        mBluetoothLeScanner.stopScan(mBleScanCallback);
        
        mBtnScan.setEnabled(true);
        mBtnStop.setEnabled(false);
        Log.d(BluetoothUtility.TAG, "scanning stopped");
    }

    private final ScanCallback mBleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceInfo = result.getScanRecord().getDeviceName();

            if (mDeviceNameList.contains(deviceInfo))
                return;

            if (deviceInfo != null) {
                mDeviceNameList.add(deviceInfo);
                mDeviceList.add(result.getDevice());
                Log.d(BluetoothUtility.TAG, "device: " + deviceInfo + " found!");
                runOnUiThread(() -> {
                    // TODO: fix
                    mBtnConnect.setEnabled(true);
                    centralAdapter.notifyDataSetChanged();
                });
            }
        }
    };
    
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = BluetoothProfile.STATE_CONNECTED;

                if (gatt != null) {
                    Log.i(BluetoothUtility.TAG, "connected to gatt server.");
                    runOnUiThread(() -> {
                        Toast.makeText(ClientActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                        mBtnSend.setEnabled(true);
                        mBtnConnect.setEnabled(false);
                    });

                	mBtGatt = gatt;
                    if (gatt.discoverServices()) {
                        Log.d(BluetoothUtility.TAG, "attempt to discover Service");
                    } else {
                        Log.d(BluetoothUtility.TAG, "failed to discover Service");
                    }
                } else {
                    Toast.makeText(ClientActivity.this, "Invalid server", Toast.LENGTH_SHORT).show();
                    Log.d(BluetoothUtility.TAG, "btGatt == null");
                }
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                mConnectionState = BluetoothProfile.STATE_CONNECTING;
                Log.i(BluetoothUtility.TAG, "connecting gatt server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                mBtGatt.close();
                runOnUiThread(() -> {
                    Toast.makeText(ClientActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                    mBtnConnect.setEnabled(true);
                    mBtnSend.setEnabled(false);
                });
                Log.i(BluetoothUtility.TAG, "disconnected from gatt server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTING;
                Log.i(BluetoothUtility.TAG, "disconnecting from gatt server.");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(BluetoothUtility.TAG, "services discovered successfully : " + status);
            	
            	List<BluetoothGattService> gattServices = gatt.getServices();            	
            	mBtGatt = gatt;
                if (!gattServices.isEmpty()) {
            		Log.d(BluetoothUtility.TAG, "found : " + gattServices.size() + " services");
            		for (BluetoothGattService bluetoothGattService : gattServices)
						Log.d(BluetoothUtility.TAG, "uuid = " + bluetoothGattService.getUuid().toString());
            		
            		BluetoothGattService gattServ = gatt.getService(UUID.fromString(BluetoothUtility.SERVICE_UUID_1));
                    if (gattServ != null) {
    					mGattService = gattServ;
    					BluetoothGattCharacteristic gattChar = gattServ.getCharacteristic(UUID.fromString(BluetoothUtility.CHAR_UUID_1));
    					if (gattChar != null) {
                            mGattCharacteristic = gattChar;
                            gatt.readCharacteristic(gattChar);
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(ClientActivity.this, "getCharacteristic is null", Toast.LENGTH_SHORT).show();
                                mBtnSend.setEnabled(true);
                                mBtnConnect.setEnabled(false);
                            });
                            Log.e(BluetoothUtility.TAG, "getCharacteristic == null");
                        }

    				} else {
                        runOnUiThread(() -> {
                            Toast.makeText(ClientActivity.this, "BluetoothGattService is null", Toast.LENGTH_SHORT).show();
                            mBtnSend.setEnabled(false);
                            mBtnConnect.setEnabled(false);
                        });
                        Log.e(BluetoothUtility.TAG, "gattServ == null");
                    }
    			} else {
                    Log.d(BluetoothUtility.TAG, "gattServices.size() == 0");
                }
            } else {
                Log.w(BluetoothUtility.TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(BluetoothUtility.TAG, "characteristic is read");
                gatt.setCharacteristicNotification(characteristic, true);
            }
        }

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			byte[] data = characteristic.getValue();
            if (data != null) {
				final String tmp = BluetoothUtility.byteArrayToString(data);
                Log.d(BluetoothUtility.TAG, "changed data : " + tmp);
				runOnUiThread(() -> mTvClient.setText(tmp));
			} else {
                Log.d(BluetoothUtility.TAG, "changed Data is null");
            }
		}
    };
}
