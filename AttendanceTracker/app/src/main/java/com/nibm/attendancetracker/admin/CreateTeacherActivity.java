package com.nibm.attendancetracker.admin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nibm.attendancetracker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateTeacherActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 100;

    private ImageView btnBack, ivProfilePicture;
    private Button btnSelectPhoto, btnCancel, btnCreateTeacher;
    private TextInputEditText etFirstName, etLastName, etEmail, etPhone, etDateOfBirth;
    private TextInputEditText etEmployeeId, etJoinDate, etQualification, etPassword, etConfirmPassword;
    private AutoCompleteTextView spinnerDepartment, spinnerSubject;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri imageUri;
    private String selectedSubject;

    private final String[] departments = {
            "School of Computing",
            "School of Engineering",
            "School of Business",
            "School of Language",
            "School of Design",
            "School of Humanities"
    };

    private final String[] subjects = {
            "Introduction to Programming",
            "Database Management",
            "Business Analytics",
            "Structural Engineering",
            "English Literature",
            "Ethics in Technology",
            "Fashion Design Basics",
            "Project Management",
            "Graphic Design Fundamentals",
            "Psychology"
    };

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_create_teacher);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                imageUri = result.getData().getData();
                Log.d("CreateTeacher", "Image URI: " + imageUri);
                ivProfilePicture.setImageURI(imageUri);
            } else {
                Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        initViews();
        setupSpinners();
        setupClickListeners();
        requestStoragePermission();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivProfilePicture = findViewById(R.id.iv_profile_picture);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etDateOfBirth = findViewById(R.id.et_date_of_birth);
        etEmployeeId = findViewById(R.id.et_employee_id);
        spinnerDepartment = findViewById(R.id.spinner_department);
        spinnerSubject = findViewById(R.id.spinner_subject);
        etJoinDate = findViewById(R.id.et_join_date);
        etQualification = findViewById(R.id.et_qualification);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnCancel = findViewById(R.id.btn_cancel);
        btnCreateTeacher = findViewById(R.id.btn_create_teacher);
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, STORAGE_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    private void setupSpinners() {
        // Department spinner
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, departments);
        spinnerDepartment.setAdapter(departmentAdapter);

        // Subject spinner
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, subjects);
        spinnerSubject.setAdapter(subjectAdapter);
        spinnerSubject.setOnItemClickListener((parent, view, position, id) -> {
            selectedSubject = subjects[position];
            Log.d("CreateTeacher", "Selected subject: " + selectedSubject);
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSelectPhoto.setOnClickListener(v -> selectProfilePhoto());

        etDateOfBirth.setOnClickListener(v -> showDatePickerDialog(etDateOfBirth));

        etJoinDate.setOnClickListener(v -> showDatePickerDialog(etJoinDate));

        btnCancel.setOnClickListener(v -> finish());

        btnCreateTeacher.setOnClickListener(v -> createTeacher());
    }

    private void selectProfilePhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private void showDatePickerDialog(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    editText.setText(dateFormat.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void createTeacher() {
        if (!validateInputs()) {
            return;
        }

        Teacher teacher = new Teacher();
        teacher.setFirstName(etFirstName.getText().toString().trim());
        teacher.setLastName(etLastName.getText().toString().trim());
        teacher.setEmail(etEmail.getText().toString().trim());
        teacher.setPhone(etPhone.getText().toString().trim());
        teacher.setDateOfBirth(etDateOfBirth.getText().toString().trim());
        teacher.setEmployeeId(etEmployeeId.getText().toString().trim());
        teacher.setDepartment(spinnerDepartment.getText().toString().trim());
        teacher.setJoinDate(etJoinDate.getText().toString().trim());
        teacher.setQualification(etQualification.getText().toString().trim());
        teacher.setPassword(etPassword.getText().toString().trim());
        teacher.setImageUri(imageUri);
        teacher.setSubjects(selectedSubject != null ? Arrays.asList(selectedSubject) : new ArrayList<>());

        saveTeacherToDatabase(teacher);
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etFirstName.getText())) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etLastName.getText())) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etEmail.getText())) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString().trim()).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etPhone.getText())) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etDateOfBirth.getText())) {
            etDateOfBirth.setError("Date of birth is required");
            etDateOfBirth.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etEmployeeId.getText())) {
            etEmployeeId.setError("Employee ID is required");
            etEmployeeId.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(spinnerDepartment.getText())) {
            spinnerDepartment.setError("Please select a department");
            spinnerDepartment.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(spinnerSubject.getText())) {
            spinnerSubject.setError("Please select a subject");
            spinnerSubject.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etJoinDate.getText())) {
            etJoinDate.setError("Join date is required");
            etJoinDate.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etQualification.getText())) {
            etQualification.setError("Qualification is required");
            etQualification.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etPassword.getText())) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return false;
        }
        if (etPassword.getText().toString().length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etConfirmPassword.getText())) {
            etConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return false;
        }
        if (!etPassword.getText().toString().equals(etConfirmPassword.getText().toString())) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }
        if (imageUri == null) {
            Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveTeacherToDatabase(Teacher teacher) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("role", "teacher");
        userData.put("name", teacher.getFirstName() + " " + teacher.getLastName());
        userData.put("email", teacher.getEmail());
        userData.put("phone", teacher.getPhone());
        userData.put("firstName", teacher.getFirstName());
        userData.put("lastName", teacher.getLastName());
        userData.put("dob", teacher.getDateOfBirth());
        userData.put("employeeId", teacher.getEmployeeId());
        userData.put("department", teacher.getDepartment());
        userData.put("joinDate", teacher.getJoinDate());
        userData.put("qualification", teacher.getQualification());
        userData.put("password", teacher.getPassword()); // Note: In production, hash the password
        userData.put("subjects", teacher.getSubjects());

        if (teacher.getImageUri() != null) {
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("profile_pictures/" + teacher.getEmployeeId() + ".jpg");

            Log.d("CreateTeacher", "Uploading image to: " + imageRef.getPath());
            UploadTask uploadTask = imageRef.putFile(teacher.getImageUri());
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                Log.d("CreateTeacher", "Upload progress: " + progress + "%");
            }).continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    Log.e("CreateTeacher", "Image upload failed", task.getException());
                    throw task.getException();
                }
                return imageRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    Log.d("CreateTeacher", "Image uploaded, download URL: " + downloadUri);
                    userData.put("profilePictureUrl", downloadUri.toString());

                    db.collection("users")
                            .document(teacher.getEmployeeId())
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("CreateTeacher", "Teacher document saved with ID: " + teacher.getEmployeeId());
                                Toast.makeText(CreateTeacherActivity.this, "Teacher created successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.w("CreateTeacher", "Error adding document", e);
                                Toast.makeText(CreateTeacherActivity.this, "Failed to create teacher: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                } else {
                    Log.w("CreateTeacher", "Error getting download URL", task.getException());
                    Toast.makeText(CreateTeacherActivity.this, "Failed to upload image: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            db.collection("users")
                    .document(teacher.getEmployeeId())
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("CreateTeacher", "Teacher document saved with ID: " + teacher.getEmployeeId());
                        Toast.makeText(CreateTeacherActivity.this, "Teacher created successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.w("CreateTeacher", "Error adding document", e);
                        Toast.makeText(CreateTeacherActivity.this, "Failed to create teacher: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    public static class Teacher {
        private String firstName, lastName, email, phone, dateOfBirth;
        private String employeeId, department, joinDate, qualification, password;
        private Uri imageUri;
        private List<String> subjects;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getJoinDate() { return joinDate; }
        public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

        public String getQualification() { return qualification; }
        public void setQualification(String qualification) { this.qualification = qualification; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Uri getImageUri() { return imageUri; }
        public void setImageUri(Uri imageUri) { this.imageUri = imageUri; }

        public List<String> getSubjects() { return subjects; }
        public void setSubjects(List<String> subjects) { this.subjects = subjects; }
    }
}