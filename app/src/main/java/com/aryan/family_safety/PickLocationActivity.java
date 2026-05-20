package com.aryan.family_safety;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PickLocationActivity extends AppCompatActivity {

    MapView pickMap;
    Button btnCancelPick, btnConfirmLocation;
    TextView tvPickHint, tvSelectedAddress;
    View bottomPanel;

    String locationId;
    GeoPoint selectedPoint = null;
    Marker selectedMarker  = null;
    String selectedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance()
                .setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_pick_location);

        // Get location ID passed from LocationsActivity
        locationId = getIntent().getStringExtra("locationId");

        // Connect UI
        pickMap             = findViewById(R.id.pickMap);
        btnCancelPick       = findViewById(R.id.btnCancelPick);
        btnConfirmLocation  = findViewById(R.id.btnConfirmLocation);
        tvPickHint          = findViewById(R.id.tvPickHint);
        tvSelectedAddress   = findViewById(R.id.tvSelectedAddress);
        bottomPanel         = findViewById(R.id.bottomPanel);

        // Setup map
        pickMap.setTileSource(TileSourceFactory.MAPNIK);
        pickMap.setMultiTouchControls(true);
        pickMap.getController().setZoom(15.0);

        // Center on Jabalpur by default
        pickMap.getController().setCenter(
                new GeoPoint(23.1815, 79.9864));

        // Tap on map to select location
        MapEventsOverlay mapEventsOverlay =
                new MapEventsOverlay(new MapEventsReceiver() {
                    @Override
                    public boolean singleTapConfirmedHelper(GeoPoint p) {
                        selectLocation(p);
                        return true;
                    }

                    @Override
                    public boolean longPressHelper(GeoPoint p) {
                        return false;
                    }
                });

        pickMap.getOverlays().add(mapEventsOverlay);

        // Cancel button
        btnCancelPick.setOnClickListener(v -> finish());

        // Confirm button
        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedPoint == null) return;

            // Send result back to LocationsActivity
            Intent result = new Intent();
            result.putExtra("locationId", locationId);
            result.putExtra("latitude",   selectedPoint.getLatitude());
            result.putExtra("longitude",  selectedPoint.getLongitude());
            result.putExtra("address",    selectedAddress);
            setResult(RESULT_OK, result);
            finish();
        });
    }

    private void selectLocation(GeoPoint point) {
        selectedPoint = point;

        // Remove old marker
        if (selectedMarker != null) {
            pickMap.getOverlays().remove(selectedMarker);
        }

        // Add new marker
        selectedMarker = new Marker(pickMap);
        selectedMarker.setPosition(point);
        selectedMarker.setAnchor(
                Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedMarker.setTitle("Selected location");
        pickMap.getOverlays().add(selectedMarker);
        pickMap.invalidate();

        // Show bottom panel
        bottomPanel.setVisibility(View.VISIBLE);
        tvPickHint.setVisibility(View.GONE);
        tvSelectedAddress.setText("Getting address...");

        // Get address in background thread
        new Thread(() -> {
            selectedAddress = getAddressFromLocation(
                    point.getLatitude(), point.getLongitude());

            // Update UI on main thread
            runOnUiThread(() -> {
                tvSelectedAddress.setText(selectedAddress);
            });
        }).start();
    }

    private String getAddressFromLocation(
            double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(
                    this, Locale.getDefault());
            List<Address> addresses =
                    geocoder.getFromLocation(lat, lng, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                if (addr.getSubLocality() != null)
                    sb.append(addr.getSubLocality()).append(", ");
                if (addr.getLocality() != null)
                    sb.append(addr.getLocality());
                if (addr.getAdminArea() != null
                        && sb.length() == 0)
                    sb.append(addr.getAdminArea());

                return sb.length() > 0
                        ? sb.toString() : "Selected location";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Selected location";
    }

    @Override
    public void onResume() {
        super.onResume();
        pickMap.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        pickMap.onPause();
    }
}
