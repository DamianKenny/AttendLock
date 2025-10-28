package com.nibm.attendancetracker.student;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.NavigationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClassScheduleActivity extends AppCompatActivity {

    private static final String TAG = "ClassScheduleActivity";

    private LinearLayout classesContainer;
    private FirebaseFirestore db;
    private String todayDate;
    private String currentStudentEmail;
    private String currentStudentId;
    private String currentStudentName;
    private String assignedScheduleId;

    private TextView scheduleTitle, scheduleSubtitle, todayDateText, studentNameText;
    private ScheduleData scheduleData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_class_schedule);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupFirebase();

        // Setup navigation with role
        NavigationHelper.setupNavigation(this, "student");

        loadStudentData();
    }

    private void initializeViews() {
        classesContainer = findViewById(R.id.classes_container);
        scheduleTitle = findViewById(R.id.schedule_title);
        scheduleSubtitle = findViewById(R.id.schedule_subtitle);
        todayDateText = findViewById(R.id.today_date_text);
        studentNameText = findViewById(R.id.student_name_text);

        // Set today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        todayDate = dateFormat.format(new Date());

        if (todayDateText != null) {
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
            String displayDate = displayFormat.format(new Date());
            todayDateText.setText("Today • " + displayDate);
        }
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();

        // Get student info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        currentStudentEmail = prefs.getString("current_user_email", "");
        currentStudentId = prefs.getString("student_id", "");
        currentStudentName = prefs.getString("student_name", "Student");

        Log.d(TAG, "Student Email: " + currentStudentEmail);
        Log.d(TAG, "Student ID: " + currentStudentId);
    }

    private void loadStudentData() {
        if (currentStudentEmail == null || currentStudentEmail.isEmpty()) {
            Toast.makeText(this, "Student data not available", Toast.LENGTH_SHORT).show();
            showNoDataMessage();
            return;
        }

        showLoading(true);

        // Fetch student document to get assignedScheduleId
        db.collection("users")
                .whereEqualTo("email", currentStudentEmail)
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot studentDoc = queryDocumentSnapshots.getDocuments().get(0);
                        assignedScheduleId = studentDoc.getString("assignedScheduleId");
                        currentStudentName = studentDoc.getString("name");
                        currentStudentId = studentDoc.getString("studentId");

                        // Update student name in UI
                        if (studentNameText != null && currentStudentName != null) {
                            studentNameText.setText("Hello, " + currentStudentName.split(" ")[0]);
                        }

                        Log.d(TAG, "Assigned Schedule ID: " + assignedScheduleId);

                        if (assignedScheduleId != null && !assignedScheduleId.isEmpty()) {
                            loadScheduleFromFirebase();
                        } else {
                            showLoading(false);
                            showNoScheduleAssignedMessage();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show();
                        showNoDataMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading student data", e);
                    Toast.makeText(this, "Error loading student data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadScheduleFromFirebase() {
        db.collection("schedules")
                .document(assignedScheduleId)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            try {
                                scheduleData = parseScheduleData(document);

                                Log.d(TAG, "Loaded schedule: " + scheduleData.batch +
                                        " with " + scheduleData.subjects.size() + " subjects");

                                updateUIWithSchedule();
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing schedule: " + e.getMessage(), e);
                                Toast.makeText(this, "Error parsing schedule data",
                                        Toast.LENGTH_SHORT).show();
                                showNoDataMessage();
                            }
                        } else {
                            Toast.makeText(this, "Schedule document not found",
                                    Toast.LENGTH_SHORT).show();
                            showNoScheduleAssignedMessage();
                        }
                    } else {
                        Toast.makeText(this, "Error loading schedule: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                        showNoDataMessage();
                    }
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

                                Log.d(TAG, "Added lecture: " + subject.name +
                                        " - Lecture " + lecture.number +
                                        " on " + lecture.date);
                            }
                        }
                    }

                    // Sort lectures by date
                    if (subject.lectures != null && !subject.lectures.isEmpty()) {
                        Collections.sort(subject.lectures, new LectureComparator());
                    }

                    schedule.subjects.add(subject);
                }
            }
        }

        return schedule;
    }

    private void updateUIWithSchedule() {
        runOnUiThread(() -> {
            if (classesContainer != null) {
                classesContainer.removeAllViews();

                if (scheduleData == null || scheduleData.subjects.isEmpty()) {
                    showNoClassesMessage();
                    return;
                }

                // Update Today's Schedule card
                updateTodaysScheduleCard();

                // Display subjects with their lectures
                displaySubjectsWithLectures();
            }
        });
    }

    private void updateTodaysScheduleCard() {
        if (scheduleTitle != null && scheduleSubtitle != null) {
            int totalSubjects = scheduleData.subjects.size();
            int totalLectures = 0;

            for (SubjectData subject : scheduleData.subjects) {
                if (subject.lectures != null) {
                    totalLectures += subject.lectures.size();
                }
            }

            scheduleTitle.setText("My Class Schedule");
            scheduleSubtitle.setText(totalSubjects + " Subjects • " + totalLectures + " Total Lectures");
        }
    }

    private void displaySubjectsWithLectures() {
        for (SubjectData subject : scheduleData.subjects) {
            addSubjectCard(subject);
        }
    }

    private void addSubjectCard(SubjectData subject) {
        // Create main subject card
        CardView subjectCard = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(16));
        subjectCard.setLayoutParams(cardParams);
        subjectCard.setCardBackgroundColor(0xFF2A2A2A);
        subjectCard.setRadius(dpToPx(12));
        subjectCard.setCardElevation(dpToPx(2));

        // Main container
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Subject header
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setPadding(0, 0, 0, dpToPx(12));

        // Color indicator
        View colorIndicator = new View(this);
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                dpToPx(8),
                dpToPx(40)
        );
        indicatorParams.setMargins(0, 0, dpToPx(12), 0);
        colorIndicator.setLayoutParams(indicatorParams);
        colorIndicator.setBackgroundColor(getIndicatorColor(subject.name));

        // Subject info container
        LinearLayout subjectInfoLayout = new LinearLayout(this);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        subjectInfoLayout.setLayoutParams(infoParams);
        subjectInfoLayout.setOrientation(LinearLayout.VERTICAL);

        // Subject name
        TextView subjectNameView = new TextView(this);
        subjectNameView.setText(subject.name);
        subjectNameView.setTextColor(0xFFFFFFFF);
        subjectNameView.setTextSize(18);
        subjectNameView.setTypeface(null, android.graphics.Typeface.BOLD);
        subjectNameView.setPadding(0, 0, 0, dpToPx(4));

        // Subject details
        TextView subjectDetailsView = new TextView(this);
        subjectDetailsView.setText(subject.lectures.size() + " Lectures • " + subject.credits + " Credits");
        subjectDetailsView.setTextColor(0xFF999999);
        subjectDetailsView.setTextSize(14);

        subjectInfoLayout.addView(subjectNameView);
        subjectInfoLayout.addView(subjectDetailsView);

        headerLayout.addView(colorIndicator);
        headerLayout.addView(subjectInfoLayout);

        // Divider
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        ));
        divider.setBackgroundColor(0xFF3A3A3A);

        // Lectures container
        LinearLayout lecturesContainer = new LinearLayout(this);
        lecturesContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        lecturesContainer.setOrientation(LinearLayout.VERTICAL);
        lecturesContainer.setPadding(0, dpToPx(12), 0, 0);

        // Add lectures header
        TextView lecturesHeader = new TextView(this);
        lecturesHeader.setText("Lecture Schedule");
        lecturesHeader.setTextColor(0xFFCCCCCC);
        lecturesHeader.setTextSize(14);
        lecturesHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        lecturesHeader.setPadding(0, 0, 0, dpToPx(8));
        lecturesContainer.addView(lecturesHeader);

        // Add each lecture
        if (subject.lectures != null && !subject.lectures.isEmpty()) {
            for (LectureData lecture : subject.lectures) {
                addLectureItem(lecturesContainer, lecture, subject.name);
            }
        } else {
            TextView noLectures = new TextView(this);
            noLectures.setText("No lectures scheduled");
            noLectures.setTextColor(0xFF666666);
            noLectures.setTextSize(14);
            noLectures.setPadding(dpToPx(12), dpToPx(8), 0, dpToPx(8));
            lecturesContainer.addView(noLectures);
        }

        // Add all to main layout
        mainLayout.addView(headerLayout);
        mainLayout.addView(divider);
        mainLayout.addView(lecturesContainer);

        subjectCard.addView(mainLayout);
        classesContainer.addView(subjectCard);
    }

    private void addLectureItem(LinearLayout container, LectureData lecture, String subjectName) {
        // Create lecture item layout
        LinearLayout lectureLayout = new LinearLayout(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, dpToPx(8));
        lectureLayout.setLayoutParams(layoutParams);
        lectureLayout.setOrientation(LinearLayout.HORIZONTAL);
        lectureLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        boolean isToday = lecture.date != null && lecture.date.equals(todayDate);

        if (isToday) {
            lectureLayout.setBackgroundColor(0xFF1E3A5F); // Blue highlight for today
        } else {
            lectureLayout.setBackgroundColor(0xFF1E1E1E);
        }

        LinearLayout leftLayout = new LinearLayout(this);
        leftLayout.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(50),
                dpToPx(50)
        ));
        leftLayout.setOrientation(LinearLayout.VERTICAL);
        leftLayout.setGravity(android.view.Gravity.CENTER);

        // circle background
        TextView lectureNumberView = new TextView(this);
        lectureNumberView.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        ));
        lectureNumberView.setText(String.valueOf(lecture.number));
        lectureNumberView.setTextColor(0xFFFFFFFF);
        lectureNumberView.setTextSize(16);
        lectureNumberView.setTypeface(null, android.graphics.Typeface.BOLD);
        lectureNumberView.setGravity(android.view.Gravity.CENTER);
        lectureNumberView.setBackgroundColor(getIndicatorColor(subjectName));

        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(getIndicatorColor(subjectName));
        lectureNumberView.setBackground(shape);

        leftLayout.addView(lectureNumberView);

        // lecture details
        LinearLayout middleLayout = new LinearLayout(this);
        LinearLayout.LayoutParams middleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        middleParams.setMargins(dpToPx(12), 0, dpToPx(12), 0);
        middleLayout.setLayoutParams(middleParams);
        middleLayout.setOrientation(LinearLayout.VERTICAL);

        // date
        TextView dateView = new TextView(this);
        dateView.setText(formatDisplayDate(lecture.date));
        dateView.setTextColor(0xFFFFFFFF);
        dateView.setTextSize(15);
        dateView.setTypeface(null, android.graphics.Typeface.BOLD);
        dateView.setPadding(0, 0, 0, dpToPx(4));

        // time
        TextView timeView = new TextView(this);
        timeView.setText("🕒 " + lecture.startTime + " - " + lecture.endTime);
        timeView.setTextColor(0xFF999999);
        timeView.setTextSize(14);

        if (isToday) {
            TextView todayBadge = new TextView(this);
            todayBadge.setText("TODAY");
            todayBadge.setTextColor(0xFF4CAF50);
            todayBadge.setTextSize(11);
            todayBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            todayBadge.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
            todayBadge.setBackgroundColor(0xFF1B5E20);
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            badgeParams.setMargins(0, dpToPx(4), 0, 0);
            todayBadge.setLayoutParams(badgeParams);
            middleLayout.addView(dateView);
            middleLayout.addView(timeView);
            middleLayout.addView(todayBadge);
        } else {
            middleLayout.addView(dateView);
            middleLayout.addView(timeView);
        }

        LinearLayout rightLayout = new LinearLayout(this);
        rightLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        rightLayout.setOrientation(LinearLayout.VERTICAL);
        rightLayout.setGravity(android.view.Gravity.CENTER);

        TextView roomView = new TextView(this);
        roomView.setText(getRoomForSubject(subjectName));
        roomView.setTextColor(0xFF999999);
        roomView.setTextSize(12);

        ImageView arrowIcon = new ImageView(this);
        arrowIcon.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(20),
                dpToPx(20)
        ));
        arrowIcon.setImageResource(R.drawable.ic_arrow_forward);
        arrowIcon.setColorFilter(0xFF666666);

        rightLayout.addView(roomView);
        rightLayout.addView(arrowIcon);

        lectureLayout.addView(leftLayout);
        lectureLayout.addView(middleLayout);
        lectureLayout.addView(rightLayout);

        lectureLayout.setOnClickListener(v -> showLectureDetails(lecture, subjectName));

        container.addView(lectureLayout);
    }

    private void showLectureDetails(LectureData lecture, String subjectName) {
        StringBuilder details = new StringBuilder();
        details.append("Subject: ").append(subjectName).append("\n\n");
        details.append("Lecture: ").append(lecture.number).append("\n");
        details.append("Date: ").append(formatDisplayDate(lecture.date)).append("\n");
        details.append("Time: ").append(lecture.startTime).append(" - ").append(lecture.endTime).append("\n");
        details.append("Batch: ").append(scheduleData.batch).append("\n");
        details.append("Faculty: ").append(scheduleData.faculty).append("\n");
        details.append("Room: ").append(getRoomForSubject(subjectName));

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lecture Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private int getIndicatorColor(String subjectName) {
        if (subjectName == null) return 0xFFFF9800;

        int hash = Math.abs(subjectName.hashCode());
        int[] colors = {
                0xFFFF9800, // orange
                0xFF2196F3, // blue
                0xFF4CAF50, // green
                0xFF9C27B0, // purple
                0xFFFFEB3B, // yellow
                0xFFE91E63, // pink
                0xFF00BCD4, // cyan
                0xFFFF5722  // deep Orange
        };
        return colors[hash % colors.length];
    }

    private String getRoomForSubject(String subjectName) {
        if (subjectName == null) return "TBA";

        String lowerName = subjectName.toLowerCase();

        if (lowerName.contains("lab") || lowerName.contains("practical")) {
            return "Lab A1";
        } else if (lowerName.contains("network") || lowerName.contains("system")) {
            return "Room 203";
        } else if (lowerName.contains("security") || lowerName.contains("cyber")) {
            return "Room 301";
        } else if (lowerName.contains("data") || lowerName.contains("algorithm")) {
            return "Room 105";
        } else if (lowerName.contains("web") || lowerName.contains("mobile")) {
            return "Lab B2";
        } else {
            return "Room 101";
        }
    }

    private String formatDisplayDate(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (Exception e) {
            return date;
        }
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            classesContainer.removeAllViews();

            if (show) {
                TextView loadingText = new TextView(this);
                loadingText.setText("Loading your schedule...");
                loadingText.setTextColor(0xFFFFFFFF);
                loadingText.setTextSize(16);
                loadingText.setPadding(dpToPx(50), dpToPx(50), dpToPx(50), dpToPx(50));
                loadingText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                classesContainer.addView(loadingText);
            }
        });
    }

    private void showNoClassesMessage() {
        TextView noClassesText = new TextView(this);
        noClassesText.setText("📚\n\nNo classes scheduled");
        noClassesText.setTextColor(0xFFFFFFFF);
        noClassesText.setTextSize(16);
        noClassesText.setPadding(dpToPx(50), dpToPx(100), dpToPx(50), dpToPx(50));
        noClassesText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        classesContainer.addView(noClassesText);
    }

    private void showNoDataMessage() {
        TextView noDataText = new TextView(this);
        noDataText.setText("❌\n\nUnable to load schedule\n\nPlease try again later");
        noDataText.setTextColor(0xFFFFFFFF);
        noDataText.setTextSize(16);
        noDataText.setPadding(dpToPx(50), dpToPx(100), dpToPx(50), dpToPx(50));
        noDataText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        classesContainer.addView(noDataText);
    }

    private void showNoScheduleAssignedMessage() {
        TextView noScheduleText = new TextView(this);
        noScheduleText.setText("📋\n\nNo schedule assigned\n\nPlease contact your admin to get a schedule assigned");
        noScheduleText.setTextColor(0xFFFFFFFF);
        noScheduleText.setTextSize(16);
        noScheduleText.setPadding(dpToPx(50), dpToPx(100), dpToPx(50), dpToPx(50));
        noScheduleText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        classesContainer.addView(noScheduleText);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int getIntValue(Object value) {
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

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

    private class LectureComparator implements Comparator<LectureData> {
        private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        @Override
        public int compare(LectureData l1, LectureData l2) {
            try {
                Date d1 = dateFormat.parse(l1.date);
                Date d2 = dateFormat.parse(l2.date);
                return d1.compareTo(d2);
            } catch (Exception e) {
                return 0;
            }
        }
    }
}