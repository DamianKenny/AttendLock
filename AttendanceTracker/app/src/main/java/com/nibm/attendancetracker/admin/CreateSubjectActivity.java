package com.nibm.attendancetracker.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;

import java.util.HashMap;
import java.util.Map;

public class CreateSubjectActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText etSubjectName, etDepartment, etCourse, etUniversity, etCategory;
    private LinearLayout btnCreateSubject;

    private LinearLayout navHome, navDocuments, navChat, navMenu, navProfile;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_create_subject);

        db = FirebaseFirestore.getInstance();

        initializeViews();

        setClickListeners();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }

    }

    private void initializeViews() {
        // Form elements
        btnBack = findViewById(R.id.btn_back);
        etSubjectName = findViewById(R.id.et_subject_name);
        etDepartment = findViewById(R.id.et_department);
        etCourse = findViewById(R.id.et_course);
        etUniversity = findViewById(R.id.et_university);
        etCategory = findViewById(R.id.et_category);
        btnCreateSubject = findViewById(R.id.btn_create_subject);

        // Navigation elements
        navHome = findViewById(R.id.nav_home);
        navDocuments = findViewById(R.id.nav_documents);
        navChat = findViewById(R.id.nav_chat);
        navMenu = findViewById(R.id.nav_menu);
        navProfile = findViewById(R.id.nav_profile);
    }

    private void setClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Create Subject button
        btnCreateSubject.setOnClickListener(v -> createSubject());

        // Navigation buttons
        navHome.setOnClickListener(v -> navigateToHome());
        navDocuments.setOnClickListener(v -> navigateToDocuments());
        navChat.setOnClickListener(v -> navigateToChat());
        navMenu.setOnClickListener(v -> navigateToMenu());
        navProfile.setOnClickListener(v -> navigateToProfile());
    }

    private void createSubject() {
        // Get input values
        String subjectName = etSubjectName.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String course = etCourse.getText().toString().trim();
        String university = etUniversity.getText().toString().trim();
        String category = etCategory.getText().toString().trim();

        // Validate inputs
        if (subjectName.isEmpty()) {
            etSubjectName.setError("Subject name is required");
            etSubjectName.requestFocus();
            return;
        }

        if (department.isEmpty()) {
            etDepartment.setError("Department is required");
            etDepartment.requestFocus();
            return;
        }

        if (course.isEmpty()) {
            etCourse.setError("Course is required");
            etCourse.requestFocus();
            return;
        }

        if (university.isEmpty()) {
            etUniversity.setError("University is required");
            etUniversity.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            etUniversity.setError("Category is required");
            etUniversity.requestFocus();
            return;
        }

        Map<String, Object> subject = new HashMap<>();
        subject.put("subjectName", subjectName);
        subject.put("department", department);
        subject.put("course", course);
        subject.put("awardingUniversity", university);
        subject.put("createdAt", System.currentTimeMillis());
        subject.put("category", category);

        btnCreateSubject.setEnabled(false);

        db.collection("subjects")
                .add(subject)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(CreateSubjectActivity.this,
                            "Subject created successfully!",
                            Toast.LENGTH_SHORT).show();

                    clearForm();

                    btnCreateSubject.setEnabled(true);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateSubjectActivity.this,
                            "Failed to create subject: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    btnCreateSubject.setEnabled(true);
                });
    }

    private void clearForm() {
        etSubjectName.setText("");
        etDepartment.setText("");
        etCourse.setText("");
        etUniversity.setText("");
        etCategory.setText("");
    }

    private void navigateToHome() {
        Intent intent = new Intent(CreateSubjectActivity.this, AdminDashboardActivity.class);
        startActivity(intent);
    }

    private void navigateToDocuments() {
        // Already on documents/subjects page
        // Could refresh or do nothing
    }

    private void navigateToChat() {
        Intent intent = new Intent(CreateSubjectActivity.this, CreateTeacherActivity.class);
        startActivity(intent);
    }

    private void navigateToMenu() {
        Intent intent = new Intent(CreateSubjectActivity.this, CreateStudentActivity.class);
        startActivity(intent);
    }

    private void navigateToProfile() {
        Intent intent = new Intent(CreateSubjectActivity.this, AddSubjectsToTeacherActivity.class);
        startActivity(intent);
    }
}
