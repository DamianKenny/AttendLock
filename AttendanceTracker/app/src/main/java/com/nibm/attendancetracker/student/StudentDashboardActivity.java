package com.nibm.attendancetracker.student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final String TAG = "StudentDashboard";

    private CardView qrScannerCard;
    private LinearLayout btnSchedule, btnAccount, btnAttendanceHistory;
    private ImageView profileImage;
    private TextView greetingText, dateText;
    private TextView[] dayTextViews = new TextView[7];

    private FirebaseFirestore db;
    private String currentStudentEmail;
    private String currentStudentName = "Student";
    private String profilePictureUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_dashboard);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        updateDateAndGreeting();
        setupCalendar();
        loadStudentProfile();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();

        // Get student email from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        currentStudentEmail = prefs.getString("current_user_email", "");

        // Try to get cached student name
        currentStudentName = prefs.getString("student_name", "Student");
        profilePictureUrl = prefs.getString("profile_picture_url", "");

        Log.d(TAG, "Student Email: " + currentStudentEmail);
    }

    private void initializeViews() {
        // Header views
        greetingText = findViewById(R.id.greetingText);
        dateText = findViewById(R.id.dateText);
        profileImage = findViewById(R.id.profileImage);

        // Action cards
        qrScannerCard = findViewById(R.id.qrScannerCard);
        btnSchedule = findViewById(R.id.btn_schedule);
        btnAccount = findViewById(R.id.btn_account);
        btnAttendanceHistory = findViewById(R.id.btn_attendance_history);

        // Calendar days
        dayTextViews[0] = findViewById(R.id.day1);
        dayTextViews[1] = findViewById(R.id.day2);
        dayTextViews[2] = findViewById(R.id.day3);
        dayTextViews[3] = findViewById(R.id.day4);
        dayTextViews[4] = findViewById(R.id.day5);
        dayTextViews[5] = findViewById(R.id.day6);
        dayTextViews[6] = findViewById(R.id.day7);
    }

    private void loadStudentProfile() {
        if (currentStudentEmail == null || currentStudentEmail.isEmpty()) {
            Log.e(TAG, "No student email found");
            return;
        }

        // Show cached data immediately if available
        if (!currentStudentName.equals("Student")) {
            updateGreetingWithName(currentStudentName);
        }
        if (!profilePictureUrl.isEmpty()) {
            loadProfileImage(profilePictureUrl);
        }

        // Fetch fresh data from Firestore
        db.collection("users")
                .whereEqualTo("email", currentStudentEmail)
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot studentDoc = queryDocumentSnapshots.getDocuments().get(0);

                        // Get student data
                        String name = studentDoc.getString("name");
                        String firstName = studentDoc.getString("firstName");
                        String profileUrl = studentDoc.getString("profilePictureUrl");
                        String studentId = studentDoc.getString("studentId");
                        String batch = studentDoc.getString("batch");
                        String assignedScheduleId = studentDoc.getString("assignedScheduleId");

                        // Use firstName if available, otherwise use full name
                        currentStudentName = (firstName != null && !firstName.isEmpty()) ? firstName : name;
                        profilePictureUrl = profileUrl;

                        Log.d(TAG, "Student loaded: " + currentStudentName);
                        Log.d(TAG, "Profile URL: " + profilePictureUrl);

                        // Update UI
                        updateGreetingWithName(currentStudentName);

                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            loadProfileImage(profilePictureUrl);
                        }

                        // Save to SharedPreferences for quick access
                        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("student_name", currentStudentName);
                        editor.putString("full_name", name);
                        editor.putString("student_id", studentId);
                        editor.putString("batch", batch);
                        editor.putString("profile_picture_url", profilePictureUrl != null ? profilePictureUrl : "");
                        editor.putString("assigned_schedule_id", assignedScheduleId != null ? assignedScheduleId : "");
                        editor.apply();

                    } else {
                        Log.e(TAG, "Student document not found");
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading student profile", e);
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateGreetingWithName(String name) {
        runOnUiThread(() -> {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH", Locale.getDefault());
            int hour = Integer.parseInt(timeFormat.format(new Date()));

            String greeting;
            if (hour < 12) {
                greeting = "Good Morning";
            } else if (hour < 17) {
                greeting = "Good Afternoon";
            } else {
                greeting = "Good Evening";
            }

            greetingText.setText(greeting + ", " + name);
        });
    }

    private void loadProfileImage(String imageUrl) {
        if (profileImage == null || imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        runOnUiThread(() -> {
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .placeholder(R.drawable.ic_person_outline) // Placeholder while loading
                    .error(R.drawable.ic_person_outline) // Error image
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            Glide.with(this)
                    .load(imageUrl)
                    .apply(options)
                    .into(profileImage);

            Log.d(TAG, "Profile image loaded successfully");
        });
    }

    private void setupClickListeners() {
        // Profile image click - Navigate to profile
        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, StudentProfileActivity.class);
            startActivity(intent);
        });

        // QR Scanner - Navigate to QR Scanner activity
        qrScannerCard.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, QRScannerActivity.class);
            startActivity(intent);
        });

        // View Schedule - Navigate to Class Schedule
        btnSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, ClassScheduleActivity.class);
            startActivity(intent);
        });

        // View Account - Navigate to Student Profile
        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, StudentProfileActivity.class);
            startActivity(intent);
        });

        // View Attendance History - Navigate to Attendance History
        btnAttendanceHistory.setOnClickListener(v -> {
            Intent intent = new Intent(StudentDashboardActivity.this, AttendanceHistoryActivity.class);
            startActivity(intent);
        });

    }

    private void updateDateAndGreeting() {
        // Set current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMM", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        dateText.setText(currentDate);

        // Update greeting with current student name
        updateGreetingWithName(currentStudentName);
    }

    private void setupCalendar() {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.util.Calendar today = java.util.Calendar.getInstance();

        // Move calendar to Sunday of this week
        int currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        int daysFromSunday = currentDayOfWeek - java.util.Calendar.SUNDAY;
        calendar.add(java.util.Calendar.DAY_OF_MONTH, -daysFromSunday);

        for (int i = 0; i < 7; i++) {
            String day = dayFormat.format(calendar.getTime());
            String date = dateFormat.format(calendar.getTime());
            dayTextViews[i].setText(day + "\n" + date);

            // Highlight current day
            if (calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) &&
                    calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR)) {
                dayTextViews[i].setBackgroundResource(R.drawable.today_background);
                dayTextViews[i].setTextColor(getColor(android.R.color.white));
            } else {
                dayTextViews[i].setBackgroundResource(R.drawable.normal_day_background);
                dayTextViews[i].setTextColor(getColor(android.R.color.black));
            }

            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDateAndGreeting();

        // Reload profile image if URL exists
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            loadProfileImage(profilePictureUrl);
        }
    }
}