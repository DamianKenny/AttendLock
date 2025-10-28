package com.nibm.attendancetracker.admin;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.models.Teacher;

import java.util.ArrayList;
import java.util.List;

public class TeacherDetailActivity extends AppCompatActivity {

    private static final String TAG = "TeacherDetailActivity";
    private static final int EDIT_TEACHER_REQUEST = 1001;

    private ImageView btnBack, btnEdit, ivProfile;
    private TextView tvName, tvEmployeeId, tvEmail, tvPhone, tvDob, tvJoinDate;
    private TextView tvDepartment, tvQualification, tvNoSubjects;
    private RecyclerView rvSubjects;

    private String teacherId;
    private Teacher currentTeacher;
    private SubjectsAdapter subjectsAdapter;
    private List<String> subjectNames;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_teacher_detail);

        initViews();
        loadTeacherData();
        setupButtons();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEdit = findViewById(R.id.btn_edit);
        ivProfile = findViewById(R.id.iv_profile);
        tvName = findViewById(R.id.tv_name);
        tvEmployeeId = findViewById(R.id.tv_employee_id);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvDob = findViewById(R.id.tv_dob);
        tvJoinDate = findViewById(R.id.tv_join_date);
        tvDepartment = findViewById(R.id.tv_department);
        tvQualification = findViewById(R.id.tv_qualification);
        tvNoSubjects = findViewById(R.id.tv_no_subjects);
        rvSubjects = findViewById(R.id.rv_subjects);

        db = FirebaseFirestore.getInstance();
        subjectNames = new ArrayList<>();
        currentTeacher = new Teacher();

        // Setup subjects RecyclerView
        subjectsAdapter = new SubjectsAdapter(subjectNames);
        rvSubjects.setLayoutManager(new LinearLayoutManager(this));
        rvSubjects.setAdapter(subjectsAdapter);
    }

    private void loadTeacherData() {
        Intent intent = getIntent();
        teacherId = intent.getStringExtra("teacher_id");

        if (teacherId != null) {
            // Load fresh data from Firestore
            db.collection("users").document(teacherId)
                    .get()
                    .addOnSuccessListener(this::displayTeacherData)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading teacher: " + e.getMessage());
                        Toast.makeText(this, "Failed to load teacher details", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            // Fallback to intent extras
            displayFromIntent(intent);
        }
    }

    private void displayTeacherData(DocumentSnapshot document) {
        if (!document.exists()) {
            Toast.makeText(this, "Teacher not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Store teacher data
        currentTeacher.setId(document.getId());
        currentTeacher.setName(document.getString("name"));
        currentTeacher.setFirstName(document.getString("firstName"));
        currentTeacher.setLastName(document.getString("lastName"));
        currentTeacher.setEmployeeId(document.getString("employeeId"));
        currentTeacher.setEmail(document.getString("email"));
        currentTeacher.setPhone(document.getString("phone"));
        currentTeacher.setDob(document.getString("dob"));
        currentTeacher.setJoinDate(document.getString("joinDate"));
        currentTeacher.setDepartment(document.getString("department"));
        currentTeacher.setQualification(document.getString("qualification"));
        currentTeacher.setProfilePictureUrl(document.getString("profilePictureUrl"));

        // Update UI
        tvName.setText(currentTeacher.getName() != null ? currentTeacher.getName() : "N/A");
        tvEmployeeId.setText("ID: " + (currentTeacher.getEmployeeId() != null ? currentTeacher.getEmployeeId() : "N/A"));
        tvEmail.setText(currentTeacher.getEmail() != null ? currentTeacher.getEmail() : "N/A");
        tvPhone.setText(currentTeacher.getPhone() != null ? currentTeacher.getPhone() : "N/A");
        tvDob.setText(currentTeacher.getDob() != null ? currentTeacher.getDob() : "N/A");
        tvJoinDate.setText(currentTeacher.getJoinDate() != null ? currentTeacher.getJoinDate() : "N/A");
        tvDepartment.setText(currentTeacher.getDepartment() != null ? currentTeacher.getDepartment() : "N/A");
        tvQualification.setText(currentTeacher.getQualification() != null ? currentTeacher.getQualification() : "N/A");

        // Load profile picture
        String profileUrl = currentTeacher.getProfilePictureUrl();
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_teacher)
                    .error(R.drawable.ic_teacher)
                    .circleCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Failed to load profile picture: " + (e != null ? e.getMessage() : "Unknown error"));
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_teacher);
        }

        // Load assigned subjects
        if (document.contains("assignedSubjectNames")) {
            List<String> subjects = (List<String>) document.get("assignedSubjectNames");
            if (subjects != null && !subjects.isEmpty()) {
                subjectNames.clear();
                subjectNames.addAll(subjects);
                subjectsAdapter.notifyDataSetChanged();
                rvSubjects.setVisibility(View.VISIBLE);
                tvNoSubjects.setVisibility(View.GONE);
            } else {
                rvSubjects.setVisibility(View.GONE);
                tvNoSubjects.setVisibility(View.VISIBLE);
            }
        } else {
            rvSubjects.setVisibility(View.GONE);
            tvNoSubjects.setVisibility(View.VISIBLE);
        }
    }

    private void displayFromIntent(Intent intent) {
        tvName.setText(intent.getStringExtra("teacher_name"));
        tvEmployeeId.setText("ID: " + intent.getStringExtra("teacher_employee_id"));
        tvEmail.setText(intent.getStringExtra("teacher_email"));
        tvPhone.setText(intent.getStringExtra("teacher_phone"));
        tvDob.setText(intent.getStringExtra("teacher_dob"));
        tvJoinDate.setText(intent.getStringExtra("teacher_join_date"));
        tvDepartment.setText(intent.getStringExtra("teacher_dept"));
        tvQualification.setText(intent.getStringExtra("teacher_qualification"));

        String profileUrl = intent.getStringExtra("teacher_profile_url");
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_teacher)
                    .error(R.drawable.ic_teacher)
                    .circleCrop()
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_teacher);
        }
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            if (teacherId == null) {
                Toast.makeText(this, "Teacher ID not available", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, EditTeacherActivity.class);
            intent.putExtra("teacher_id", teacherId);

            // Pass current teacher data for better UX
            if (currentTeacher != null) {
                intent.putExtra("teacher_name", currentTeacher.getName());
                intent.putExtra("teacher_email", currentTeacher.getEmail());
                intent.putExtra("teacher_phone", currentTeacher.getPhone());
                intent.putExtra("teacher_dob", currentTeacher.getDob());
                intent.putExtra("teacher_join_date", currentTeacher.getJoinDate());
                intent.putExtra("teacher_dept", currentTeacher.getDepartment());
                intent.putExtra("teacher_qualification", currentTeacher.getQualification());
                intent.putExtra("teacher_employee_id", currentTeacher.getEmployeeId());
            }

            startActivityForResult(intent, EDIT_TEACHER_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_TEACHER_REQUEST) {
            if (resultCode == RESULT_OK) {
                // Teacher was updated successfully, refresh the data
                Toast.makeText(this, "Teacher updated successfully", Toast.LENGTH_SHORT).show();
                loadTeacherData();
            } else if (resultCode == RESULT_CANCELED) {
                // Edit was cancelled, no action needed
                Log.d(TAG, "Edit teacher cancelled");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload teacher data when returning from any activity
        if (teacherId != null) {
            db.collection("users").document(teacherId)
                    .get()
                    .addOnSuccessListener(this::displayTeacherData)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error reloading teacher data: " + e.getMessage());
                    });
        }
    }

    // Simple adapter for displaying subject names
    private static class SubjectsAdapter extends RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder> {

        private final List<String> subjects;

        SubjectsAdapter(List<String> subjects) {
            this.subjects = subjects;
        }

        @Override
        public SubjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subject_chip, parent, false);
            return new SubjectViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SubjectViewHolder holder, int position) {
            holder.tvSubject.setText(subjects.get(position));
        }

        @Override
        public int getItemCount() {
            return subjects.size();
        }

        static class SubjectViewHolder extends RecyclerView.ViewHolder {
            TextView tvSubject;

            SubjectViewHolder(View itemView) {
                super(itemView);
                tvSubject = itemView.findViewById(R.id.tv_subject_name);
            }
        }
    }
}