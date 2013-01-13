package com.meyertee.power_switch;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

/**
 * http://techtej.blogspot.de/2011/02/android-passing-data-between-main.html
 * 
 * @author Thomas Meyer (thomas@meyertee.com)
 * 
 */
public class StatusRequestTask extends AsyncTask<String, Integer, Boolean> {

	public static final String STATUS_HOSTNAME = "raspberrypi.local";
	public static final int STATUS_PORT = 1337;

	// private static final String STATUS_URL = "http://raspberrypi.local:80/powerswitch/status";
	public static final String STATUS_URL = "http://meyertee-mba.local:1337/powerswitch/status";
	private static final int REQUEST_TIMEOUT_MILLISEC = 5000;

	private Delegate delegate;

	public interface Delegate {
		void onStatusRequestComplete(Boolean remoteStatus);
	}

	public StatusRequestTask(Delegate delegate) {
		this.delegate = delegate;
	}

	@Override
	protected Boolean doInBackground(String... params) {

		try {
			HttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, REQUEST_TIMEOUT_MILLISEC);
			HttpConnectionParams.setSoTimeout(httpParams, REQUEST_TIMEOUT_MILLISEC);

			HttpParams p = new BasicHttpParams();
			p.setParameter("s", "1");

			// Instantiate an HttpClient
			HttpClient httpClient = new DefaultHttpClient(p);

			String url;
			if (params.length > 0) {
				url = "http://" + params[0] + ":" + STATUS_PORT;
			} else {
				url = "http://" + STATUS_HOSTNAME + ":" + STATUS_PORT;
			}

			HttpGet method = new HttpGet(url);
			method.addHeader("host", "raspberrypi.local:1337");
			method.addHeader("accept", "application/json;q=0.9,*/*;q=0.8");

			// Instantiate a GET HTTP method
			try {
				// List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				// nameValuePairs.add(new BasicNameValuePair("user", "1"));
				// method.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				ResponseHandler<String> responseHandler = new BasicResponseHandler();
				String responseBody = httpClient.execute(method, responseHandler);

				Log.i(getClass().getSimpleName(), "Got response");
				Log.i(getClass().getSimpleName(), responseBody);

				// parses response
				JSONObject jsonResponse = new JSONObject(responseBody);
				boolean remoteStatus = jsonResponse.getBoolean("on");

				return remoteStatus;
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Throwable t) {
			Log.i(getClass().getSimpleName(), "Request failed: " + t.toString());
		}
		return null;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		this.delegate.onStatusRequestComplete(result);
	}

}
