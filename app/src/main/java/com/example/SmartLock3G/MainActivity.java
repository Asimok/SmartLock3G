package com.example.SmartLock3G;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.example.SmartLock3G.CH340Ser.MyApp;
import com.example.SmartLock3G.Track.trackAllActivity;
import com.example.SmartLock3G.bluetooth.BluetoothClientActivity_mytimer_machinery;
import com.example.SmartLock3G.bluetooth.Bluetooth_band;
import com.example.SmartLock3G.tools.Codes;
import com.example.SmartLock3G.tools.HexStringUtil;
import com.example.SmartLock3G.tools.aboutByte;
import com.example.SmartLock3G.tools.bluetooth_Pref;
import com.example.SmartLock3G.tools.gpsinfo;
import com.example.SmartLock3G.utils.DateUtil;
import com.example.SmartLock3G.tools.Pref;
import com.lpoint.tcpsocketlib.TcpClient;
import com.lpoint.tcpsocketlib.TcpSocketListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.Set;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

import static java.lang.Math.abs;
import static java.lang.Math.pow;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public LocationClient mLocationClient = null;
    public Button bt_send, mStart, sendgps,autoOpen;
    TcpClient tcpClient;
    String thisGpsInfos;
    Boolean nowsend = false;
    Boolean autoBluetoothOpen = false;
    private MyLocationListener myListener = new MyLocationListener();
    private TextView tv_content,tvBandBluetooth;
    private EditText ed_send_text, EDIP, EDPORT;
    private String ThisPhoneIP = "";
    private List<gpsinfo> gpsdata;
    private Pref sp;
    private bluetooth_Pref blue_sp;
    readThread readata;
    // 获取到蓝牙适配器
    public BluetoothAdapter mBluetoothAdapter;
    // 用来保存搜索到的设备信息
    public OutputStream os;
    private static final String ACTION_USB_PERMISSION = "com.example.SmartLock3G.USB_PERMISSION";

    private EditText writeText;
    private boolean isOpen;

    private Button writeButton, openButton, clearButton;

    public byte[] writeBuffer;
    public byte[] readBuffer;

    /**
     * 设置参数
     */
    private final int BAUDRATE = 9600;//波特率

    private final byte STOPBIT = 1;//停止位
    private final byte DATABIT = 8;//数据位
    private final byte PARITY  = 0;//奇偶校验位
    private final byte FLOW_CONTROL = 0;//停止位

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                mStart.setText("断开连接");
            } else if (msg.what == 1) {
                Toast.makeText(MainActivity.this, "无法连接", Toast.LENGTH_SHORT).show();
                mStart.setText("连接");
            } else if (msg.what == 3) {
                tcpClient.sendMsg("test");
            } else if (msg.what == 4) {
                nowsend = true;
                sendgps.setText("自动发送坐标 开");
            }
            else if (msg.what == 5) {
                nowsend = false;
                sendgps.setText("自动发送坐标 关");
            }
            else if (msg.what == 6) {
                autoBluetoothOpen = true;
                autoOpen.setText("蓝牙自动开锁 开");
                scanBluth();
            }
            else if (msg.what == 7) {
                autoBluetoothOpen = false;
                autoOpen.setText("蓝牙自动开锁 关");
            }
            else if(msg.what==8)
            {
                tv_content.append("\n");
                tv_content.append(DateUtil.formatTime());
                tv_content.append("  =>  \r\r");
                tv_content.append(msg.obj.toString().trim());
                checkCode(msg.obj.toString().trim());
            }
        }
    };

    private void checkCode(String code) {
        //TODO 向串口发送开关指令
        Log.d("wifi","进入检测");
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.OPEN))))
        {
            tv_content.append("\n");
            tv_content.append("3G 立即开");
            writeCodeToSerail(Codes.OPEN);
        }
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.alarm))))
        {
            tv_content.append("\n");
            tv_content.append("开启设备保护");
            writeCodeToSerail(Codes.alarm);
        }
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.CLOSE))))
        {
            tv_content.append("\n");
            tv_content.append("3G 立即关");
            writeCodeToSerail(Codes.CLOSE);
        }
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.relayOPEN))))
        {
            tv_content.append("\n");
            tv_content.append("蓝牙 立即开");
            writeCodeToSerail(Codes.OPEN);

            try {
                if(!blue_sp.getBluetoothAd().equals("null")) {
                    send(blue_sp.getBluetoothAd(), Codes.relayOPEN);
                }
                else
                    Toast.makeText(this,"请绑定蓝牙设备",Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.relayCLOSE))))
        {
            tv_content.append("\n");
            tv_content.append("蓝牙 立即关");
            writeCodeToSerail(Codes.CLOSE);

            try {
                if(!blue_sp.getBluetoothAd().equals("null")) {
                    send(blue_sp.getBluetoothAd(), Codes.relayCLOSE);
                }
                else
                    Toast.makeText(this,"请绑定蓝牙设备",Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.autoBluetoothOpen))))
        {
            tv_content.append("\n");
            tv_content.append("蓝牙自动开锁 开");
            autoBluetoothOpen=true;
            //打开
            Message msg = new Message();
            msg.what = 6;
            handler.sendMessage(msg);
        }
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.autoBluetoothClose))))
        {
            tv_content.append("\n");
            tv_content.append("蓝牙自动开锁 关");
            autoBluetoothOpen=false;
            //关闭
            Message msg = new Message();
            msg.what = 7;
            handler.sendMessage(msg);
        }
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.autoGetGPS))))
        {
            tv_content.append("\n");
            tv_content.append("发送坐标 开");
                nowsend=true;
                Message msg = new Message();
                msg.what = 4;
                handler.sendMessage(msg);



        }
        if(code.equals(Arrays.toString(aboutByte.bytesToInt(Codes.closeAutoGetGPS))))
        {
            tv_content.append("\n");
            tv_content.append("发送坐标 关");
            nowsend=false;
            Message msg = new Message();
            msg.what = 5;
            handler.sendMessage(msg);
        }
    }

    // 用于循环扫描蓝牙的hangdler
    @SuppressLint("HandlerLeak")
    Handler mBLHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    if (autoBluetoothOpen) {
                        scanBluth();
                    }
                    break;
                case 2:
                    if(!blue_sp.getBluetoothAd().equals("null")&&autoBluetoothOpen) {
                        //send(blue_sp.getBluetoothAd(), Codes.relayOPEN);
                        Log.d("aa1","已自动开锁");
                        setTitle("已自动开锁");
                        writeCodeToSerail(Codes.OPEN);
                    }
                    break;
                case 3:
                    if(!blue_sp.getBluetoothAd().equals("null")&&autoBluetoothOpen) {
                        //send(blue_sp.getBluetoothAd(), Codes.relayOPEN);
                        Log.d("aa1","已自动关锁");
                        setTitle("已自动关锁");
                        writeCodeToSerail(Codes.CLOSE);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    @SuppressLint("HandlerLeak")
    private Handler handler1 = new Handler() {
//TODO 显示串口读数据
        public void handleMessage(Message msg) {

            tv_content.append( msg.obj.toString());


        }
    };
    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // 获取到广播的action
            String action = intent.getAction();
            // 判断广播是搜索到设备还是搜索完成
            //蓝牙自动开锁状态才做相应处理
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
                    //TODO 蓝牙自动开锁
                    if (device.getAddress().equals(blue_sp.getBluetoothAd()) && (Float.parseFloat(mm) < 2.00)&&autoBluetoothOpen) {
                        Log.d("aa1", "小于2米 自动打开 "+blue_sp.getBluetoothName()+"  设备距离+"+Float.parseFloat(mm));
                        mBLHandler.sendEmptyMessage(2);

                        Toast.makeText(MainActivity.this,"设备距离+"+Float.parseFloat(mm)+" m 将自动解锁",Toast.LENGTH_SHORT).show();
                    }
                    else if(device.getAddress().equals(blue_sp.getBluetoothAd()) && (Float.parseFloat(mm) > 2.00)&&autoBluetoothOpen) {
                        Log.d("aa1", "大于2米 自动关闭  "+blue_sp.getBluetoothName()+" 设备距离+"+Float.parseFloat(mm));
                        Toast.makeText(MainActivity.this,"设备距离+"+Float.parseFloat(mm)+" m 将自动上锁",Toast.LENGTH_SHORT).show();
                        mBLHandler.sendEmptyMessage(3);
                    }
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d("aa", "搜索完成");
                setTitle("搜索完成");
                mBLHandler.sendEmptyMessageDelayed(1, 4000);
            }

        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tcp);
        InitView();
        InitDate();
        initLocation();
        initBlueTooth();
        scanBluth();
        getPremessionBluetooth();
        InitCH340();
         readata =new readThread();


    }

    //右上角三个点
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_1:
                Toast.makeText(this, "连接蓝牙", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, BluetoothClientActivity_mytimer_machinery.class);
                startActivity(intent);
                break;
            case R.id.menu_2:
                Toast.makeText(this, "绑定蓝牙", Toast.LENGTH_SHORT).show();
                Intent intent1 = new Intent(MainActivity.this, Bluetooth_band.class);
                startActivity(intent1);
                break;
            case R.id.menu_3:
                Toast.makeText(this, "正在开发", Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_4:
                Toast.makeText(this, "正在开发", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    private void InitDate() {
        mLocationClient = new LocationClient(getApplicationContext());
        //声明LocationClient类
        mLocationClient.registerLocationListener(myListener);
        //注册监听函数
        gpsdata = new ArrayList<gpsinfo>();
        ThisPhoneIP = getLocalIpAddress();  //获取本机IP
    }

    private void initLocation() {

//定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        mLocationClient = new LocationClient(this);
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        //声明LocationClient类实例并配置定位参数
        LocationClientOption locationOption = new LocationClientOption();

        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        locationOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
        locationOption.setCoorType("bd09ll");
        //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
        locationOption.setScanSpan(1000);
        //可选，设置是否需要地址信息，默认不需要
        locationOption.setIsNeedAddress(true);
        //可选，设置是否需要地址描述
        locationOption.setIsNeedLocationDescribe(true);
        //可选，设置是否需要设备方向结果
        locationOption.setNeedDeviceDirect(true);
        //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        locationOption.setLocationNotify(true);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        locationOption.setIgnoreKillProcess(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        locationOption.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        locationOption.setIsNeedLocationPoiList(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        locationOption.SetIgnoreCacheException(false);
        //可选，默认false，设置是否开启Gps定位
        locationOption.setOpenGps(true);
        //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        locationOption.setIsNeedAltitude(true);
        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回             调给开发者
        //locationOption.setOpenAutoNotifyMode();
        //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
        locationOption.setOpenAutoNotifyMode(1000, 1, LocationClientOption.LOC_SENSITIVITY_HIGHT);
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        mLocationClient.setLocOption(locationOption);
        //开始定位
        mLocationClient.start();
    }
//TODO 初始化数据
    private void InitView() {



//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //需要在子线程中处理的逻辑
//                byte[] buffer = new byte[128];
//                while (true) {
//                    Message msg = Message.obtain();
//                    if (!isOpen) {
//                        break;
//                    }
//                    int length = MyApp.driver.ReadData(buffer, 128);
//                    if (length > 0) {
//                        String recv = aboutByte.toHexString(buffer, length);
//                        msg.obj = recv;
//                        handler1.sendMessage(msg);
//                    }
//                }
//            }
//        }).start();


        EDIP = (EditText) findViewById(R.id.EDIP);
        EDPORT = (EditText) findViewById(R.id.EDPORT);
        tv_content = (TextView) findViewById(R.id.tv_content);
        tvBandBluetooth= (TextView) findViewById(R.id.tvBandBluetooth);
        bt_send = (Button) findViewById(R.id.bt_send);
        autoOpen = (Button) findViewById(R.id.autoOpen);
        ed_send_text = (EditText) findViewById(R.id.ed_send_text);
        bt_send.setOnClickListener(this);
        mStart = (Button) findViewById(R.id.mStart);
        sendgps = (Button) findViewById(R.id.sendgps);
        mStart.setOnClickListener(this);
        sp = Pref.getInstance(this);
        EDIP.setText(sp.getHost());
        ed_send_text.setText(sp.getIdCode());
        EDPORT.setText(String.valueOf(sp.getPort()));
        blue_sp=bluetooth_Pref.getInstance(this);
        tvBandBluetooth.setText(String.format("已绑定设备：  %s  %s", blue_sp.getBluetoothName(), blue_sp.getBluetoothAd()));
        writeText = (EditText) findViewById(R.id.WriteValues);
        writeButton = (Button) findViewById(R.id.WriteButton);
        openButton = (Button) findViewById(R.id.open_device);
        clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                tv_content.setText("");
            }
        });
        //打开流程主要步骤为ResumeUsbList，UartInit
        openButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!isOpen) {
                    if (!MyApp.driver.ResumeUsbList())// ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
                    {
                        Toast.makeText(MainActivity.this, "打开设备失败!",
                                Toast.LENGTH_SHORT).show();
                        MyApp.driver.CloseDevice();
                    } else {
                        if (!MyApp.driver.UartInit()) {//对串口设备进行初始化操作
                            Toast.makeText(MainActivity.this, "设备初始化失败!",
                                    Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "打开" +
                                            "设备失败!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        else {

//TODO 打开串口
                            if (MyApp.driver.SetConfig(BAUDRATE, DATABIT, STOPBIT, PARITY,//配置串口波特率，函数说明可参照编程手册
                                    FLOW_CONTROL)) {
                                Toast.makeText(MainActivity.this, "打开设备和串口设置成功!",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "打开设备成功，串口设置失败!",
                                        Toast.LENGTH_SHORT).show();
                            }
                            Toast.makeText(MainActivity.this, "打开设备成功!",
                                    Toast.LENGTH_SHORT).show();
                            isOpen = true;
                            openButton.setText("Close");
                            writeButton.setEnabled(true);
                          //new readThread().start();//开启读线程读取串口接收的数据
                            //readata.run();




                        }
                    }
                } else {
                    MyApp.driver.CloseDevice();
                    openButton.setText("Open");
                    writeButton.setEnabled(false);
                    isOpen = false;
                }
            }
        });


        writeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                byte[] to_send = aboutByte.toByteArray(writeText.getText().toString());
                int retval = MyApp.driver.WriteData(to_send, to_send.length);//写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
                if (retval < 0)
                    Toast.makeText(MainActivity.this, "串口写失败!",
                            Toast.LENGTH_SHORT).show();
            }
        });
       // return;



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

    private void InitCH340() {
        MyApp.driver = new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), this,
                ACTION_USB_PERMISSION);
        if (!MyApp.driver.UsbFeatureSupported())// 判断系统是否支持USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton("确认",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    System.exit(0);
                                }
                            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// 保持常亮的屏幕的状态
        writeBuffer = new byte[512];
        readBuffer = new byte[512];
        isOpen = false;
        writeButton.setEnabled(false);
    }

    public void sendGPS(View view) {

        if (nowsend) {
            Message msg = new Message();
            msg.what = 5;
            handler.sendMessage(msg);
        } else {
            Message msg = new Message();
            msg.what = 4;
            handler.sendMessage(msg);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_send:  //发送
                if (mStart.getText().equals("断开连接")) {
                    String str = ed_send_text.getText().toString();
                    if (str.equals("")) {
                        Toast.makeText(this, "请输入数据", Toast.LENGTH_SHORT).show();
                    } else
                        tcpClient.sendMsg(str);
                } else Toast.makeText(this, "Socket未连接", Toast.LENGTH_SHORT).show();
                break;
            case R.id.mStart:  //  连接/断开服务器
                tcpClient = new TcpClient(EDIP.getText().toString(), Integer.parseInt(EDPORT.getText().toString()));
                if (mStart.getText().equals("连接")) {
                    tcpClient.startConn();
                    SocketListener();
                    //保存host和port
                    sp.setHost(EDIP.getText().toString());
                    sp.setPort(Integer.parseInt(EDPORT.getText().toString()));
                    mStart.setText("断开连接");

                } else {
                    tcpClient.closeTcpSocket();
                    tv_content.setText("已清空");
                    mStart.setText("连接");
                }
                break;
        }

    }

    public void gettrack(View view) {
        //轨迹
        Intent intent = new Intent(this, trackAllActivity.class);
        startActivity(intent);
    }

    public void bluetoothOpen(View view) {
        try {
            if(!blue_sp.getBluetoothAd().equals("null")) {
                send(blue_sp.getBluetoothAd(), Codes.relayOPEN);
            }
            else
                Toast.makeText(this,"请绑定蓝牙设备",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void bluetoothClose(View view) {
        try {
            if(!blue_sp.getBluetoothAd().equals("null")) {
                send(blue_sp.getBluetoothAd(), Codes.relayCLOSE);

            }
            else
                Toast.makeText(this,"请绑定蓝牙设备",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void autoBluetoothOpen(View view) {
        if (!autoBluetoothOpen) {
            //打开
            Message msg = new Message();
            msg.what = 6;
            handler.sendMessage(msg);
        } else {
            //关闭
            Message msg = new Message();
            msg.what = 7;
            handler.sendMessage(msg);

        }
    }
    /**
     * 获取WIFI下ip地址
     */
    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();
        Log.d("wifi", "本机	IP	" + String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff)));
        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }


    //搜索蓝牙设备
    public void scanBluth() {
        // 点击搜索周边设备，如果正在搜索，则暂停搜索
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            setTitle("暂停搜索");
            Log.d("aa", "暂停搜索");
        }

        mBluetoothAdapter.startDiscovery();
        Log.d("aa", "正在扫描...");
        setTitle("正在扫描...");
    }
    //检查权限
    private boolean isNeedRequestPermissions(List<String> permissions) {
        // 定位精确位置
        addPermission(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
        // 存储权限
        addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        // 读取手机状态
        addPermission(permissions, Manifest.permission.READ_PHONE_STATE);
        return permissions.size() > 0;
    }

    private void addPermission(List<String> permissionsList, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
        }
    }

    public void getPremessionBluetooth() {
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        // 判断手机是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 判断是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled()) {
            // 弹出对话框提示用户是后打开
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
            // 不做提示，强行打开
            // mBluetoothAdapter.enable();
        } else {
            // 不做提示，强行打开
            mBluetoothAdapter.enable();
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
            Toast.makeText(this,"发送成功",Toast.LENGTH_SHORT).show();
        } finally {
            if (lvOs != null) lvOs.close();
            //lvSocket.close(); outputstream close时已经关闭socket了,所以无需再close
        }
    }

    public void alarm(View view) {
        //TODO 异常报警
        if(mStart.getText().toString().equals("断开连接"))
        {
            tcpClient.sendMsg("alarm");
        }
    }

    //TODO 串口读数据
    private class readThread extends Thread {

        public void run() {

            byte[] buffer = new byte[128];

            while (true) {
                Message msg = Message.obtain();
                if (!isOpen) {
                    break;
                }
                int length = MyApp.driver.ReadData(buffer, 128);
                if (length > 0) {
                    String recv = aboutByte.toHexString(buffer, length);
                    msg.obj = recv;
                    handler1.sendMessage(msg);
                }
            }
        }
    }



    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

            double latitude = location.getLatitude();    //获取纬度信息
            double longitude = location.getLongitude();    //获取经度信息
            float radius = location.getRadius();    //获取定位精度，默认值为0.0f
            float direction = location.getDirection();

            String errorCode = location.getCoorType();
            //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明

            Log.d("gps1", "第一次  " + latitude + "   " + longitude + "   " + errorCode);

            //封装
            gpsinfo thisgps = new gpsinfo();
            thisgps.setLatitude(latitude);
            thisgps.setLongitude(longitude);
            thisgps.setRadius(radius);
            thisgps.setDirection(direction);
            gpsdata.add(thisgps);

            JSONArray jsonArray = new JSONArray();
            JSONObject tmpObj = null;
            int count = gpsdata.size();
            for (int i = 0; i < count; i++) {
                tmpObj = new JSONObject();
                try {
                    tmpObj.put("latitude", gpsdata.get(i).getLatitude());
                    tmpObj.put("longitude", gpsdata.get(i).getLongitude());
                    tmpObj.put("radius", gpsdata.get(i).getRadius());
                    tmpObj.put("direction", gpsdata.get(i).getDirection());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(tmpObj);
                tmpObj = null;
            }
            thisGpsInfos = jsonArray.toString(); // 将JSONArray转换得到String
            Log.d("gps1", "personInfos    " + thisGpsInfos);

            if (nowsend) {
                Log.d("gps1", "允许发送");
                if (mStart.getText().equals("断开连接")) {
                    gpsdata.clear();
                    tcpClient.sendMsg(thisGpsInfos);
                } else Toast.makeText(MainActivity.this, "Socket未连接", Toast.LENGTH_SHORT).show();
            }
            else
                Log.d("gps1", "禁止发送");
        }
    }

    public void SocketListener() {
        TcpSocketListener tcpSocketListener = new TcpSocketListener() {
            /**
               * 发起TCP连接时报出的异常
               */
            @Override
            public void onConnException(Exception e) {
                Log.d("wifi", "发起TCP连接时报出的异常   ");
                Message msg = new Message();
                msg.what = 1;
                handler.sendMessage(msg);

            }

            /**
               * 当TCP通道收到消息时执行此回调
               */
            @Override
            public void onMessage(String s) {
                Log.d("wifi", "收到数据   " + s.trim());
                Message msg = new Message();
                msg.what = 8;
                msg.obj=s;
                handler.sendMessage(msg);

            }

            /**
               * 当TCP消息监听时遇到异常，从这里抛出
               */
            @Override
            public void onListenerException(Exception e) {
                Log.d("wifi", "TCP消息监听时遇到异常，从这里抛出  ");
                Message msg = new Message();
                msg.what = 1;
                handler.sendMessage(msg);
            }

            /**
               * 当sendMsg()方法成功执行完毕后，执行此方法
               */
            @Override
            public void onSendMsgSuccess(String s) {
                Message msg = new Message();
                msg.what = 0;
                handler.sendMessage(msg);
            }

            /**
               * 发送消息时遇到异常，从这里抛出
               */
            @Override
            public void onSendMsgException(Exception e) {
                Log.d("wifi", "发送消息时遇到异常，从这里抛出 ");
                Message msg = new Message();
                msg.what = 1;
                handler.sendMessage(msg);

            }

            /**
               * 当TCP连接断开时遇到异常，从这里抛出
               */
            @Override
            public void onCloseException(Exception e) {
                Log.d("wifi", "当TCP连接断开时遇到异常，从这里抛出 ");
            }
        };
        tcpClient.setTcpSocketListener(tcpSocketListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 适配android M，检查权限
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isNeedRequestPermissions(permissions)) {
            requestPermissions(permissions.toArray(new String[permissions.size()]), 0);
        }
    }
    @Override
    protected void onDestroy() {

        tcpClient.closeTcpSocket();
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        tvBandBluetooth.setText(String.format("已绑定设备：  %s  %s", blue_sp.getBluetoothName(), blue_sp.getBluetoothAd()));
        super.onResume();
    }

private   void  writeCodeToSerail(byte[] to_send)
{
    //byte[] to_send = aboutByte.toByteArray(writeText.getText().toString());
    int retval = MyApp.driver.WriteData(to_send, to_send.length);//写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
    if (retval < 0)
        Toast.makeText(MainActivity.this, "串口写失败!",
                Toast.LENGTH_SHORT).show();
}


}