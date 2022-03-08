package com.example.android.bluetoothlegatt;

import android.os.Parcel;
import android.os.Parcelable;


public class BluetoothLeData implements Parcelable{
    private String mAddress;
    private String mData;

    public String getDeviceAddress() { return mAddress; }

    public void setDeviceAddress(String address) {
        this.mAddress = address;
    }

    public String getDataPiece() {
        return mData;
    }

    public void setDataPiece(String data) {
        this.mData = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mAddress);
        parcel.writeString(mData);
    }

    public static final Creator<BluetoothLeData> CREATOR = new Creator<BluetoothLeData>() {
        @Override
        public BluetoothLeData createFromParcel(Parcel parcel) {
            BluetoothLeData bd = new BluetoothLeData();
            bd.setDeviceAddress(parcel.readString());
            bd.setDataPiece(parcel.readString());
            return bd;
        }

        @Override
        public BluetoothLeData[] newArray(int size) {
            return new BluetoothLeData[size];
        }
    };

}
