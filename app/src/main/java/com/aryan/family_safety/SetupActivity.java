package com.aryan.family_safety;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class SetupActivity extends AppCompatActivity {

    EditText etNickname;
    Button btnSaveSetup;
    View viewPreviewAvatar;
    View colorGreen, colorBlue, colorRed,
            colorOrange, colorPurple;
    TextView tvPreviewInitial, tvSelectedColor;

    DatabaseReference devicesRef;
    String deviceId;

    // Default color
    String selectedColor     = "#43A047";
    String selectedColorName = "Green";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // Get unique device ID
        deviceId = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ANDROID_ID);

        devicesRef = FirebaseDatabase.getInstance()
                .getReference("devices").child(deviceId);

        // Connect UI
        etNickname       = findViewById(R.id.etNickname);
        btnSaveSetup     = findViewById(R.id.btnSaveSetup);
        viewPreviewAvatar = findViewById(R.id.viewPreviewAvatar);
        tvPreviewInitial = findViewById(R.id.tvPreviewInitial);
        tvSelectedColor  = findViewById(R.id.tvSelectedColor);
        colorGreen       = findViewById(R.id.colorGreen);
        colorBlue        = findViewById(R.id.colorBlue);
        colorRed         = findViewById(R.id.colorRed);
        colorOrange      = findViewById(R.id.colorOrange);
        colorPurple      = findViewById(R.id.colorPurple);

        // Update preview initial as user types
        etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s,
                                      int start, int before, int count) {
                if (s.length() > 0) {
                    tvPreviewInitial.setText(
                            String.valueOf(s.charAt(0))
                                    .toUpperCase());
                } else {
                    tvPreviewInitial.setText("?");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Color click listeners
        colorGreen.setOnClickListener(v ->
                selectColor("#43A047", "Green"));
        colorBlue.setOnClickListener(v ->
                selectColor("#1E88E5", "Blue"));
        colorRed.setOnClickListener(v ->
                selectColor("#E53935", "Red"));
        colorOrange.setOnClickListener(v ->
                selectColor("#FB8C00", "Orange"));
        colorPurple.setOnClickListener(v ->
                selectColor("#8E24AA", "Purple"));

        // Save button
        btnSaveSetup.setOnClickListener(v -> {
            String nickname = etNickname.getText()
                    .toString().trim();

            if (nickname.isEmpty()) {
                etNickname.setError("Enter your name");
                return;
            }

            saveToFirebase(nickname);
        });
    }

    private void selectColor(String colorHex, String colorName) {
        selectedColor     = colorHex;
        selectedColorName = colorName;

        // Update preview avatar color
        viewPreviewAvatar.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor(colorHex)));

        // Update selected color text
        tvSelectedColor.setText(colorName + " selected");
        tvSelectedColor.setTextColor(
                Color.parseColor(colorHex));

        // Show border on selected color
        resetColorBorders();
        // Highlight selected
        switch (colorName) {
            case "Green":
                colorGreen.setScaleX(1.2f);
                colorGreen.setScaleY(1.2f);
                break;
            case "Blue":
                colorBlue.setScaleX(1.2f);
                colorBlue.setScaleY(1.2f);
                break;
            case "Red":
                colorRed.setScaleX(1.2f);
                colorRed.setScaleY(1.2f);
                break;
            case "Orange":
                colorOrange.setScaleX(1.2f);
                colorOrange.setScaleY(1.2f);
                break;
            case "Purple":
                colorPurple.setScaleX(1.2f);
                colorPurple.setScaleY(1.2f);
                break;
        }
    }

    private void resetColorBorders() {
        colorGreen.setScaleX(1f);
        colorGreen.setScaleY(1f);
        colorBlue.setScaleX(1f);
        colorBlue.setScaleY(1f);
        colorRed.setScaleX(1f);
        colorRed.setScaleY(1f);
        colorOrange.setScaleX(1f);
        colorOrange.setScaleY(1f);
        colorPurple.setScaleX(1f);
        colorPurple.setScaleY(1f);
    }

    private void saveToFirebase(String nickname) {
        btnSaveSetup.setEnabled(false);

        Map<String, Object> data = new HashMap<>();
        data.put("nickname",  nickname);
        data.put("color",     selectedColor);
        data.put("deviceId",  deviceId);
        data.put("isOnline",  true);

        devicesRef.updateChildren(data)
                .addOnSuccessListener(e -> {
                    Toast.makeText(this,
                            "Welcome, " + nickname + "!",
                            Toast.LENGTH_SHORT).show();

                    // Go to MainActivity
                    startActivity(new Intent(
                            this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveSetup.setEnabled(true);
                    Toast.makeText(this,
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}