package com.meyertee.power_switch;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

public class DnsDiscovery extends HandlerThread {

	public interface Delegate {
		void onServiceDiscovered(String message);
	}

	private Context context;
	private Handler uiHandler;
	private Delegate delegate;

	android.net.wifi.WifiManager.MulticastLock lock;
	private String type = "_device-info._tcp.local.";
	private JmDNS jmdns = null;
	private ServiceListener listener = null;
	private ServiceInfo serviceInfo;

	public DnsDiscovery(Context context, Handler uiHandler, Delegate delegate) {
		super("DnsDiscovery");

		this.context = context;
		this.uiHandler = uiHandler;
		this.delegate = delegate;
	}

	public void run() {
		startDnsDiscovery();
	}

	private void startDnsDiscovery() {
		WifiManager wifi = (WifiManager) context.getSystemService(android.content.Context.WIFI_SERVICE);

		Log.i(getClass().getSimpleName(), "DNS discovery");

		lock = wifi.createMulticastLock("DnsDiscovery");
		lock.setReferenceCounted(true);
		lock.acquire();

		try {
			jmdns = JmDNS.create();

			Log.i(getClass().getSimpleName(), "Adding Listeners");

			jmdns.addServiceListener(type, listener = new ServiceListener() {

				@Override
				public void serviceResolved(ServiceEvent ev) {

					Log.i(getClass().getSimpleName(), "Resolved " + ev.getInfo().getName());

					Log.i(getClass().getSimpleName(), "Resolved " + ev.getInfo().getInetAddresses().length);
					Log.i(getClass().getSimpleName(), TextUtils.join("", ev.getInfo().getInetAddresses()));

					Log.i(getClass().getSimpleName(), "Resolved " + ev.getInfo().getHostAddresses().length);
					Log.i(getClass().getSimpleName(), TextUtils.join("", ev.getInfo().getHostAddresses()));

					// notifyUser("Service resolved: " + ev.getInfo().getNiceTextString() + " port:"
					// + ev.getInfo().getPort());
					notifyUser("Service resolved: " + ev.getInfo().getQualifiedName() + " port:" + ev.getInfo().getPort());
				}

				@Override
				public void serviceRemoved(ServiceEvent ev) {
					notifyUser("Service removed: " + ev.getName());
				}

				@Override
				public void serviceAdded(ServiceEvent event) {
					// Required to force serviceResolved to be called again (after the first search)
					jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
				}
			});

			Log.i(getClass().getSimpleName(), "Creating service");

			serviceInfo = ServiceInfo.create("_test._tcp.local.", "AndroidTest", 0, "plain test service from android");
			jmdns.registerService(serviceInfo);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private void notifyUser(final String msg) {
		Log.i(getClass().getSimpleName(), msg);
		uiHandler.postDelayed(new Runnable() {
			public void run() {
				delegate.onServiceDiscovered(msg);
			}
		}, 1);

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
