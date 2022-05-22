package com.example.android.bluetoothlegatt;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BleUartDataReceiver {
    private ConcurrentLinkedQueue<StringBuffer> lines_buffer = new ConcurrentLinkedQueue<>();
    private StringBuffer line = new StringBuffer();
    private BleUartDataReceiverCallback cb;
    private final static String TAG = "BleUartDataReceiver";

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
        Log.i(TAG, "data received.");
    }

    public synchronized void parseData() {
        if(!lines_buffer.isEmpty()){
            String l;
            BleUartData parsedData = new BleUartData();
            l = new String(lines_buffer.poll());
            String[] words = l.split(" +"); //split by spaces
            parsedData.fromStringArray(words);
            Log.i(TAG, "data parsed.");
            cb.onBleUartDataReceived(parsedData); //callback
        }
    }

    public static class BleUartData {
        public int timeStamp;
        public float value_ao, voltage_ao, press_ao; //压力
        public float attitude_roll, attitude_pitch, attitude_yaw; //欧拉角
        public float acc_x, acc_y, acc_z; //加速度
        public float gyro_x, gyro_y, gyro_z; //角速度
        public final static int COUNT = 13;
        private boolean isInit = true;

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
            isInit = true;
        }

        public void fromStringArray(String[] data){
            try {
                isInit = false;
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
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e00000000000){
                //容错，因已初始化为零
            }
        }

        public static float[] uniform(float[] array){
            float min=0f , max=0f;
            float[] temp = Arrays.copyOf(array, array.length);

            for (float v : array){
                if (v < min){ min = v;}
                if (v > max){ max = v;}
            }

            int count = 0;
            for (float v : array){
                float v_i = (v + min) / (max - min);
                temp[count] = v_i;
                count++;
            }

            return temp;
        }

        public float[] toFloatList(){//TODO: uniform
            float[] attitude = uniform(new float[]{attitude_roll, attitude_pitch, attitude_yaw});
            float[] acc = uniform(new float[]{acc_x, acc_y, acc_z});
            float[] gyro = uniform(new float[]{gyro_x, gyro_y, gyro_z});

            return new float[]{0,0,0,attitude[0], attitude[1], attitude[2], acc[0], acc[1], acc[2], gyro[0], gyro[1], gyro[2]};
        }

        public static String getColumns(){
            return "timeStamp,value_ao,voltage_ao,press_ao,attitude_roll,attitude_pitch,attitude_yaw,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z";
        }

        @SuppressLint("DefaultLocale")
        public String toString(){
            return String.format("%d, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f",timeStamp,value_ao,voltage_ao,press_ao,attitude_roll,attitude_pitch,attitude_yaw,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z);
        }

        public boolean isParsed(){
            return !isInit;
        }
    }

    public interface BleUartDataReceiverCallback {
        void onBleUartDataReceived(BleUartData data);
    }
}
