package com.aryan.family_safety;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    ProgressBar progressBar;
    TextView tvError;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // If already logged in → go to MainActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
            return;
        }

        // Connect UI elements
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvError     = findViewById(R.id.tvError);

        // Login button click
        btnLogin.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validation
            if (email.isEmpty()) {
                etEmail.setError("Enter email");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Enter password");
                return;
            }

            // Show loading
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            tvError.setVisibility(View.GONE);

            // Firebase login
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        goToMain();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        tvError.setVisibility(View.VISIBLE);
                        tvError.setText("Login failed: " + e.getMessage());
                    });
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // close login screen so user can't go back to it
    }
}
