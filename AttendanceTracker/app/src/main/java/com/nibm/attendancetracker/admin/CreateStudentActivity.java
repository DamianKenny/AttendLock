package com.nibm.attendancetracker.admin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nibm.attendancetracker.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateStudentActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView btnBack, ivProfilePicture;
    private Button btnSelectPhoto, btnCancel, btnCreateStudent;
    private TextInputEditText etFirstName, etLastName, etDateOfBirth, etStudentId, etBatch;
    private TextInputEditText etRollNumber, etAdmissionDate, etParentName, etParentPhone;
    private TextInputEditText etParentEmail, etAddress, etStudentEmail, etPassword, etConfirmPassword;
    private AutoCompleteTextView spinnerGender, spinnerFaculty, spinnerProgramme, spinnerCourses;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri imageUri;

    private final String[] genders = {"Male", "Female", "Other"};
    private final String[] faculties = {
            "School of Computing",
            "School of Engineering",
            "School of Business",
            "School of Language",
            "School of Design",
            "School of Humanities"
    };
    private final List<String> computingProgrammes = Arrays.asList(
            "BSc (Hons) in Ethical Hacking and Network Security",
            "BSc (Hons) in Information Technology for Business",
            "BSc (Hons) in Computing (Software Engineering Pathway)",
            "BA (Hons) in Creative Multimedia",
            "Higher National Diploma in Network Engineering (Part Time)",
            "Higher National Diploma in Software Engineering (Part Time)",
            "Higher National Diploma in Network Engineering (Full Time)",
            "Higher National Diploma in Computer Science with Artificial Intelligence",
            "Diploma in Information and Communication Technology",
            "Diploma in Network Engineering (Part Time)",
            "Diploma in Software Engineering (Full Time)",
            "Diploma in Computer Science with Artificial Intelligence"
    );
    private final List<String> businessProgrammes = Arrays.asList(
            "Advanced Diploma in Business Management",
            "Higher National Diploma in Business Management",
            "Advanced Diploma in Marketing Management",
            "Advanced Diploma in Human Resource Management",
            "Advanced Diploma in Project Management",
            "Advanced Diploma in Logistics Management",
            "Advanced Diploma in Manufacturing Management",
            "Advanced Diploma in Maintenance Management",
            "Advanced Diploma in Supplies & Materials Management",
            "Advanced Diploma in Financial & Management Accounting",
            "Advanced Diploma in Company Administration & Secretarial Proficiency",
            "Advanced Certificate in Business Management",
            "Advanced Certificate in Marketing Management",
            "Advanced Certificate in Human Resource Management",
            "Advanced Certificate in Financial & Management Accounting",
            "Certificate Course in Entrepreneurship",
            "Certificate in Psychological Counselling Skills",
            "Foundation Course in Human Resource Management",
            "Foundation Course in Marketing Management"
    );
    private final List<String> engineeringProgrammes = Arrays.asList(
            "Degree in Civil Engineering",
            "Degree in Electro-Mechanical Engineering",
            "Degree in Quantity Surveying",
            "Diploma in Civil Engineering",
            "Diploma in Electro-Mechanical Engineering",
            "Diploma in Quantity Surveying",
            "Advanced Diploma in AI & Robotics",
            "Advanced Diploma in Mechatronics",
            "Advanced Diploma in Construction Management",
            "Diploma in CAD/CAM",
            "Diploma in Robotics & IoT",
            "Certificate in CAD/CAM",
            "Certificate in Robotics & IoT"
    );
    private final List<String> languageProgrammes = Arrays.asList(
            "Diploma in English Language",
            "Certificate in English Language",
            "Diploma in Other Languages",
            "Certificate in Other Languages"
    );
    private final List<String> designProgrammes = Arrays.asList(
            "Diploma in Fashion Design",
            "Higher National Diploma in Fashion Design",
            "Diploma in Interior Architecture",
            "Higher National Diploma in Interior Architecture",
            "Certificate in Digital Photography",
            "Certificate in Graphic Designing",
            "Certificate in Event Management"
    );
    private final List<String> humanitiesProgrammes = Arrays.asList(
            "Diploma in Humanities",
            "Certificate in Humanities"
    );
    private final String[] courses = {
            "Introduction to Programming",
            "Database Management",
            "Business Analytics",
            "Structural Engineering",
            "English Literature",
            "Fashion Design Basics",
            "Ethics in Technology",
            "Project Management",
            "Graphic Design Fundamentals"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_create_student);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupSpinners();
        setupClickListeners();

        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivProfilePicture = findViewById(R.id.iv_profile_picture);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etDateOfBirth = findViewById(R.id.et_date_of_birth);
        spinnerGender = findViewById(R.id.spinner_gender);
        etStudentId = findViewById(R.id.et_student_id);
        spinnerFaculty = findViewById(R.id.spinner_grade);
        spinnerProgramme = findViewById(R.id.spinner_section);
        spinnerCourses = findViewById(R.id.spinner_courses);
        etRollNumber = findViewById(R.id.et_roll_number);
        etAdmissionDate = findViewById(R.id.et_admission_date);
        etParentName = findViewById(R.id.et_parent_name);
        etParentPhone = findViewById(R.id.et_parent_phone);
        etParentEmail = findViewById(R.id.et_parent_email);
        etAddress = findViewById(R.id.et_address);
        etStudentEmail = findViewById(R.id.et_student_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnCancel = findViewById(R.id.btn_cancel);
        btnCreateStudent = findViewById(R.id.btn_create_student);
        etBatch = findViewById(R.id.et_batch);
    }

    private void setupSpinners() {
        // Gender spinner
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(genderAdapter);

        // Faculty spinner
        ArrayAdapter<String> facultyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, faculties);
        spinnerFaculty.setAdapter(facultyAdapter);

        // Programme spinner (initially empty)
        ArrayAdapter<String> programmeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new String[]{});
        spinnerProgramme.setAdapter(programmeAdapter);

        // Courses spinner
        ArrayAdapter<String> coursesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, courses);
        spinnerCourses.setAdapter(coursesAdapter);

        // Update programme dropdown based on faculty selection
        spinnerFaculty.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFaculty = faculties[position];
            List<String> programmes;
            switch (selectedFaculty) {
                case "School of Computing":
                    programmes = computingProgrammes;
                    break;
                case "School of Business":
                    programmes = businessProgrammes;
                    break;
                case "School of Engineering":
                    programmes = engineeringProgrammes;
                    break;
                case "School of Language":
                    programmes = languageProgrammes;
                    break;
                case "School of Design":
                    programmes = designProgrammes;
                    break;
                case "School of Humanities":
                    programmes = humanitiesProgrammes;
                    break;
                default:
                    programmes = Arrays.asList();
            }
            ArrayAdapter<String> newProgrammeAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, programmes);
            spinnerProgramme.setAdapter(newProgrammeAdapter);
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSelectPhoto.setOnClickListener(v -> selectProfilePhoto());

        etDateOfBirth.setOnClickListener(v -> showDatePickerDialog(etDateOfBirth));

        etAdmissionDate.setOnClickListener(v -> showDatePickerDialog(etAdmissionDate));

        btnCancel.setOnClickListener(v -> finish());

        btnCreateStudent.setOnClickListener(v -> createStudent());
    }

    private void selectProfilePhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivProfilePicture.setImageURI(imageUri);
        }
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

    private void createStudent() {
        if (!validateInputs()) {
            return;
        }

        // Create student object
        Student student = new Student();
        student.setFirstName(etFirstName.getText().toString().trim());
        student.setLastName(etLastName.getText().toString().trim());
        student.setDateOfBirth(etDateOfBirth.getText().toString().trim());
        student.setGender(spinnerGender.getText().toString().trim());
        student.setStudentId(etStudentId.getText().toString().trim());
        student.setFaculty(spinnerFaculty.getText().toString().trim());
        student.setProgramme(spinnerProgramme.getText().toString().trim());
        student.setCourses(spinnerCourses.getText().toString().trim());
        student.setBatch(etBatch.getText().toString().trim());
        student.setRollNumber(etRollNumber.getText().toString().trim());
        student.setAdmissionDate(etAdmissionDate.getText().toString().trim());
        student.setParentName(etParentName.getText().toString().trim());
        student.setParentPhone(etParentPhone.getText().toString().trim());
        student.setParentEmail(etParentEmail.getText().toString().trim());
        student.setAddress(etAddress.getText().toString().trim());
        student.setStudentEmail(etStudentEmail.getText().toString().trim());
        student.setPassword(etPassword.getText().toString().trim());
        student.setImageUri(imageUri);

        // Save to Firestore and Storage
        saveStudentToDatabase(student);
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
        if (TextUtils.isEmpty(etDateOfBirth.getText())) {
            etDateOfBirth.setError("Date of birth is required");
            etDateOfBirth.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(spinnerGender.getText())) {
            spinnerGender.setError("Please select gender");
            spinnerGender.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etStudentId.getText())) {
            etStudentId.setError("Student ID is required");
            etStudentId.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(spinnerFaculty.getText())) {
            spinnerFaculty.setError("Please select faculty");
            spinnerFaculty.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(spinnerProgramme.getText())) {
            spinnerProgramme.setError("Please select programme");
            spinnerProgramme.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(spinnerCourses.getText())) {
            spinnerCourses.setError("Please select a course");
            spinnerCourses.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etBatch.getText())) {
            etBatch.setError("Student Batch is required");
            etBatch.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etRollNumber.getText())) {
            etRollNumber.setError("Roll number is required");
            etRollNumber.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etAdmissionDate.getText())) {
            etAdmissionDate.setError("Admission date is required");
            etAdmissionDate.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etParentName.getText())) {
            etParentName.setError("Parent/Guardian name is required");
            etParentName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(etParentPhone.getText())) {
            etParentPhone.setError("Parent phone number is required");
            etParentPhone.requestFocus();
            return false;
        }
        if (!TextUtils.isEmpty(etParentEmail.getText())) {
            String parentEmail = etParentEmail.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(parentEmail).matches()) {
                etParentEmail.setError("Please enter a valid parent email address");
                etParentEmail.requestFocus();
                return false;
            }
        }
        if (!TextUtils.isEmpty(etStudentEmail.getText())) {
            String studentEmail = etStudentEmail.getText().toString().trim();
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(studentEmail).matches()) {
                etStudentEmail.setError("Please enter a valid student email address");
                etStudentEmail.requestFocus();
                return false;
            }
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

    private void saveStudentToDatabase(Student student) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("role", "student");
        userData.put("name", student.getFirstName() + " " + student.getLastName());
        userData.put("email", student.getStudentEmail());
        userData.put("phone", student.getParentPhone());
        userData.put("firstName", student.getFirstName());
        userData.put("lastName", student.getLastName());
        userData.put("dob", student.getDateOfBirth());
        userData.put("gender", student.getGender());
        userData.put("studentId", student.getStudentId());
        userData.put("faculty", student.getFaculty());
        userData.put("programme", student.getProgramme());
        userData.put("courses", student.getCourses());
        userData.put("batch", student.getBatch());
        userData.put("roll", student.getRollNumber());
        userData.put("admissionDate", student.getAdmissionDate());
        userData.put("parentName", student.getParentName());
        userData.put("parentEmail", student.getParentEmail());
        userData.put("address", student.getAddress());
        userData.put("password", student.getPassword()); // Note: In production, hash the password

        if (student.getImageUri() != null) {
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("profile_pictures/" + student.getStudentId() + ".jpg");

            UploadTask uploadTask = imageRef.putFile(student.getImageUri());
            uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return imageRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    userData.put("profilePictureUrl", downloadUri.toString());

                    db.collection("users")
                            .document(student.getStudentId())
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("CreateStudent", "Student document saved with ID: " + student.getStudentId());
                                Toast.makeText(CreateStudentActivity.this, "Student created successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.w("CreateStudent", "Error adding document", e);
                                Toast.makeText(CreateStudentActivity.this, "Failed to create student: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                } else {
                    Log.w("CreateStudent", "Error getting download URL", task.getException());
                    Toast.makeText(CreateStudentActivity.this, "Failed to upload image: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            db.collection("users")
                    .document(student.getStudentId())
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("CreateStudent", "Student document saved with ID: " + student.getStudentId());
                        Toast.makeText(CreateStudentActivity.this, "Student created successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.w("CreateStudent", "Error adding document", e);
                        Toast.makeText(CreateStudentActivity.this, "Failed to create student: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    public static class Student {
        private String firstName, lastName, dateOfBirth, gender, studentId, batch;
        private String faculty, programme, courses, rollNumber, admissionDate;
        private String parentName, parentPhone, parentEmail, address;
        private String studentEmail, password;
        private Uri imageUri;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getFaculty() { return faculty; }
        public void setFaculty(String faculty) { this.faculty = faculty; }

        public String getProgramme() { return programme; }
        public void setProgramme(String programme) { this.programme = programme; }

        public String getCourses() { return courses; }
        public void setCourses(String courses) { this.courses = courses; }
        public String getBatch() { return batch; }
        public void setBatch(String batch) { this.batch = batch; }

        public String getRollNumber() { return rollNumber; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

        public String getAdmissionDate() { return admissionDate; }
        public void setAdmissionDate(String admissionDate) { this.admissionDate = admissionDate; }

        public String getParentName() { return parentName; }
        public void setParentName(String parentName) { this.parentName = parentName; }

        public String getParentPhone() { return parentPhone; }
        public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }

        public String getParentEmail() { return parentEmail; }
        public void setParentEmail(String parentEmail) { this.parentEmail = parentEmail; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getStudentEmail() { return studentEmail; }
        public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Uri getImageUri() { return imageUri; }
        public void setImageUri(Uri imageUri) { this.imageUri = imageUri; }
    }
}