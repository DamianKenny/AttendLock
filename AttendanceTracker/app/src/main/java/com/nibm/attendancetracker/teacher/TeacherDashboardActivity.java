package com.nibm.attendancetracker.teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class TeacherDashboardActivity extends AppCompatActivity {

    private CardView cardManageAccount, cardDisplayQR, cardStudentAttendance;
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

        firestore = FirebaseFirestore.getInstance();
        initializeViews();
        setupClickListeners();

        // Get logged-in email from SharedPreferences
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
                    } else {
                        Toast.makeText(this, "Teacher profile not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
