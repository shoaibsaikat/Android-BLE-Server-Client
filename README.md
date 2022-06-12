A Simple BLE server client both on Android Phone side.

Step:
1. Run two instances of this app in two devices, one for server and other for client.
2. Turn on Bluetooth on both devices and also give app location permission by going to settings.
3. Pair both devices.
4. For Server:
	i. Click "Server".
	ii. Click "Start Advertising".
5. For Client:
	i. Click "Client".
	ii. Cick "Scan for Devices".
	iii. You will see the server name on dropdown list. Select the appropriate device.
6. Now type text you want to send from both client and server and click "Send". Message should arrive on opposite device.


NOTE:
i. Sometimes it takes several scan to find device.
ii. We're also getting BluetoothGattService as null, as a result client can't connect. Need to check why.
