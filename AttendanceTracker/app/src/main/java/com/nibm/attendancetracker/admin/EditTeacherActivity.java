package com.nibm.attendancetracker.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.models.Teacher;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditTeacherActivity extends AppCompatActivity {

    private static final String TAG = "EditTeacherActivity";

    private ImageView btnBack;
    private EditText etFirstName, etLastName, etEmail, etPhone;
    private EditText etDob, etJoinDate, etDepartment, etQualification;
    private MaterialButton btnSave;

    private String teacherId;
    private Teacher currentTeacher;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_edit_teacher);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupDatePickers();
        loadTeacherData();
        setupSaveButton();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDob = findViewById(R.id.et_dob);
        etJoinDate = findViewById(R.id.et_join_date);
        etDepartment = findViewById(R.id.et_department);
        etQualification = findViewById(R.id.et_qualification);
        btnSave = findViewById(R.id.btn_save);

        db = FirebaseFirestore.getInstance();
        teacherId = getIntent().getStringExtra("teacher_id");
        currentTeacher = new Teacher();

        if (teacherId == null) {
            Toast.makeText(this, "Teacher ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupDatePickers() {
        etDob.setOnClickListener(v -> showDatePicker(etDob));
        etJoinDate.setOnClickListener(v -> showDatePicker(etJoinDate));
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    editText.setText(date);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void loadTeacherData() {
        if (teacherId == null) {
            Toast.makeText(this, "Teacher ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(teacherId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Set current teacher data
                        currentTeacher.setId(document.getId());
                        currentTeacher.setFirstName(document.getString("firstName"));
                        currentTeacher.setLastName(document.getString("lastName"));
                        currentTeacher.setEmail(document.getString("email"));
                        currentTeacher.setPhone(document.getString("phone"));
                        currentTeacher.setDob(document.getString("dob"));
                        currentTeacher.setJoinDate(document.getString("joinDate"));
                        currentTeacher.setDepartment(document.getString("department"));
                        currentTeacher.setQualification(document.getString("qualification"));
                        currentTeacher.setEmployeeId(document.getString("employeeId"));
                        currentTeacher.setPassword(document.getString("password"));
                        currentTeacher.setProfilePictureUrl(document.getString("profilePictureUrl"));

                        // Populate form fields
                        etFirstName.setText(currentTeacher.getFirstName());
                        etLastName.setText(currentTeacher.getLastName());
                        etEmail.setText(currentTeacher.getEmail());
                        etPhone.setText(currentTeacher.getPhone());
                        etDob.setText(currentTeacher.getDob());
                        etJoinDate.setText(currentTeacher.getJoinDate());
                        etDepartment.setText(currentTeacher.getDepartment());
                        etQualification.setText(currentTeacher.getQualification());

                        Log.d(TAG, "Teacher data loaded successfully: " +
                                (currentTeacher.getFirstName() + " " + currentTeacher.getLastName()));
                    } else {
                        Toast.makeText(this, "Teacher not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading teacher: " + e.getMessage());
                    Toast.makeText(this, "Failed to load teacher data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> saveTeacherData());
    }

    private void saveTeacherData() {
        // Validate inputs
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String joinDate = etJoinDate.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String qualification = etQualification.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // Disable save button to prevent multiple clicks
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Create full name
        String fullName = firstName + " " + lastName;

        // Prepare update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("name", fullName);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("dob", dob);
        updates.put("joinDate", joinDate);
        updates.put("department", department);
        updates.put("qualification", qualification);

        // Update Firestore
        db.collection("users").document(teacherId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Teacher updated successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Teacher updated: " + teacherId);

                    // Set result to refresh the previous activity
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating teacher: " + e.getMessage());
                    Toast.makeText(this, "Failed to update teacher: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Details");
                });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}