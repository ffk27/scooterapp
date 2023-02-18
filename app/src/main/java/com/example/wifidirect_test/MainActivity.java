package com.example.wifidirect_test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity { //  implements WifiP2pManager.ActionListener
    private final IntentFilter intentFilter = new IntentFilter();
    private final String BLUETOOTH_NAME = "xiao-esp32-c3";
    private WebView webView;
    //private TimeServer timeServer;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    private boolean scanning;
    private Handler handler = new Handler();
    private BluetoothDevice bluetoothDevice;
    private Context context;

    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }


    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @SuppressLint("MissingPermission")
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    String name = result.getDevice().getName();
                    if (name != null && name.equals(BLUETOOTH_NAME)) {
                        bluetoothDevice = result.getDevice();
                        bluetoothLeScanner.stopScan(leScanCallback);
                        scanning = false;
                        //bluetoothDevice.createBond();
                        bluetoothDevice.connectGatt(context, true, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                    }
                }
            };

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            boolean isSuccess = status == BluetoothGatt.GATT_SUCCESS;
            boolean isConnected = newState == BluetoothProfile.STATE_CONNECTED;
            if (isSuccess && isConnected) {
                gatt.discoverServices();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        webView.evaluateJavascript("connected();", null);
                    }
                });
            } else {

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        webView.evaluateJavascript("disconnected();", null);
                    }
                });
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("508c16b9-7ccc-46a8-907d-802b91e2b1f8"));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("7a6082ec-79ea-45da-8129-0709de8f5d50"));
                gatt.setCharacteristicNotification(characteristic, true);
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();
            ByteBuffer bbt = ByteBuffer.wrap(new byte[] {0, 0, 0, 0, value[0],value[1],value[2],value[3]});
            long tms = bbt.getLong();
            ByteBuffer bbr = ByteBuffer.wrap(new byte[] {0, 0, 0, 0, value[4],value[5],value[6],value[7]});
            long rpm = bbr.getLong();
            String json = "{\"time\": " + String.valueOf(tms) + ", \"rpm\": " + String.valueOf((rpm)) + "}";
            Log.d("------------", json);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    webView.evaluateJavascript("onReceive('"+json+"')", null);
                }
            });
        }


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context = this;

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setMessage("missing bluetooth permissions").show();
            return;
        }
        scanLeDevice();
        /*
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

         */
    }

//    private class TimeServer extends AsyncTask {
//        @Override
//        protected Object doInBackground(Object[] objects) {
//            try {
//                ServerSocket serverSocket = new ServerSocket(3123);
//                serverSocket.setReuseAddress(true);
//                Socket client = serverSocket.accept();
//                long unixTime = System.currentTimeMillis() / 1000;
////                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
////                in.read();
//                OutputStream outputStream = client.getOutputStream();
//                outputStream.write(Long.toString(unixTime).getBytes());
//                outputStream.close();
//                String address = client.getInetAddress().getHostAddress();
//                client.close();
//                serverSocket.close();
//                return address;
//            } catch (Exception e) {
//                Log.d("--------------", e.getMessage());
//                return null;
//            }
//        }
//
//        @Override
//        protected void onPostExecute(Object o) {
//            if (o != null) {
//                webView.evaluateJavascript("startConnection('"+o+"')", null);
//            }
//        }
//    }
//
//    @Override
//    public void onSuccess() {
//        if (timeServer == null || timeServer.getStatus() == AsyncTask.Status.FINISHED) {
//           timeServer = new TimeServer();
//           timeServer.execute();
//        }
//    }
//
//    @Override
//    public void onFailure(int reason) {
//        new AlertDialog.Builder(this).setMessage("\nFailed to create group (reason "+reason+")").show();
//    }
}