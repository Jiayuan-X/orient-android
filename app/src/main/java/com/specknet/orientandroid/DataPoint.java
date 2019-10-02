package com.specknet.orientandroid;

public class DataPoint {
    public long timestamp;
    public float gyro;

    public DataPoint(long time, float gyro){
        this.timestamp = time;
        this.gyro = gyro;
    }
}
