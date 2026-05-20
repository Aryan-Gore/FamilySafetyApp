package com.aryan.family_safety;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.HashMap;
import java.util.Map;

public class MapActivity extends AppCompatActivity {

    MapView map;
    LinearLayout deviceListContainer;
    DatabaseReference devicesRef;
    Map<String, Marker> deviceMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(
                getPackageName());

        setContentView(R.layout.activity_map);

        // Initialize views
        map                 = findViewById(R.id.map);
        deviceListContainer = findViewById(R.id.deviceListContainer);

        // Setup map
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(13.0);

        // Default center - India
        GeoPoint startPoint = new GeoPoint(20.5937, 78.9629);
        map.getController().setCenter(startPoint);

        // Firebase reference
        devicesRef = FirebaseDatabase.getInstance()
                .getReference("devices");

        // Start listening to Firebase
        listenToDevices();
    }

    private void listenToDevices() {
        devicesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                deviceListContainer.removeAllViews();

                for (DataSnapshot deviceSnap : snapshot.getChildren()) {
                    String deviceId  = deviceSnap.getKey();
                    String nickname  = deviceSnap.child("nickname")
                            .getValue(String.class);
                    String color     = deviceSnap.child("color")
                            .getValue(String.class);
                    Double lat       = deviceSnap.child("latitude")
                            .getValue(Double.class);
                    Double lng       = deviceSnap.child("longitude")
                            .getValue(Double.class);
                    String address   = deviceSnap.child("address")
                            .getValue(String.class);
                    Long lastSeen    = deviceSnap.child("lastSeen")
                            .getValue(Long.class);
                    Integer battery  = deviceSnap.child("battery")
                            .getValue(Integer.class);
                    Boolean isMoving = deviceSnap.child("isMoving")
                            .getValue(Boolean.class);

                    // Skip if missing required data
                    if (lat == null || lng == null
                            || nickname == null) continue;

                    // Default values
                    if (color    == null) color    = "#43A047";
                    if (address  == null) address  = "Unknown";
                    if (battery  == null) battery  = 0;
                    if (lastSeen == null) lastSeen = 0L;
                    if (isMoving == null) isMoving = false;

                    // Build DeviceModel
                    DeviceModel device  = new DeviceModel();
                    device.deviceId     = deviceId;
                    device.nickname     = nickname;
                    device.color        = color;
                    device.latitude     = lat;
                    device.longitude    = lng;
                    device.address      = address;
                    device.battery      = battery;
                    device.lastSeen     = lastSeen;
                    device.isMoving     = isMoving;

                    // Update map pin
                    updateMapPin(device);

                    // Add device card to bottom list
                    addDeviceCard(device);
                }

                // Refresh map
                map.invalidate();
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void updateMapPin(DeviceModel device) {
        GeoPoint point = new GeoPoint(
                device.latitude, device.longitude);

        if (deviceMarkers.containsKey(device.deviceId)) {
            // Update existing marker
            deviceMarkers.get(device.deviceId)
                    .setPosition(point);
        } else {
            // Create new marker
            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setTitle(device.nickname);
            marker.setSnippet("Battery: "
                    + device.battery + "%"
                    + "\n" + device.address);
            marker.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM);

            deviceMarkers.put(device.deviceId, marker);
            map.getOverlays().add(marker);

            // Click marker → open PersonDetailActivity
            final DeviceModel d = device;
            marker.setOnMarkerClickListener((m, mapView) -> {
                openPersonDetail(d);
                return true;
            });
        }
    }

    private void addDeviceCard(DeviceModel device) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_member,
                        deviceListContainer, false);

        TextView tvName     = card.findViewById(R.id.tvMemberName);
        TextView tvLastSeen = card.findViewById(R.id.tvMemberLastSeen);
        TextView tvBattery  = card.findViewById(R.id.tvMemberBattery);
        TextView tvInitial  = card.findViewById(R.id.tvMemberInitial);
        TextView tvStatus   = card.findViewById(R.id.tvMemberStatus);
        TextView tvAddress  = card.findViewById(R.id.tvMemberAddress);
        View colorView      = card.findViewById(R.id.viewMemberColor);

        tvName.setText(device.nickname);
        tvBattery.setText(device.battery + "%");
        tvLastSeen.setText(getTimeAgo(device.lastSeen));
        tvAddress.setText(device.address);
        tvInitial.setText(device.nickname
                .substring(0, 1).toUpperCase());

        // Online/Offline status
        long diff = System.currentTimeMillis() - device.lastSeen;
        if (diff < 5 * 60 * 1000) {
            tvStatus.setText("Online");
            tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            tvStatus.setBackgroundResource(R.drawable.badge_green);
        } else {
            tvStatus.setText("Offline");
            tvStatus.setTextColor(Color.parseColor("#757575"));
            tvStatus.setBackgroundResource(R.drawable.badge_grey);
        }

        // Set avatar color
        try {
            colorView.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor(device.color)));
        } catch (Exception e) {
            colorView.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#43A047")));
        }

        // Click card → center map + open detail
        card.setOnClickListener(v -> {
            GeoPoint point = new GeoPoint(
                    device.latitude, device.longitude);
            map.getController().animateTo(point);
            map.getController().setZoom(16.0);
            openPersonDetail(device);
        });

        deviceListContainer.addView(card);
    }

    private void openPersonDetail(DeviceModel device) {
        Intent intent = new Intent(
                this, PersonDetailActivity.class);
        intent.putExtra("deviceId",  device.deviceId);
        intent.putExtra("nickname",  device.nickname);
        intent.putExtra("color",     device.color);
        intent.putExtra("latitude",  device.latitude);
        intent.putExtra("longitude", device.longitude);
        intent.putExtra("address",   device.address);
        intent.putExtra("battery",   device.battery);
        intent.putExtra("lastSeen",  device.lastSeen);
        startActivity(intent);
    }

    private String getTimeAgo(long timestamp) {
        if (timestamp == 0) return "Never";
        long diff    = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        long hours   = minutes / 60;
        if (minutes < 1)  return "Just now";
        if (minutes < 60) return minutes + " mins ago";
        if (hours < 24)   return hours + " hrs ago";
        return "Over a day ago";
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}