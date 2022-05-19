package com.example.android.bluetoothlegatt;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BleUartDataReceiver {
    private ConcurrentLinkedQueue<StringBuffer> lines_buffer = new ConcurrentLinkedQueue<>();
    private StringBuffer line = new StringBuffer();
    private BleUartDataReceiverCallback cb;

    public BleUartDataReceiver(BleUartDataReceiverCallback callback){
        cb = callback;
    }

    public synchronized void receiveData(byte[] data) {
        String temp = new String(data);
        if (temp.contains("\n")) {
            int i = temp.indexOf('\n');
            String pre = temp.substring(0, i);//should not contains "\n"
            String aft = temp.substring(i+1);
            line.append(pre);
            lines_buffer.add(line);
            line = new StringBuffer();
            line.append(aft);
        } else {
            line.append(temp);
        }
    }

    public synchronized BleUartData parseData() {
        String l;
        if(!lines_buffer.isEmpty()){
            l = new String(lines_buffer.poll());
            String[] words = l.split(" +");
            BleUartData parsedData = new BleUartData(words);
            cb.onBleUartDataReceived(parsedData); //callback
        }
    }


    public static class BleUartData {
        int timeStamp;
        float value_ao, voltage_ao, press_ao; //压力
        float attitude_roll, attitude_pitch, attitude_yaw; //欧拉角
        float acc_x, acc_y, acc_z; //加速度
        float gyro_x, gyro_y, gyro_z; //角速度
        final static int COUNT = 13;

        public BleUartData(){
            timeStamp = 0;
            value_ao = 0;
            voltage_ao = 0;
            press_ao = 0; //压力
            attitude_roll = 0;
            attitude_pitch = 0;
            attitude_yaw = 0; //欧拉角
            acc_x = 0;
            acc_y = 0;
            acc_z = 0; //加速度
            gyro_x = 0;
            gyro_y = 0;
            gyro_z = 0; //角速度
        }

        public BleUartData(String[] data) throws Error{
            timeStamp = 0;
            value_ao = 0;
            voltage_ao = 0;
            press_ao = 0; //压力
            attitude_roll = 0;
            attitude_pitch = 0;
            attitude_yaw = 0; //欧拉角
            acc_x = 0;
            acc_y = 0;
            acc_z = 0; //加速度
            gyro_x = 0;
            gyro_y = 0;
            gyro_z = 0; //角速度
            try {
                timeStamp = Integer.parseInt(data[0]);
                value_ao = Float.parseFloat(data[1]);
                voltage_ao = Float.parseFloat(data[2]);
                press_ao = Float.parseFloat(data[3]); //压力
                attitude_roll = Float.parseFloat(data[4]);
                attitude_pitch = Float.parseFloat(data[5]);
                attitude_yaw = Float.parseFloat(data[6]); //欧拉角
                acc_x = Float.parseFloat(data[7]);
                acc_y = Float.parseFloat(data[8]);
                acc_z = Float.parseFloat(data[9]); //加速度
                gyro_x = Float.parseFloat(data[10]);
                gyro_y = Float.parseFloat(data[11]);
                gyro_z = Float.parseFloat(data[12]); //角速度
            } catch (ArrayIndexOutOfBoundsException e){
                //容错，因已初始化为零
            }
        }
    }

    public interface BleUartDataReceiverCallback {
        void onBleUartDataReceived(BleUartData data);
    }
}
