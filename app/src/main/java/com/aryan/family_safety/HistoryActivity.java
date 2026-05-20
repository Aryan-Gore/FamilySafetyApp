package com.aryan.family_safety;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import org.osmdroid.views.overlay.Polyline;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    TextView tvTitle, tvSelectedDate;
    Button btnBack, btnPickDate;
    RecyclerView recyclerHistory;
    MapView historyMap;

    String deviceId, nickname;
    String selectedDate;
    DatabaseReference historyRef;
    List<HistoryPoint> historyList = new ArrayList<>();
    HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_history);

        // Get data from intent
        deviceId = getIntent().getStringExtra("deviceId");
        nickname = getIntent().getStringExtra("nickname");

        // Connect UI
        tvTitle         = findViewById(R.id.tvTitle);
        tvSelectedDate  = findViewById(R.id.tvSelectedDate);
        btnBack         = findViewById(R.id.btnBack);
        btnPickDate     = findViewById(R.id.btnPickDate);
        recyclerHistory = findViewById(R.id.recyclerHistory);
        historyMap      = findViewById(R.id.historyMap);

        tvTitle.setText(nickname + " History");

        // Setup map
        historyMap.setTileSource(TileSourceFactory.MAPNIK);
        historyMap.setMultiTouchControls(true);
        historyMap.getController().setZoom(13.0);

        // Setup RecyclerView
        adapter = new HistoryAdapter(historyList);
        recyclerHistory.setLayoutManager(
                new LinearLayoutManager(this));
        recyclerHistory.setAdapter(adapter);

        // Default to today
        selectedDate = new SimpleDateFormat(
                "yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        tvSelectedDate.setText(selectedDate);

        // Firebase reference
        historyRef = FirebaseDatabase.getInstance()
                .getReference("history").child(deviceId);

        // Load today's history
        loadHistory(selectedDate);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Date picker button
        btnPickDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(
                    Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day);
            tvSelectedDate.setText(selectedDate);
            loadHistory(selectedDate);
        },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadHistory(String date) {
        historyRef.child(date)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        historyList.clear();
                        historyMap.getOverlays().clear();

                        List<GeoPoint> routePoints = new ArrayList<>();

                        for (DataSnapshot pointSnap : snapshot.getChildren()) {
                            Double lat      = pointSnap.child("latitude")
                                    .getValue(Double.class);
                            Double lng      = pointSnap.child("longitude")
                                    .getValue(Double.class);
                            String address  = pointSnap.child("address")
                                    .getValue(String.class);
                            Long timestamp  = pointSnap.child("timestamp")
                                    .getValue(Long.class);

                            if (lat == null || lng == null) continue;
                            if (address   == null) address   = "Unknown";
                            if (timestamp == null) timestamp = 0L;

                            // Add to list
                            historyList.add(
                                    new HistoryPoint(lat, lng, address, timestamp));

                            // Add to route
                            routePoints.add(new GeoPoint(lat, lng));

                            // Add marker on map
                            Marker marker = new Marker(historyMap);
                            marker.setPosition(new GeoPoint(lat, lng));
                            marker.setTitle(formatTime(timestamp));
                            marker.setSnippet(address);
                            historyMap.getOverlays().add(marker);
                        }

                        // Draw route line on map
                        if (routePoints.size() > 1) {
                            Polyline route = new Polyline();
                            route.setPoints(routePoints);
                            route.getOutlinePaint().setColor(
                                    android.graphics.Color.BLUE);
                            route.getOutlinePaint().setStrokeWidth(5f);
                            historyMap.getOverlays().add(route);

                            // Center map on first point
                            historyMap.getController()
                                    .setCenter(routePoints.get(0));
                        }

                        adapter.notifyDataSetChanged();
                        historyMap.invalidate();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "Unknown time";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(timestamp));
    }

    @Override
    public void onResume() {
        super.onResume();
        historyMap.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        historyMap.onPause();
    }

    // HistoryPoint model
    static class HistoryPoint {
        double lat, lng;
        String address;
        long timestamp;

        HistoryPoint(double lat, double lng,
                     String address, long timestamp) {
            this.lat       = lat;
            this.lng       = lng;
            this.address   = address;
            this.timestamp = timestamp;
        }
    }

    // RecyclerView Adapter for history list
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

        List<HistoryPoint> list;

        HistoryAdapter(List<HistoryPoint> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public HistoryViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull HistoryViewHolder holder, int position) {
            HistoryPoint point = list.get(position);
            holder.tvTime.setText(formatTime(point.timestamp));
            holder.tvAddress.setText(point.address);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvAddress;

            HistoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime    = itemView.findViewById(R.id.tvHistoryTime);
                tvAddress = itemView.findViewById(R.id.tvHistoryAddress);
            }
        }
    }
}