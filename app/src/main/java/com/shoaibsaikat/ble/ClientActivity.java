package com.shoaibsaikat.ble;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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


public class ClientActivity extends Activity {
    private static final String TAG = "BLE";

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

    public final static int SCAN_PERIOD = 2000;

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

    private List<BluetoothDevice> mDeviceList;
    private ArrayList<String> mDeviceNameList;
    private static ArrayAdapter<String> centralAdapter;
    
    private Handler mHandler;
    
    private BluetoothGatt mBtGatt = null;
    private BluetoothGattService mGattService = null;
    BluetoothGattCharacteristic mGattCharacteristic = null;
    
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    
    @SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        
        isScanning = false;
        
        mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        btnScan = (Button)findViewById(R.id.buttonScan);
        btnStop = (Button)findViewById(R.id.buttonStopScan);
        btnSend = (Button) findViewById(R.id.buttonSendClient);
        btnConnect = (Button) findViewById(R.id.buttonConnect);
        spnCentralList = (Spinner) findViewById(R.id.spinnerCentralList);
        etInput = (EditText) findViewById(R.id.editTextInputClient);
        tvClient = (TextView) findViewById(R.id.textViewClient);
        etInput.setText("Client");
        btnConnect.setEnabled(false);
        
        mDeviceNameList = new ArrayList<String>();
        centralAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mDeviceNameList);
        centralAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCentralList.setAdapter(centralAdapter);
        mDeviceList = new ArrayList<BluetoothDevice>();
        
        mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
//				super.handleMessage(msg);
			}
        	
        };
    }

    @Override
    protected void onStop() {
    	if(mDeviceNameList != null) {
    		mDeviceNameList.clear();
    		mDeviceNameList = null;
    	}
		if(mBtGatt != null)
			mBtGatt.close();
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
    	if(mDeviceNameList != null) {
    		mDeviceNameList.clear();
    		mDeviceNameList = null;
    	}
		if(mBtGatt != null)
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
    	
    	Log.d(TAG, "choosen: " + choice);
    	Log.d(TAG, "devices size: " + mDeviceList.size());
    	
    	if(mDeviceList.size() > 0) {
    		if(mBtGatt != null)
    			mBtGatt.close();
    		
	    	mBtGatt = mDeviceList.get(choice).connectGatt(getApplicationContext(), false, mGattCallback);
    	}
    	else
    		Log.d(TAG, "No device to connect");
    }
    
    public void handleSend(View view) {
    	if(mBtGatt != null) {
			mGattCharacteristic.setValue(BluetoothUtility.stringToByte(etInput.getText().toString()));
			if(mBtGatt.writeCharacteristic(mGattCharacteristic))
				Log.d(TAG, "Data sent");
			else
				Log.d(TAG, "Data not sent");
    	}
    }
    
    //Check if bluetooth is enabled, if not, then request enable
    private void enableBluetooth() {
        if(mBluetoothAdapter == null) {
            Log.d(TAG, "Bluetooth NOT supported");
        } else if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }
    
    /**
     * BLE Scanning
     */
    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void startBleScan() {
        if(isScanning) return;
        enableBluetooth();
        isScanning = true;
        
        // Stops scanning after a pre-defined scan period.
		mHandler.postDelayed(new Runnable() {
			@SuppressLint("NewApi")
			@Override
			public void run() {
				isScanning = false;
				
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				
				btnScan.setEnabled(true);
	            btnStop.setEnabled(false);
			}
		}, SCAN_PERIOD);
		
		mBluetoothAdapter.startLeScan(mLeScanCallback);
		
        btnScan.setEnabled(false);
        btnStop.setEnabled(true);
        Log.d(TAG, "Bluetooth is currently scanning...");
    }

    @SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void stopBleScan() {
        if(!isScanning) return;
        isScanning = false;
        
    	mBluetoothAdapter.stopLeScan(mLeScanCallback);
        
        btnScan.setEnabled(true);
        btnStop.setEnabled(false);
        Log.d(TAG, "Scanning has been stopped");
    }

    // Device scan callback for previous sdk version
 	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
 		@Override
 		public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        	String deviceInfo = device.getName() + " - " + device.getAddress();
        	if(mDeviceNameList.contains(deviceInfo)) return;
        	
            mDeviceNameList.add(deviceInfo);
            mDeviceList.add(device);
            
            Log.d(TAG, "Device: " + deviceInfo + " Scanned!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    centralAdapter.notifyDataSetChanged();
                }
            });
            
            btnConnect.setEnabled(true);
            //stop scanning after we find ble device
//            stopBleScan();
//            btnScan.setEnabled(true);
//            btnStop.setEnabled(false);
		}
 	};
    
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {           
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                
                if(gatt != null) {
                	mBtGatt = gatt;
                	if(gatt.discoverServices()) Log.d(TAG, "Attempt to discover Service");
                	else Log.d(TAG, "Failed to discover Service");
    			} else Log.d(TAG, "btGatt == null");

            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                mConnectionState = BluetoothProfile.STATE_CONNECTING;
                Log.i(TAG, "Connecting GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                mBtGatt.close();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTING;
                Log.i(TAG, "Disconnecting from GATT server.");
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Log.d(TAG, "Services Discovered successfully : " + status);
            	
            	List<BluetoothGattService> gattServices = gatt.getServices();            	
            	mBtGatt = gatt;
            	if(gattServices.size() > 0) {
            		Log.d(TAG, "Found : " + gattServices.size() + " services");
            		for (BluetoothGattService bluetoothGattService : gattServices)
						Log.d(TAG, "UUID = " + bluetoothGattService.getUuid().toString());
            		
            		BluetoothGattService gattServ = gatt.getService(UUID.fromString(BluetoothUtility.SERVICE_UUID_1));
    				if(gattServ != null) {
    					mGattService = gattServ;
    					BluetoothGattCharacteristic gattChar = gattServ.getCharacteristic(UUID.fromString(BluetoothUtility.CHAR_UUID_1));
    					mGattCharacteristic = gattChar;
    					gatt.readCharacteristic(gattChar);
    				}
    				else
    					Log.d(TAG, "gattServ == null");
    			} else
					Log.d(TAG, "gattServices.size() == 0");
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Log.d(TAG, "Characteristic is read");
            	
            	gatt.setCharacteristicNotification(characteristic, true);
            }
        }

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			byte[] data = characteristic.getValue();
			if(data != null) {
				final String tmp = BluetoothUtility.byteArraytoString(data);
				Log.d(TAG, "Changed data : " + tmp);
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tvClient.setText(tmp);
					}
				});
			}
			else
				Log.d(TAG, "Changed Data is null");
		}
    };
}
