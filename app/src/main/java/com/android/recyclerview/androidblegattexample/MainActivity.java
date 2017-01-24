package com.android.recyclerview.androidblegattexample;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private boolean mScanning;

    private static final int ENABLE_BLUETOOTH = 1;

    Button scanButton;
    ListView BLElistView;

    List<BluetoothDevice> bluetoothDeviceList;
    ListAdapter adapterScanResult;

    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,
                    "BLE not supported in this device", Toast.LENGTH_SHORT).show();
            finish();
        }



        mBluetoothAdapter = checkForBluetoothAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Blutooth adapter not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scanButton = (Button)findViewById(R.id.scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanDevice(true);
            }
        });
        BLElistView = (ListView)findViewById(R.id.blelist);
        bluetoothDeviceList = new ArrayList<>();
        adapterScanResult = new ArrayAdapter<BluetoothDevice>(
                this, android.R.layout.simple_list_item_1, bluetoothDeviceList);
        BLElistView.setAdapter(adapterScanResult);
        BLElistView.setOnItemClickListener(scanResultOnItemClickListener);

    }


    AdapterView.OnItemClickListener scanResultOnItemClickListener =
            new AdapterView.OnItemClickListener(){

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);

                    String msg = device.getAddress() + "\n"
                            + device.getBluetoothClass().toString() + "\n"
                            + getDeviceType(device);

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(device.getName())
                            .setMessage(msg)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .setNeutralButton("CONNECT", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final Intent intent = new Intent(MainActivity.this,
                                            ServiceActivity.class);
                                    intent.putExtra(ServiceActivity.DEVICE_NAME,
                                            device.getName());
                                    intent.putExtra(ServiceActivity.DEVICE_ADDRESS,
                                            device.getAddress());

                                    if (mScanning) {
                                        //mBluetoothLeScanner.stopScan(scanCallback);
                                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                        mScanning = false;
                                        scanButton.setEnabled(true);
                                    }
                                    startActivity(intent);
                                }
                            })
                            .show();

                }
            };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }
    private String getDeviceType(BluetoothDevice d){
        String type = "";

        switch (d.getType()){
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;
            default:
                type = "Default type";
        }

        return type;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        mBluetoothAdapter = checkForBluetoothAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Blutooth adapter not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private BluetoothAdapter checkForBluetoothAdapter(){

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mScanning = false;

        return mBluetoothAdapter;
    }


    private void scanDevice(final boolean enable) {
        if (enable) {
            bluetoothDeviceList.clear();
            BLElistView.invalidateViews();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    BLElistView.invalidateViews();
                    Toast.makeText(MainActivity.this, "Scan timeout", Toast.LENGTH_LONG).show();
                    scanButton.setEnabled(true);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            scanButton.setEnabled(false);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            scanButton.setEnabled(true);
        }
    }



        private BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi,
                                         byte[] scanRecord) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("HELLO!!!!!");
                                addBluetoothDevice(device);

                            }
                        });
                    }

//                    @Override
//                    public void onScanFailed(int errorCode) {
//                        super.onScanFailed(errorCode);
//                        Toast.makeText(MainActivity.this,
//                                "onScanFailed: " + String.valueOf(errorCode),
//                                Toast.LENGTH_LONG).show();
//                    }

                    private void addBluetoothDevice(BluetoothDevice device){
                        if(!bluetoothDeviceList.contains(device)){
                            bluetoothDeviceList.add(device);
                            BLElistView.invalidateViews();
                        }
                    }






    };








}