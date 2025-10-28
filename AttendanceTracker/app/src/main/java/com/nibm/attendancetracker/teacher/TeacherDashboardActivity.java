package com.nibm.attendancetracker.teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.LoginActivity;
import com.nibm.attendancetracker.common.NavigationHelper;

public class TeacherDashboardActivity extends AppCompatActivity {

    private static final String TAG = "TeacherDashboard";

    private CardView cardManageAccount, cardDisplayQR, cardStudentAttendance;
    private CardView btnLogoutCard;
    private ImageView profileImageView;
    private TextView greetingTextView;
    private FirebaseFirestore firestore;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.teacher_dashboard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        NavigationHelper.setupNavigation(this, "teacher");

        firestore = FirebaseFirestore.getInstance();
        initializeViews();
        setupClickListeners();

        SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
        currentUserEmail = sharedPreferences.getString("current_user_email", null);

        if (currentUserEmail != null) {
            loadTeacherProfile(currentUserEmail);
        } else {
            Toast.makeText(this, "No logged-in user found!", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        cardManageAccount = findViewById(R.id.card_generate_qr);
        cardDisplayQR = findViewById(R.id.card_attendance_records);
        cardStudentAttendance = findViewById(R.id.card_student_history);
        btnLogoutCard = findViewById(R.id.btn_logout_card);

        greetingTextView = findViewById(R.id.teacher_greeting);
        profileImageView = findViewById(R.id.teacher_profile_image);
    }

    private void setupClickListeners() {
        cardManageAccount.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, ProfileTeacherActivity.class);
            startActivity(intent);
        });

        cardDisplayQR.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, TeacherQRActivity.class);
            startActivity(intent);
        });

        cardStudentAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboardActivity.this, TeacherViewStudentAttendanceActivity.class);
            startActivity(intent);
        });

        // Profile image click
        if (profileImageView != null) {
            profileImageView.setOnClickListener(v -> {
                Intent intent = new Intent(TeacherDashboardActivity.this, ProfileTeacherActivity.class);
                startActivity(intent);
            });
        }

        // Logout button
        if (btnLogoutCard != null) {
            btnLogoutCard.setOnClickListener(v -> {
                showLogoutConfirmationDialog();
            });
        }
    }

    private void loadTeacherProfile(String email) {
        firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                        String name = document.getString("name");
                        String profileUrl = document.getString("profilePictureUrl");

                        // Update UI
                        if (greetingTextView != null && name != null) {
                            greetingTextView.setText("Hello, " + name.split(" ")[0]);
                        }

                        if (profileImageView != null && profileUrl != null && !profileUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_person_filled)
                                    .error(R.drawable.ic_person_filled)
                                    .circleCrop()
                                    .into(profileImageView);
                        }

                        // Cache profile data
                        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("teacher_name", name);
                        editor.putString("profile_picture_url", profileUrl != null ? profileUrl : "");
                        editor.apply();

                    } else {
                        Toast.makeText(this, "Teacher profile not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading teacher profile", e);
                });
    }

    private void showLogoutConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(R.drawable.ic_logout)
                .show();
    }

    private void performLogout() {
        // Clear SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Clear any other session data if needed
        SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor loginEditor = loginPrefs.edit();
        loginEditor.clear();
        loginEditor.apply();

        Log.d(TAG, "Teacher logged out successfully");

        // Show logout message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to LoginActivity
        Intent intent = new Intent(TeacherDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload profile if needed
        if (currentUserEmail != null) {
            SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
            String cachedName = prefs.getString("teacher_name", null);
            String cachedProfileUrl = prefs.getString("profile_picture_url", "");

            // Update UI with cached data
            if (greetingTextView != null && cachedName != null) {
                greetingTextView.setText("Hello, " + cachedName.split(" ")[0]);
            }

            if (profileImageView != null && !cachedProfileUrl.isEmpty()) {
                Glide.with(this)
                        .load(cachedProfileUrl)
                        .placeholder(R.drawable.ic_person_filled)
                        .error(R.drawable.ic_person_filled)
                        .circleCrop()
                        .into(profileImageView);
            }
        }
    }
}