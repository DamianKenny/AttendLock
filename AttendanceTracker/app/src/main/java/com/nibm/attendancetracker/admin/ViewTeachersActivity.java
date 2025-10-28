package com.nibm.attendancetracker.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.models.Teacher;
import com.nibm.attendancetracker.teacher.TeachersAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewTeachersActivity extends AppCompatActivity implements TeachersAdapter.OnTeacherClickListener {

    private static final String TAG = "ViewTeachersActivity";

    private RecyclerView rvTeachers;
    private EditText etSearch;
    private ImageView btnBack;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    private TeachersAdapter teachersAdapter;
    private List<Teacher> teachersList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_view_teachers);

        initViews();
        setupRecyclerView();
        setupSearchFunctionality();
        loadTeachers();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        rvTeachers = findViewById(R.id.rv_teachers);
        etSearch = findViewById(R.id.search);
        btnBack = findViewById(R.id.btn_back);
        emptyState = findViewById(R.id.empty_state1);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        teachersList = new ArrayList<>();

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        teachersAdapter = new TeachersAdapter(teachersList, this);
        rvTeachers.setLayoutManager(new LinearLayoutManager(this));
        rvTeachers.setAdapter(teachersAdapter);
    }

    private void setupSearchFunctionality() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                teachersAdapter.filter(s.toString());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTeachers() {
        showLoading(true);
        Log.d(TAG, "Starting to load teachers from Firestore...");

        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    teachersList.clear();
                    Log.d(TAG, "Firestore query completed. Number of documents: " + queryDocumentSnapshots.size());

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w(TAG, "No teachers found in Firestore with role='teacher'");
                        showLoading(false);
                        updateEmptyState();
                        return;
                    }

                    int teacherCount = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Log.d(TAG, "Processing teacher document: " + document.getId());

                            Teacher teacher = new Teacher();

                            teacher.setId(document.getId());
                            teacher.setName(document.getString("name"));
                            teacher.setFirstName(document.getString("firstName"));
                            teacher.setLastName(document.getString("lastName"));
                            teacher.setEmail(document.getString("email"));
                            teacher.setDepartment(document.getString("department"));
                            teacher.setEmployeeId(document.getString("employeeId"));
                            teacher.setPhone(document.getString("phone"));
                            teacher.setDob(document.getString("dob"));
                            teacher.setJoinDate(document.getString("joinDate"));
                            teacher.setQualification(document.getString("qualification"));
                            teacher.setProfilePictureUrl(document.getString("profilePictureUrl"));
                            teacher.setPassword(document.getString("password"));

                            // Handle assigned subjects arrays
                            if (document.contains("assignedSubjects")) {
                                teacher.setAssignedSubjects((List<String>) document.get("assignedSubjects"));
                            }
                            if (document.contains("assignedSubjectNames")) {
                                teacher.setAssignedSubjectNames((List<String>) document.get("assignedSubjectNames"));
                            }

                            teachersList.add(teacher);
                            teacherCount++;
                            Log.d(TAG, "Successfully added teacher: " + teacher.getName() + " (ID: " + teacher.getId() + ")");

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing teacher document: " + document.getId(), e);
                        }
                    }

                    // Update the adapter
                    if (teachersAdapter != null) {
                        teachersAdapter.updateTeachersList(new ArrayList<>(teachersList));
                    }

                    showLoading(false);
                    updateEmptyState();

                    Log.d(TAG, "Successfully loaded " + teacherCount + " teachers");

                    if (teachersList.isEmpty()) {
                        Toast.makeText(this, "No teachers found in database", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Loaded " + teachersList.size() + " teachers", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading teachers from Firestore: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to load teachers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoading(false);
                    updateEmptyState();
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            rvTeachers.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            rvTeachers.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        boolean shouldShowEmptyState = teachersList.isEmpty();
        emptyState.setVisibility(shouldShowEmptyState ? View.VISIBLE : View.GONE);
        rvTeachers.setVisibility(shouldShowEmptyState ? View.GONE : View.VISIBLE);

        Log.d(TAG, "UpdateEmptyState - Empty: " + shouldShowEmptyState + ", Teachers count: " + teachersList.size());
    }

    @Override
    public void onTeacherClick(Teacher teacher) {
        Intent intent = new Intent(this, TeacherDetailActivity.class);
        intent.putExtra("teacher_id", teacher.getId());
        intent.putExtra("teacher_name", teacher.getName());
        intent.putExtra("teacher_email", teacher.getEmail());
        intent.putExtra("teacher_dept", teacher.getDepartment());
        intent.putExtra("teacher_employee_id", teacher.getEmployeeId());
        intent.putExtra("teacher_phone", teacher.getPhone());
        intent.putExtra("teacher_dob", teacher.getDob());
        intent.putExtra("teacher_join_date", teacher.getJoinDate());
        intent.putExtra("teacher_qualification", teacher.getQualification());
        intent.putExtra("teacher_profile_url", teacher.getProfilePictureUrl());
        intent.putExtra("teacher_first_name", teacher.getFirstName());
        intent.putExtra("teacher_last_name", teacher.getLastName());

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTeachers();
    }
}