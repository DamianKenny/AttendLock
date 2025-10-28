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

    private Button loginButton;
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
        loginButton = findViewById(R.id.loginButton);
        welcomeTitle = findViewById(R.id.welcomeTitle);
        descriptionText = findViewById(R.id.descriptionText);
    }

    private void setupClickListeners() {

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);

                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
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
