package com.example.android.bluetoothlegatt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MultiDeviceControlActivity extends ListActivity {
    private final static String TAG = MultiDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_LIST = "DEVICE_LIST";
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    //先定义
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    //private static final int CREATE_FILE = 1;

    private MultiControlGattListAdapter mMultiDeviceListAdapter;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected;
    private ArrayList<BluetoothLeData> mDeviceList;
    private ConcurrentHashMap<String, Boolean> mDeviceConnectionState = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> mDeviceDataValue = new ConcurrentHashMap<>();
    //private HashMap<String, ExpandableListView> mDeviceGattServicesListView = new HashMap<>();
    //private HashMap<String, ArrayList<ArrayList<BluetoothGattCharacteristic>>> mDeviceGattCharacteristics =
    //        new HashMap<>();
    private ConcurrentHashMap<String, BluetoothGattCharacteristic> mDeviceNotifyCharacteristic = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, FileOutputStream> mDeviceOutputFileDict = new ConcurrentHashMap<>();
    //private ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

//   private Uri mInitialUri;
//   private int mTakeFlags;

    //private HashMap<String, String> mBleAddrGattDict = new HashMap<>();
    //private HashMap<String, ParcelFileDescriptor> mBleFileDesDict = new HashMap<>();
    //private HashMap<String, FileOutputStream> mBleFileOutStreamDict = new HashMap<>();


//    // for open and saving data
//    private void initialUri() {
//        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_TITLE, R.string.uri_initial);
//
//        startActivityForResult(intent, CREATE_FILE);
//    }
    //private static final int CREATE_FILE = 1;
    // for open and saving data
    private void createFile(String address) {
        if(!mDeviceOutputFileDict.containsKey(address) || mDeviceOutputFileDict.get(address) == null) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, address.replace(':', '_') + ".txt");

//        // Optionally, specify a URI for the directory that should be opened in
//        // the system file picker when your app creates the document.
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

            startActivityForResult(intent, mMultiDeviceListAdapter.getItemId(address)); // request code as item id
        } else {
            //pass
        }
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode <= mMultiDeviceListAdapter.getCount()
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.

            if (resultData != null) {
                Uri initialUri = resultData.getData();
                final int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data.
                getContentResolver().takePersistableUriPermission(initialUri, takeFlags);
                // Perform operations on the document using its URI.
                try {
                    FileOutputStream fo = new FileOutputStream(getContentResolver().openFileDescriptor(initialUri, "w").getFileDescriptor());
                    mDeviceOutputFileDict.put(mMultiDeviceListAdapter.getDevice(requestCode).getDeviceAddress(), fo);
                    Toast.makeText(this, R.string.file_created, Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                Toast.makeText(this, R.string.file_opened, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*当设备点击/连接时 确定保存文件 自动激活TX特征*/
    //onListDeviceClickedOrConnectedOr...
    private void openOutputStream(String address){
//        if(!mDeviceOutputFileDict.containsKey(address)) {
//            FileOutputStream fos;
//            try {
//                fos = openFileOutput(address.trim().replace(':', '.').concat(".txt"), MODE_APPEND);//处理文件名
//                mDeviceOutputFileDict.put(address, fos);
//                Toast.makeText(this, R.string.file_created, Toast.LENGTH_SHORT).show();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//            Toast.makeText(this, R.string.file_existed, Toast.LENGTH_SHORT).show();
//        }

        //runOnUiThread(()-> {
            //激活TX特征Notify
            // DONE: - determine NOTIFY or READ!! : NOTIFY
            runOnUiThread(()->{if (!mDeviceNotifyCharacteristic.containsKey(address)) {
               for (BluetoothGattService service : mBluetoothLeService.getSupportedGattServices(address)) {
                   for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                       if (characteristic.getUuid().equals(UUID.fromString(SampleGattAttributes.BLE_UART_TX))) {//TO IGNORE CASE
                           mDeviceNotifyCharacteristic.put(address, characteristic);
                       }
                   }
               }
//            } else {
//                mBluetoothLeService.setCharacteristicNotification(
//                        address,
//                        mDeviceNotifyCharacteristic.get(address),
//                        true);
//            Log.d(TAG, "GATT PROPERTY_NOTIFY: " + address);
        }});

        createFile(address);
    }

    //must called when OutPutStream should be closed on disconnected...
    private void closeOutputStream(String address) {
        if(mDeviceOutputFileDict.containsKey(address)) {
            try {
                mDeviceOutputFileDict.get(address).close();
            } catch (IOException e){
                Log.e(TAG, "closeOutputStream: " + address);
                e.printStackTrace();
            }
            finally {
                mDeviceOutputFileDict.remove(address);
                Toast.makeText(this, R.string.file_closed, Toast.LENGTH_SHORT).show();
            }
        }
        if(mDeviceNotifyCharacteristic.containsKey(address)){
            mDeviceNotifyCharacteristic.remove(address);
            Log.d(TAG, "REMOVED: GATT PROPERTY_NOTIFY: " + address);
        }
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // // Automatically connects to the device upon successful start-up initialization.
            //for (BluetoothLeData device: mDeviceList) {
            //    final boolean result = mBluetoothLeService.connect(device.getDeviceAddress());
            //    Log.d(TAG, "Connect" + device.getDataPiece() + "request result=" + result);
            //}

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                //mConnected = true;
                mDeviceConnectionState.put(address, true);//updateConnectionState(address, true);
                //open a FileOutputStream
                openOutputStream(address);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //mConnected = false;
                mDeviceConnectionState.put(address, false);//updateConnectionState(address, false);
                mDeviceDataValue.remove(address);
                invalidateOptionsMenu();
                //clearUI(address);
                //TODO: ...
                closeOutputStream(address);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(address, mBluetoothLeService.getSupportedGattServices(address));

                //set Notify Characteristic
                mBluetoothLeService.setCharacteristicNotification(
                        address,
                        mDeviceNotifyCharacteristic.get(address),
                        true);
                Toast.makeText(context, "Set Characteristic Notification: " + address, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "GATT PROPERTY_NOTIFY: " + address);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                mDeviceDataValue.put(address, data);//displayData(address, data); // decouple data receive and data display (buffer)
                // write to file
                try {
                    //mReadWriteLock.readLock().lock();
                    Objects.requireNonNull(mDeviceOutputFileDict.get(address)).write(data.getBytes());//getOutputStream
                    Objects.requireNonNull(mDeviceOutputFileDict.get(address)).flush();
                    //mReadWriteLock.readLock().unlock();
                } catch (IOException e) {
                    Toast.makeText(context, R.string.file_error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onReceive: ble data or fos error");

                    e.printStackTrace();
                }
                invalidateOptionsMenu();
                //else { }
                // TODO：处理危险范围...再显示
                //notifyData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            mMultiDeviceListAdapter.notifyDataSetChanged();
        }
    };



    private void clearUI(String address) {
        //mReadWriteLock.readLock().lock();
        //mDeviceGattServicesListView.get(address).setAdapter((SimpleExpandableListAdapter) null);
        //mReadWriteLock.readLock().unlock();
        //mReadWriteLock.writeLock().lock();
        mDeviceDataValue.remove(address);
        //mReadWriteLock.writeLock().lock();
    }

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //然后通过一个函数来申请
            try {
                //检测是否有写的权限
                int permission = ActivityCompat.checkSelfPermission(this,
                        "android.permission.WRITE_EXTERNAL_STORAGE");
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    // 没有写的权限，去申请写的权限，会弹出对话框
                    ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        getActionBar().setTitle(R.string.title_multi);

        final Intent intent = getIntent();
        //得到设备列表
        mDeviceList = intent.getParcelableArrayListExtra(MultiDeviceControlActivity.EXTRAS_DEVICE_LIST);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //this.setContentView(R.layout.multi_device_list);
//        //Set up Notification channel.
//        createNotificationChannel();
//        mInitialUri = null;
//        mParcelFileDescriptor = null;
//        mFileOutputStream = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        mMultiDeviceListAdapter = new MultiControlGattListAdapter();
        setListAdapter(mMultiDeviceListAdapter);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if (mBluetoothLeService != null) {
//            for (BluetoothLeData device : mDeviceList) {
//                final boolean result = mBluetoothLeService.connect(device.getDeviceAddress());
//                Log.d(TAG, "Connect" + device.getDataPiece() + "request result=" + result);
//            }
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //TODO: ...
        for (BluetoothLeData device : mDeviceList) {
            closeOutputStream(device.getDeviceAddress());
            mBluetoothLeService.disconnect(device.getDeviceAddress());
            mDeviceConnectionState.put(device.getDeviceAddress(), false);
            mDeviceDataValue.clear();
        }
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        //menu.findItem(R.id.menu_set_file).setVisible(true);
        //if (mConnected) {
        menu.findItem(R.id.menu_connect).setVisible(true);
        menu.findItem(R.id.menu_disconnect).setVisible(true);
        //} else {
        //  menu.findItem(R.id.menu_connect).setVisible(true);
        // menu.findItem(R.id.menu_disconnect).setVisible(false);
        //}
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override

    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothLeData device = mMultiDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if(mDeviceConnectionState.get(device.getDeviceAddress()) == null || !mDeviceConnectionState.get(device.getDeviceAddress())) {
            boolean result = mBluetoothLeService.connect(device.getDeviceAddress());
            Log.d(TAG, "Connect:" + device.getDataPiece() + "request result=" + result);
        }
//        if(mDeviceConnectionState.get(device.getDeviceAddress())) {//if already connected, then set char notification
//            mBluetoothLeService.setCharacteristicNotification(
//                    device.getDeviceAddress(),
//                    mDeviceNotifyCharacteristic.get(device.getDeviceAddress()),
//                    true);
//            Toast.makeText(this, "Set Characteristic Notification: " + device.getDeviceAddress(), Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "GATT PROPERTY_NOTIFY: " + device.getDeviceAddress());
////            mBluetoothLeService.disconnect(device.getDeviceAddress());
////            Log.d(TAG, "Disconnect:" + device.getDataPiece());
//        }
        mMultiDeviceListAdapter.notifyDataSetChanged();
    }


    @SuppressLint("NonConstantResourceId")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                for (BluetoothLeData device : mDeviceList) {
                    runOnUiThread(()->{
                    boolean result = mBluetoothLeService.connect(device.getDeviceAddress());
                    mMultiDeviceListAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Connect:" + device.getDataPiece() + "request result=" + result);
                    });
                }
                return true;
            case R.id.menu_disconnect:
                for (BluetoothLeData device : mDeviceList) {
                    runOnUiThread(()->{
                    mBluetoothLeService.disconnect(device.getDeviceAddress());
                    closeOutputStream(device.getDeviceAddress());
                    mMultiDeviceListAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Disconnect:" + device.getDataPiece());
                    });
                }
                return true;
            //case R.id.menu_set_file:
            //    initialUri();//设置保存文件目录205
            //    return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MultiControlGattListAdapter extends BaseAdapter {
        private final ArrayList<BluetoothLeData> mLeDevices;
        private final LayoutInflater mInflator;

        public MultiControlGattListAdapter() {
            super();
            mLeDevices = mDeviceList;
            mInflator = MultiDeviceControlActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothLeData device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public void removeDevice(BluetoothLeData device) {
            mLeDevices.remove(device);
        }

        public BluetoothLeData getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        public int getItemId(String address) {
            for (int i = 0; i < mLeDevices.size(); i++) {
                if (mLeDevices.get(i).getDeviceAddress().equalsIgnoreCase(address))
                    return i;
            }
            return -1;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            MultiDeviceControlActivity.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.gatt_services_characteristics, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.connectionState = (TextView) view.findViewById(R.id.connection_state);
                viewHolder.dataValue = (TextView) view.findViewById(R.id.data_value);
                //viewHolder.gattServicesList = (ExpandableListView) view.findViewById(R.id.gatt_services_list);
                view.setTag(viewHolder);
            } else {
                viewHolder = (MultiDeviceControlActivity.ViewHolder) view.getTag();
            }

            BluetoothLeData device = mLeDevices.get(i);
            String deviceAddress = device.getDeviceAddress();

            viewHolder.deviceAddress.setText(device.getDataPiece() + deviceAddress);

            viewHolder.connectionState.setText(mDeviceConnectionState.containsKey(deviceAddress) ?
                    (mDeviceConnectionState.get(deviceAddress).booleanValue() ?
                            R.string.connected : R.string.disconnected) : R.string.disconnected);
            if (mDeviceDataValue.containsKey(deviceAddress) && mDeviceDataValue.get(deviceAddress) != null) {
                viewHolder.dataValue.setText(mDeviceDataValue.get(deviceAddress));
            } else {
                viewHolder.dataValue.setText(R.string.no_data);
            }
                //mReadWriteLock.readLock().unlock();

            return view;
        }
    }

//    private void displayData(String address, String data) {
//        if (data != null) {
//            //mReadWriteLock.writeLock().lock();
//            mDeviceDataValue.put(address, data);
//            //mReadWriteLock.writeLock().lock();
//        }
//    }

//    private void updateConnectionState(String address, boolean connected) {
//        //mReadWriteLock.writeLock().lock();
//        mDeviceConnectionState.put(address, connected);
//        //mReadWrite77.writeLock().unlock();
//    }

    static class ViewHolder {
        TextView deviceAddress;
        TextView connectionState;
        TextView dataValue;
        //ExpandableListView gattServicesList;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITABLE);
        return intentFilter;
    }
}