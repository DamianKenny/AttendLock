package com.nibm.attendancetracker.admin;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;

public class StudentDetailActivity extends AppCompatActivity {

    private static final String TAG = "StudentDetailActivity";

    private ImageView btnBack, btnEdit, ivProfile;
    private TextView tvName, tvStudentId, tvEmail, tvPhone, tvDob, tvGender, tvAddress;
    private TextView tvProgramme, tvFaculty, tvBatch, tvAdmissionDate;
    private TextView tvParentName, tvParentEmail;

    private String studentId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_student_detail);

        initViews();
        loadStudentData();
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
        tvStudentId = findViewById(R.id.tv_student_id);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvDob = findViewById(R.id.tv_dob);
        tvGender = findViewById(R.id.tv_gender);
        tvAddress = findViewById(R.id.tv_address);
        tvProgramme = findViewById(R.id.tv_programme);
        tvFaculty = findViewById(R.id.tv_faculty);
        tvBatch = findViewById(R.id.tv_batch);
        tvAdmissionDate = findViewById(R.id.tv_admission_date);
        tvParentName = findViewById(R.id.tv_parent_name);
        tvParentEmail = findViewById(R.id.tv_parent_email);

        db = FirebaseFirestore.getInstance();
    }

    private void loadStudentData() {
        Intent intent = getIntent();
        studentId = intent.getStringExtra("student_id");

        if (studentId != null) {
            // Load fresh data from Firestore
            db.collection("users").document(studentId)
                    .get()
                    .addOnSuccessListener(this::displayStudentData)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading student: " + e.getMessage());
                        Toast.makeText(this, "Failed to load student details", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            // Fallback to intent extras
            displayFromIntent(intent);
        }
    }

    private void displayStudentData(DocumentSnapshot document) {
        if (!document.exists()) {
            Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String name = document.getString("name");
        String studentIdStr = document.getString("studentId");
        String email = document.getString("email");
        String phone = document.getString("phone");
        String dob = document.getString("dob");
        String gender = document.getString("gender");
        String address = document.getString("address");
        String programme = document.getString("programme");
        String faculty = document.getString("faculty");
        String batch = document.getString("batch");
        String admissionDate = document.getString("admissionDate");
        String parentName = document.getString("parentName");
        String parentEmail = document.getString("parentEmail");
        String profileUrl = document.getString("profilePictureUrl");

        tvName.setText(name != null ? name : "N/A");
        tvStudentId.setText(studentIdStr != null ? studentIdStr : "N/A");
        tvEmail.setText(email != null ? email : "N/A");
        tvPhone.setText(phone != null ? phone : "N/A");
        tvDob.setText(dob != null ? dob : "N/A");
        tvGender.setText(gender != null ? gender : "N/A");
        tvAddress.setText(address != null ? address : "N/A");
        tvProgramme.setText(programme != null ? programme : "N/A");
        tvFaculty.setText(faculty != null ? faculty : "N/A");
        tvBatch.setText(batch != null ? batch : "N/A");
        tvAdmissionDate.setText(admissionDate != null ? admissionDate : "N/A");
        tvParentName.setText(parentName != null ? parentName : "N/A");
        tvParentEmail.setText(parentEmail != null ? parentEmail : "N/A");

        // Load profile picture
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_student)
                    .error(R.drawable.ic_student)
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
            ivProfile.setImageResource(R.drawable.ic_student);
        }
    }

    private void displayFromIntent(Intent intent) {
        tvName.setText(intent.getStringExtra("student_name"));
        tvStudentId.setText(intent.getStringExtra("student_student_id"));
        tvEmail.setText(intent.getStringExtra("student_email"));
        tvPhone.setText(intent.getStringExtra("student_phone"));
        tvDob.setText(intent.getStringExtra("student_dob"));
        tvGender.setText(intent.getStringExtra("student_gender"));
        tvAddress.setText(intent.getStringExtra("student_address"));
        tvProgramme.setText(intent.getStringExtra("student_programme"));
        tvFaculty.setText(intent.getStringExtra("student_faculty"));
        tvBatch.setText(intent.getStringExtra("student_batch"));
        tvAdmissionDate.setText(intent.getStringExtra("student_admission_date"));
        tvParentName.setText(intent.getStringExtra("student_parent_name"));
        tvParentEmail.setText(intent.getStringExtra("student_parent_email"));

        String profileUrl = intent.getStringExtra("student_profile_url");
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .placeholder(R.drawable.ic_student)
                    .error(R.drawable.ic_student)
                    .circleCrop()
                    .into(ivProfile);
        } else {
            ivProfile.setImageResource(R.drawable.ic_student);
        }
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            if (studentId != null && !studentId.isEmpty()) {
                Intent intent = new Intent(StudentDetailActivity.this, EditStudentActivity.class);
                intent.putExtra("student_id", studentId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Student ID not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload student data when returning from edit activity
        if (studentId != null) {
            db.collection("users").document(studentId)
                    .get()
                    .addOnSuccessListener(this::displayStudentData);
        }
    }
}
