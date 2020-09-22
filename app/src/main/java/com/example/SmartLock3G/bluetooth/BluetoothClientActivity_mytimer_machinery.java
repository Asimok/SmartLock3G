package com.example.SmartLock3G.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.SmartLock3G.R;
import com.example.SmartLock3G.tools.Codes;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Formatter;
import java.util.Set;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class BluetoothClientActivity_mytimer_machinery extends Activity {
    // 用于循环扫描蓝牙的hangdler
    @SuppressLint("HandlerLeak")
    Handler mBLHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    scanBluth();
                    break;
                case 2:
                    try {
                        send("C3:3C:01:07:1E:C3", Codes.relayOPEN);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    // 获取到蓝牙适配器
    public BluetoothAdapter mBluetoothAdapter;
    // 用来保存搜索到的设备信息
    public OutputStream os;
    // 注册广播接收者
    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // 获取到广播的action
            String action = intent.getAction();
            // 判断广播是搜索到设备还是搜索完成
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                // 找到设备后获取其设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 设备没有绑定过
                    Log.d("aa", "设备没有绑定过   " + device.getName() + ":" + device.getAddress() + "\n");
                    short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                    int iRssi = abs(rssi);
                    double power = (iRssi - 59) / 25.0;
                    String mm = new Formatter().format("%.2f", pow(10, power)).toString();
                    Log.d("aa", "距离    " + mm);
                } else {
                    //设备绑定过
                    Log.d("aa", "设备绑定过   " + device.getName() + ":" + device.getAddress() + "\n");
                    short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                    int iRssi = abs(rssi);
                    double power = (iRssi - 59) / 25.0;
                    String mm = new Formatter().format("%.2f", pow(10, power)).toString();
                    Log.d("aa", "距离    " + mm);
                    if (device.getAddress().equals("C3:3C:01:07:1E:C3") && Float.parseFloat(mm) < 10.00) {
                        Log.d("aa", "自动打开    ");
                        mBLHandler.sendEmptyMessage(2);
                    }
                }
            } else if (action
                    .equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d("aa", "搜索完成");
                mBLHandler.sendEmptyMessageDelayed(1, 2000);
            }

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        initBlueTooth();
        scanBluth();
    }

    private void initView() {
        getPremession();//获取虚拟定位权限
        setContentView(R.layout.layout_buletooth_seacher_mytimer_machinery);
    }

    public void op1(View view) {
        try {
            send("C3:3C:01:07:1E:C3", Codes.relayOPEN);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cl2(View view) {
        try {
            send("C3:3C:01:07:1E:C3", Codes.relayCLOSE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initBlueTooth() {
        // 获取到蓝牙默认的适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 用Set集合保持已绑定的设备   将绑定的设备添加到Set集合。
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        // 因为蓝牙搜索到设备和完成搜索都是通过广播来告诉其他应用的
        // 这里注册找到设备和完成搜索广播
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

    }

    //搜索蓝牙设备
    public void scanBluth() {
        // 点击搜索周边设备，如果正在搜索，则暂停搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d("aa", "暂停搜索");
        }

        mBluetoothAdapter.startDiscovery();
        Log.d("aa", "正在扫描...");
    }


    public void getPremession() {
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("aa", "模糊定位");
//请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    0x114);
//判断是否需要 向用户解释，为什么要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                Log.d("aa", "判断是否需要 向用户解释，为什么要申请该权限");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean grantedLocation = true;
        if (requestCode == 0x114) {
            Log.d("aa", "允许获取权限");
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    grantedLocation = false;
                }
            }
        }

        if (!grantedLocation) {
            Log.d("aa", "Permission error !!!");
            Toast.makeText(this, "定位权限已拒绝，请手动打开权限!", Toast.LENGTH_LONG).show();

        }
    }

    /***
     * 向指定的蓝牙设备发送数据
     * @param pvsMac
     * @param pvsContent
     * @throws IOException
     */
    public void send(String pvsMac, byte[] pvsContent) throws IOException {
        BluetoothDevice lvDevice = mBluetoothAdapter.getRemoteDevice(pvsMac);
        BluetoothSocket lvSocket = null;
        try {
            lvSocket = (BluetoothSocket) lvDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(lvDevice, 1);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        OutputStream lvOs = null;
        try {
            try {
                lvSocket.connect();
            } catch (Exception e) {
                lvSocket.close();
                throw e;
            }
            lvOs = lvSocket.getOutputStream();
            lvOs.write(pvsContent);
        } finally {
            if (lvOs != null) lvOs.close();
            //lvSocket.close(); outputstream close时已经关闭socket了,所以无需再close
        }
    }
}
