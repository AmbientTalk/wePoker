package edu.vub.at.nfcpoker.ui;


import edu.vub.at.commlib.CommLib;
import edu.vub.at.nfcpoker.Constants;
import edu.vub.at.nfcpoker.R;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class QRJoinerActivity extends Activity {
	
	IntentFilter intentFilter;

	protected boolean currentlyJoining = false;
	
	protected static String[] status_strings = {
		"Disabling wireless...",
		"Wireless disabled",
		"Enabling wireless...",
		"Wireless enabled",
		"Wireless state unknown"
	};
	
	private BroadcastReceiver wifiChangeReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d("wePoker - QRJoiner", "Received intent " + intent);
			String newStatus = null;
			WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);

			if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
				NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				WifiInfo wifiInfo = wm.getConnectionInfo();
				if (netInfo == null || wifiInfo == null)
					return;
				if (currentlyJoining && netInfo.getState() == State.CONNECTED && wifi_name.equals(wifiInfo.getSSID())) {
					publishProgress("Joining game!");
					startClientActivity();
				}
			}
			if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int new_wifi_status = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
				newStatus = status_strings[new_wifi_status];
				
				if (new_wifi_status == WifiManager.WIFI_STATE_DISABLED) {
					publishProgress("Enabling Wireless...");
					wm.setWifiEnabled(true);
				}
				if (new_wifi_status == WifiManager.WIFI_STATE_ENABLED) {
					if (currentlyJoining) {
						publishProgress("Connecting to network...");
						if (!wifi_pass.equals("")) {
							// If we have the password
							WifiConfiguration config = new WifiConfiguration();
							config.SSID = '"' + wifi_name + '"';
							config.preSharedKey = '"' + wifi_pass + '"';
							config.hiddenSSID = true;
							config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
							config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
							config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
							config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
							config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
							config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
							
							int id = wm.addNetwork(config);
							Log.d("wePoker - QRJoiner", "Connecting to network " + wifi_name + ": " + id);
							boolean ret = wm.enableNetwork(id, true);
							Log.d("wePoker - QRJoiner", "enableNetwork returned " + ret);
						} else {
							boolean joined = false;
							for (WifiConfiguration config : wm.getConfiguredNetworks()) {
								if (config.SSID.equals(wifi_name)) {
									Log.d("wePoker - QRJoiner", "Found preconfigured network " + wifi_name);
									wm.enableNetwork(config.networkId, true);
									joined = true;
									break;
								}
							}
							if (!joined) {
								Log.d("wePoker - QRJoiner", "Asking user to connect manually");
								new AlertDialog.Builder(QRJoinerActivity.this)
								    .setMessage("Please connect to the network '" + wifi_name + "' manually and scan the barcode again.")
								    .setCancelable(false)
								    .show();
							}
						}
					}
				}

			}
			if (newStatus != null) {
				TextView progressTxt = (TextView) findViewById(R.id.Discovering);
				progressTxt.setText(newStatus);
			}
		}
	};

	protected String wifi_name;
	protected String wifi_pass;
	protected String wifi_server;
	protected boolean wifi_isDedicated;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_splash);
        
        Uri launcher = getIntent().getData();
        wifi_name = launcher.getQueryParameter(Constants.INTENT_WIFI_NAME);
        wifi_pass = launcher.getQueryParameter(Constants.INTENT_WIFI_PASSWORD);
        wifi_server = launcher.getQueryParameter(Constants.INTENT_SERVER_IP);
        wifi_isDedicated = launcher.getQueryParameter(Constants.INTENT_IS_DEDICATED).equals("true");
        
        intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        String joinText = getResources().getString(R.string.qr_code_join_confirmation, wifi_name);
        TextView progressTxt = (TextView) findViewById(R.id.Discovering);
        progressTxt.setText(joinText);
		
		joinServer();
    }
    
    private void joinServer() {
		currentlyJoining = true;
		registerReceiver(wifiChangeReceiver, intentFilter);
    }
    

    protected void publishProgress(String string) {
    	TextView progressTxt = (TextView) findViewById(R.id.Discovering);
    	progressTxt.setText(string);
    	Log.d("WePoker - QRJoiner", "Progress update: " + string);
	}

	protected void startClientActivity() {
		Intent i = new Intent(this, ClientActivity.class);
		i.putExtra("ip", wifi_server);
		i.putExtra("port", CommLib.SERVER_PORT);
		i.putExtra("isDedicated", wifi_isDedicated);
		startActivity(i);
		finish();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(wifiChangeReceiver );
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(wifiChangeReceiver, intentFilter);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_qrjoiner, menu);
        return true;
    }
	
	public String currentWifiNetwork() {
		WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wi = wm.getConnectionInfo();
		String ssid = wi.getSSID();
		return ssid;
	}
}
