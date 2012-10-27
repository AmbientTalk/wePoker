package edu.vub.at.nfcpoker.ui;

import java.net.InetAddress;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import edu.vub.at.nfcpoker.ui.DiscoveryAsyncTask.DiscoveryCompletionListener;
import edu.vub.at.nfcpoker.ui.ServerActivity.ServerStarter;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class WifiDirectManager extends BroadcastReceiver implements GroupInfoListener, PeerListListener {

	public static class Creator implements Runnable {
		
		private Activity act;
		private ServerStarter serverStarter;

		Creator(Activity act, ServerStarter startServer) {
			this.act = act;
			this.serverStarter = startServer;
		} 

		@Override
		public void run() {
			WifiDirectManager wdm = WifiDirectManager.create(act, act.getMainLooper(), true);
    		wdm.createGroup(serverStarter);			
		}

	}

	public class LoggingActionListener implements ActionListener {
		
		String prefix;
		
		public LoggingActionListener(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public void onFailure(int reason) {
			Log.e("wePoker - Wifi-Direct", prefix + " FAILED: " + reason);
		}

		@Override
		public void onSuccess() {
			Log.d("wePoker - Wifi-Direct", prefix + " SUCCESS!");
		}

	}

	private static WifiDirectManager instance;
	private boolean isRunning;
	private WifiP2pManager manager;
	private Channel channel;
	private Activity act;
	protected DiscoveryCompletionListener dcl;
	private WDMType type;
	private IntentFilter mWifiDirectIntentFilter;
	public WifiP2pGroup mCurrentGroup;
	private boolean alreadyConnecting = false;
	private ServerStarter serverStarter;
	private InetAddress myAddress;
	
	enum WDMType { CLIENT, SERVER };

	private WifiDirectManager(Activity act, Looper l, WDMType type) {
		this.isRunning = true;
		this.act = act;
		this.type = type;
		this.manager = (WifiP2pManager) act.getSystemService(Activity.WIFI_P2P_SERVICE);
		if (manager == null) {
			this.isRunning = false;
		} else {
			this.channel = manager.initialize(act, l, null);
			mWifiDirectIntentFilter = new IntentFilter();
		    mWifiDirectIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		    mWifiDirectIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		    mWifiDirectIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		    mWifiDirectIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		}
	}

	public static WifiDirectManager create(Activity act, Looper mainLooper, boolean isServer) {
		if (instance != null) {
			instance.stop();
			instance = null;
		}
		
		instance = new WifiDirectManager(act, mainLooper, (isServer ? WDMType.SERVER : WDMType.CLIENT));
		return instance;
	}

	private void stop() {
		isRunning = false;
		unregisterReceiver();
	}

	@Override
	public void onGroupInfoAvailable(WifiP2pGroup group) {
		if (group == null)
			return;
		
		Log.d("wePoker - Wifi-Direct", "Got group info!" + group);
		if (isRunning) {

			Log.d("wePoker - Wifi-Direct", "Device specifics: " + myAddress);
			final String groupName = group.getNetworkName();
			final String password = group.getPassphrase();
			mCurrentGroup = group;
			//TODO: advertise password for legacy clients.
			Log.d("wePoker - Wifi-Direct", "Created group " + groupName + " with password '" + password + "'");
			act.runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Toast.makeText(act, "Created group " + groupName + " with password '" + password + "'", Toast.LENGTH_LONG).show();
				}
			});
			
			String ipAddress = myAddress.getHostAddress();
			serverStarter.setWifiDirect(groupName, password, ipAddress);
			serverStarter.start(ipAddress, null);
			unregisterReceiver();
		}
	}

	public void discover(DiscoveryCompletionListener dcl) {
		Log.d("wePoker - Wifi-Direct", "Starting discovery...");
		this.dcl = dcl;
	}

	public void createGroup(final ServerStarter startServer) {
		serverStarter = startServer;
		registerReceiver();
	}

	@Override
	public void onPeersAvailable(WifiP2pDeviceList peers) {
		final int numDevices = peers.getDeviceList().size();
		Log.d("wePoker - Wifi-Direct", "Got peer list: " + numDevices);
		if (numDevices != 1 || alreadyConnecting)
			return;					
		
		alreadyConnecting  = true;
		WifiP2pDevice first = peers.getDeviceList().iterator().next();
		Log.d("wePoker - Wifi-Direct", "Single device found: " + first);
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = first.deviceAddress;
		manager.connect(channel, config, new ActionListener() {
			
			@Override
			public void onSuccess() {
				Log.d("wePoker - Wifi-Direct", "Connected to device!");
				act.runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						stop();
						new DiscoveryAsyncTask(act, dcl).execute();
					}
				});
			}
			
			@Override
			public void onFailure(int reason) {
				Log.d("wePoker - Wifi-Direct", "Connection to device failed :'(");
			}
		});
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!isRunning)
			return;
		String action = intent.getAction();
		Log.d("wePoker - Wifi-Direct", "Received intent: " + action);
		if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
			int enabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0);
			if (enabled == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
				if (serverStarter != null) {
//					Log.d("wePoker - Wifi-Direct", "Creating group");
//					manager.createGroup(channel, new LoggingActionListener("Creating group"));
				}
			}

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
//        	manager.requestPeers(this.channel, this);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        	WifiP2pInfo wp2pi = (WifiP2pInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
        	if (type == WDMType.CLIENT)
        		return;
        	
        	if (wp2pi.groupFormed && wp2pi.isGroupOwner) {
        		myAddress = wp2pi.groupOwnerAddress;
        		manager.requestGroupInfo(channel, this);
        		return;
        	}
        	
        	if (!wp2pi.groupFormed) {
        		Log.d("wePoker - Wifi-Direct", "Creating group");
        		manager.createGroup(channel, new LoggingActionListener("Group creation"));
        		return;
        	}
       	
        	manager.removeGroup(channel, new ActionListener() {
				
				@Override
				public void onSuccess() {
					Log.d("wePoker - Wifi-Direct", "Group removal succeeded, creating new group...");
					manager.createGroup(channel, null);
				}
				
				@Override
				public void onFailure(int reason) {
					Log.d("wePoker - Wifi-Direct", "Could not remove group:" + reason);
				}
			});
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
        	// Respond to this device's wifi state changing
        }
	}
	
	public void registerReceiver() {
		act.registerReceiver(this, mWifiDirectIntentFilter);
	}
	
	public void unregisterReceiver() {
		act.unregisterReceiver(this);
	}

}
