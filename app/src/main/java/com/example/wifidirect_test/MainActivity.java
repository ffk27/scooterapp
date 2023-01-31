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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collection;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, WifiP2pManager.ActionListener {
    private final IntentFilter intentFilter = new IntentFilter();
    private TextView tv1;
    private TimeServer timeServer;

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
        builder.setNetworkName("DIRECT-sa-app1");
        builder.setPassphrase("{!^[lYAX6QYw.6B s=8u");
        builder.setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_2GHZ);
        manager.createGroup(channel, builder.build(), this);
        //timeServer = new TimeServer(3123);
        long unixTime = System.currentTimeMillis() / 1000L;
        tv1.append(""+unixTime);

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
                    tv1.append("peers changed");
                    manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                            tv1.append("\npeers available" + wifiP2pDeviceList.describeContents());
                            Object[] devices = wifiP2pDeviceList.getDeviceList().toArray();
                            if (devices.length > 0) tv1.append(devices[0].toString());
                        }
                    });
                } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                    tv1.append("connection changed");
                } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                    tv1.append("device changed");
                }
            }
        };
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onSuccess() { // group created successfully
        //this.timeServer.run();
    }

    private class TimeServer extends Thread {
        private int port;
        private ServerSocket serverSocket;

        public TimeServer(int port) {
            this.port = port;
            try {
                this.serverSocket = new ServerSocket(this.port);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Socket socket = this.serverSocket.accept();
                    OutputStream outputStream = socket.getOutputStream();
                    long unixTime = System.currentTimeMillis() / 1000;
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                    buffer.putLong(unixTime);
                    outputStream.write(buffer.array());
                    outputStream.close();
                    socket.close();
                }
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        }
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
