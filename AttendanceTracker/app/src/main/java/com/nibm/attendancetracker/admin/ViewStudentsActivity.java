package com.nibm.attendancetracker.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.models.Student;
import com.nibm.attendancetracker.student.StudentsAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewStudentsActivity extends AppCompatActivity implements StudentsAdapter.OnStudentClickListener {

    private static final String TAG = "ViewStudentsActivity";

    private RecyclerView rvStudents;
    private EditText etSearch;
    private ImageView btnBack;
    private Spinner spinnerProgramme;
    private TextView tvStudentCount;
    private LinearLayout emptyState;
    private ProgressBar progressBar;

    private StudentsAdapter studentAdapter;
    private List<Student> allStudents;
    private List<Student> filteredStudents;

    private List<String> programmes;
    private FirebaseFirestore db;

    private String selectedProgramme = "All Programmes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_view_students);

        initViews();
        setupRecyclerView();
        setupSearchFunctionality();
        loadStudents();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        rvStudents = findViewById(R.id.rv_students);
        etSearch = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);
        spinnerProgramme = findViewById(R.id.spinner_programme);
        tvStudentCount = findViewById(R.id.tv_student_count);
        emptyState = findViewById(R.id.empty_state);
        progressBar = findViewById(R.id.progress_bar);

        db = FirebaseFirestore.getInstance();
        allStudents = new ArrayList<>();
        filteredStudents = new ArrayList<>();
        programmes = new ArrayList<>();
        programmes.add("All Programmes");

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        studentAdapter = new StudentsAdapter(filteredStudents, this);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(studentAdapter);
    }

    private void setupSearchFunctionality() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupProgrammeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_dark,
                programmes
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark);

        spinnerProgramme.setAdapter(adapter);

        spinnerProgramme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedProgramme = programmes.get(position);
                filterStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }


    private void loadStudents() {
        showLoading(true);

        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allStudents.clear();
                    Set<String> uniqueProgrammes = new HashSet<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Student student = new Student();

                            student.setId(document.getId());
                            student.setName(document.getString("name"));
                            student.setFirstName(document.getString("firstName"));
                            student.setLastName(document.getString("lastName"));
                            student.setEmail(document.getString("email"));
                            student.setStudentId(document.getString("studentId"));
                            student.setRoll(document.getString("roll"));
                            student.setBatch(document.getString("batch"));
                            student.setProgramme(document.getString("programme"));
                            student.setFaculty(document.getString("faculty"));
                            student.setCourses(document.getString("courses"));
                            student.setPhone(document.getString("phone"));
                            student.setDob(document.getString("dob"));
                            student.setGender(document.getString("gender"));
                            student.setAddress(document.getString("address"));
                            student.setAdmissionDate(document.getString("admissionDate"));
                            student.setParentName(document.getString("parentName"));
                            student.setParentEmail(document.getString("parentEmail"));
                            student.setProfilePictureUrl(document.getString("profilePictureUrl"));
                            student.setPassword(document.getString("password"));

                            allStudents.add(student);

                            // Collect unique programmes
                            String programme = document.getString("programme");
                            if (programme != null && !programme.isEmpty()) {
                                uniqueProgrammes.add(programme);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing student: " + e.getMessage());
                        }
                    }

                    // Update programmes list
                    programmes.clear();
                    programmes.add("All Programmes");
                    programmes.addAll(uniqueProgrammes);
                    setupProgrammeSpinner();

                    // Initial filter
                    filterStudents();
                    showLoading(false);

                    Log.d(TAG, "Loaded " + allStudents.size() + " students");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading students: " + e.getMessage());
                    Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    updateEmptyState();
                });
    }

    private void filterStudents() {
        filteredStudents.clear();
        String searchQuery = etSearch.getText().toString().trim().toLowerCase();

        for (Student student : allStudents) {
            boolean matchesProgramme = selectedProgramme.equals("All Programmes") ||
                    (student.getProgramme() != null && student.getProgramme().equals(selectedProgramme));

            boolean matchesSearch = searchQuery.isEmpty() ||
                    (student.getStudentId() != null && student.getStudentId().toLowerCase().contains(searchQuery)) ||
                    (student.getName() != null && student.getName().toLowerCase().contains(searchQuery));

            if (matchesProgramme && matchesSearch) {
                filteredStudents.add(student);
            }
        }

        studentAdapter.notifyDataSetChanged();
        updateStudentCount();
        updateEmptyState();
    }

    private void updateStudentCount() {
        int count = filteredStudents.size();
        tvStudentCount.setText(count + (count == 1 ? " student" : " students"));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvStudents.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState() {
        emptyState.setVisibility(filteredStudents.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStudentClick(Student student) {
        Intent intent = new Intent(this, StudentDetailActivity.class);
        intent.putExtra("student_id", student.getId());
        intent.putExtra("student_name", student.getName());
        intent.putExtra("student_student_id", student.getStudentId());
        intent.putExtra("student_email", student.getEmail());
        intent.putExtra("student_programme", student.getProgramme());
        intent.putExtra("student_batch", student.getBatch());
        intent.putExtra("student_faculty", student.getFaculty());
        intent.putExtra("student_phone", student.getPhone());
        intent.putExtra("student_dob", student.getDob());
        intent.putExtra("student_gender", student.getGender());
        intent.putExtra("student_address", student.getAddress());
        intent.putExtra("student_admission_date", student.getAdmissionDate());
        intent.putExtra("student_parent_name", student.getParentName());
        intent.putExtra("student_parent_email", student.getParentEmail());
        intent.putExtra("student_profile_url", student.getProfilePictureUrl());
        intent.putExtra("student_first_name", student.getFirstName());
        intent.putExtra("student_last_name", student.getLastName());
        intent.putExtra("student_roll", student.getRoll());
        intent.putExtra("student_courses", student.getCourses());

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload students when returning from detail activity
        loadStudents();
    }
}