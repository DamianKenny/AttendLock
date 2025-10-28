package com.nibm.attendancetracker.admin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateScheduleActivity extends AppCompatActivity {

    private static final String TAG = "CreateScheduleActivity";

    private ImageView btnBack, btnAddSubject;
    private AutoCompleteTextView spinnerFaculty, spinnerProgramme, spinnerBatch;
    private LinearLayout subjectsContainer;
    private MaterialButton btnCreateSchedule;
    private LinearLayout nav_home, nav_documents, nav_chat, nav_menu, nav_profile;
    private FirebaseFirestore db;
    private List<SubjectItem> subjectItems;

    // Faculty and Programme data
    private final String[] faculties = {
            "School of Computing",
            "School of Engineering",
            "School of Business",
            "School of Language",
            "School of Design",
            "School of Humanities"
    };

    private final Map<String, List<String>> facultyProgrammes = new HashMap<>();
    private final Map<String, List<String>> programmeBatches = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_create_schedule);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception", throwable);
            throwable.printStackTrace();
        });

        db = FirebaseFirestore.getInstance();
        subjectItems = new ArrayList<>();

        initializeProgrammeData();
        initViews();
        setupSpinners();
        setupListeners();

        // Add first subject by default
        addSubjectItem();

        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initializeProgrammeData() {
        List<String> computingProgrammes = Arrays.asList(
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
        facultyProgrammes.put("School of Computing", computingProgrammes);

        List<String> businessProgrammes = Arrays.asList(
                "Advanced Diploma in Business Management",
                "Higher National Diploma in Business Management",
                "Advanced Diploma in Marketing Management",
                "Advanced Diploma in Human Resource Management",
                "Advanced Diploma in Project Management"
        );
        facultyProgrammes.put("School of Business", businessProgrammes);

        List<String> engineeringProgrammes = Arrays.asList(
                "Degree in Civil Engineering",
                "Degree in Electro-Mechanical Engineering",
                "Diploma in Civil Engineering",
                "Diploma in Electro-Mechanical Engineering",
                "Advanced Diploma in AI & Robotics"
        );
        facultyProgrammes.put("School of Engineering", engineeringProgrammes);

        List<String> languageProgrammes = Arrays.asList(
                "Diploma in English Language",
                "Certificate in English Language"
        );
        facultyProgrammes.put("School of Language", languageProgrammes);

        List<String> designProgrammes = Arrays.asList(
                "Diploma in Fashion Design",
                "Higher National Diploma in Fashion Design",
                "Diploma in Interior Architecture"
        );
        facultyProgrammes.put("School of Design", designProgrammes);

        List<String> humanitiesProgrammes = Arrays.asList(
                "Diploma in Humanities",
                "Certificate in Humanities"
        );
        facultyProgrammes.put("School of Humanities", humanitiesProgrammes);

        programmeBatches.put("Higher National Diploma in Network Engineering (Part Time)",
                Arrays.asList("HNDNE242F", "HNDNE241F", "HNDNE232F"));
        programmeBatches.put("Higher National Diploma in Software Engineering (Part Time)",
                Arrays.asList("HNDSE242F", "HNDSE241F", "HNDSE232F"));
        programmeBatches.put("Higher National Diploma in Network Engineering (Full Time)",
                Arrays.asList("HNDNE242P", "HNDNE241P", "HNDNE232P"));
        programmeBatches.put("BSc (Hons) in Ethical Hacking and Network Security",
                Arrays.asList("BSCEH242", "BSCEH241", "BSCEH232"));
        programmeBatches.put("Diploma in Fashion Design",
                Arrays.asList("DFD242", "DFD241", "DFD232"));
        programmeBatches.put("Higher National Diploma in Business Management",
                Arrays.asList("HNDBM242", "HNDBM241", "HNDBM232"));
        programmeBatches.put("Diploma in Civil Engineering",
                Arrays.asList("DCE242", "DCE241", "DCE232"));

        // default batches
        for (String faculty : facultyProgrammes.keySet()) {
            for (String programme : facultyProgrammes.get(faculty)) {
                if (!programmeBatches.containsKey(programme)) {
                    String code = generateBatchCode(programme);
                    programmeBatches.put(programme, Arrays.asList(
                            code + "242", code + "241", code + "232"
                    ));
                }
            }
        }
    }

    private String generateBatchCode(String programme) {
        String[] words = programme.split(" ");
        StringBuilder code = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0 && !word.equals("in") && !word.equals("of") && !word.equals("and")) {
                code.append(word.charAt(0));
            }
        }
        return code.toString().toUpperCase();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnAddSubject = findViewById(R.id.btn_add_subject);
        spinnerFaculty = findViewById(R.id.spinner_faculty);
        spinnerProgramme = findViewById(R.id.spinner_programme);
        spinnerBatch = findViewById(R.id.spinner_batch);
        subjectsContainer = findViewById(R.id.subjects_container);
        btnCreateSchedule = findViewById(R.id.btn_create_schedule);
        nav_home = findViewById(R.id.nav_home);
        nav_documents = findViewById(R.id.nav_documents);
        nav_chat = findViewById(R.id.nav_chat);
        nav_menu = findViewById(R.id.nav_menu);
        nav_profile = findViewById(R.id.nav_profile);
    }

    private void setupSpinners() {
        // Faculty spinner
        ArrayAdapter<String> facultyAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, faculties);
        spinnerFaculty.setAdapter(facultyAdapter);

        // Faculty selection listener
        spinnerFaculty.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFaculty = faculties[position];
            updateProgrammeSpinner(selectedFaculty);
            spinnerProgramme.setText("");
            spinnerBatch.setText("");
        });

        // Programme selection listener
        spinnerProgramme.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProgramme = spinnerProgramme.getText().toString();
            updateBatchSpinner(selectedProgramme);
            spinnerBatch.setText("");
        });
    }

    private void updateProgrammeSpinner(String faculty) {
        List<String> programmes = facultyProgrammes.get(faculty);
        if (programmes != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, programmes);
            spinnerProgramme.setAdapter(adapter);
        }
    }

    private void updateBatchSpinner(String programme) {
        List<String> batches = programmeBatches.get(programme);
        if (batches != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, batches);
            spinnerBatch.setAdapter(adapter);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnAddSubject.setOnClickListener(v -> addSubjectItem());
        btnCreateSchedule.setOnClickListener(v -> createSchedule());

        nav_home.setOnClickListener(v -> {
            Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show();
        });

        nav_documents.setOnClickListener(v -> {
            Toast.makeText(this, "Documents clicked", Toast.LENGTH_SHORT).show();
        });

        nav_chat.setOnClickListener(v -> {
            Toast.makeText(this, "Chat clicked", Toast.LENGTH_SHORT).show();
        });

        nav_menu.setOnClickListener(v -> {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
        });

        nav_profile.setOnClickListener(v -> {
            Toast.makeText(this, "Profile clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void addSubjectItem() {
        try {
            View subjectView = LayoutInflater.from(this).inflate(R.layout.item_subject_schedule, subjectsContainer, false);

            if (subjectView == null) {
                Log.e(TAG, "Failed to inflate item_subject_schedule layout");
                return;
            }

            SubjectItem subjectItem = new SubjectItem(subjectView, subjectItems.size() + 1);
            subjectItems.add(subjectItem);
            subjectsContainer.addView(subjectView);

            // Update subject number for all items
            updateSubjectNumbers();

            // Setup remove subject button
            subjectItem.btnRemoveSubject.setOnClickListener(v -> {
                if (subjectItems.size() > 1) {
                    subjectsContainer.removeView(subjectView);
                    subjectItems.remove(subjectItem);
                    updateSubjectNumbers();
                } else {
                    Toast.makeText(this, "At least one subject is required", Toast.LENGTH_SHORT).show();
                }
            });

            // Setup add lecture button
            subjectItem.btnAddLecture.setOnClickListener(v -> addLectureTimeItem(subjectItem));

            Log.d(TAG, "Subject item added successfully. Total subjects: " + subjectItems.size());

        } catch (Exception e) {
            Log.e(TAG, "Error adding subject item", e);
            Toast.makeText(this, "Error adding subject", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSubjectNumbers() {
        for (int i = 0; i < subjectItems.size(); i++) {
            SubjectItem subjectItem = subjectItems.get(i);
            subjectItem.tvSubjectNumber.setText("Subject " + (i + 1));
            subjectItem.position = i + 1;
        }
    }

    private void addLectureTimeItem(SubjectItem subjectItem) {
        try {
            View lectureView = LayoutInflater.from(this).inflate(R.layout.item_schedule_time, subjectItem.lectureTimesContainer, false);

            if (lectureView == null) {
                Log.e(TAG, "Failed to inflate item_schedule_time layout");
                return;
            }

            ScheduleTimeItem lectureItem = new ScheduleTimeItem(lectureView);
            subjectItem.scheduleTimeItems.add(lectureItem);
            subjectItem.lectureTimesContainer.addView(lectureView);

            // Setup date picker
            lectureItem.etDate.setOnClickListener(v -> showDatePicker(lectureItem.etDate));

            // Setup time pickers
            lectureItem.etStartTime.setOnClickListener(v -> showTimePicker(lectureItem.etStartTime));
            lectureItem.etEndTime.setOnClickListener(v -> showTimePicker(lectureItem.etEndTime));

            // Setup remove button
            lectureItem.btnRemove.setOnClickListener(v -> {
                subjectItem.lectureTimesContainer.removeView(lectureView);
                subjectItem.scheduleTimeItems.remove(lectureItem);
            });

            Log.d(TAG, "Lecture time item added successfully for subject " + subjectItem.position);

        } catch (Exception e) {
            Log.e(TAG, "Error adding lecture time item", e);
            Toast.makeText(this, "Error adding lecture schedule", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePicker(EditText editText) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());
                    editText.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePicker(EditText editText) {
        int hour = 8;
        int minute = 0;

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    editText.setText(time);
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private void createSchedule() {
        if (!validateInputs()) {
            return;
        }

        String faculty = spinnerFaculty.getText().toString().trim();
        String programme = spinnerProgramme.getText().toString().trim();
        String batch = spinnerBatch.getText().toString().trim();

        // Prepare subjects data
        List<Map<String, Object>> subjectsData = new ArrayList<>();

        for (SubjectItem subjectItem : subjectItems) {
            Map<String, Object> subjectData = new HashMap<>();
            subjectData.put("subjectName", subjectItem.etSubjectName.getText().toString().trim());
            subjectData.put("totalClasses", Integer.parseInt(subjectItem.etTotalClasses.getText().toString().trim()));
            subjectData.put("credits", Integer.parseInt(subjectItem.etCredits.getText().toString().trim()));

            // Prepare lecture schedules for this subject
            List<Map<String, String>> scheduleTimes = new ArrayList<>();
            for (ScheduleTimeItem lectureItem : subjectItem.scheduleTimeItems) {
                Map<String, String> timeSlot = new HashMap<>();
                timeSlot.put("date", lectureItem.etDate.getText().toString());
                timeSlot.put("startTime", lectureItem.etStartTime.getText().toString());
                timeSlot.put("endTime", lectureItem.etEndTime.getText().toString());
                scheduleTimes.add(timeSlot);
            }
            subjectData.put("lectureSchedules", scheduleTimes);

            subjectsData.add(subjectData);
        }

        // Create schedule document
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("faculty", faculty);
        scheduleData.put("programme", programme);
        scheduleData.put("batch", batch);
        scheduleData.put("subjects", subjectsData);
        scheduleData.put("createdAt", System.currentTimeMillis());

        // Save to Firestore
        db.collection("schedules")
                .add(scheduleData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Schedule created with ID: " + documentReference.getId());
                    Toast.makeText(this, "Schedule created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating schedule", e);
                    Toast.makeText(this, "Failed to create schedule: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateInputs() {
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

        if (TextUtils.isEmpty(spinnerBatch.getText())) {
            spinnerBatch.setError("Please select batch");
            spinnerBatch.requestFocus();
            return false;
        }

        if (subjectItems.isEmpty()) {
            Toast.makeText(this, "Please add at least one subject", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate each subject
        for (int i = 0; i < subjectItems.size(); i++) {
            SubjectItem subjectItem = subjectItems.get(i);

            if (TextUtils.isEmpty(subjectItem.etSubjectName.getText())) {
                subjectItem.etSubjectName.setError("Subject name is required");
                subjectItem.etSubjectName.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(subjectItem.etTotalClasses.getText())) {
                subjectItem.etTotalClasses.setError("Total classes is required");
                subjectItem.etTotalClasses.requestFocus();
                return false;
            }

            if (TextUtils.isEmpty(subjectItem.etCredits.getText())) {
                subjectItem.etCredits.setError("Credits is required");
                subjectItem.etCredits.requestFocus();
                return false;
            }

            // Validate each lecture schedule for this subject
            for (int j = 0; j < subjectItem.scheduleTimeItems.size(); j++) {
                ScheduleTimeItem lectureItem = subjectItem.scheduleTimeItems.get(j);

                if (TextUtils.isEmpty(lectureItem.etDate.getText())) {
                    Toast.makeText(this, "Please select date for lecture " + (j + 1) + " in subject " + (i + 1),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (TextUtils.isEmpty(lectureItem.etStartTime.getText())) {
                    Toast.makeText(this, "Please select start time for lecture " + (j + 1) + " in subject " + (i + 1),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }

                if (TextUtils.isEmpty(lectureItem.etEndTime.getText())) {
                    Toast.makeText(this, "Please select end time for lecture " + (j + 1) + " in subject " + (i + 1),
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            // Check if subject has at least one lecture schedule
            if (subjectItem.scheduleTimeItems.isEmpty()) {
                Toast.makeText(this, "Please add at least one lecture schedule for subject " + (i + 1),
                        Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    // Helper class to hold subject item views
    private static class SubjectItem {
        TextView tvSubjectNumber;
        EditText etSubjectName, etTotalClasses, etCredits;
        ImageView btnRemoveSubject, btnAddLecture;
        LinearLayout lectureTimesContainer;
        List<ScheduleTimeItem> scheduleTimeItems;
        int position;

        SubjectItem(View subjectView, int position) {
            try {
                this.position = position;
                this.scheduleTimeItems = new ArrayList<>();

                tvSubjectNumber = subjectView.findViewById(R.id.tv_subject_number);
                etSubjectName = subjectView.findViewById(R.id.et_subject_name);
                etTotalClasses = subjectView.findViewById(R.id.et_total_classes);
                etCredits = subjectView.findViewById(R.id.et_credits);
                btnRemoveSubject = subjectView.findViewById(R.id.btn_remove_subject);
                btnAddLecture = subjectView.findViewById(R.id.btn_add_lecture);
                lectureTimesContainer = subjectView.findViewById(R.id.lecture_times_container);

                // Set subject number
                tvSubjectNumber.setText("Subject " + position);

                Log.d(TAG, "SubjectItem views initialized for position: " + position);

            } catch (Exception e) {
                Log.e(TAG, "Error initializing SubjectItem", e);
            }
        }
    }

    // Helper class to hold schedule time item views
    private static class ScheduleTimeItem {
        EditText etDate;
        EditText etStartTime;
        EditText etEndTime;
        ImageView btnRemove;

        ScheduleTimeItem(View itemView) {
            try {
                etDate = itemView.findViewById(R.id.et_date);
                etStartTime = itemView.findViewById(R.id.et_start_time);
                etEndTime = itemView.findViewById(R.id.et_end_time);
                btnRemove = itemView.findViewById(R.id.btn_remove_schedule);

                Log.d(TAG, "ScheduleTimeItem views initialized");

            } catch (Exception e) {
                Log.e(TAG, "Error initializing ScheduleTimeItem", e);
            }
        }
    }
}