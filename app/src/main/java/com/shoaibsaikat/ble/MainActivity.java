package com.shoaibsaikat.ble;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener  {
	
	Button btnServer, btnClient;
	private static final String TAG = "BLE";

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btnServer = (Button) findViewById(R.id.buttonServer);
        btnClient = (Button) findViewById(R.id.buttonClient);
        
        btnClient.setOnClickListener(this);
        btnServer.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonServer:
			
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
	        	Toast.makeText(this, "Go get The Lollipop", Toast.LENGTH_LONG).show();
	        } else {
				Intent intent = new Intent(MainActivity.this, ServerActivity.class);
				startActivity(intent);
	        }
			
			break;
			
		case R.id.buttonClient:
			
			Intent intent = new Intent(MainActivity.this, ClientActivity.class);
			startActivity(intent);
			
			break;

		default:
			break;
		}
	}
}
