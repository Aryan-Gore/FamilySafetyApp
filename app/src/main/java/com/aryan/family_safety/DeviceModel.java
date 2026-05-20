package com.aryan.family_safety;

public class DeviceModel {
    public String deviceId;
    public String nickname;
    public String color;
    public double latitude;
    public double longitude;
    public String address;
    public long lastSeen;
    public int battery;
    public boolean isMoving;
    public float speed;

    // Empty constructor required by Firebase
    public DeviceModel() {}

    public DeviceModel(String deviceId, String nickname, String color) {
        this.deviceId = deviceId;
        this.nickname = nickname;
        this.color = color;
    }
}
