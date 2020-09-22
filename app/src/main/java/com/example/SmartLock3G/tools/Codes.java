package com.example.SmartLock3G.tools;

public class Codes {
    public static byte[] CLOSE = {(byte) 0xff, 0x02, 0x01, 0x0A, (byte) 0xfe};//3G立即关闭
    public static byte[] OPEN = {(byte) 0xff, 0x02, 0x00, 0x0A, (byte) 0xfe};//3G立即开启
    public static byte[] alarm = {(byte) 0xff, 0x02, 0x06, 0x0A, (byte) 0xfe};//报警

    public static byte[] relayOPEN = {(byte) 0xA0, 0x01, 0x01, (byte) 0xA2, };//蓝牙继电器立即开启
    public static byte[] relayCLOSE = {(byte) 0xA0, 0x01, 0x00, (byte) 0xA1, };//蓝牙继电器立即开启
    public static byte[] autoGetGPS = {(byte) 0xff, 0x02, 0x03, 0x0A, (byte) 0xfe};//立即获取GPS
    public static byte[] closeAutoGetGPS = {(byte) 0xff, 0x02, 0x04, 0x0A, (byte) 0xfe};//立即关闭获取GPS
    public static byte[] autoBluetoothOpen = {(byte) 0xff, 0x02, 0x05, 0x0A, (byte) 0xfe};//开启蓝牙自动开锁
    public static byte[] autoBluetoothClose = {(byte) 0xff, 0x02, 0x06, 0x0A, (byte) 0xfe};//关闭蓝牙自动开锁
}
