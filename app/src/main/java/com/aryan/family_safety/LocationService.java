package com.aryan.family_safety;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference devicesRef;
    private DatabaseReference historyRef;
    private String deviceId;

    // Last location for smart tracking
    private double lastLat = 0;
    private double lastLng = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // Get unique device ID
        deviceId = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase references
        devicesRef = FirebaseDatabase.getInstance()
                .getReference("devices").child(deviceId);

        // Save device nickname if not set
        devicesRef.child("nickname").get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                // Get device name
                String deviceName = android.os.Build.MODEL;
                devicesRef.child("nickname").setValue(deviceName);
                devicesRef.child("color").setValue("#1A237E");
            }
        });

        historyRef = FirebaseDatabase.getInstance()
                .getReference("history").child(deviceId);

        // Initialize location client
        fusedLocationClient = LocationServices
                .getFusedLocationProviderClient(this);

        createNotificationChannel();
        startForeground(1, buildNotification());
        startLocationUpdates();
    }

    private void startLocationUpdates() {

        // Location request - updates every 30 seconds
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 30000)
                .setMinUpdateIntervalMillis(15000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                android.location.Location location =
                        locationResult.getLastLocation();
                if (location == null) return;

                double lat = location.getLatitude();
                double lng = location.getLongitude();
                float speed = location.getSpeed();

                // Smart tracking - check if device is moving
                boolean isMoving = isDeviceMoving(lat, lng);

                // Get battery level
                int battery = getBatteryLevel();

                // Get address from coordinates
                String address = getAddressFromLocation(lat, lng);

                // Save to Firebase
                saveToFirebase(lat, lng, speed, isMoving, battery, address);

                // Save to history
                saveToHistory(lat, lng, address);

                // Update last known location
                lastLat = lat;
                lastLng = lng;
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Smart tracking - faster when moving, slower when still
    private boolean isDeviceMoving(double lat, double lng) {
        if (lastLat == 0 && lastLng == 0) return false;

        // Calculate distance from last location
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                lastLat, lastLng, lat, lng, results);
        float distanceMeters = results[0];

        // If moved more than 20 meters = moving
        return distanceMeters > 20;
    }

    // Save current location to Firebase devices node
    private void saveToFirebase(double lat, double lng,
                                float speed, boolean isMoving,
                                int battery, String address) {

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude",  lat);
        locationData.put("longitude", lng);
        locationData.put("speed",     speed);
        locationData.put("isMoving",  isMoving);
        locationData.put("battery",   battery);
        locationData.put("address",   address);
        locationData.put("lastSeen",  System.currentTimeMillis());

        devicesRef.updateChildren(locationData);
    }

    // Save location point to history node
    private void saveToHistory(double lat, double lng, String address) {
        // Get today's date as key
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new java.util.Date());

        // Create history point
        Map<String, Object> historyPoint = new HashMap<>();
        historyPoint.put("latitude",  lat);
        historyPoint.put("longitude", lng);
        historyPoint.put("address",   address);
        historyPoint.put("timestamp", System.currentTimeMillis());

        // Push creates a unique key for each point
        historyRef.child(today).push().setValue(historyPoint);
    }

    // Convert lat/lng to readable address
    private String getAddressFromLocation(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (addr.getSubLocality() != null)
                    sb.append(addr.getSubLocality()).append(", ");
                if (addr.getLocality() != null)
                    sb.append(addr.getLocality());
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown location";
    }

    // Get device battery percentage
    private int getBatteryLevel() {
        IntentFilter filter =
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);
        if (batteryStatus == null) return 0;
        int level = batteryStatus.getIntExtra(
                BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(
                BatteryManager.EXTRA_SCALE, -1);
        return (int) ((level / (float) scale) * 100);
    }

    // Build the required foreground notification
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Family Safety")
                .setContentText("Location sharing is active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // Required for Android 8+
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop location updates when service is destroyed
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        // Mark device as offline in Firebase
        devicesRef.child("isOnline").setValue(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
