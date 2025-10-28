package com.nibm.attendancetracker.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nibm.attendancetracker.R;

public class WelcomeActivity extends AppCompatActivity {

    private Button createAccountButton;
    private Button loginButton;
    private ImageView doctorImage;
    private TextView welcomeTitle;
    private TextView descriptionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void initializeViews() {
        createAccountButton = findViewById(R.id.createAccountButton);
        loginButton = findViewById(R.id.loginButton);
        doctorImage = findViewById(R.id.doctorImage);
        welcomeTitle = findViewById(R.id.welcomeTitle);
        descriptionText = findViewById(R.id.descriptionText);
    }

    private void setupClickListeners() {
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to registration/sign up activity
                Intent intent = new Intent(WelcomeActivity.this, SignUpActivity.class);
                startActivity(intent);

                // Add smooth transition animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login activity
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);

                // Add smooth transition animation
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        // Optional: Add click listener to doctor image for easter egg or info
        doctorImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // You can add some fun interaction here
                // For example, show a toast or animate the image
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Handle back button press - maybe show exit dialog
        super.onBackPressed();
        finish();
    }
}
