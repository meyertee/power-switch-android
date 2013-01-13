package com.meyertee.power_switch;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

/**
 * https://github.com/twitwi/AndroidDnssdDemo
 * http://techtej.blogspot.de/2011/02/android-passing-data-between-main.html
 * 
 * @author Thomas Meyer (thomas@meyertee.com)
 */
public class LocalIpResolver extends HandlerThread {

	public interface Delegate {
		void onResolvedIpAddress(String ipAddress);
	}

	private Context context;
	private Handler uiHandler;
	private Delegate delegate;

	android.net.wifi.WifiManager.MulticastLock lock;
	private String type = "_device-info._tcp.local.";
	private JmDNS jmdns = null;
	private ServiceListener listener = null;
	
	private String nameToResolve;
	private boolean nameFound;

	public LocalIpResolver(String nameToResolve, Context context, Handler uiHandler, Delegate delegate) {
		super("LocalIpResolver");

		this.nameToResolve = nameToResolve;
		
		this.nameFound = false;

		this.context = context;
		this.uiHandler = uiHandler;
		this.delegate = delegate;
	}

	public void run() {
		startDnsDiscovery();
		try {
			sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (!nameFound){
			reportIpAddress(null);
		}
	}

	private void startDnsDiscovery() {
		WifiManager wifi = (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);

		lock = wifi.createMulticastLock("DnsDiscovery");
		lock.setReferenceCounted(true);
		lock.acquire();

		try {
			jmdns = JmDNS.create();

			jmdns.addServiceListener(type, listener = new ServiceListener() {

				@Override
				public void serviceResolved(ServiceEvent ev) {
					String name = ev.getInfo().getName();
					if (nameToResolve.equals(name)) {
						nameFound = true;
						if (ev.getInfo().getHostAddresses().length > 0) {
							reportIpAddress(ev.getInfo().getHostAddresses()[0]);
						} else {
							reportIpAddress(null);
						}
					}
				}

				@Override
				public void serviceRemoved(ServiceEvent ev) {}

				@Override
				public void serviceAdded(ServiceEvent event) {
					// Required to force serviceResolved to be called again (after the first search)
					jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private void reportIpAddress(final String msg) {
		Log.i(getClass().getSimpleName(), "Got ip" + msg);

		uiHandler.post(new Runnable() {
			public void run() {
				delegate.onResolvedIpAddress(msg);
			}
		});
		stopDiscovery();
	}

	public synchronized void stopDiscovery() {
		if (jmdns != null) {
			if (listener != null) {
				jmdns.removeServiceListener(type, listener);
				listener = null;
			}
			jmdns.unregisterAllServices();
			try {
				jmdns.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			jmdns = null;
		}

		if (lock != null) {
			lock.release();
			lock = null;
		}
	}
}
