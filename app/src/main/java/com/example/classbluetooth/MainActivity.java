package com.example.classbluetooth;

import androidx.annotation.LongDef;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sdwfqin.cbt.CbtManager;
import com.sdwfqin.cbt.callback.ScanCallback;

import java.io.IOException;
import java.io.InputStream;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import top.wuhaojie.bthelper.BtHelperClient;
import top.wuhaojie.bthelper.OnSearchDeviceListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    Button buttonClassic;
    Button buttonLink;

    RecyclerView recyclerView;

    BtHelperClient btHelperClient;
    BluetoothAdapter mBluetoothAdapter;

    Gson gson = new Gson();

    DeviceRvAdapter deviceRvAdapter;
    List<BluetoothDevice> list = new ArrayList<>();
    Set<BluetoothDevice> bindedSet;
    // 广播接收发现蓝牙设备
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //发现蓝牙设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                list.add(device);
                deviceRvAdapter.notifyDataSetChanged();
                Log.d(TAG, "onReceive: ------" + device.getName() + "\n" + device.getAddress());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //蓝牙搜索完成
                // 蓝牙搜索是非常消耗系统资源开销的过程，搜索完毕后应该及时取消搜索
                mBluetoothAdapter.cancelDiscovery();
                Toast.makeText(context, "搜索结束", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onReceive: --------cancelDiscovery");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btHelperClient = BtHelperClient.from(MainActivity.this);

        buttonClassic = findViewById(R.id.button_searchclassic);
        buttonClassic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothAdapter.startDiscovery();
            }
        });

        buttonLink = findViewById(R.id.button_link);
        buttonLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {



                //开启线程进行蓝牙连接
                String deviceSaved = (String)SPUtils.get(MainActivity.this, "deviceBlueTooth", "");
//                BluetoothDevice device = gson.fromJson(deviceSaved, BluetoothDevice.class);
                Log.d(TAG, "deviceGetFrom: " + deviceSaved);
                for (int i = 0; i < list.size(); i++) {
                    Log.d(TAG, "listItem mac address: " + list.get(i).getAddress());
                    if (deviceSaved.equals("\""+list.get(i).getAddress()+"\"")) {
                        Log.d(TAG, "reStartFromSP: ");
                        new Thread(new ClientThread(list.get(i))).start();
                    }
                }
            }
        });

        recyclerView = findViewById(R.id.recyclerview);
        deviceRvAdapter = new DeviceRvAdapter(this, list, new DeviceRvAdapter.GetClickPosition() {
            @Override
            public void getPosition(int i) {
                new Thread(new ClientThread(list.get(i))).start();

                Log.d(TAG, "保存: " + list.get(i).getName());

                String deviceToSave = gson.toJson(list.get(i).getAddress());
                Log.d(TAG, "设备Gson: " + deviceToSave);
                SPUtils.put(MainActivity.this,"deviceBlueTooth",deviceToSave);



            }
        });
        recyclerView.setAdapter(deviceRvAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));


    }


    @Override
    protected void onResume() {
        super.onResume();




        //蓝牙初始化,获取BluetoothAdapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            //若没打开则打开蓝牙
            boolean isEnable = mBluetoothAdapter.enable();
            if (!isEnable) {
                //蓝牙权限被禁止，请在权限管理中打开
            }
        }

        // 注册用以接收到已搜索到的蓝牙设备的receiver
        IntentFilter mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBluetoothReceiver, mFilter);
        // 注册搜索完时的receiver
        mFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBluetoothReceiver, mFilter);

        bindedSet = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG, "getBindedDeviceList: "+mBluetoothAdapter.getBondedDevices());



        //开启线程进行蓝牙连接
        String deviceSaved = (String)SPUtils.get(MainActivity.this, "deviceBlueTooth", "");
//                BluetoothDevice device = gson.fromJson(deviceSaved, BluetoothDevice.class);
        Log.d(TAG, "deviceGetFrom: " + deviceSaved);


        for(Iterator<BluetoothDevice> iterator = bindedSet.iterator(); iterator.hasNext();)
        {
            BluetoothDevice device = iterator.next();
            if (deviceSaved.equals("\""+device.getAddress()+"\"")) {
                Log.d(TAG, "startBluetoothConnectFromSp: " + deviceSaved);
                new Thread(new ClientThread(device)).start();
            }
        }
    }


    private BluetoothDevice mSelectBluetooth;
    private BluetoothSocket socket = null;
    private final String BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //蓝牙通信的UUID，必须为这个，如果换成其他的UUID会无法通信

    /**
     * 蓝牙连接的线程
     */
    private class ClientThread extends Thread {

        private BluetoothDevice device;

        public ClientThread(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {

            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString(BLUETOOTH_UUID));

                Log.d(TAG, "连接服务端...");
                socket.connect();

                Log.d(TAG, "连接建立.");

                ReadThread readThread = new ReadThread();  //连接成功后开启读取该蓝牙设备数据的线程
                readThread.start();
            } catch (Exception e) {
                //连接已断开
            }
        }
    }

    //连接设备和读取数据都需要新启线程在子线程中进行，因为过程比较耗时，如果不在子线程影响效率和阻塞UI。代码我就不做解释了，上面注释写的很清楚了。下面贴读取数据的操作：
    //读写线程
    private class ReadThread extends Thread {
        @Override
        public void run() {
            StringBuilder stringBuilder = new StringBuilder();

            byte[] buffer = new byte[1024];
            int bytes;
            InputStream mmInStream = null;   //建立输入流读取数据
            try {
                mmInStream = socket.getInputStream();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            while (true) {  //无限循环读取数据

                try {
                    // Read from the InputStream
                    if ((bytes = mmInStream.read(buffer)) > 0) {
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }


                        System.arraycopy(buffer, 0, buf_data, 0, bytes);
                        final int finalBytes = bytes;

                        String s = HexUtils.byte2HexStr(buf_data, finalBytes);
                        Log.d(TAG, "接收: " + s);
                        String s_decode = HexUtils.hexStr2Str(s);
                        Log.d(TAG, "接收解码: " + s_decode);


                        stringBuilder.append(s_decode);
                        if (stringBuilder.toString().contains("g")) {
                            Log.d(TAG, "完美解码: " + stringBuilder.toString());
                            stringBuilder.delete(0, stringBuilder.length());
                        }


                    }
                } catch (Exception e) {
                    try {
                        mmInStream.close();
                    } catch (IOException e1) {

                    }

                    break;  //异常的时候break跳出无限循环
                }
            }
        }
    }


}