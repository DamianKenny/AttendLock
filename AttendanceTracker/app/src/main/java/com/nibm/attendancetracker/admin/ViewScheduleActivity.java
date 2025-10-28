package com.nibm.attendancetracker.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.ScheduleAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ViewScheduleActivity extends AppCompatActivity implements ScheduleAdapter.OnScheduleClickListener {

    private static final String TAG = "ViewSchedulesActivity";

    private RecyclerView rvSchedules;
    private ImageView btnBack, btnAdd;
    private Spinner spinnerBatchFilter;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    private ScheduleAdapter scheduleAdapter;
    private List<Schedule> allSchedules;
    private List<Schedule> filteredSchedules;
    private List<String> batches;
    private FirebaseFirestore db;

    private String selectedBatch = "All Batches";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_view_schedules);

        initViews();
        setupRecyclerView();
        loadSchedules();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        rvSchedules = findViewById(R.id.rv_schedules);
        btnBack = findViewById(R.id.btn_back);
        btnAdd = findViewById(R.id.btn_add);
        spinnerBatchFilter = findViewById(R.id.spinner_batch_filter);
        emptyState = findViewById(R.id.empty_state);
        progressBar = findViewById(R.id.progress_bar);

        db = FirebaseFirestore.getInstance();
        allSchedules = new ArrayList<>();
        filteredSchedules = new ArrayList<>();
        batches = new ArrayList<>();
        batches.add("All Batches");

        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateScheduleActivity.class));
        });
    }

    private void setupRecyclerView() {
        scheduleAdapter = new ScheduleAdapter(filteredSchedules, this);
        rvSchedules.setLayoutManager(new LinearLayoutManager(this));
        rvSchedules.setAdapter(scheduleAdapter);
    }

    private void setupBatchFilter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                batches
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBatchFilter.setAdapter(adapter);

        spinnerBatchFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBatch = batches.get(position);
                filterSchedules();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadSchedules() {
        showLoading(true);

        db.collection("schedules")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allSchedules.clear();
                    Set<String> uniqueBatches = new HashSet<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Schedule schedule = new Schedule();
                            schedule.setId(document.getId());
                            schedule.setFaculty(document.getString("faculty"));
                            schedule.setProgramme(document.getString("programme"));
                            schedule.setBatch(document.getString("batch"));
                            schedule.setCreatedAt(document.getLong("createdAt"));

                            // Parse subjects array
                            List<Object> subjectsData = (List<Object>) document.get("subjects");
                            if (subjectsData != null) {
                                List<Subject> subjects = new ArrayList<>();
                                for (Object subjectObj : subjectsData) {
                                    if (subjectObj instanceof Map) {
                                        Map<String, Object> subjectMap = (Map<String, Object>) subjectObj;
                                        Subject subject = new Subject();
                                        subject.setSubjectName((String) subjectMap.get("subjectName"));

                                        Object totalClassesObj = subjectMap.get("totalClasses");
                                        if (totalClassesObj instanceof Long) {
                                            subject.setTotalClasses(((Long) totalClassesObj).intValue());
                                        } else if (totalClassesObj instanceof Integer) {
                                            subject.setTotalClasses((Integer) totalClassesObj);
                                        }

                                        Object creditsObj = subjectMap.get("credits");
                                        if (creditsObj instanceof Long) {
                                            subject.setCredits(((Long) creditsObj).intValue());
                                        } else if (creditsObj instanceof Integer) {
                                            subject.setCredits((Integer) creditsObj);
                                        }

                                        subject.setLectureSchedules((List<Object>) subjectMap.get("lectureSchedules"));
                                        subjects.add(subject);
                                    }
                                }
                                schedule.setSubjects(subjects);
                            }

                            allSchedules.add(schedule);

                            // Collect unique batches
                            String batch = document.getString("batch");
                            if (batch != null && !batch.isEmpty()) {
                                uniqueBatches.add(batch);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing schedule: " + e.getMessage());
                        }
                    }

                    // Update batches list
                    batches.clear();
                    batches.add("All Batches");
                    batches.addAll(uniqueBatches);
                    setupBatchFilter();

                    // Initial filter
                    filterSchedules();
                    showLoading(false);

                    Log.d(TAG, "Loaded " + allSchedules.size() + " schedules");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedules: " + e.getMessage());
                    Toast.makeText(this, "Failed to load schedules", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    updateEmptyState();
                });
    }

    private void filterSchedules() {
        filteredSchedules.clear();

        for (Schedule schedule : allSchedules) {
            boolean matchesBatch = selectedBatch.equals("All Batches") ||
                    (schedule.getBatch() != null && schedule.getBatch().equals(selectedBatch));

            if (matchesBatch) {
                filteredSchedules.add(schedule);
            }
        }

        scheduleAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvSchedules.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        emptyState.setVisibility(filteredSchedules.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onScheduleClick(Schedule schedule) {
        Intent intent = new Intent(this, ScheduleDetailActivity.class);
        intent.putExtra("schedule_id", schedule.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Schedule schedule) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete this schedule?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSchedule(schedule))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSchedule(Schedule schedule) {
        db.collection("schedules").document(schedule.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Schedule deleted", Toast.LENGTH_SHORT).show();
                    loadSchedules();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete schedule", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSchedules();
    }

    // Subject Model Class
    public static class Subject {
        private String subjectName;
        private int totalClasses;
        private int credits;
        private List<Object> lectureSchedules;

        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

        public int getTotalClasses() { return totalClasses; }
        public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

        public int getCredits() { return credits; }
        public void setCredits(int credits) { this.credits = credits; }

        public List<Object> getLectureSchedules() { return lectureSchedules; }
        public void setLectureSchedules(List<Object> lectureSchedules) {
            this.lectureSchedules = lectureSchedules;
        }
    }

    // Schedule Model Class
    public static class Schedule {
        private String id;
        private String faculty;
        private String programme;
        private String batch;
        private List<Subject> subjects;
        private Long createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getFaculty() { return faculty; }
        public void setFaculty(String faculty) { this.faculty = faculty; }

        public String getProgramme() { return programme; }
        public void setProgramme(String programme) { this.programme = programme; }

        public String getBatch() { return batch; }
        public void setBatch(String batch) { this.batch = batch; }

        public List<Subject> getSubjects() { return subjects; }
        public void setSubjects(List<Subject> subjects) { this.subjects = subjects; }

        public Long getCreatedAt() { return createdAt; }
        public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

        // Helper methods for totals
        public int getTotalClasses() {
            int total = 0;
            if (subjects != null) {
                for (Subject subject : subjects) {
                    total += subject.getTotalClasses();
                }
            }
            return total;
        }

        public int getTotalCredits() {
            int total = 0;
            if (subjects != null) {
                for (Subject subject : subjects) {
                    total += subject.getCredits();
                }
            }
            return total;
        }

        public int getTotalLectures() {
            int total = 0;
            if (subjects != null) {
                for (Subject subject : subjects) {
                    if (subject.getLectureSchedules() != null) {
                        total += subject.getLectureSchedules().size();
                    }
                }
            }
            return total;
        }

        public String getSubjectNamesString() {
            if (subjects == null || subjects.isEmpty()) {
                return "No subjects";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < subjects.size(); i++) {
                sb.append(subjects.get(i).getSubjectName());
                if (i < subjects.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }
}