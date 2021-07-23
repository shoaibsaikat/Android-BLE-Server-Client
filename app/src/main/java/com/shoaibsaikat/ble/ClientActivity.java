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
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ClientActivity extends AppCompatActivity {
    private boolean isScanning;
    
    private Button btnScan;
    private Button btnStop;
    private Button btnConnect;
    private Button btnSend;
    private EditText etInput;
    private TextView tvClient;
    private Spinner spnCentralList;
    
    private BluetoothManager mBluetoothManager;
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
        setContentView(R.layout.activity_client);
        
        isScanning = false;
        
        mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        btnScan = findViewById(R.id.buttonScan);
        btnStop = findViewById(R.id.buttonStopScan);
        btnSend = findViewById(R.id.buttonSendClient);
        btnConnect = findViewById(R.id.buttonConnect);
        spnCentralList = findViewById(R.id.spinnerCentralList);
        etInput = findViewById(R.id.editTextInputClient);
        tvClient = findViewById(R.id.textViewClient);

        etInput.setText("Client");
        btnConnect.setEnabled(false);
        
        mDeviceNameList = new ArrayList<String>();
        centralAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mDeviceNameList);
        centralAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCentralList.setAdapter(centralAdapter);
        mDeviceList = new ArrayList<BluetoothDevice>();

        mHandler = new Handler();
    }

    @Override
    protected void onStop() {
        if (mDeviceNameList != null) {
    		mDeviceNameList.clear();
    		mDeviceNameList = null;
    	}
        if (mBtGatt != null)
			mBtGatt.close();
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        if (mDeviceNameList != null) {
    		mDeviceNameList.clear();
    		mDeviceNameList = null;
    	}
		if (mBtGatt != null)
			mBtGatt.close();
        super.onDestroy();
    }

    public void handleScanStart(View view) {
        mDeviceNameList.clear();
        mDeviceList.clear();
        btnConnect.setEnabled(false);
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	centralAdapter.clear();
            	centralAdapter.notifyDataSetChanged();
            }
        });
        startBleScan();
    }

    public void handleScanStop(View view) {
        stopBleScan();
    }
    
    public void handleConnect(View view) {
    	int choice = spnCentralList.getSelectedItemPosition();
    	
    	Log.d(BluetoothUtility.TAG, "choosen: " + choice);
    	Log.d(BluetoothUtility.TAG, "devices size: " + mDeviceList.size());
    	
        if (mDeviceList.size() > 0) {
            if (mBtGatt != null)
    			mBtGatt.close();
	    	mBtGatt = mDeviceList.get(choice).connectGatt(getApplicationContext(), false, mGattCallback);
    	} else {
            Log.d(BluetoothUtility.TAG, "No device to connect");
        }
    }
    
    public void handleSend(View view) {
        if (mBtGatt != null) {
			mGattCharacteristic.setValue(BluetoothUtility.stringToByte(etInput.getText().toString()));
			if (mBtGatt.writeCharacteristic(mGattCharacteristic))
                Log.d(BluetoothUtility.TAG, "Data sent");
			else
                Log.d(BluetoothUtility.TAG, "Data not sent");
    	}
    }
    
    //Check if bluetooth is enabled, if not, then request enable
    private void enableBluetooth() {
        if (mBluetoothAdapter == null) {
            Log.e(BluetoothUtility.TAG, "Bluetooth NOT supported");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }
    
    /**
     * BLE Scanning
     */
	public void startBleScan() {
        if (isScanning)
            return;
        enableBluetooth();
        isScanning = true;
        
        // Stops scanning after a pre-defined scan period.
		mHandler.postDelayed(new Runnable() {
			public void run() {
                stopBleScan();
			}
		}, BluetoothUtility.SCAN_PERIOD);
		
        mBluetoothLeScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                String deviceInfo = result.getScanRecord().getDeviceName();

                if (mDeviceNameList == null)
                    mDeviceNameList = new ArrayList<>();

                if (mDeviceNameList.contains(deviceInfo))
                    return;
                mDeviceNameList.add(deviceInfo);
                mDeviceList.add(result.getDevice());
                Log.d(BluetoothUtility.TAG, "Device: " + deviceInfo + " Scanned!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO: fix
                        centralAdapter.notifyDataSetChanged();
                    }
                });
                btnConnect.setEnabled(true);
            }
        });
        btnScan.setEnabled(false);
        btnStop.setEnabled(true);
    }

	public void stopBleScan() {
        if (!isScanning)
            return;
        isScanning = false;
        
        mBluetoothLeScanner.stopScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
            }
        });
        
        btnScan.setEnabled(true);
        btnStop.setEnabled(false);
        Log.d(BluetoothUtility.TAG, "Scanning has been stopped");
    }
    
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {           
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
                Log.i(BluetoothUtility.TAG, "Connected to GATT server.");
                
                if (gatt != null) {
                	mBtGatt = gatt;
                    if (gatt.discoverServices()) {
                        Log.d(BluetoothUtility.TAG, "Attempt to discover Service");
                    } else {
                        Log.d(BluetoothUtility.TAG, "Failed to discover Service");
                    }
                } else {
                    Log.d(BluetoothUtility.TAG, "btGatt == null");
                }
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                mConnectionState = BluetoothProfile.STATE_CONNECTING;
                Log.i(BluetoothUtility.TAG, "Connecting GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                mBtGatt.close();
                Log.i(BluetoothUtility.TAG, "Disconnected from GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTING;
                Log.i(BluetoothUtility.TAG, "Disconnecting from GATT server.");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(BluetoothUtility.TAG, "Services Discovered successfully : " + status);
            	
            	List<BluetoothGattService> gattServices = gatt.getServices();            	
            	mBtGatt = gatt;
                if (gattServices.size() > 0) {
            		Log.d(BluetoothUtility.TAG, "Found : " + gattServices.size() + " services");
            		for (BluetoothGattService bluetoothGattService : gattServices)
						Log.d(BluetoothUtility.TAG, "UUID = " + bluetoothGattService.getUuid().toString());
            		
            		BluetoothGattService gattServ = gatt.getService(UUID.fromString(BluetoothUtility.SERVICE_UUID_1));
                    if (gattServ != null) {
    					mGattService = gattServ;
    					BluetoothGattCharacteristic gattChar = gattServ.getCharacteristic(UUID.fromString(BluetoothUtility.CHAR_UUID_1));
    					mGattCharacteristic = gattChar;
    					gatt.readCharacteristic(gattChar);
    				} else {
                        Log.d(BluetoothUtility.TAG, "gattServ == null");
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
                Log.d(BluetoothUtility.TAG, "Characteristic is read");
            	gatt.setCharacteristicNotification(characteristic, true);
            }
        }

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			byte[] data = characteristic.getValue();
            if (data != null) {
				final String tmp = BluetoothUtility.byteArrayToString(data);
                Log.d(BluetoothUtility.TAG, "Changed data : " + tmp);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tvClient.setText(tmp);
					}
				});
			} else {
                Log.d(BluetoothUtility.TAG, "Changed Data is null");
            }
		}
    };
}
