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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.LoginActivity;
import com.nibm.attendancetracker.common.NavigationHelper;
import com.nibm.attendancetracker.common.UpcomingClassAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Calendar;

public class StudentDashboardActivity extends AppCompatActivity {

    private RecyclerView rvUpcomingClasses;
    private TextView tvNoClasses;
    private UpcomingClassAdapter upcomingClassAdapter;
    private List<UpcomingClassAdapter.UpcomingClass> upcomingClasses;
    private String assignedScheduleId;
    private static final String TAG = "StudentDashboard";

    private CardView qrScannerCard;

    private CardView btnLogoutCard;
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

        NavigationHelper.setupNavigation(this, "student");

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
        btnLogoutCard = findViewById(R.id.btn_logout_card);

        // Calendar days
        dayTextViews[0] = findViewById(R.id.day1);
        dayTextViews[1] = findViewById(R.id.day2);
        dayTextViews[2] = findViewById(R.id.day3);
        dayTextViews[3] = findViewById(R.id.day4);
        dayTextViews[4] = findViewById(R.id.day5);
        dayTextViews[5] = findViewById(R.id.day6);
        dayTextViews[6] = findViewById(R.id.day7);

        rvUpcomingClasses = findViewById(R.id.rv_upcoming_classes);
        tvNoClasses = findViewById(R.id.tv_no_classes);

        upcomingClasses = new ArrayList<>();
        upcomingClassAdapter = new UpcomingClassAdapter(upcomingClasses);
        rvUpcomingClasses.setLayoutManager(new LinearLayoutManager(this));
        rvUpcomingClasses.setAdapter(upcomingClassAdapter);
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
                        assignedScheduleId = studentDoc.getString("assignedScheduleId");

                        // Use firstName if available, otherwise use full name
                        currentStudentName = (firstName != null && !firstName.isEmpty()) ? firstName : name;
                        profilePictureUrl = profileUrl;

                        Log.d(TAG, "Student loaded: " + currentStudentName);
                        Log.d(TAG, "Assigned Schedule ID: " + assignedScheduleId);

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

                        // Load upcoming classes
                        if (assignedScheduleId != null && !assignedScheduleId.isEmpty()) {
                            loadUpcomingClasses();
                        } else {
                            showNoUpcomingClasses();
                        }

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

    private void loadUpcomingClasses() {
        if (assignedScheduleId == null || assignedScheduleId.isEmpty()) {
            showNoUpcomingClasses();
            return;
        }

        db.collection("schedules")
                .document(assignedScheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        debugScheduleDocument(documentSnapshot);
                        parseAndDisplayUpcomingClasses(documentSnapshot);
                    } else {
                        Log.e(TAG, "Schedule document not found");
                        showNoUpcomingClasses();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedule", e);
                    showNoUpcomingClasses();
                });
    }

    private void parseAndDisplayUpcomingClasses(DocumentSnapshot document) {
        upcomingClasses.clear();

        // CHANGED: Use yyyy-MM-dd format to match Firestore data
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());

        Date now = new Date();

        // Strip time from current date for proper comparison
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(now);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        Date todayStart = cal.getTime();

        String todayDateStr = dateFormat.format(now);

        Log.d(TAG, "Loading classes for schedule: " + document.getId());
        Log.d(TAG, "Today's date: " + todayDateStr);

        List<Object> subjectsData = (List<Object>) document.get("subjects");
        if (subjectsData == null || subjectsData.isEmpty()) {
            Log.e(TAG, "No subjects found in schedule");
            showNoUpcomingClasses();
            return;
        }

        Log.d(TAG, "Found " + subjectsData.size() + " subjects");

        for (Object subjectObj : subjectsData) {
            if (subjectObj instanceof Map) {
                Map<String, Object> subjectMap = (Map<String, Object>) subjectObj;
                String subjectName = (String) subjectMap.get("subjectName");

                List<Object> lecturesData = (List<Object>) subjectMap.get("lectureSchedules");
                if (lecturesData != null && !lecturesData.isEmpty()) {
                    Log.d(TAG, "Subject: " + subjectName + " has " + lecturesData.size() + " lectures");

                    for (Object lectureObj : lecturesData) {
                        if (lectureObj instanceof Map) {
                            Map<String, String> lectureMap = (Map<String, String>) lectureObj;

                            String lectureDate = lectureMap.get("date");
                            String startTime = lectureMap.get("startTime");
                            String endTime = lectureMap.get("endTime");

                            Log.d(TAG, "Processing lecture: " + subjectName + " on " + lectureDate + " at " + startTime);

                            try {
                                Date classDate = dateFormat.parse(lectureDate);

                                // Only show today's and future classes (comparing dates only, not time)
                                if (classDate != null && !classDate.before(todayStart)) {
                                    UpcomingClassAdapter.UpcomingClass upcomingClass =
                                            new UpcomingClassAdapter.UpcomingClass();

                                    upcomingClass.subjectName = subjectName;
                                    upcomingClass.date = lectureDate;
                                    upcomingClass.startTime = startTime;
                                    upcomingClass.endTime = endTime;
                                    upcomingClass.formattedDate = displayDateFormat.format(classDate);

                                    // Calculate duration
                                    try {
                                        Date start = timeFormat.parse(startTime);
                                        Date end = timeFormat.parse(endTime);
                                        if (start != null && end != null) {
                                            long diffMinutes = (end.getTime() - start.getTime()) / (60 * 1000);
                                            long hours = diffMinutes / 60;
                                            long minutes = diffMinutes % 60;
                                            upcomingClass.duration = hours + "h" + (minutes > 0 ? " " + minutes + "m" : "");
                                        } else {
                                            upcomingClass.duration = "N/A";
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error calculating duration", e);
                                        upcomingClass.duration = "N/A";
                                    }

                                    // Determine status
                                    if (lectureDate.equals(todayDateStr)) {
                                        try {
                                            Date startDateTime = timeFormat.parse(startTime);
                                            Date endDateTime = timeFormat.parse(endTime);
                                            Date currentTime = timeFormat.parse(timeFormat.format(now));

                                            if (currentTime != null && startDateTime != null && endDateTime != null) {
                                                if (currentTime.after(startDateTime) && currentTime.before(endDateTime)) {
                                                    upcomingClass.status = "Ongoing";
                                                } else if (currentTime.before(startDateTime)) {
                                                    upcomingClass.status = "Today";
                                                } else {
                                                    upcomingClass.status = "Completed";
                                                }
                                            } else {
                                                upcomingClass.status = "Today";
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error determining status", e);
                                            upcomingClass.status = "Today";
                                        }
                                    } else {
                                        upcomingClass.status = "Upcoming";
                                    }

                                    upcomingClasses.add(upcomingClass);
                                    Log.d(TAG, "✓ Added upcoming class: " + subjectName + " on " + lectureDate + " - Status: " + upcomingClass.status);
                                } else {
                                    Log.d(TAG, "✗ Skipped past class: " + subjectName + " on " + lectureDate);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing date: " + lectureDate, e);
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "Subject: " + subjectName + " has no lectures");
                }
            }
        }

        Log.d(TAG, "Total upcoming classes found: " + upcomingClasses.size());

        // Sort by date and time
        Collections.sort(upcomingClasses, (c1, c2) -> {
            try {
                Date d1 = dateFormat.parse(c1.date);
                Date d2 = dateFormat.parse(c2.date);
                int dateCompare = d1.compareTo(d2);
                if (dateCompare != 0) return dateCompare;

                // If same date, sort by time
                Date t1 = timeFormat.parse(c1.startTime);
                Date t2 = timeFormat.parse(c2.startTime);
                return t1.compareTo(t2);
            } catch (Exception e) {
                return 0;
            }
        });

        // Limit to next 5 classes
        if (upcomingClasses.size() > 5) {
            upcomingClasses = new ArrayList<>(upcomingClasses.subList(0, 5));
        }

        // Update UI
        runOnUiThread(() -> {
            if (upcomingClasses.isEmpty()) {
                Log.d(TAG, "No upcoming classes to display");
                showNoUpcomingClasses();
            } else {
                Log.d(TAG, "Displaying " + upcomingClasses.size() + " upcoming classes");
                rvUpcomingClasses.setVisibility(View.VISIBLE);
                tvNoClasses.setVisibility(View.GONE);
                upcomingClassAdapter = new UpcomingClassAdapter(upcomingClasses);
                rvUpcomingClasses.setAdapter(upcomingClassAdapter);
            }
        });
    }

    private void showNoUpcomingClasses() {
        runOnUiThread(() -> {
            rvUpcomingClasses.setVisibility(View.GONE);
            tvNoClasses.setVisibility(View.VISIBLE);
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

        // Logout Button
        btnLogoutCard.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
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

        Log.d(TAG, "User logged out successfully");

        // Show logout message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to LoginActivity
        Intent intent = new Intent(StudentDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    private void debugScheduleDocument(DocumentSnapshot document) {
        Log.d(TAG, "=== SCHEDULE DEBUG ===");
        Log.d(TAG, "Document ID: " + document.getId());
        Log.d(TAG, "Batch: " + document.getString("batch"));
        Log.d(TAG, "Programme: " + document.getString("programme"));

        List<Object> subjects = (List<Object>) document.get("subjects");
        if (subjects != null) {
            Log.d(TAG, "Number of subjects: " + subjects.size());
            for (int i = 0; i < subjects.size(); i++) {
                Map<String, Object> subject = (Map<String, Object>) subjects.get(i);
                Log.d(TAG, "Subject " + (i+1) + ": " + subject.get("subjectName"));

                List<Object> lectures = (List<Object>) subject.get("lectureSchedules");
                if (lectures != null) {
                    Log.d(TAG, "  Lectures: " + lectures.size());
                    for (int j = 0; j < Math.min(lectures.size(), 3); j++) {
                        Map<String, String> lecture = (Map<String, String>) lectures.get(j);
                        Log.d(TAG, "    Lecture " + (j+1) + ": " + lecture.get("date") + " " + lecture.get("startTime"));
                    }
                }
            }
        }
        Log.d(TAG, "===================");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDateAndGreeting();

        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            loadProfileImage(profilePictureUrl);
        }

        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        assignedScheduleId = prefs.getString("assigned_schedule_id", "");
        if (assignedScheduleId != null && !assignedScheduleId.isEmpty()) {
            loadUpcomingClasses();
        }
    }
}