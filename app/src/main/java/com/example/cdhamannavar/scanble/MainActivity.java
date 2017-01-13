package com.example.cdhamannavar.scanble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.kafka.clients.consumer.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.os.Vibrator;

public class MainActivity extends Activity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    // String Variables to hold device name and device address
    String devname;
    String devaddress;
    View v1;
    String rssi1;
    String screc;
    int reclen;
    private BluetoothDevice d1; //bluetooth device
    private BluetoothGatt mGatt;
    private BluetoothGattCallback mblecallback;
    String message = "S";
    SparseArray<byte[]> md;
    String md1;

    public static String HM_RX_TX = "0000a002-0000-1000-8000-00805f9b34fb";
    public final static UUID UUID_HM_RX_TX = UUID.fromString(HM_RX_TX);

    public static String HM_DATA1 = "0000a001-0000-1000-8000-00805f9b34fb";
    public final static UUID HM_UUID1 = UUID.fromString(HM_DATA1);

    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;
    private BluetoothGattCharacteristic hmuuid1;


    //This the fist method that is called
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Enable Bluetooth?
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled?
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        // Supports LE ?
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Set the Scan settings and start the BLE scanning process.....
    public void startscand(View view) {
        Log.i("TAG1", "Start Scanning");
        final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        // ParcelUuid Service_UUID = ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb");
        // ParcelUuid Service_UUID = ParcelUuid.fromString("00001800-0000-1000-8000-00805f9b34fb");
        ScanFilter beaconFilter = new ScanFilter.Builder()
                .build();
        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(beaconFilter);
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanner.startScan(filters, settings, mcallback);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };

    public void stopscand(View view) {
        Log.i("TAG1", "Stop Scanning");
        final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

        scanner.stopScan(mcallback);
        mGatt.disconnect();

    }

    // Callback for scanning device for devices
    ScanCallback mcallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            d1 = result.getDevice();
            ScanRecord n1;
            int n;
            String record1;
            String devicename = d1.getName();
            String ddata = d1.getAddress();
            ParcelUuid[] THERM_SERVICE = d1.getUuids();
            Log.i("T", devicename);
            Log.i("T", ddata);
            n=result.getRssi();
            n1=result.getScanRecord();
            Log.i("T-----:",result.toString());
            int n2 = n1.toString().length();
            Log.i("ScanRec-----:",n1.toString()+"CCC:"+n2);
            record1 =Integer.toString(n);
            rssi1=record1;
            screc=n1.toString();
            Log.i("T",Integer.toString(n));
            Log.i("ScanRecord",record1);
            Log.i("Length",Integer.toString(record1.length()));
            devname = d1.getName();
            devaddress = d1.getAddress();
            reclen = screc.length();
            md1 = n1.getManufacturerSpecificData().toString();
            updatetext(v1);

        }

    };

    public void updatetext(View view)
    {
        TextView t=new TextView(this);
        t=(TextView)findViewById(R.id.device_name);
        //String t1 = Integer.toString(rs);
        //t.setText(t1);
        t.setText(devname);
        t=(TextView)findViewById(R.id.device_address);
        t.setText(devaddress);
        t=(TextView)findViewById(R.id.rssi);
        t.setText(rssi1);
        t=(TextView)findViewById(R.id.scanrec);
        t.setText(screc+reclen);
    }

    // Method for connecting to the found bluetooth device.
    public void deviceconnect(View view){
        connectToDevice(d1);
    }

    public void connectToDevice(BluetoothDevice device) {
            Log.i("Connecting Device......", "Connecting Device.....!!!");
            mGatt = device.connectGatt(this, false, gattCallback);
            Log.i("Calling GattCallback..", "Calling GattCallback");
        Log.i("TAG1", "Stop Scanning");
        final BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

        scanner.stopScan(mcallback);
        mGatt.disconnect();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            //int servicsize = services.size();
            //BluetoothGattCharacteristic ch1;
            //byte[] data_to_write;
            //BluetoothGattService b1;
            //Log.i("Tag", String.valueOf(servicsize));
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            final byte b1[]=characteristic.getValue();
            for (int i=0;i<b1.length;i++){
                Log.i(":",String.valueOf(b1[i]));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicWrite??", characteristic.toString());
            final byte b1[]=characteristic.getValue();
            for (int i=0;i<b1.length;i++){
                Log.i(":",String.valueOf(b1[i]));
            }

        }
    };


    public void sendata1(View view) {
        //TextView t1=new TextView(this);
        TextView t1=new TextView(this);
        t1=(TextView)findViewById(R.id.rx);
        message=t1.getText().toString();
        setupSerial();
        Log.i("TAG", "Sending:........... " + message);
        final byte[] tx = message.getBytes();
        for (int i=0;i<tx.length;i++){
            Log.i(":",String.valueOf(tx[i]));
        }
        characteristicTX.setValue(tx);
        writeCharacteristic(characteristicTX);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
            }
        }, 100);
        mGatt.readCharacteristic(characteristicRX);
    }

    private void sendSerial() {
        Log.i("TAG", "Sending:........... " + message);
        final byte[] tx = message.getBytes();
        for (int i=0;i<tx.length;i++){
            Log.i(":",String.valueOf(tx[i]));
        }
        characteristicTX.setValue(tx);
        writeCharacteristic(characteristicTX);
        Log.i("TX-DATA", characteristicTX.toString());
        mGatt.readCharacteristic(hmuuid1);
        final byte[] rx = characteristicRX.getValue();
        String test1 = rx.toString();
        Log.i("T", test1);

    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mGatt == null) {
            Log.w("TAG", "BluetoothAdapter not initialized");
            return;
        }
        mGatt.writeCharacteristic(characteristic);
    }

    private void setupSerial() {
        String uuid;
        for (BluetoothGattService gattService : getSupportedGattServices()) {
            uuid = gattService.getUuid().toString();
            Log.i("UD",uuid);
            if(uuid.equals("0000a000-0000-1000-8000-00805f9b34fb")) {
                characteristicTX = gattService
                        .getCharacteristic(UUID_HM_RX_TX);
                Log.i("CR",characteristicTX.getUuid().toString());
                characteristicRX = gattService
                        .getCharacteristic(HM_UUID1);
                break;
            }
        }
    }

    private void setupSerial1() {
        String uuid;
        for (BluetoothGattService gattService : getSupportedGattServices()) {
            uuid = gattService.getUuid().toString();
            Log.i("UD",uuid);
            if(uuid.equals("00001800-0000-1000-8000-00805f9b34fb")) {
                // get characteristic when UUID matches RX/TX UUID
                hmuuid1 = gattService.getCharacteristic(HM_UUID1);
                Log.i("UUID:",hmuuid1.toString());
                break;
            }
        }
    }



    public List<BluetoothGattService> getSupportedGattServices() {
        if (mGatt == null) return null;
        return mGatt.getServices();

    }


}
