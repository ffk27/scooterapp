package com.example.wifidirect_test;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.vkpapps.apmanager.APManager;

import java.util.Collection;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, WifiP2pManager.ActionListener {
    private final IntentFilter intentFilter = new IntentFilter();
    private TextView tv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv1 = this.findViewById(R.id.tv1);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiP2pManager manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        WifiP2pManager.Channel channel = manager.initialize(this, getMainLooper(), this);
        WifiP2pConfig.Builder builder = new WifiP2pConfig.Builder();
        builder.setNetworkName("DIRECT-xy-app1");
        builder.setPassphrase("app1-pw-123");
        builder.setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_2GHZ);
        WifiP2pConfig config = builder.build();
        manager.createGroup(channel, builder.build(), this);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                tv1.append("\n" + action);
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        tv1.append("\nWifi P2P is enabled");
                    } else {
                        tv1.append("\nWifi P2P is disabled");
                    }
                } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    tv1.append("\nCall WifiP2pManager.requestPeers() to get a list of current peers");
                    manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                            Object[] devices = wifiP2pDeviceList.getDeviceList().toArray();
                            if (devices.length > 0) tv1.append(devices[0].toString());
                        }
                    });
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    tv1.append("\nRespond to new connection or disconnections");
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    tv1.append("\nRespond to this device's wifi state changing");
                }
            }
        };
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onSuccess() {
        tv1.append("\nGroup created successfully");
    }

    @Override
    public void onFailure(int reason) {
        tv1.append("\nFailed to create group (reason "+reason+")");
    }

    @Override
    public void onChannelDisconnected() {
        tv1.append("\nonChannelDisconnected");
    }
}
