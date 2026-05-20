package com.aryan.family_safety;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PersonDetailActivity extends AppCompatActivity {

    TextView tvTopName, tvNickname, tvLastSeen,
            tvAddress, tvDistance, tvBattery,
            tvSpeed, tvInitial;
    Button btnBack, btnViewOnMap, btnViewHistory;
    View viewAvatar;

    String deviceId, nickname, color, address;
    double latitude, longitude;
    int battery;
    long lastSeen;

    DatabaseReference deviceRef;
    DatabaseReference locationsRef;

    // Home location loaded from Firebase
    double homeLat = 0;
    double homeLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_detail);

        // Get data from intent
        deviceId = getIntent().getStringExtra("deviceId");
        nickname = getIntent().getStringExtra("nickname");
        color = getIntent().getStringExtra("color");
        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);
        address = getIntent().getStringExtra("address");
        battery = getIntent().getIntExtra("battery", 0);
        lastSeen = getIntent().getLongExtra("lastSeen", 0);

        // Connect UI
        tvTopName = findViewById(R.id.tvTopName);
        tvNickname = findViewById(R.id.tvNickname);
        tvLastSeen = findViewById(R.id.tvLastSeen);
        tvAddress = findViewById(R.id.tvAddress);
        tvDistance = findViewById(R.id.tvDistance);
        tvBattery = findViewById(R.id.tvBattery);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvInitial = findViewById(R.id.tvInitial);
        btnBack = findViewById(R.id.btnBack);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        viewAvatar = findViewById(R.id.viewAvatar);

        // Set initial data
        tvTopName.setText(nickname);
        tvNickname.setText(nickname);
        tvAddress.setText(address != null ? address : "Loading...");
        tvBattery.setText(battery + "%");
        tvLastSeen.setText("Last seen: " + getTimeAgo(lastSeen));
        tvInitial.setText(nickname.substring(0, 1).toUpperCase());
        tvDistance.setText("Loading home location...");

        // Set avatar color
        setAvatarColor(color);

        // Load home location from Firebase then calculate distance
        loadHomeLocation();

        // Listen to real-time updates
        deviceRef = FirebaseDatabase.getInstance()
                .getReference("devices").child(deviceId);
        listenToUpdates();

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // View on Map
        btnViewOnMap.setOnClickListener(v -> {
            startActivity(new Intent(this, MapActivity.class));
        });

        // View History
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            intent.putExtra("deviceId", deviceId);
            intent.putExtra("nickname", nickname);
            startActivity(intent);
        });
    }

    // Load home location from Firebase saved locations
    private void loadHomeLocation() {
        locationsRef = FirebaseDatabase.getInstance()
                .getReference("saved_locations");

        locationsRef.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot locSnap : snapshot.getChildren()) {
                            String name = locSnap.child("name")
                                    .getValue(String.class);

                            // Find the Home location
                            if (name != null &&
                                    name.equalsIgnoreCase("Home")) {
                                Double lat = locSnap.child("latitude")
                                        .getValue(Double.class);
                                Double lng = locSnap.child("longitude")
                                        .getValue(Double.class);

                                if (lat != null && lng != null) {
                                    homeLat = lat;
                                    homeLng = lng;
                                    updateDistance();
                                }
                                return;
                            }
                        }
                        // No home set yet
                        tvDistance.setText("Home not set yet");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    // Calculate and show distance from home
    private void updateDistance() {
        if (homeLat == 0 && homeLng == 0) {
            tvDistance.setText("Home not set");
            return;
        }
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                homeLat, homeLng, latitude, longitude, results);
        float distanceKm = results[0] / 1000;
        tvDistance.setText(
                String.format("%.1f km from home", distanceKm));
    }

    // Listen to real-time location updates from Firebase
    private void listenToUpdates() {
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Double lat = snapshot.child("latitude")
                        .getValue(Double.class);
                Double lng = snapshot.child("longitude")
                        .getValue(Double.class);
                String addr = snapshot.child("address")
                        .getValue(String.class);
                Integer bat = snapshot.child("battery")
                        .getValue(Integer.class);
                Long seen = snapshot.child("lastSeen")
                        .getValue(Long.class);
                Boolean moving = snapshot.child("isMoving")
                        .getValue(Boolean.class);
                Float speed = snapshot.child("speed")
                        .getValue(Float.class);

                if (lat != null && lng != null) {
                    latitude = lat;
                    longitude = lng;
                    updateDistance();
                }

                if (addr != null) tvAddress.setText(addr);
                if (bat != null) tvBattery.setText(bat + "%");
                if (seen != null) tvLastSeen.setText(
                        "Last seen: " + getTimeAgo(seen));

                // Show moving status
                if (moving != null) {
                    if (moving && speed != null && speed > 0) {
                        tvSpeed.setText(
                                String.format("Moving %.1f km/h",
                                        speed * 3.6));
                    } else {
                        tvSpeed.setText("Stationary");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    private void setAvatarColor(String colorHex) {
        try {
            viewAvatar.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor(colorHex)));
        } catch (Exception e) {
            viewAvatar.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#43A047")));
        }
    }

    private String getTimeAgo(long timestamp) {
        if (timestamp == 0) return "Never";
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " mins ago";
        if (hours < 24) return hours + " hrs ago";
        return "Over a day ago";
    }
}
