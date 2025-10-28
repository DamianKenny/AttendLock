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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.SubjectsAdapter;
import com.nibm.attendancetracker.models.Teacher;
import com.nibm.attendancetracker.teacher.TeachersAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddSubjectsToTeacherActivity extends AppCompatActivity {

    private static final String TAG = "AddSubjectsToTeacher";

    private ImageView btnBack, btnClearSelection, ivSelectedTeacherProfile;
    private EditText etSearchTeacher;
    private RecyclerView rvTeachers, rvSubjects;
    private CardView cardSelectedTeacher;
    private TextView tvSelectedTeacherName, tvSelectedTeacherDept, tvSubjectsCount;
    private MaterialButton btnAllSubjects, btnEngineeringSubjects, btnComputingSubjects;
    private MaterialButton btnCancel, btnAssignSubjects;
    private LinearLayout navHome, navDocuments, navChat, navMenu, navProfile;

    private FirebaseFirestore db;

    private TeachersAdapter teachersAdapter;
    private SubjectsAdapter subjectsAdapter;

    private List<Teacher> teachersList;
    private List<Subject> subjectsList;
    private List<Subject> filteredSubjects;
    private Teacher selectedTeacher;
    private List<Subject> selectedSubjects;
    private Set<String> assignedSubjectIds;

    private static final List<String> SUBJECT_COLORS = Arrays.asList(
            "#FB923C", "#60A5FA", "#34D399", "#F472B6", "#8B5CF6",
            "#F59E0B", "#EF4444", "#10B981", "#6366F1", "#EC4899"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subject_to_teachers);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerViews();
        setupClickListeners();
        setupSearch();

        fetchTeachersFromFirestore();
        fetchSubjectsFromFirestore();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnClearSelection = findViewById(R.id.btn_clear_selection);
        etSearchTeacher = findViewById(R.id.et_search_teacher);
        rvTeachers = findViewById(R.id.rv_teachers);
        rvSubjects = findViewById(R.id.rv_subjects);
        cardSelectedTeacher = findViewById(R.id.card_selected_teacher);
        tvSelectedTeacherName = findViewById(R.id.tv_selected_teacher_name);
        tvSelectedTeacherDept = findViewById(R.id.tv_selected_teacher_dept);
        tvSubjectsCount = findViewById(R.id.tv_subjects_count);
        ivSelectedTeacherProfile = findViewById(R.id.iv_selected_teacher_profile);

        btnAllSubjects = findViewById(R.id.btn_all_subjects);
        btnEngineeringSubjects = findViewById(R.id.btn_engineering_subjects);
        btnComputingSubjects = findViewById(R.id.btn_computing_subjects);

        btnCancel = findViewById(R.id.btn_cancel);
        btnAssignSubjects = findViewById(R.id.btn_assign_subjects);

        navHome = findViewById(R.id.nav_home);
        navDocuments = findViewById(R.id.nav_documents);
        navChat = findViewById(R.id.nav_chat);
        navMenu = findViewById(R.id.nav_menu);
        navProfile = findViewById(R.id.nav_profile);

        teachersList = new ArrayList<>();
        subjectsList = new ArrayList<>();
        filteredSubjects = new ArrayList<>();
        selectedSubjects = new ArrayList<>();
        assignedSubjectIds = new HashSet<>();

        if (tvSubjectsCount == null) {
            Log.e(TAG, "tv_subjects_count is null - check subject_to_teachers.xml");
        } else {
            Log.d(TAG, "tv_subjects_count initialized");
        }

        if (ivSelectedTeacherProfile == null) {
            Log.e(TAG, "ivSelectedTeacherProfile is null - check subject_to_teachers.xml");
        } else {
            Log.d(TAG, "ivSelectedTeacherProfile initialized");
        }
    }

    private void setupRecyclerViews() {
        rvTeachers.setLayoutManager(new LinearLayoutManager(this));
        subjectsAdapter = new SubjectsAdapter(filteredSubjects, this::toggleSubjectSelection);
        rvSubjects.setLayoutManager(new GridLayoutManager(this, 2));
        rvSubjects.setAdapter(subjectsAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnClearSelection.setOnClickListener(v -> clearTeacherSelection());
        btnAllSubjects.setOnClickListener(v -> filterSubjects("All"));
        btnEngineeringSubjects.setOnClickListener(v -> filterSubjects("Engineering"));
        btnComputingSubjects.setOnClickListener(v -> filterSubjects("Computing"));
        btnCancel.setOnClickListener(v -> finish());
        btnAssignSubjects.setOnClickListener(v -> assignSubjectsToTeacher());
        setupNavigation();
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
        });

        navDocuments.setOnClickListener(v -> {
            // Already on this page
        });

        navChat.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateStudentActivity.class));
        });

        navMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateTeacherActivity.class));
        });

        navProfile.setOnClickListener(v -> {
            // Navigate to profile
        });
    }

    private void setupSearch() {
        etSearchTeacher.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTeachers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchTeachersFromFirestore() {
        Log.d(TAG, "Fetching teachers from Firestore...");

        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    teachersList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w(TAG, "No teachers found in Firestore");
                        Toast.makeText(this, "No teachers found. Loading default teachers.", Toast.LENGTH_LONG).show();
                        loadFallbackTeachers();
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String name = document.getString("name");
                        String department = document.getString("department");
                        String email = document.getString("email");
                        String profilePictureUrl = document.getString("profilePictureUrl");

                        if (name != null && !name.isEmpty()) {
                            teachersList.add(new Teacher(
                                    id,
                                    name,
                                    department != null ? department : "Unknown",
                                    email != null ? email : "No email",
                                    profilePictureUrl != null ? profilePictureUrl : ""
                            ));
                            Log.d(TAG, "Added teacher: " + name + " | ID: " + id + " | Profile URL: " + profilePictureUrl);
                        } else {
                            Log.w(TAG, "Skipping teacher with null or empty name, ID: " + id);
                        }
                    }

                    if (teachersAdapter == null) {
                        teachersAdapter = new TeachersAdapter(teachersList, this::selectTeacher);
                        rvTeachers.setAdapter(teachersAdapter);
                    }
                    teachersAdapter.notifyDataSetChanged();

                    Log.d(TAG, "Successfully fetched " + teachersList.size() + " teachers");

                    if (teachersList.isEmpty()) {
                        Toast.makeText(this, "No valid teachers found. Loading default teachers.", Toast.LENGTH_LONG).show();
                        loadFallbackTeachers();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching teachers", e);
                    Toast.makeText(this, "Failed to load teachers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    loadFallbackTeachers();
                });
    }

    private void loadFallbackTeachers() {
        teachersList.clear();

        List<Teacher> fallbackTeachers = Arrays.asList(
                new Teacher("T001", "John Doe", "Computer Science", "john.doe@example.com", "https://via.placeholder.com/150"),
                new Teacher("T002", "Jane Smith", "Mathematics", "jane.smith@example.com", "https://via.placeholder.com/150"),
                new Teacher("T003", "Emily Johnson", "English", "emily.johnson@example.com", "https://via.placeholder.com/150"),
                new Teacher("T004", "Michael Brown", "Physics", "michael.brown@example.com", "https://via.placeholder.com/150"),
                new Teacher("T005", "Sarah Davis", "History", "sarah.davis@example.com", "https://via.placeholder.com/150")
        );

        teachersList.addAll(fallbackTeachers);

        if (teachersAdapter == null) {
            teachersAdapter = new TeachersAdapter(teachersList, this::selectTeacher);
            rvTeachers.setAdapter(teachersAdapter);
        }
        teachersAdapter.notifyDataSetChanged();

        Log.d(TAG, "Loaded " + teachersList.size() + " fallback teachers");
    }

    private void filterTeachers(String query) {
        if (teachersAdapter != null) {
            teachersAdapter.filter(query);
            Log.d(TAG, "Filtering teachers with query: " + query);
        } else {
            Log.w(TAG, "TeachersAdapter is null, cannot filter");
        }
    }

    private void fetchSubjectsFromFirestore() {
        Log.d(TAG, "Fetching subjects from Firestore...");

        db.collection("subjects")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    subjectsList.clear();
                    int colorIndex = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String name = document.getString("subjectName");
                        String category = document.getString("category");
                        String course = document.getString("course");
                        String department = document.getString("department");

                        String finalCategory = (category != null && !category.isEmpty()) ?
                                category.trim().toLowerCase() :
                                (course != null && !course.isEmpty()) ?
                                        course.trim().toLowerCase() :
                                        determineCategoryFromDepartment(department);

                        String color = SUBJECT_COLORS.get(colorIndex % SUBJECT_COLORS.size());
                        colorIndex++;

                        if (name != null && !name.isEmpty()) {
                            subjectsList.add(new Subject(id, name, finalCategory, color));
                            Log.d(TAG, "Added subject: " + name + " | Category: " + finalCategory);
                        } else {
                            Log.w(TAG, "Skipping subject with null or empty name, ID: " + id);
                        }
                    }

                    filteredSubjects.clear();
                    filteredSubjects.addAll(subjectsList);
                    if (subjectsAdapter != null) {
                        subjectsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Notified SubjectsAdapter with " + filteredSubjects.size() + " subjects");
                    } else {
                        Log.e(TAG, "SubjectsAdapter is null in fetchSubjectsFromFirestore");
                    }

                    Log.d(TAG, "Successfully fetched " + subjectsList.size() + " subjects");

                    if (subjectsList.isEmpty()) {
                        Toast.makeText(this, "No subjects found. Please create subjects first.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching subjects", e);
                    Toast.makeText(this, "Failed to load subjects: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String determineCategoryFromDepartment(String department) {
        if (department == null) return "engineering";

        String deptLower = department.toLowerCase();

        if (deptLower.contains("computer") || deptLower.contains("information technology") ||
                deptLower.contains("software") || deptLower.contains("computing")) {
            return "computing";
        }
        if (deptLower.contains("engineering") || deptLower.contains("mechanical") ||
                deptLower.contains("civil") || deptLower.contains("electrical") ||
                deptLower.contains("chemical")) {
            return "engineering";
        }

        return "engineering";
    }

    private void selectTeacher(Teacher teacher) {
        selectedTeacher = teacher;
        tvSelectedTeacherName.setText(teacher.getName());
        tvSelectedTeacherDept.setText(teacher.getDepartment());

        String profileUrl = teacher.getProfilePictureUrl();
        Log.d(TAG, "Profile URL for " + teacher.getName() + ": " + profileUrl);
        if (ivSelectedTeacherProfile != null) {
            if (profileUrl != null && !profileUrl.isEmpty()) {
                try {
                    Glide.with(this)
                            .load(profileUrl)
                            .placeholder(R.drawable.ic_person_filled)
                            .error(R.drawable.ic_person_filled)
                            .circleCrop()
                            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                @Override
                                public boolean onLoadFailed(GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                    Log.e(TAG, "Failed to load profile picture for " + teacher.getName() + ": " + (e != null ? e.getMessage() : "Unknown error"));
                                    ivSelectedTeacherProfile.setImageResource(R.drawable.ic_person_filled);
                                    return true;
                                }

                                @Override
                                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                    Log.d(TAG, "Successfully loaded profile picture for " + teacher.getName());
                                    return false;
                                }
                            })
                            .into(ivSelectedTeacherProfile);
                } catch (Exception e) {
                    Log.e(TAG, "Glide exception for " + teacher.getName(), e);
                    ivSelectedTeacherProfile.setImageResource(R.drawable.ic_person_filled);
                }
            } else {
                Log.w(TAG, "No profile picture URL for " + teacher.getName());
                ivSelectedTeacherProfile.setImageResource(R.drawable.ic_person_filled);
            }
        } else {
            Log.e(TAG, "ivSelectedTeacherProfile is null");
        }

        cardSelectedTeacher.setVisibility(View.VISIBLE);

        selectedSubjects.clear();
        updateSubjectsCount();
        fetchAssignedSubjects(teacher.getId());
        updateAssignButton();

        Log.d(TAG, "Selected teacher: " + teacher.getName());
    }

    private void fetchAssignedSubjects(String teacherId) {
        Log.d(TAG, "Fetching assigned subjects for teacher ID: " + teacherId);
        db.collection("users")
                .document(teacherId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    assignedSubjectIds.clear();
                    if (documentSnapshot.exists()) {
                        List<String> assigned = (List<String>) documentSnapshot.get("assignedSubjects");
                        if (assigned != null) {
                            assignedSubjectIds.addAll(assigned);
                            Log.d(TAG, "Assigned subjects for teacher " + teacherId + ": " + assignedSubjectIds);
                        } else {
                            Log.d(TAG, "No assigned subjects found for teacher " + teacherId);
                        }
                    } else {
                        Log.w(TAG, "No teacher document found for ID " + teacherId);
                    }
                    filterSubjects("All");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching assigned subjects for teacher " + teacherId, e);
                    Toast.makeText(this, "Failed to load assigned subjects: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    filterSubjects("All");
                });
    }

    private void clearTeacherSelection() {
        selectedTeacher = null;
        cardSelectedTeacher.setVisibility(View.GONE);
        if (ivSelectedTeacherProfile != null) {
            ivSelectedTeacherProfile.setImageDrawable(null);
            Log.d(TAG, "Cleared profile picture in iv_selected_teacher_profile");
        }
        selectedSubjects.clear();
        assignedSubjectIds.clear();
        updateSubjectsCount();
        if (subjectsAdapter != null) {
            filterSubjects("All");
            subjectsAdapter.updateSelectedSubjects(selectedSubjects);
        } else {
            Log.e(TAG, "SubjectsAdapter is null in clearTeacherSelection");
        }
        updateAssignButton();

        Log.d(TAG, "Cleared teacher selection");
    }

    private void toggleSubjectSelection(Subject subject) {
        if (selectedTeacher == null) {
            Toast.makeText(this, "Please select a teacher first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subject == null) {
            Log.e(TAG, "Attempted to toggle null subject");
            return;
        }

        if (selectedSubjects.contains(subject)) {
            selectedSubjects.remove(subject);
            subject.setSelected(false);
            Log.d(TAG, "Deselected subject: " + subject.getName() + ", New count: " + selectedSubjects.size());
        } else {
            selectedSubjects.add(subject);
            subject.setSelected(true);
            Log.d(TAG, "Selected subject: " + subject.getName() + ", New count: " + selectedSubjects.size());
        }

        updateSubjectsCount();
        if (subjectsAdapter != null) {
            subjectsAdapter.updateSelectedSubjects(selectedSubjects);
        } else {
            Log.e(TAG, "SubjectsAdapter is null in toggleSubjectSelection");
        }
        updateAssignButton();
    }

    private void filterSubjects(String category) {
        resetCategoryButtons();
        filteredSubjects.clear();

        String filterCategory = category.trim().toLowerCase();

        for (Subject subject : subjectsList) {
            if (assignedSubjectIds.contains(subject.getId())) {
                continue;
            }
            String subjectCategory = subject.getCategory() != null ? subject.getCategory().toLowerCase() : "";
            if ("all".equals(filterCategory) || filterCategory.equals(subjectCategory)) {
                filteredSubjects.add(subject);
            }
        }

        switch (filterCategory) {
            case "all":
                btnAllSubjects.setBackgroundResource(R.drawable.category_button_active_blue);
                break;
            case "engineering":
                btnEngineeringSubjects.setBackgroundResource(R.drawable.category_button_active_blue);
                break;
            case "computing":
                btnComputingSubjects.setBackgroundResource(R.drawable.category_button_active_blue);
                break;
        }

        if (subjectsAdapter != null) {
            subjectsAdapter.notifyDataSetChanged();
            Log.d(TAG, "Filtered subjects by: " + category + ", Count: " + filteredSubjects.size());
        } else {
            Log.e(TAG, "SubjectsAdapter is null in filterSubjects");
        }
    }

    private void resetCategoryButtons() {
        btnAllSubjects.setBackgroundResource(R.drawable.category_button_inactive);
        btnEngineeringSubjects.setBackgroundResource(R.drawable.category_button_inactive);
        btnComputingSubjects.setBackgroundResource(R.drawable.category_button_inactive);
    }

    private void updateSubjectsCount() {
        int count = selectedSubjects.size();
        tvSubjectsCount.setText(String.valueOf(count));
        Log.d(TAG, "Updated tv_subjects_count to: " + count);
    }

    private void updateAssignButton() {
        btnAssignSubjects.setEnabled(selectedTeacher != null && !selectedSubjects.isEmpty());
        Log.d(TAG, "Assign button enabled: " + btnAssignSubjects.isEnabled());
    }

    private void assignSubjectsToTeacher() {
        if (selectedTeacher == null) {
            Toast.makeText(this, "Please select a teacher", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSubjects.isEmpty()) {
            Toast.makeText(this, "Please select at least one subject", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAssignSubjects.setEnabled(false);

        List<String> newSubjectIds = new ArrayList<>();
        List<String> newSubjectNames = new ArrayList<>();

        for (Subject subject : selectedSubjects) {
            newSubjectIds.add(subject.getId());
            newSubjectNames.add(subject.getName());
        }

        Map<String, Object> updates = new HashMap<>();
        for (String id : newSubjectIds) {
            updates.put("assignedSubjects", FieldValue.arrayUnion(id));
        }
        for (String name : newSubjectNames) {
            updates.put("assignedSubjectNames", FieldValue.arrayUnion(name));
        }

        db.collection("users")
                .document(selectedTeacher.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    String message = "Successfully assigned " + newSubjectIds.size() +
                            " new subject(s) to " + selectedTeacher.getName();
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                    Log.d(TAG, "Assigned subjects to teacher: " + selectedTeacher.getId());

                    clearTeacherSelection();
                    btnAssignSubjects.setEnabled(true);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error assigning subjects", e);
                    Toast.makeText(this, "Failed to assign subjects: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    btnAssignSubjects.setEnabled(true);
                });
    }

    // Inner Subject class remains the same as it's UI-specific
    public static class Subject {
        private String id, name, category, color;
        private boolean isSelected;

        public Subject(String id, String name, String category, String color) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.color = color;
            this.isSelected = false;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getColor() { return color; }
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean selected) { isSelected = selected; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Subject subject = (Subject) obj;
            return id.equals(subject.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}