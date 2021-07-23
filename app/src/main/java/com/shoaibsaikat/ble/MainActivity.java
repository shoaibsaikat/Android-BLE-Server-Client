package com.shoaibsaikat.ble;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements OnClickListener  {
	
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
