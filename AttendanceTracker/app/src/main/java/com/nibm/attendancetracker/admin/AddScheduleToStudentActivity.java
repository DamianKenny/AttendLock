package com.nibm.attendancetracker.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.StudentCheckboxAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddScheduleToStudentActivity extends AppCompatActivity {

    private static final String TAG = "AddScheduleToStudent";

    private ImageView btnBack;
    private Spinner spinnerBatch;
    private EditText etSearch;
    private CheckBox checkboxSelectAll;
    private RecyclerView rvStudents;
    private TextView tvSelectedCount;
    private MaterialButton btnAssignSchedule;
    private LinearLayout nav_home, nav_documents, nav_chat, nav_menu, nav_profile;

    private FirebaseFirestore db;
    private StudentCheckboxAdapter studentAdapter;
    private List<StudentItem> allStudents;
    private List<String> batches;
    private String selectedBatch = "";
    private String selectedScheduleId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_add_schedule_to_student);

        db = FirebaseFirestore.getInstance();
        allStudents = new ArrayList<>();
        batches = new ArrayList<>();

        initViews();
        loadBatches();
        setupListeners();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        spinnerBatch = findViewById(R.id.spinner_batch);
        etSearch = findViewById(R.id.et_search);
        checkboxSelectAll = findViewById(R.id.checkbox_select_all);
        rvStudents = findViewById(R.id.rv_students);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        btnAssignSchedule = findViewById(R.id.btn_assign_schedule);
        nav_home = findViewById(R.id.nav_home);
        nav_documents = findViewById(R.id.nav_documents);
        nav_chat = findViewById(R.id.nav_chat);
        nav_menu = findViewById(R.id.nav_menu);
        nav_profile = findViewById(R.id.nav_profile);

        studentAdapter = new StudentCheckboxAdapter(allStudents, this::updateSelectedCount);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(studentAdapter);
    }

    private void loadBatches() {
        db.collection("schedules")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniqueBatches = new HashSet<>();
                    Map<String, String> batchToScheduleId = new HashMap<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String batch = document.getString("batch");
                        if (batch != null && !batch.isEmpty()) {
                            uniqueBatches.add(batch);
                            batchToScheduleId.put(batch, document.getId());
                        }
                    }

                    batches.clear();
                    batches.add("Select Batch");
                    batches.addAll(uniqueBatches);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            batches
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBatch.setAdapter(adapter);

                    spinnerBatch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position > 0) {
                                selectedBatch = batches.get(position);
                                selectedScheduleId = batchToScheduleId.get(selectedBatch);
                                loadStudents();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading batches: " + e.getMessage());
                    Toast.makeText(this, "Failed to load batches", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadStudents() {
        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allStudents.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        StudentItem student = new StudentItem();
                        student.id = document.getId();
                        student.name = document.getString("name");
                        student.studentId = document.getString("studentId");
                        student.batch = document.getString("batch");
                        student.profileUrl = document.getString("profilePictureUrl");
                        student.isSelected = false;

                        allStudents.add(student);
                    }

                    studentAdapter.notifyDataSetChanged();
                    updateSelectedCount();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading students: " + e.getMessage());
                    Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                studentAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            studentAdapter.selectAll(isChecked);
            updateSelectedCount();
        });

        btnAssignSchedule.setOnClickListener(v -> assignScheduleToStudents());
    }

    private void updateSelectedCount() {
        int count = studentAdapter.getSelectedCount();
        tvSelectedCount.setText(count + " selected");
    }

    private void assignScheduleToStudents() {
        if (selectedBatch.isEmpty()) {
            Toast.makeText(this, "Please select a batch", Toast.LENGTH_SHORT).show();
            return;
        }

        List<StudentItem> selectedStudents = studentAdapter.getSelectedStudents();
        if (selectedStudents.isEmpty()) {
            Toast.makeText(this, "Please select at least one student", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAssignSchedule.setEnabled(false);
        int totalStudents = selectedStudents.size();
        final int[] completed = {0};

        for (StudentItem student : selectedStudents) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("assignedScheduleId", selectedScheduleId);
            updates.put("assignedBatch", selectedBatch);

            db.collection("users").document(student.id)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        completed[0]++;
                        if (completed[0] == totalStudents) {
                            Toast.makeText(this, "Schedule assigned to " + totalStudents + " students", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error assigning schedule to " + student.name + ": " + e.getMessage());
                        completed[0]++;
                        if (completed[0] == totalStudents) {
                            Toast.makeText(this, "Schedule assigned with some errors", Toast.LENGTH_SHORT).show();
                            btnAssignSchedule.setEnabled(true);
                        }
                    });
        }
    }

    public static class StudentItem {
        public String id;
        public String name;
        public String studentId;
        public String batch;
        public String profileUrl;
        public boolean isSelected;
    }

}