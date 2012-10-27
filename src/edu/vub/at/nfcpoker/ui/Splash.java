package edu.vub.at.nfcpoker.ui;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import edu.vub.at.commlib.CommLib;
import edu.vub.at.commlib.CommLibConnectionInfo;
import edu.vub.at.nfcpoker.ConcretePokerServer;
import edu.vub.at.nfcpoker.R;
import edu.vub.at.nfcpoker.settings.Settings;
import com.flurry.android.FlurryAgent;

public class Splash extends Activity {
	private static final boolean LODE = false;

	// Shared globals
	public static final String WEPOKER_WEBSITE = "http://wepoker.info";
	public static String possibleEmail;

	// Connectivity state
	private BroadcastReceiver wifiWatcher;
	
	// Discovery
	private volatile DiscoveryAsyncTask discoveryTask;
	private volatile Timer client_startClientServerTimer;
	private volatile Dialog client_startClientServerAsk;
	
	// UI
	public static Handler messageHandler;
	private boolean isTablet = false;
	private int startClientServerTimerTimeout = 10000;
	private int startClientServerTimerTimeout2 = 30000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		// Settings
		Settings.loadSettings(this);
		
		View tablet_layout = findViewById(R.id.tablet_layout);
		if (tablet_layout != null)
			isTablet = true;
		
		Button server = (Button) findViewById(R.id.server);
		if (server != null)
			server.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startServer();
				}
			});

		// UI
		messageHandler = new IncomingHandler(this);
		
		
		if (LODE) {
			Intent i = new Intent(this, ClientActivity.class);
			i.putExtra("isDedicated", false);
			startActivity(i);
			return;
		}
				
		final DiscoveryAsyncTask.DiscoveryCompletionListener dcl = new DiscoveryAsyncTask.DiscoveryCompletionListener() {
			@Override
			public void onDiscovered(CommLibConnectionInfo result) {
				if (client_startClientServerTimer != null) {
					client_startClientServerTimer.cancel();
					client_startClientServerTimer = null;
					if (client_startClientServerAsk != null) {
						client_startClientServerAsk.dismiss();
						client_startClientServerAsk = null;
					}
				}
				startClient(result);
			}
		};
		
		if (!isTablet) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			
			//TODO: Only try Wifi-direct if available and no wifi is found.
			discoveryTask = new DiscoveryAsyncTask(this, dcl);
			discoveryTask.execute();
			
			// If there is no server responding after 10 seconds, ask the user to start one without a dedicated table
			scheduleAskStartClientServer(startClientServerTimerTimeout);
		} else {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			final Button disc = (Button) findViewById(R.id.discover_button);
			disc.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					startDiscovery(dcl);
					disc.setEnabled(false);
				}
			});			
		}
	}
	
	public boolean isWifiDirectSupported() {
		return getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
	}
	
	public boolean isNFCSupported() {
		return false;
	}

	@Override
	public void onResume() {
		super.onResume();
//		registerWifiWatcher();
//		mWifiDirectManager.registerReceiver();
	}
	
	@Override
	public void onPause() {
		super.onPause();
//		unregisterReceiver(wifiWatcher); wifiWatcher = null;
//		mWifiDirectManager.unregisterReceiver();
		// TODO pause discovery 
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		FlurryAgent.onStartSession(this, "6N6DX8KMBFZSS7XYB8S4");
		FlurryAgent.setUseHttps(true);
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(this).getAccounts();
		for (Account account : accounts) {
		    if (emailPattern.matcher(account.name).matches()) {
		        possibleEmail = account.name;
		    }
		}
		
		// if there is no email associated
		if(possibleEmail == null || possibleEmail.isEmpty()){
			possibleEmail = "no.name@fake.com";
		}
		
		Map<String, String> userInfo = new HashMap<String, String>();
		userInfo.put("user", md5(possibleEmail)); // Capture user info as a hash of the email
        FlurryAgent.logEvent("Initialising_Game", userInfo, true);
		
	}
	
	public String md5(String s) {
	    try {
	        // Create MD5 Hash
	        MessageDigest digest = MessageDigest.getInstance("MD5");
	        digest.update(s.getBytes());
	        byte messageDigest[] = digest.digest();
	        
	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        return hexString.toString();
	        
	    } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    }
	    return "";
	}
	// see http://androidsnippets.com/create-a-md5-hash-and-dump-as-a-hex-string
	
	@Override
	public void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
		Settings.saveSettings(this);
	}
	
	// Connectivity
	private class ConnectionChangeReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent ) {
			Log.d("wePoker - Splash", "My IP Address changed!");
		}
	}
	
	private void registerWifiWatcher() {
		if (wifiWatcher != null) return;
		wifiWatcher = new ConnectionChangeReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		registerReceiver(wifiWatcher, intentFilter);
	}
	
	// UI
	static class IncomingHandler extends Handler {
		private final WeakReference<Context> mCtx;

		IncomingHandler(Context ctx) {
			mCtx = new WeakReference<Context>(ctx);
		}

		@Override
		public void handleMessage(Message msg) {
			String txt;
			Context ctx = mCtx.get();
			if (ctx != null) {
				switch(msg.what) {
				case UIMessage.MESSAGE_TOAST:
					txt = msg.getData().getString("message");
					if (txt == null) return;
					Toast.makeText(ctx, txt, Toast.LENGTH_SHORT).show();
					break;
				case UIMessage.MESSAGE_DISCOVERY_FAILED:
					// TODO
					break;
				}
			}
		}
	}
	
	public void startClient(CommLibConnectionInfo clci) {
		Intent i = new Intent(this, ClientActivity.class);
		i.putExtra("ip", clci.getAddress());
		i.putExtra("port", Integer.parseInt(clci.getPort()));
		i.putExtra("isDedicated", clci.isDedicated());
		startActivity(i);
		finish();
	}
	    
	
	// Discovery
	private void scheduleAskStartClientServer(int timeout) {
		client_startClientServerTimer = new Timer();
		client_startClientServerTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						askStartClientServer();
					}
				});
			}
		}, timeout);
	}
	
	public void startDiscovery(DiscoveryAsyncTask.DiscoveryCompletionListener dcl) {
		if (discoveryTask != null)
			discoveryTask.cancel(true);
		
		discoveryTask = new DiscoveryAsyncTask(Splash.this, dcl);
		discoveryTask.execute();
	}
	
	public void restartDiscovery() {
		if (discoveryTask == null)
			return;
		
		startDiscovery(discoveryTask.dcl);
	}
	
	private void askStartClientServer() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					if (client_startClientServerTimer != null) {
						client_startClientServerTimer.cancel();
						client_startClientServerTimer = null;
						new Thread() {
							@Override
							public void run() {
					    		String ipAddress = CommLib.getIpAddress(Splash.this);
					    		String broadcastAddress = CommLib.getBroadcastAddress(Splash.this);
						    	ConcretePokerServer cps = new ConcretePokerServer(
						    			new DummServerView(), false, ipAddress, broadcastAddress);
						    	cps.start();
							}
						}.start();
					}
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					scheduleAskStartClientServer(startClientServerTimerTimeout2);
					break;
				}
				client_startClientServerAsk = null;
			}
		};
		
		DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				scheduleAskStartClientServer(startClientServerTimerTimeout2);
				client_startClientServerAsk = null;
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		client_startClientServerAsk =
				builder.setMessage("No Ambient-Poker game discovered.\nDo you wish to start one?")
				.setOnCancelListener(onCancelListener)
				.setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener).create();
		client_startClientServerAsk.show();
	}

	protected void startServer() {
		if (discoveryTask != null) {
			discoveryTask.cancel(true);
			discoveryTask = null;
		}
		
		WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		wm.setWifiEnabled(true);
		WifiInfo connInfo = wm.getConnectionInfo();
		boolean enabled = wm.isWifiEnabled();
		boolean connected = connInfo != null && connInfo.getNetworkId() != -1;
		
		Intent i = new Intent(this, ServerActivity.class);
		i.putExtra("wifiDirect", isWifiDirectSupported() && !(enabled && connected));
		startActivity(i);
		finish();
	}

}

