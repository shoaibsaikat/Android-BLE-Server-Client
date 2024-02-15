A Simple BLE server and client both can be implementable on Android Phone.
[It's a demo app for learning purpose only, not ready for commercial use. Use it on your own risk!]

# Steps:

# Before running app:
1. Turn on Bluetooth, on both devices and also give app location permission by going to settings.
2. Pair both devices.

# App:
1. Run two instances of this app in two devices, one for server and other for client.
2. For Server:
	i. Click "Server".
	ii. Click "Start Advertising".
3. For Client:
	i. Click "Client".
	ii. Click "Scan for Devices".
	iii. You will see the server name on dropdown list. Select the appropriate device.
4. Now type the text you want to send from both client and server and click "Send". Message should arrive on the opposite device.


# NOTE:
i. Sometimes it takes several scan to find device.
ii. We're also getting BluetoothGattService as null, as a result client can't connect. Need to check why.
