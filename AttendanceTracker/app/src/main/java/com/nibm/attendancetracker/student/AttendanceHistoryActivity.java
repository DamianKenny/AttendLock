package com.nibm.attendancetracker.student;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.NavigationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceHistoryActivity extends AppCompatActivity {

    private ImageView backButton;
    private FirebaseFirestore firestore;
    private String currentStudentEmail;
    private String currentStudentId;
    private String assignedScheduleId;

    private TextView tvPresentCount, tvAbsentCount, tvLateCount, tvTotalClasses;
    private ProgressBar overallProgressBar;
    private LinearLayout subjectListContainer;
    private View loadingView;

    private static final String TAG = "AttendanceHistory";

    // Data structures
    private ScheduleData scheduleData;
    private Map<String, List<AttendanceRecord>> attendanceBySubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attendance_history);

        initializeViews();
        setupClickListeners();
        initializeFirebase();

        // Setup navigation with role
        NavigationHelper.setupNavigation(this, "student");

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        loadStudentData();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvTotalClasses = findViewById(R.id.tvTotalClasses);
        overallProgressBar = findViewById(R.id.overallProgressBar);
        subjectListContainer = findViewById(R.id.subjectListContainer);
    }

    private void initializeFirebase() {
        firestore = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        currentStudentEmail = prefs.getString("current_user_email", "");
        currentStudentId = prefs.getString("student_id", "");

        Log.d(TAG, "Student Email: " + currentStudentEmail);
        Log.d(TAG, "Student ID: " + currentStudentId);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadStudentData() {
        if (currentStudentEmail == null || currentStudentEmail.isEmpty()) {
            Toast.makeText(this, "Student data not available", Toast.LENGTH_SHORT).show();
            showNoDataMessage();
            return;
        }

        showLoading(true);

        // Fetch student document to get assignedScheduleId
        firestore.collection("users")
                .whereEqualTo("email", currentStudentEmail)
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot studentDoc = queryDocumentSnapshots.getDocuments().get(0);
                        assignedScheduleId = studentDoc.getString("assignedScheduleId");
                        currentStudentId = studentDoc.getString("studentId");

                        Log.d(TAG, "Assigned Schedule ID: " + assignedScheduleId);

                        if (assignedScheduleId != null && !assignedScheduleId.isEmpty()) {
                            loadScheduleData();
                        } else {
                            showLoading(false);
                            showNoScheduleMessage();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading student data", e);
                    Toast.makeText(this, "Error loading student data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadScheduleData() {
        firestore.collection("schedules")
                .document(assignedScheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        scheduleData = parseScheduleData(documentSnapshot);
                        loadAttendanceData();
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Schedule not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading schedule", e);
                    Toast.makeText(this, "Error loading schedule", Toast.LENGTH_SHORT).show();
                });
    }

    private ScheduleData parseScheduleData(DocumentSnapshot document) {
        ScheduleData schedule = new ScheduleData();
        schedule.id = document.getId();
        schedule.batch = document.getString("batch");
        schedule.programme = document.getString("programme");
        schedule.faculty = document.getString("faculty");
        schedule.subjects = new ArrayList<>();

        List<Object> subjectsData = (List<Object>) document.get("subjects");
        if (subjectsData != null) {
            for (Object subjectObj : subjectsData) {
                if (subjectObj instanceof Map) {
                    Map<String, Object> subjectMap = (Map<String, Object>) subjectObj;

                    SubjectData subject = new SubjectData();
                    subject.name = (String) subjectMap.get("subjectName");
                    subject.totalClasses = getIntValue(subjectMap.get("totalClasses"));
                    subject.credits = getIntValue(subjectMap.get("credits"));
                    subject.lectures = new ArrayList<>();

                    List<Object> lecturesData = (List<Object>) subjectMap.get("lectureSchedules");
                    if (lecturesData != null) {
                        for (int i = 0; i < lecturesData.size(); i++) {
                            if (lecturesData.get(i) instanceof Map) {
                                Map<String, String> lectureMap = (Map<String, String>) lecturesData.get(i);

                                LectureData lecture = new LectureData();
                                lecture.id = "L" + (i + 1);
                                lecture.number = i + 1;
                                lecture.date = lectureMap.get("date");
                                lecture.startTime = lectureMap.get("startTime");
                                lecture.endTime = lectureMap.get("endTime");

                                subject.lectures.add(lecture);
                            }
                        }
                    }

                    schedule.subjects.add(subject);
                }
            }
        }

        return schedule;
    }

    private void loadAttendanceData() {
        // Load attendance records for this student and schedule
        firestore.collection("schedules")
                .document(assignedScheduleId)
                .collection("attendance")
                .whereEqualTo("studentEmail", currentStudentEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceBySubject = new HashMap<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        AttendanceRecord record = new AttendanceRecord();
                        record.studentId = document.getString("studentId");
                        record.studentEmail = document.getString("studentEmail");
                        record.subjectName = document.getString("subjectName");
                        record.lectureId = document.getString("lectureId");
                        record.lectureNumber = getIntValue(document.get("lectureNumber"));
                        record.date = document.getString("date");
                        record.startTime = document.getString("startTime");
                        record.endTime = document.getString("endTime");
                        record.status = document.getString("status");
                        record.markedAt = getLongValue(document.get("markedAt"));

                        // Group by subject
                        if (!attendanceBySubject.containsKey(record.subjectName)) {
                            attendanceBySubject.put(record.subjectName, new ArrayList<>());
                        }
                        attendanceBySubject.get(record.subjectName).add(record);
                    }

                    showLoading(false);
                    calculateAndDisplayStats();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading attendance", e);
                    Toast.makeText(this, "Error loading attendance data", Toast.LENGTH_SHORT).show();
                    calculateAndDisplayStats(); // Still show schedule even if no attendance
                });
    }

    private void calculateAndDisplayStats() {
        if (scheduleData == null || scheduleData.subjects == null) {
            showNoDataMessage();
            return;
        }

        int totalLectures = 0;
        int totalAttended = 0;
        int totalAbsent = 0;

        // Calculate totals across all subjects
        for (SubjectData subject : scheduleData.subjects) {
            int subjectTotalLectures = subject.lectures.size();
            totalLectures += subjectTotalLectures;

            List<AttendanceRecord> subjectAttendance = attendanceBySubject.get(subject.name);
            if (subjectAttendance != null) {
                for (AttendanceRecord record : subjectAttendance) {
                    if ("present".equalsIgnoreCase(record.status)) {
                        totalAttended++;
                    }
                }
            }
        }

        totalAbsent = totalLectures - totalAttended;

        // Calculate percentages
        int presentPercentage = totalLectures > 0 ? (totalAttended * 100) / totalLectures : 0;
        int absentPercentage = totalLectures > 0 ? (totalAbsent * 100) / totalLectures : 0;

        // Update overall stats UI
        updateOverallStatsUI(presentPercentage, absentPercentage, 0, totalAttended, totalLectures);

        // Display subject-wise breakdown
        displaySubjectBreakdown();
    }

    private void updateOverallStatsUI(int presentPercentage, int absentPercentage,
                                      int latePercentage, int attendedCount, int totalCount) {
        runOnUiThread(() -> {
            tvPresentCount.setText(presentPercentage + "%");
            tvAbsentCount.setText(absentPercentage + "%");
            tvLateCount.setText(latePercentage + "%");
            tvTotalClasses.setText(attendedCount + " of " + totalCount + " classes attended");
            overallProgressBar.setProgress(presentPercentage);
        });
    }

    private void displaySubjectBreakdown() {
        runOnUiThread(() -> {
            subjectListContainer.removeAllViews();

            if (scheduleData.subjects.isEmpty()) {
                showNoSubjectsMessage();
                return;
            }

            for (SubjectData subject : scheduleData.subjects) {
                createSubjectCard(subject);
            }
        });
    }

    private void createSubjectCard(SubjectData subject) {
        // Calculate attendance for this subject
        int totalLectures = subject.lectures.size();
        int attendedLectures = 0;

        List<AttendanceRecord> subjectAttendance = attendanceBySubject.get(subject.name);
        if (subjectAttendance != null) {
            for (AttendanceRecord record : subjectAttendance) {
                if ("present".equalsIgnoreCase(record.status)) {
                    attendedLectures++;
                }
            }
        }

        int percentage = totalLectures > 0 ? (attendedLectures * 100) / totalLectures : 0;

        // Create card view
        CardView subjectCard = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        subjectCard.setLayoutParams(cardParams);
        subjectCard.setCardBackgroundColor(0xFF2A2A2A); // #2A2A2A
        subjectCard.setRadius(dpToPx(12));
        subjectCard.setCardElevation(0);

        // Main container
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Color indicator
        View colorIndicator = new View(this);
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                dpToPx(8),
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        indicatorParams.setMargins(0, 0, dpToPx(16), 0);
        colorIndicator.setLayoutParams(indicatorParams);
        colorIndicator.setBackgroundColor(getSubjectColor(subject.name, percentage));

        // Content container
        LinearLayout contentLayout = new LinearLayout(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        contentLayout.setLayoutParams(contentParams);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // Subject name
        TextView subjectNameView = new TextView(this);
        subjectNameView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        subjectNameView.setText(subject.name);
        subjectNameView.setTextColor(0xFFFFFFFF);
        subjectNameView.setTextSize(16);
        subjectNameView.setTypeface(null, android.graphics.Typeface.BOLD);
        subjectNameView.setPadding(0, 0, 0, dpToPx(4));

        // Class count
        TextView classCountView = new TextView(this);
        classCountView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        classCountView.setText(attendedLectures + " of " + totalLectures + " classes");
        classCountView.setTextColor(0xFF999999);
        classCountView.setTextSize(14);
        classCountView.setPadding(0, 0, 0, dpToPx(8));

        // Progress bar
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                dpToPx(120),
                dpToPx(4)
        );
        progressBar.setLayoutParams(progressParams);
        progressBar.setProgress(percentage);
        progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.mini_progress_bar));

        // Add views to content layout
        contentLayout.addView(subjectNameView);
        contentLayout.addView(classCountView);
        contentLayout.addView(progressBar);

        // Percentage container
        LinearLayout percentageLayout = new LinearLayout(this);
        percentageLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        percentageLayout.setOrientation(LinearLayout.VERTICAL);
        percentageLayout.setGravity(android.view.Gravity.END);

        // Percentage text
        TextView percentageView = new TextView(this);
        percentageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        percentageView.setText(percentage + "%");
        percentageView.setTextColor(getAttendanceColor(percentage));
        percentageView.setTextSize(18);
        percentageView.setTypeface(null, android.graphics.Typeface.BOLD);
        percentageView.setPadding(0, 0, 0, dpToPx(4));

        // Arrow icon
        ImageView arrowIcon = new ImageView(this);
        arrowIcon.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(24),
                dpToPx(24)
        ));
        arrowIcon.setImageResource(R.drawable.ic_arrow_forward);
        arrowIcon.setColorFilter(0xFF666666);

        // Add views to percentage layout
        percentageLayout.addView(percentageView);
        percentageLayout.addView(arrowIcon);

        // Add all views to main layout
        mainLayout.addView(colorIndicator);
        mainLayout.addView(contentLayout);
        mainLayout.addView(percentageLayout);

        // Add main layout to card
        subjectCard.addView(mainLayout);

        // Add card to container
        subjectListContainer.addView(subjectCard);

        // Add click listener for subject details
        int finalAttendedLectures = attendedLectures;
        int finalTotalLectures = totalLectures;
        subjectCard.setOnClickListener(v -> {
            showSubjectDetails(subject, finalAttendedLectures, finalTotalLectures);
        });
    }

    private void showSubjectDetails(SubjectData subject, int attended, int total) {
        // You can create a detailed view dialog or new activity
        StringBuilder details = new StringBuilder();
        details.append(subject.name).append("\n\n");
        details.append("Total Lectures: ").append(total).append("\n");
        details.append("Attended: ").append(attended).append("\n");
        details.append("Absent: ").append(total - attended).append("\n");
        details.append("Credits: ").append(subject.credits).append("\n\n");

        details.append("Lecture Details:\n");
        for (LectureData lecture : subject.lectures) {
            details.append("Lecture ").append(lecture.number)
                    .append(" - ").append(lecture.date)
                    .append(" (").append(lecture.startTime)
                    .append(" - ").append(lecture.endTime).append(")");

            // Check if attended
            boolean attended_lecture = false;
            List<AttendanceRecord> subjectAttendance = attendanceBySubject.get(subject.name);
            if (subjectAttendance != null) {
                for (AttendanceRecord record : subjectAttendance) {
                    if (record.lectureId.equals(lecture.id)) {
                        attended_lecture = true;
                        break;
                    }
                }
            }

            details.append(attended_lecture ? " ✓" : " ✗").append("\n");
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Subject Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private int getSubjectColor(String subjectName, int percentage) {
        // Color based on attendance percentage
        if (percentage >= 80) return 0xFF4CAF50; // Green
        else if (percentage >= 60) return 0xFFFF9800; // Orange
        else return 0xFFF44336; // Red
    }

    private int getAttendanceColor(int percentage) {
        if (percentage >= 80) return 0xFF4CAF50; // Green
        else if (percentage >= 60) return 0xFFFF9800; // Orange
        else return 0xFFF44336; // Red
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showLoading(boolean show) {
        // You can add a loading indicator if needed
        runOnUiThread(() -> {
            if (show) {
                subjectListContainer.removeAllViews();
                TextView loadingText = new TextView(this);
                loadingText.setText("Loading attendance data...");
                loadingText.setTextColor(0xFFFFFFFF);
                loadingText.setTextSize(16);
                loadingText.setGravity(android.view.Gravity.CENTER);
                loadingText.setPadding(0, dpToPx(50), 0, 0);
                subjectListContainer.addView(loadingText);
            }
        });
    }

    private void showNoDataMessage() {
        runOnUiThread(() -> {
            TextView noDataText = new TextView(this);
            noDataText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            noDataText.setText("No attendance data available");
            noDataText.setTextColor(0xFFFFFFFF);
            noDataText.setTextSize(16);
            noDataText.setGravity(android.view.Gravity.CENTER);
            noDataText.setPadding(0, dpToPx(50), 0, 0);

            subjectListContainer.removeAllViews();
            subjectListContainer.addView(noDataText);
        });
    }

    private void showNoScheduleMessage() {
        runOnUiThread(() -> {
            TextView noScheduleText = new TextView(this);
            noScheduleText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            noScheduleText.setText("No schedule assigned to you.\nPlease contact admin.");
            noScheduleText.setTextColor(0xFFFFFFFF);
            noScheduleText.setTextSize(16);
            noScheduleText.setGravity(android.view.Gravity.CENTER);
            noScheduleText.setPadding(0, dpToPx(50), 0, 0);

            subjectListContainer.removeAllViews();
            subjectListContainer.addView(noScheduleText);
        });
    }

    private void showNoSubjectsMessage() {
        TextView noSubjectsText = new TextView(this);
        noSubjectsText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        noSubjectsText.setText("No subjects found in your schedule");
        noSubjectsText.setTextColor(0xFFFFFFFF);
        noSubjectsText.setTextSize(14);
        noSubjectsText.setGravity(android.view.Gravity.CENTER);
        noSubjectsText.setPadding(0, dpToPx(20), 0, 0);

        subjectListContainer.addView(noSubjectsText);
    }

    private int getIntValue(Object value) {
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    private long getLongValue(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return 0;
    }

    // Data Classes
    private static class ScheduleData {
        String id;
        String batch;
        String programme;
        String faculty;
        List<SubjectData> subjects;
    }

    private static class SubjectData {
        String name;
        int totalClasses;
        int credits;
        List<LectureData> lectures;
    }

    private static class LectureData {
        String id;
        int number;
        String date;
        String startTime;
        String endTime;
    }

    private static class AttendanceRecord {
        String studentId;
        String studentEmail;
        String subjectName;
        String lectureId;
        int lectureNumber;
        String date;
        String startTime;
        String endTime;
        String status;
        long markedAt;
    }
}