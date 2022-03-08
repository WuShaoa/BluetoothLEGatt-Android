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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiDeviceControlActivity extends ListActivity {
    private final static String TAG = MultiDeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_LIST = "DEVICE_LIST";
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private static final int CREATE_FILE = 1;

    private MultiControlGattListAdapter mMultiDeviceListAdapter;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected;
    private ArrayList<BluetoothLeData> mDeviceList;
    private HashMap<String, Boolean> mDeviceConnectionState = new HashMap<>();
    private HashMap<String, String> mDeviceDataValue = new HashMap<>();
    private HashMap<String, ExpandableListView> mDeviceGattServicesListView = new HashMap<>();
    private HashMap<String, ArrayList<ArrayList<BluetoothGattCharacteristic>>> mDeviceGattCharacteristics =
            new HashMap<>();
    private HashMap<String, BluetoothGattCharacteristic> mDeviceNotifyCharacteristic = new HashMap<>();

    private ReentrantReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    private Uri mInitialUri;
    private int mTakeFlags;

    private HashMap<String, String> mBleAddrGattDict = new HashMap<>();
    private HashMap<String, ParcelFileDescriptor> mBleFileDesDict = new HashMap<>();
    private HashMap<String, FileOutputStream> mBleFileOutStreamDict = new HashMap<>();


    // for open and saving data
    private void initialUri() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, R.string.uri_initial);

        startActivityForResult(intent, CREATE_FILE);
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == CREATE_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            if (resultData != null) {
                mInitialUri = resultData.getData();
                mTakeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                // Perform operations on the document using its URI.
                Toast.makeText(this, R.string.uri_initialized, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private FileOutputStream getOutputStream(String address) {
//        try {
//            if (mBleFileOutStreamDict.containsKey(address))
//                return mBleFileOutStreamDict.get(address);
//            else {
//                Uri fileOutputUri = new Uri.Builder().path(mInitialUri.getPath()).appendPath(address.replace(':', '-') + ".txt").build();
//                // Check for the freshest data.
//                getContentResolver().takePersistableUriPermission(fileOutputUri, mTakeFlags);
//
//                ParcelFileDescriptor parcelFileDescriptor = getContentResolver().
//                        openFileDescriptor(fileOutputUri, "w");
//                FileOutputStream fileOutputStream =
//                        new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
//                mBleFileDesDict.put(address, parcelFileDescriptor);
//                mBleFileOutStreamDict.put(address, fileOutputStream);
//                return fileOutputStream;
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        return null;
    }

    private void closeOutputStream(String address) {
//        // Let the document provider know you're done by closing the stream.
//        try {
//            if (!mBleFileOutStreamDict.containsKey(address) && !mBleFileDesDict.containsKey(address))
//                ;
//            else {
//                mBleFileOutStreamDict.get(address).close();
//                mBleFileOutStreamDict.remove(address);
//                mBleFileDesDict.get(address).close();
//                mBleFileDesDict.remove(address);
//            }
//            Toast.makeText(this, R.string.file_closed, Toast.LENGTH_SHORT).show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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
                updateConnectionState(address, true);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //mConnected = false;
                updateConnectionState(address, false);
                invalidateOptionsMenu();
                clearUI(address);
                //TODO: ...
                closeOutputStream(address);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(address, mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                displayData(address, data);
                // write to file
                //TODO:...
                try {
                    FileOutputStream fos = getOutputStream(address);
                    mBleFileOutStreamDict.put(address, fos);
                    fos.write(data.getBytes());
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //else { Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show(); }
                // TODO：处理危险范围...再显示
                //notifyData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            mMultiDeviceListAdapter.notifyDataSetChanged();
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        mReadWriteLock.readLock().lock();
        ExpandableListView mGattServicesList = mDeviceGattServicesListView.get(address);
        mReadWriteLock.readLock().unlock();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid.toUpperCase(), unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid.toUpperCase(), unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
        mReadWriteLock.writeLock().lock();
        mDeviceGattCharacteristics.put(address, mGattCharacteristics);
        mReadWriteLock.writeLock().unlock();
    }

    private void clearUI(String address) {
        mReadWriteLock.readLock().lock();
        mDeviceGattServicesListView.get(address).setAdapter((SimpleExpandableListAdapter) null);
        mReadWriteLock.readLock().unlock();
        mReadWriteLock.writeLock().lock();
        mDeviceDataValue.remove(address);
        mReadWriteLock.writeLock().lock();
    }

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        if (mBluetoothLeService != null) {
            for (BluetoothLeData device : mDeviceList) {
                final boolean result = mBluetoothLeService.connect(device.getDeviceAddress());
                Log.d(TAG, "Connect" + device.getDataPiece() + "request result=" + result);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        //TODO: ...
        for (BluetoothLeData device : mDeviceList) {
            closeOutputStream(device.getDeviceAddress());
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        menu.findItem(R.id.menu_set_file).setVisible(true);
        //if (mConnected) {
        menu.findItem(R.id.menu_connect).setVisible(true);
        menu.findItem(R.id.menu_disconnect).setVisible(true);
        //} else {
        //  menu.findItem(R.id.menu_connect).setVisible(true);
        // menu.findItem(R.id.menu_disconnect).setVisible(false);
        //}
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                for (BluetoothLeData device : mDeviceList) {
                    mBluetoothLeService.connect(device.getDeviceAddress());
                }
                return true;
            case R.id.menu_disconnect:
                for (BluetoothLeData device : mDeviceList) {
                    mBluetoothLeService.disconnect(device.getDeviceAddress());
                }
                return true;
            case R.id.menu_set_file:
                initialUri();//设置保存文件目录205
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MultiControlGattListAdapter extends BaseAdapter {
        private ArrayList<BluetoothLeData> mLeDevices;
        private LayoutInflater mInflator;

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
            if (mLeDevices.contains(device)) {
                mLeDevices.remove(device);
            }
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
                viewHolder.gattServicesList = (ExpandableListView) view.findViewById(R.id.gatt_services_list);
                view.setTag(viewHolder);
            } else {
                viewHolder = (MultiDeviceControlActivity.ViewHolder) view.getTag();
            }

            BluetoothLeData device = mLeDevices.get(i);
            String deviceAddress = device.getDeviceAddress();

            viewHolder.deviceAddress.setText(device.getDataPiece() + deviceAddress);

            runOnUiThread(() -> {
                    ArrayList<ArrayList<BluetoothGattCharacteristic>> gattCharacteristics = mDeviceGattCharacteristics.get(deviceAddress);
                    viewHolder.gattServicesList.setOnChildClickListener(
                            (parent, v, groupPosition, childPosition, id) -> {
                                if (gattCharacteristics != null) {
                                    final BluetoothGattCharacteristic characteristic =
                                            gattCharacteristics.get(groupPosition).get(childPosition);
                                    final int charaProp = characteristic.getProperties();
                                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                                        // If there is an active notification on a characteristic, clear
                                        // it first so it doesn't update the data field on the user interface.
                                        if (mDeviceNotifyCharacteristic.get(deviceAddress) != null) {
                                            mBluetoothLeService.setCharacteristicNotification(
                                                    deviceAddress,
                                                    mDeviceNotifyCharacteristic.get(deviceAddress), false);
                                            mDeviceNotifyCharacteristic.remove(deviceAddress);
                                        }
                                        mBluetoothLeService.readCharacteristic(characteristic);
                                        Log.d(TAG, "GATT PROPERTY_READ: " + charaProp);
                                    }
                                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                        mDeviceNotifyCharacteristic.put(deviceAddress, characteristic);
                                        mBluetoothLeService.setCharacteristicNotification(
                                                characteristic, true);
                                        Log.d(TAG, "GATT PROPERTY_NOTIFY: " + charaProp);

                                    }
                                    return true;
                                }
                                return false;
                            });
                mReadWriteLock.writeLock().lock();
                mDeviceGattServicesListView.put(deviceAddress, viewHolder.gattServicesList);
                mReadWriteLock.writeLock().unlock();
                mReadWriteLock.readLock().lock();
                viewHolder.connectionState.setText(mDeviceConnectionState.containsKey(deviceAddress) ?
                        (mDeviceConnectionState.get(deviceAddress).booleanValue() ?
                                R.string.connected : R.string.disconnected) : R.string.disconnected);
                if (mDeviceDataValue.containsKey(deviceAddress)) {
                    viewHolder.dataValue.setText(mDeviceDataValue.get(deviceAddress));
                } else {
                    viewHolder.dataValue.setText(R.string.no_data);
                }
                mReadWriteLock.readLock().unlock();
            });

            return view;
        }
    }

    private void displayData(String address, String data) {
        if (data != null) {
            mReadWriteLock.writeLock().lock();
            mDeviceDataValue.put(address, data);
            mReadWriteLock.writeLock().lock();
        }
    }

    private void updateConnectionState(String address, boolean connected) {
        mReadWriteLock.writeLock().lock();
        mDeviceConnectionState.put(address, connected);
        mReadWriteLock.writeLock().unlock();
    }

    static class ViewHolder {
        TextView deviceAddress;
        TextView connectionState;
        TextView dataValue;
        ExpandableListView gattServicesList;
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


    public void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        if (listAdapter == null) {
            // pre-condition
            return;
        }
        int totalHeight = 0;
        View view;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, null, listView);
            //宽度为屏幕宽度
            int i1 = View.MeasureSpec.makeMeasureSpec(ScreenUtils.getScreenWidth(this), View.MeasureSpec.EXACTLY);
            //根据屏幕宽度计算高度
            int i2 = View.MeasureSpec.makeMeasureSpec(i1, View.MeasureSpec.UNSPECIFIED);
            view.measure(i1, i2);
            totalHeight += view.getMeasuredHeight();
        }
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    //获得屏幕相关的辅助类
    public static class ScreenUtils {
        private ScreenUtils() {
            /* cannot be instantiated */
            throw new UnsupportedOperationException("cannot be instantiated");
        }

        /**
         * 获得屏幕高度
         *
         * @param context
         * @return
         */
        public static int getScreenWidth(Context context) {
            WindowManager wm = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            return outMetrics.widthPixels;
        }

        /**
         * 获得屏幕宽度
         *
         * @param context
         * @return
         */
        public static int getScreenHeight(Context context) {
            WindowManager wm = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            return outMetrics.heightPixels;
        }

        /**
         * 获得状态栏的高度
         *
         * @param context
         * @return
         */
        public static int getStatusHeight(Context context) {

            int statusHeight = -1;
            try {
                Class<?> clazz = Class.forName("com.android.internal.R$dimen");
                Object object = clazz.newInstance();
                int height = Integer.parseInt(clazz.getField("status_bar_height")
                        .get(object).toString());
                statusHeight = context.getResources().getDimensionPixelSize(height);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return statusHeight;
        }

        /**
         * 获取当前屏幕截图，包含状态栏
         *
         * @param activity
         * @return
         */
        public static Bitmap snapShotWithStatusBar(Activity activity) {
            View view = activity.getWindow().getDecorView();
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap bmp = view.getDrawingCache();
            int width = getScreenWidth(activity);
            int height = getScreenHeight(activity);
            Bitmap bp = null;
            bp = Bitmap.createBitmap(bmp, 0, 0, width, height);
            view.destroyDrawingCache();
            return bp;

        }

        /**
         * 获取当前屏幕截图，不包含状态栏
         *
         * @param activity
         * @return
         */
        public static Bitmap snapShotWithoutStatusBar(Activity activity) {
            View view = activity.getWindow().getDecorView();
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap bmp = view.getDrawingCache();
            Rect frame = new Rect();
            activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
            int statusBarHeight = frame.top;

            int width = getScreenWidth(activity);
            int height = getScreenHeight(activity);
            Bitmap bp = null;
            bp = Bitmap.createBitmap(bmp, 0, statusBarHeight, width, height
                    - statusBarHeight);
            view.destroyDrawingCache();
            return bp;

        }

    }
}