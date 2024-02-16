A Simple BLE server and client for Android Phone.
[It's a demo app for learning purpose only, not ready for commercial use. Use it on your own risk!]

# Steps:

# Before running app:
Give app all permissions by going to settings. Permission asking from app is not implemented. App will crash if permission is not given.

# How to use the app:
1. Run two instances of this app in two devices, one for server and another for client.
	1. For Server:
		i. Click "Server".
		ii. Click "Start Advertising".
	2. For Client:
		i. Click "Client".
		ii. Click "Scan for Devices".
		iii. You will see the server name on dropdown list. Select the appropriate device.
2. Now type the text you want to send from both client and server and click "Send". Message should arrive on the opposite device.


# Issues:
1. Sometimes it takes several scan to find the server.
2. We're getting BluetoothGattService null, if devices are connected multiple times (need to check why). For now to fix this, when done with the app turn off bluetooth on both devices. We can also implement turn off / on bluetooth from app before start advertising and scanning to bypass the issue.
