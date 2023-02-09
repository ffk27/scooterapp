package com.example.wifidirect_test;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.vkpapps.apmanager.APManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ActionListener {
    private final IntentFilter intentFilter = new IntentFilter();
    private WebView webView;
    private TimeServer timeServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        this.webView = this.findViewById(R.id.webview);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl("file:///android_asset/index.html");

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiP2pManager manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        WifiP2pManager.Channel channel = manager.initialize(this, getMainLooper(), null);
        WifiP2pConfig.Builder builder = new WifiP2pConfig.Builder();
        builder.setNetworkName("DIRECT-sa-android");
        builder.setPassphrase("03f3747d-250d-414f-9de8-be4d7b28c6d3");
        builder.setGroupOperatingBand(WifiP2pConfig.GROUP_OWNER_BAND_2GHZ);
        WifiP2pConfig config = builder.build();
        manager.createGroup(channel, builder.build(), this);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("--------", action);
                if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                    manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
                            if (timeServer == null || timeServer.getStatus() == AsyncTask.Status.FINISHED) {
                                timeServer = new TimeServer();
                                timeServer.execute();
                            }
                        }
                    });
                }
            }
        };
        registerReceiver(receiver, intentFilter);
    }

    private class TimeServer extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                ServerSocket serverSocket = new ServerSocket(3123);
                serverSocket.setReuseAddress(true);
                Socket client = serverSocket.accept();
                long unixTime = System.currentTimeMillis() / 1000;
//                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
//                in.read();
                OutputStream outputStream = client.getOutputStream();
                outputStream.write(Long.toString(unixTime).getBytes());
                outputStream.close();
                String address = client.getInetAddress().getHostAddress();
                client.close();
                serverSocket.close();
                return address;
            } catch (Exception e) {
                Log.d("--------------", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            if (o != null) {
                webView.evaluateJavascript("startConnection('"+o+"')", null);
            }
        }
    }

    @Override
    public void onSuccess() {
        if (timeServer == null || timeServer.getStatus() == AsyncTask.Status.FINISHED) {
           timeServer = new TimeServer();
           timeServer.execute();
        }
    }

    @Override
    public void onFailure(int reason) {
        new AlertDialog.Builder(this).setMessage("\nFailed to create group (reason "+reason+")").show();
    }
}