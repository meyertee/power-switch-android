package com.meyertee.power_switch;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements
		OnClickListener,
		StatusRequestTask.Delegate,
		DnsDiscovery.Delegate,
		LocalIpResolver.Delegate {

	private Button switchButton;
	// private DnsDiscovery dnsDiscoveryThread;
	private LocalIpResolver localIpResolverThread;
	private String ipAddress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		switchButton = (Button) findViewById(R.id.activity_main_switch);
		switchButton.setOnClickListener(this);

		Handler uiHandler = new Handler();

		// dnsDiscoveryThread = new DnsDiscovery(this, uiHandler, this);
		localIpResolverThread = new LocalIpResolver("raspberrypi", this, uiHandler, this);
		// dnsDiscoveryThread.start();
		localIpResolverThread.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (ipAddress != null) {
			requestStatus();
		}
	}

	@Override
	public void onClick(View v) {}

	private void requestStatus() {
		Toast.makeText(this, "Checking status", Toast.LENGTH_SHORT).show();
		new StatusRequestTask(this).execute(ipAddress);
	}

	@Override
	public void onStatusRequestComplete(Boolean remoteStatus) {
		if (remoteStatus == null) {
			Toast.makeText(this, "Request failed.", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Status is " + (remoteStatus ? "on" : "off"), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onServiceDiscovered(String message) {
		Toast.makeText(this, "message", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onResolvedIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;

		if (ipAddress != null) {
			Toast.makeText(this, "Resolved IP " + ipAddress, Toast.LENGTH_SHORT).show();
			requestStatus();
		} else {
			Toast.makeText(this, "Resolving IP failed", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		boolean retry = true;
		// dnsDiscoveryThread.stopDiscovery();
		localIpResolverThread.stopDiscovery();
		while (retry) {
			try {
				localIpResolverThread.join();
				// dnsDiscoveryThread.join();
				retry = false;
			} catch (InterruptedException e) {}
		}
	}
}
