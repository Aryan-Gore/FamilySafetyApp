package com.aryan.family_safety;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationsActivity extends AppCompatActivity {

    RecyclerView recyclerLocations;
    Button btnBackLocations, btnAddCustom;
    DatabaseReference locationsRef;
    List<LocationModel> locationList = new ArrayList<>();
    LocationAdapter locationAdapter;

    // Request code for picking location on map
    static final int PICK_LOCATION_REQUEST = 200;
    String pendingLocationId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);

        // Connect UI
        recyclerLocations  = findViewById(R.id.recyclerLocations);
        btnBackLocations   = findViewById(R.id.btnBackLocations);
        btnAddCustom       = findViewById(R.id.btnAddCustom);

        // Firebase reference
        locationsRef = FirebaseDatabase.getInstance()
                .getReference("saved_locations");

        // Setup RecyclerView
        locationAdapter = new LocationAdapter(locationList);
        recyclerLocations.setLayoutManager(
                new LinearLayoutManager(this));
        recyclerLocations.setAdapter(locationAdapter);

        // Load locations
        loadLocations();

        // Back button
        btnBackLocations.setOnClickListener(v -> finish());

        // Add custom location
        btnAddCustom.setOnClickListener(v -> {
            showAddCustomDialog();
        });
    }

    private void loadLocations() {
        locationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                locationList.clear();

                for (DataSnapshot locSnap : snapshot.getChildren()) {
                    String id      = locSnap.getKey();
                    String name    = locSnap.child("name")
                            .getValue(String.class);
                    String address = locSnap.child("address")
                            .getValue(String.class);
                    Double lat     = locSnap.child("latitude")
                            .getValue(Double.class);
                    Double lng     = locSnap.child("longitude")
                            .getValue(Double.class);
                    Long radius    = locSnap.child("radius")
                            .getValue(Long.class);
                    Boolean alerts = locSnap.child("alerts")
                            .getValue(Boolean.class);

                    if (name == null) continue;

                    LocationModel loc = new LocationModel();
                    loc.id      = id;
                    loc.name    = name;
                    loc.address = address  != null ? address  : "Not set";
                    loc.latitude  = lat    != null ? lat      : 0;
                    loc.longitude = lng    != null ? lng      : 0;
                    loc.radius  = radius   != null ? radius   : 200;
                    loc.alerts  = alerts   != null ? alerts   : true;

                    locationList.add(loc);
                }

                // If no locations exist add defaults
                if (locationList.isEmpty()) {
                    addDefaultLocations();
                } else {
                    locationAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    // Add Home School Work by default
    private void addDefaultLocations() {
        String[] defaults = {"Home", "School", "Work"};
        for (String name : defaults) {
            String id = locationsRef.push().getKey();
            if (id == null) continue;

            Map<String, Object> loc = new HashMap<>();
            loc.put("name",      name);
            loc.put("address",   "Tap Set to pick location");
            loc.put("latitude",  0.0);
            loc.put("longitude", 0.0);
            loc.put("radius",    200L);
            loc.put("alerts",    true);

            locationsRef.child(id).setValue(loc);
        }
    }

    // Show dialog to enter custom location name
    private void showAddCustomDialog() {
        android.widget.EditText input =
                new android.widget.EditText(this);
        input.setHint("Location name e.g. Gym, Park");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Custom Location Name")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this,
                                "Enter a name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Save to Firebase then open map to pick
                    String id = locationsRef.push().getKey();
                    if (id == null) return;

                    Map<String, Object> loc = new HashMap<>();
                    loc.put("name",      name);
                    loc.put("address",   "Tap Set to pick location");
                    loc.put("latitude",  0.0);
                    loc.put("longitude", 0.0);
                    loc.put("radius",    200L);
                    loc.put("alerts",    true);

                    locationsRef.child(id).setValue(loc)
                            .addOnSuccessListener(e -> {
                                pendingLocationId = id;
                                openMapPicker(id);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Open map to pick location
    private void openMapPicker(String locationId) {
        Intent intent = new Intent(
                this, PickLocationActivity.class);
        intent.putExtra("locationId", locationId);
        startActivityForResult(intent, PICK_LOCATION_REQUEST);
    }

    // Handle result from map picker
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_LOCATION_REQUEST
                && resultCode == RESULT_OK && data != null) {
            String locationId = data.getStringExtra("locationId");
            double lat        = data.getDoubleExtra("latitude", 0);
            double lng        = data.getDoubleExtra("longitude", 0);
            String address    = data.getStringExtra("address");

            if (locationId == null) return;

            // Update Firebase with picked location
            Map<String, Object> updates = new HashMap<>();
            updates.put("latitude",  lat);
            updates.put("longitude", lng);
            updates.put("address",   address);

            locationsRef.child(locationId).updateChildren(updates);
            Toast.makeText(this,
                    "Location saved!", Toast.LENGTH_SHORT).show();
        }
    }

    // LocationModel inner class
    static class LocationModel {
        String id, name, address;
        double latitude, longitude;
        long radius;
        boolean alerts;
    }

    // LocationAdapter inner class
    class LocationAdapter extends
            RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

        List<LocationModel> list;

        LocationAdapter(List<LocationModel> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public LocationViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_location, parent, false);
            return new LocationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull LocationViewHolder holder, int position) {
            LocationModel loc = list.get(position);

            // Set icon letter
            holder.tvIcon.setText(
                    loc.name.substring(0, 1).toUpperCase());

            // Set name and address
            holder.tvName.setText(loc.name);
            holder.tvAddress.setText(loc.address);

            // Set radius text
            holder.tvRadius.setText(loc.radius + "m");
            holder.seekRadius.setProgress(
                    (int) loc.radius - 50);

            // Set alerts toggle
            holder.switchAlerts.setChecked(loc.alerts);

            // Seekbar change
            holder.seekRadius.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar,
                                                      int progress, boolean fromUser) {
                            int radius = progress + 50;
                            holder.tvRadius.setText(radius + "m");
                            if (fromUser) {
                                locationsRef.child(loc.id)
                                        .child("radius")
                                        .setValue((long) radius);
                            }
                        }
                        @Override
                        public void onStartTrackingTouch(SeekBar s) {}
                        @Override
                        public void onStopTrackingTouch(SeekBar s) {}
                    });

            // Alerts toggle
            holder.switchAlerts.setOnCheckedChangeListener(
                    (btn, isChecked) -> {
                        locationsRef.child(loc.id)
                                .child("alerts")
                                .setValue(isChecked);
                    });

            // Edit/Set button → open map picker
            holder.btnEdit.setOnClickListener(v -> {
                openMapPicker(loc.id);
            });

            // Delete button
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(
                        LocationsActivity.this)
                        .setTitle("Delete " + loc.name + "?")
                        .setMessage("This will remove the location and its zone.")
                        .setPositiveButton("Delete", (d, w) -> {
                            locationsRef.child(loc.id).removeValue();
                            Toast.makeText(LocationsActivity.this,
                                    loc.name + " deleted",
                                    Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class LocationViewHolder extends RecyclerView.ViewHolder {
            TextView tvIcon, tvName, tvAddress, tvRadius;
            SeekBar seekRadius;
            Switch switchAlerts;
            Button btnEdit, btnDelete;

            LocationViewHolder(@NonNull View itemView) {
                super(itemView);
                tvIcon       = itemView.findViewById(R.id.tvLocationIcon);
                tvName       = itemView.findViewById(R.id.tvLocationName);
                tvAddress    = itemView.findViewById(R.id.tvLocationAddress);
                tvRadius     = itemView.findViewById(R.id.tvRadius);
                seekRadius   = itemView.findViewById(R.id.seekBarRadius);
                switchAlerts = itemView.findViewById(R.id.switchAlerts);
                btnEdit      = itemView.findViewById(R.id.btnEditLocation);
                btnDelete    = itemView.findViewById(R.id.btnDeleteLocation);
            }
        }
    }
}
