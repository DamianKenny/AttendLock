package com.nibm.attendancetracker.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;

import java.util.List;
import java.util.Map;

public class ScheduleDetailActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleDetailActivity";

    private ImageView btnBack;
    private TextView tvBatchName, tvSubjectCount, tvProgramme, tvFaculty;
    private TextView tvTotalClasses, tvTotalCredits;
    private LinearLayout subjectsContainer;

    private String scheduleId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_schedule_detail);

        initViews();
        loadScheduleData();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvBatchName = findViewById(R.id.tv_subject_name);
        tvSubjectCount = findViewById(R.id.tv_batch);
        tvProgramme = findViewById(R.id.tv_programme);
        tvFaculty = findViewById(R.id.tv_faculty);
        tvTotalClasses = findViewById(R.id.tv_total_classes);
        tvTotalCredits = findViewById(R.id.tv_credits);
        subjectsContainer = findViewById(R.id.lecture_times_container);

        db = FirebaseFirestore.getInstance();
        scheduleId = getIntent().getStringExtra("schedule_id");

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadScheduleData() {
        if (scheduleId == null) {
            Toast.makeText(this, "Schedule ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("schedules").document(scheduleId)
                .get()
                .addOnSuccessListener(this::displayScheduleData)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedule: " + e.getMessage());
                    Toast.makeText(this, "Failed to load schedule details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayScheduleData(DocumentSnapshot document) {
        if (!document.exists()) {
            Toast.makeText(this, "Schedule not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String batch = document.getString("batch");
        String programme = document.getString("programme");
        String faculty = document.getString("faculty");
        List<Object> subjectsData = (List<Object>) document.get("subjects");

        // Display batch name
        tvBatchName.setText(batch != null ? batch : "N/A");

        // Display subject count
        int subjectCount = subjectsData != null ? subjectsData.size() : 0;
        tvSubjectCount.setText(subjectCount + " Subject" + (subjectCount != 1 ? "s" : ""));

        tvProgramme.setText(programme != null ? programme : "N/A");
        tvFaculty.setText(faculty != null ? faculty : "N/A");

        // Calculate totals and display subjects
        if (subjectsData != null && !subjectsData.isEmpty()) {
            int totalClasses = 0;
            int totalCredits = 0;

            subjectsContainer.removeAllViews();

            for (Object subjectObj : subjectsData) {
                if (subjectObj instanceof Map) {
                    Map<String, Object> subjectMap = (Map<String, Object>) subjectObj;

                    // Get subject data
                    String subjectName = (String) subjectMap.get("subjectName");

                    int classes = 0;
                    Object classesObj = subjectMap.get("totalClasses");
                    if (classesObj instanceof Long) {
                        classes = ((Long) classesObj).intValue();
                    } else if (classesObj instanceof Integer) {
                        classes = (Integer) classesObj;
                    }

                    int credits = 0;
                    Object creditsObj = subjectMap.get("credits");
                    if (creditsObj instanceof Long) {
                        credits = ((Long) creditsObj).intValue();
                    } else if (creditsObj instanceof Integer) {
                        credits = (Integer) creditsObj;
                    }

                    List<Object> lectureSchedules = (List<Object>) subjectMap.get("lectureSchedules");

                    // Add to totals
                    totalClasses += classes;
                    totalCredits += credits;

                    // Add subject card
                    addSubjectCard(subjectName, classes, credits, lectureSchedules);
                }
            }

            tvTotalClasses.setText(String.valueOf(totalClasses));
            tvTotalCredits.setText(String.valueOf(totalCredits));
        } else {
            tvTotalClasses.setText("0");
            tvTotalCredits.setText("0");
        }
    }

    private void addSubjectCard(String subjectName, int totalClasses, int credits, List<Object> lectureSchedules) {
        View subjectCard = LayoutInflater.from(this).inflate(R.layout.item_subject_detail, subjectsContainer, false);

        TextView tvSubjectName = subjectCard.findViewById(R.id.tv_subject_name);
        TextView tvSubjectInfo = subjectCard.findViewById(R.id.tv_subject_info);
        LinearLayout lectureTimesContainer = subjectCard.findViewById(R.id.lecture_times_container);

        tvSubjectName.setText(subjectName != null ? subjectName : "N/A");
        tvSubjectInfo.setText(totalClasses + " Classes • " + credits + " Credits");

        // Add lecture schedules
        if (lectureSchedules != null && !lectureSchedules.isEmpty()) {
            lectureTimesContainer.removeAllViews();
            for (Object lectureObj : lectureSchedules) {
                if (lectureObj instanceof Map) {
                    Map<String, String> lecture = (Map<String, String>) lectureObj;
                    addLectureTimeView(lectureTimesContainer, lecture);
                }
            }
        } else {
            TextView noLectures = new TextView(this);
            noLectures.setText("No lecture times scheduled");
            noLectures.setTextColor(getResources().getColor(R.color.text_secondary));
            noLectures.setTextSize(14);
            noLectures.setPadding(0, 8, 0, 8);
            lectureTimesContainer.addView(noLectures);
        }

        subjectsContainer.addView(subjectCard);
    }

    private void addLectureTimeView(LinearLayout container, Map<String, String> timeSlot) {
        View timeView = LayoutInflater.from(this).inflate(R.layout.item_lecture_time_display, container, false);

        TextView tvDay = timeView.findViewById(R.id.tv_day);
        TextView tvTime = timeView.findViewById(R.id.tv_time);

        String date = timeSlot.get("date");
        String startTime = timeSlot.get("startTime");
        String endTime = timeSlot.get("endTime");

        tvDay.setText(date != null ? date : "N/A");

        String timeRange = (startTime != null ? startTime : "--:--") + " - " +
                (endTime != null ? endTime : "--:--");
        tvTime.setText(timeRange);

        container.addView(timeView);
    }
}