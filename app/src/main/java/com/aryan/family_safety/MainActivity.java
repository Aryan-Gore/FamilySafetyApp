package com.aryan.family_safety;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import android.provider.Settings;
public class MainActivity extends AppCompatActivity {

    Button btnStartSharing, btnStopSharing, btnViewMap, btnLocations;
    TextView tvStatus, tvMemberCount;
    RecyclerView recyclerMembers;
    FirebaseAuth mAuth;
    SharedPreferences prefs;
    DatabaseReference devicesRef;
    List<DeviceModel> memberList = new ArrayList<>();
    MemberAdapter memberAdapter;

    private static final int LOCATION_PERMISSION_REQUEST  = 100;
    private static final int BACKGROUND_LOCATION_REQUEST  = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        checkIfSetupNeeded();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth  = FirebaseAuth.getInstance();
        prefs  = getSharedPreferences("FamilySafety", MODE_PRIVATE);

        // Connect UI
        btnStartSharing = findViewById(R.id.btnStartSharing);
        btnStopSharing  = findViewById(R.id.btnStopSharing);
        btnViewMap      = findViewById(R.id.btnViewMap);
        btnLocations    = findViewById(R.id.btnLocations);
        tvStatus        = findViewById(R.id.tvStatus);
        tvMemberCount   = findViewById(R.id.tvMemberCount);
        recyclerMembers = findViewById(R.id.recyclerMembers);

        // Setup RecyclerView
        memberAdapter = new MemberAdapter(this, memberList);
        recyclerMembers.setLayoutManager(
                new LinearLayoutManager(this));
        recyclerMembers.setAdapter(memberAdapter);

        // Firebase reference
        devicesRef = FirebaseDatabase.getInstance()
                .getReference("devices");

        // Check if already sharing
        boolean isSharing = prefs.getBoolean("isSharing", false);
        updateUI(isSharing);

        // Load family members
        loadMembers();

        // Start Sharing
        btnStartSharing.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                startLocationService();
            }
        });

        // Stop Sharing
        btnStopSharing.setOnClickListener(v -> {
            stopLocationService();
        });

        // View Map
        btnViewMap.setOnClickListener(v -> {
            startActivity(new Intent(this, MapActivity.class));
        });

        // Saved Locations
        btnLocations.setOnClickListener(v -> {
            startActivity(new Intent(this, LocationsActivity.class));
        });
    }

    private void loadMembers() {
        devicesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                memberList.clear();

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

                    if (nickname == null) continue;

                    DeviceModel device  = new DeviceModel();
                    device.deviceId     = deviceId;
                    device.nickname     = nickname;
                    device.color        = color    != null ? color    : "#43A047";
                    device.latitude     = lat      != null ? lat      : 0;
                    device.longitude    = lng      != null ? lng      : 0;
                    device.address      = address  != null ? address  : "Location unavailable";
                    device.lastSeen     = lastSeen != null ? lastSeen : 0;
                    device.battery      = battery  != null ? battery  : 0;
                    device.isMoving     = isMoving != null && isMoving;

                    memberList.add(device);
                }

                memberAdapter.notifyDataSetChanged();
                tvMemberCount.setText(memberList.size()
                        + " member" + (memberList.size() != 1 ? "s" : "")
                        + " tracked");
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                showBackgroundPermissionDialog();
                return false;
            }
        }
        return true;
    }

    private void showBackgroundPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Background Location Needed")
                .setMessage("Please select 'Allow all the time' so location works in background.")
                .setPositiveButton("OK", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            },
                            BACKGROUND_LOCATION_REQUEST);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    showBackgroundPermissionDialog();
                } else {
                    startLocationService();
                }
            } else {
                Toast.makeText(this,
                        "Location permission required!",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == BACKGROUND_LOCATION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(this,
                        "Background location needed for tracking!",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        prefs.edit().putBoolean("isSharing", true).apply();
        updateUI(true);
        Toast.makeText(this,
                "Location sharing started!", Toast.LENGTH_SHORT).show();
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        stopService(serviceIntent);
        prefs.edit().putBoolean("isSharing", false).apply();
        updateUI(false);
        Toast.makeText(this,
                "Location sharing stopped!", Toast.LENGTH_SHORT).show();
    }

    private void updateUI(boolean isSharing) {
        if (isSharing) {
            btnStartSharing.setVisibility(View.GONE);
            btnStopSharing.setVisibility(View.VISIBLE);
            tvStatus.setText("Location sharing is ON");
            tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            btnStartSharing.setVisibility(View.VISIBLE);
            btnStopSharing.setVisibility(View.GONE);
            tvStatus.setText("Location sharing is OFF");
            tvStatus.setTextColor(Color.parseColor("#757575"));
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkIfSetupNeeded() {
        String deviceId = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ANDROID_ID);

        FirebaseDatabase.getInstance()
                .getReference("devices")
                .child(deviceId)
                .child("nickname")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()
                            || snapshot.getValue() == null) {
                        // No nickname set → go to setup
                        startActivity(new Intent(
                                this, SetupActivity.class));
                        finish();
                    }
                });
    }

}