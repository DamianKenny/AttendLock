package com.nibm.attendancetracker.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditStudentActivity extends AppCompatActivity {

    private static final String TAG = "EditStudentActivity";

    private ImageView btnBack;
    private EditText etFirstName, etLastName, etEmail, etPhone, etDob;
    private EditText etAddress, etProgramme, etFaculty, etBatch, etAdmissionDate;
    private EditText etParentName, etParentEmail;
    private Spinner spinnerGender;
    private MaterialButton btnSave; // Changed from CardView to MaterialButton

    private String studentId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_edit_student);

        initViews();
        setupGenderSpinner();
        setupDatePickers();
        loadStudentData();
        setupSaveButton();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDob = findViewById(R.id.et_dob);
        spinnerGender = findViewById(R.id.spinner_gender);
        etAddress = findViewById(R.id.et_address);
        etProgramme = findViewById(R.id.et_programme);
        etFaculty = findViewById(R.id.et_faculty);
        etBatch = findViewById(R.id.et_batch);
        etAdmissionDate = findViewById(R.id.et_admission_date);
        etParentName = findViewById(R.id.et_parent_name);
        etParentEmail = findViewById(R.id.et_parent_email);
        btnSave = findViewById(R.id.btn_save); // This is MaterialButton, not CardView

        db = FirebaseFirestore.getInstance();
        studentId = getIntent().getStringExtra("student_id");

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupGenderSpinner() {
        String[] genders = {"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                genders
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);
    }

    private void setupDatePickers() {
        etDob.setOnClickListener(v -> showDatePicker(etDob));
        etAdmissionDate.setOnClickListener(v -> showDatePicker(etAdmissionDate));
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

    private void loadStudentData() {
        if (studentId == null) {
            Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(studentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        etFirstName.setText(document.getString("firstName"));
                        etLastName.setText(document.getString("lastName"));
                        etEmail.setText(document.getString("email"));
                        etPhone.setText(document.getString("phone"));
                        etDob.setText(document.getString("dob"));
                        etAddress.setText(document.getString("address"));
                        etProgramme.setText(document.getString("programme"));
                        etFaculty.setText(document.getString("faculty"));
                        etBatch.setText(document.getString("batch"));
                        etAdmissionDate.setText(document.getString("admissionDate"));
                        etParentName.setText(document.getString("parentName"));
                        etParentEmail.setText(document.getString("parentEmail"));

                        // Set gender spinner
                        String gender = document.getString("gender");
                        if (gender != null) {
                            ArrayAdapter adapter = (ArrayAdapter) spinnerGender.getAdapter();
                            int position = adapter.getPosition(gender);
                            if (position >= 0) {
                                spinnerGender.setSelection(position);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Student not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading student: " + e.getMessage());
                    Toast.makeText(this, "Failed to load student data", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> saveStudentData());
    }

    private void saveStudentData() {
        // Validate inputs
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();
        String address = etAddress.getText().toString().trim();
        String programme = etProgramme.getText().toString().trim();
        String faculty = etFaculty.getText().toString().trim();
        String batch = etBatch.getText().toString().trim();
        String admissionDate = etAdmissionDate.getText().toString().trim();
        String parentName = etParentName.getText().toString().trim();
        String parentEmail = etParentEmail.getText().toString().trim();

        if (firstName.isEmpty()) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        // Disable save button to prevent multiple clicks
        btnSave.setEnabled(false);

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
        updates.put("gender", gender);
        updates.put("address", address);
        updates.put("programme", programme);
        updates.put("faculty", faculty);
        updates.put("batch", batch);
        updates.put("admissionDate", admissionDate);
        updates.put("parentName", parentName);
        updates.put("parentEmail", parentEmail);

        // Update Firestore
        db.collection("users").document(studentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Student updated successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Student updated: " + studentId);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating student: " + e.getMessage());
                    Toast.makeText(this, "Failed to update student", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }
}