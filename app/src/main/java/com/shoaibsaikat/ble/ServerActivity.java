package com.shoaibsaikat.ble;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.shoaibsaikat.ble.BluetoothUtility;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ServerActivity extends Activity {
    private static final String TAG = "BLE";

    private Button btnStopAdv;
    private Button btnAdv;
    private Button btnSendData;
    private EditText etInput;
    private TextView tvServer;
    
    private BluetoothGattServer mGattServer;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothDevice mConnectedDevice;
    
    private boolean isAdvertising;
    private boolean isDeviceSet = false;
    
    private ArrayList<BluetoothGattService> mAdvertisingServices;
    private List<ParcelUuid> mServiceUuids;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        
        isAdvertising = false;
        
        mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        //bluetoothAdapter.setName(BLUETOOTH_ADAPTER_NAME);
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mAdvertisingServices = new ArrayList<BluetoothGattService>();
        mServiceUuids = new ArrayList<ParcelUuid>();

        btnAdv = (Button)findViewById(R.id.buttonAdvStart);
        btnStopAdv = (Button)findViewById(R.id.buttonAdvStop);
        btnSendData = (Button) findViewById(R.id.buttonSendServer);
        tvServer = (TextView) findViewById(R.id.textViewServer);
        etInput = (EditText) findViewById(R.id.editTextInputServer);
        etInput.setText("Server");

        //adding service and characteristics
        BluetoothGattService firstService = new BluetoothGattService(UUID.fromString(BluetoothUtility.SERVICE_UUID_1), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(
                UUID.fromString(BluetoothUtility.CHAR_UUID_1),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        firstService.addCharacteristic(firstServiceChar);
        
        mAdvertisingServices.add(firstService);
        mServiceUuids.add(new ParcelUuid(firstService.getUuid()));
    }

    @Override
    protected void onDestroy() {
    	if(mAdvertisingServices != null) {
    		mAdvertisingServices.clear();
    		mAdvertisingServices = null;
    	}
    	if(mServiceUuids != null) {
    		mServiceUuids.clear();
    		mServiceUuids = null;
    	}
    	stopAdvertise();
        super.onDestroy();
    }

    public void handleStartClick(View view) {
        startAdvertise();
        btnAdv.setEnabled(false);
        btnStopAdv.setEnabled(true);
    }

    public void handleStopClick(View view) {
        stopAdvertise();
        btnAdv.setEnabled(true);
        btnStopAdv.setEnabled(false);
    }
    
    public void handleSendClick(View view) {
    	if(isDeviceSet && writeCharacteristicToGatt(etInput.getText().toString())) {
    		Toast.makeText(ServerActivity.this, "Data written", Toast.LENGTH_SHORT).show();
    		Log.d(TAG, "Data written from server");
    	}
    	else {
    		Toast.makeText(ServerActivity.this, "Data not written", Toast.LENGTH_SHORT).show();
    		Log.d(TAG, "Data not written");
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
    
    private void startGattServer() {
        mGattServer = mBluetoothManager.openGattServer(getApplicationContext(), gattServerCallback);
        for(int i = 0; i < mAdvertisingServices.size(); i++) {
            mGattServer.addService(mAdvertisingServices.get(i));
            Log.d(TAG, "uuid" + mAdvertisingServices.get(i).getUuid());
        }
    }
    
    //Public method to begin advertising services
    public void startAdvertise() {
        if(isAdvertising) return;
        enableBluetooth();
        startGattServer();

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

        dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement
        dataBuilder.setIncludeDeviceName(true);
        for (ParcelUuid serviceUuid : mServiceUuids)
        	dataBuilder.addServiceUuid(serviceUuid);

        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), advertiseCallback);
        isAdvertising = true;
    }

    //Stop ble advertising and clean up
    public void stopAdvertise() {
        if(!isAdvertising) return;
        mBluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        mGattServer.clearServices();
        mGattServer.close();
        mAdvertisingServices.clear();
        isAdvertising = false;
    }
    
    public boolean writeCharacteristicToGatt(String data) {
    	final BluetoothGattService service = mGattServer.getService(UUID.fromString(BluetoothUtility.SERVICE_UUID_1));
    	final BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BluetoothUtility.CHAR_UUID_1));

    	if(mConnectedDevice != null && characteristic.setValue(data)) {
    		mGattServer.notifyCharacteristicChanged(mConnectedDevice, characteristic, true);
    		return true;
    	}
    	else
    		return false;
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
    	@Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            String successMsg = "Advertisement command attempt successful";
            Log.d(TAG, successMsg);
        }

    	@Override
        public void onStartFailure(int i) {
            String failMsg = "Advertisement command attempt failed: " + i;
            Log.e(TAG, failMsg);
        }
    };

    public BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange status=" + status + "->" + newState);
            mConnectedDevice = device;
            isDeviceSet = true;
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
        	Log.d(TAG, "service added: " + status);
        }

        @Override
        public void onCharacteristicReadRequest(
        		BluetoothDevice device,
        		int requestId,
        		int offset,
        		BluetoothGattCharacteristic characteristic
        ) {
            Log.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);

            if (characteristic.getUuid().equals(UUID.fromString(BluetoothUtility.CHAR_UUID_1))) {
                characteristic.setValue("test");
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWriteRequest(
        		BluetoothDevice device,
        		int requestId,
        		BluetoothGattCharacteristic characteristic,
        		boolean preparedWrite,
        		boolean responseNeeded,
        		int offset,
        		byte[] value
        ) {
            if(value != null) {
            	Log.d(TAG, "Data written: " + BluetoothUtility.byteArraytoString(value));
            	
            	final String tmp = BluetoothUtility.byteArraytoString(value);
            	runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						tvServer.setText(tmp);	
					}
				});
            	
            	mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
            else
            	Log.d(TAG, "value is null");
        }
    };
}
