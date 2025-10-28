package com.nibm.attendancetracker.teacher;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.nibm.attendancetracker.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TeacherQRActivity extends AppCompatActivity {

    private static final String TAG = "TeacherQRActivity";
    private static final int QR_CODE_SIZE = 500;
    private static final long REFRESH_INTERVAL = 3 * 60 * 1000; // 3 minutes

    // UI Components
    private MaterialButton btnBack, btnShare, btnSaveGallery;
    private Spinner spinnerSchedule, spinnerSubject, spinnerLecture;
    private ImageView ivQRCode;
    private TextView tvTeacherName, tvTeacherId, tvQRData, tvTimer;
    private TextView tvScheduleInfo, tvSubjectInfo, tvLectureInfo;
    private LinearLayout layoutSubjectSelector, layoutLectureSelector, layoutQRContainer;
    private ProgressBar progressBar;
    private LinearLayout nav_home, nav_documents, nav_chat, nav_menu, nav_profile;

    // Firebase
    private FirebaseFirestore db;

    // Data
    private List<ScheduleItem> schedulesList;
    private List<SubjectItem> subjectsList;
    private List<LectureItem> lecturesList;
    private ScheduleItem selectedSchedule;
    private SubjectItem selectedSubject;
    private LectureItem selectedLecture;

    // Teacher data
    private String teacherId;
    private String teacherName;
    private String teacherEmail;

    // QR Code
    private Bitmap currentQRBitmap;
    private String currentQRData;
    private String currentSessionId;
    private Handler refreshHandler;
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.teacher_qr_attendance);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        initializeData();
        loadTeacherData();
        loadSchedules();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        btnSaveGallery = findViewById(R.id.btn_save_gallery);
        spinnerSchedule = findViewById(R.id.spinner_schedule);
        spinnerSubject = findViewById(R.id.spinner_subject);
        spinnerLecture = findViewById(R.id.spinner_lecture);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvTeacherName = findViewById(R.id.tvStudentName);
        tvTeacherId = findViewById(R.id.tvStudentId);
        tvQRData = findViewById(R.id.tvQRData);
        tvTimer = findViewById(R.id.tv_timer);
        tvScheduleInfo = findViewById(R.id.tv_schedule_info);
        tvSubjectInfo = findViewById(R.id.tv_subject_info);
        tvLectureInfo = findViewById(R.id.tv_lecture_info);
        layoutSubjectSelector = findViewById(R.id.layout_subject_selector);
        layoutLectureSelector = findViewById(R.id.layout_lecture_selector);
        layoutQRContainer = findViewById(R.id.layout_qr_container);
        progressBar = findViewById(R.id.progress_bar);

        nav_home = findViewById(R.id.nav_home);
        nav_documents = findViewById(R.id.nav_documents);
        nav_chat = findViewById(R.id.nav_chat);
        nav_menu = findViewById(R.id.nav_menu);
        nav_profile = findViewById(R.id.nav_profile);

        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> shareQRCode());
        btnSaveGallery.setOnClickListener(v -> saveQRToGallery());

        // Initially hide selectors
        layoutSubjectSelector.setVisibility(View.GONE);
        layoutLectureSelector.setVisibility(View.GONE);
        layoutQRContainer.setVisibility(View.GONE);

        refreshHandler = new Handler();

        setupNavigation();
    }

    private void setupNavigation() {
        nav_home.setOnClickListener(v -> Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show());
        nav_documents.setOnClickListener(v -> Toast.makeText(this, "Documents", Toast.LENGTH_SHORT).show());
        nav_chat.setOnClickListener(v -> Toast.makeText(this, "Chat", Toast.LENGTH_SHORT).show());
        nav_menu.setOnClickListener(v -> Toast.makeText(this, "Menu", Toast.LENGTH_SHORT).show());
        nav_profile.setOnClickListener(v -> Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show());
    }

    private void initializeData() {
        db = FirebaseFirestore.getInstance();
        schedulesList = new ArrayList<>();
        subjectsList = new ArrayList<>();
        lecturesList = new ArrayList<>();
    }

    private void loadTeacherData() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        teacherEmail = prefs.getString("current_user_email", "");

        if (!teacherEmail.isEmpty()) {
            db.collection("users")
                    .whereEqualTo("email", teacherEmail)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                            teacherId = doc.getString("teacherId");
                            String firstName = doc.getString("firstName");
                            String lastName = doc.getString("lastName");
                            teacherName = firstName + " " + lastName;

                            tvTeacherName.setText("Teacher: " + teacherName);
                            tvTeacherId.setText("ID: " + teacherId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading teacher data: " + e.getMessage());
                        Toast.makeText(this, "Failed to load teacher profile", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadSchedules() {
        showLoading(true);

        db.collection("schedules")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    schedulesList.clear();
                    List<String> scheduleNames = new ArrayList<>();
                    scheduleNames.add("Select Schedule");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            ScheduleItem schedule = new ScheduleItem();
                            schedule.id = document.getId();
                            schedule.faculty = document.getString("faculty");
                            schedule.programme = document.getString("programme");
                            schedule.batch = document.getString("batch");
                            schedule.displayName = schedule.batch + " - " + schedule.programme;

                            schedulesList.add(schedule);
                            scheduleNames.add(schedule.displayName);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing schedule: " + e.getMessage());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            scheduleNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSchedule.setAdapter(adapter);

                    setupScheduleListener();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedules: " + e.getMessage());
                    Toast.makeText(this, "Failed to load schedules", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void setupScheduleListener() {
        spinnerSchedule.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedSchedule = schedulesList.get(position - 1);
                    loadSubjects(selectedSchedule.id);
                    tvScheduleInfo.setText(selectedSchedule.batch);
                    tvScheduleInfo.setVisibility(View.VISIBLE);
                    layoutSubjectSelector.setVisibility(View.VISIBLE);
                    layoutLectureSelector.setVisibility(View.GONE);
                    layoutQRContainer.setVisibility(View.GONE);
                    stopAutoRefresh();
                } else {
                    layoutSubjectSelector.setVisibility(View.GONE);
                    layoutLectureSelector.setVisibility(View.GONE);
                    layoutQRContainer.setVisibility(View.GONE);
                    stopAutoRefresh();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadSubjects(String scheduleId) {
        showLoading(true);

        db.collection("schedules").document(scheduleId)
                .get()
                .addOnSuccessListener(document -> {
                    subjectsList.clear();
                    List<String> subjectNames = new ArrayList<>();
                    subjectNames.add("Select Subject");

                    if (document.exists()) {
                        List<Object> subjectsData = (List<Object>) document.get("subjects");
                        if (subjectsData != null) {
                            int subjectIndex = 0;
                            for (Object subjectObj : subjectsData) {
                                if (subjectObj instanceof Map) {
                                    Map<String, Object> subjectMap = (Map<String, Object>) subjectObj;
                                    SubjectItem subject = new SubjectItem();
                                    subject.index = subjectIndex++;
                                    subject.name = (String) subjectMap.get("subjectName");

                                    Object totalClassesObj = subjectMap.get("totalClasses");
                                    if (totalClassesObj instanceof Long) {
                                        subject.totalClasses = ((Long) totalClassesObj).intValue();
                                    } else if (totalClassesObj instanceof Integer) {
                                        subject.totalClasses = (Integer) totalClassesObj;
                                    }

                                    subject.lectureSchedules = (List<Object>) subjectMap.get("lectureSchedules");

                                    subjectsList.add(subject);
                                    subjectNames.add(subject.name);
                                }
                            }
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            subjectNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSubject.setAdapter(adapter);

                    setupSubjectListener();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading subjects: " + e.getMessage());
                    Toast.makeText(this, "Failed to load subjects", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void setupSubjectListener() {
        spinnerSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedSubject = subjectsList.get(position - 1);
                    loadLectures(selectedSubject);
                    tvSubjectInfo.setText(selectedSubject.name);
                    tvSubjectInfo.setVisibility(View.VISIBLE);
                    layoutLectureSelector.setVisibility(View.VISIBLE);
                    layoutQRContainer.setVisibility(View.GONE);
                    stopAutoRefresh();
                } else {
                    layoutLectureSelector.setVisibility(View.GONE);
                    layoutQRContainer.setVisibility(View.GONE);
                    stopAutoRefresh();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadLectures(SubjectItem subject) {
        lecturesList.clear();
        List<String> lectureNames = new ArrayList<>();
        lectureNames.add("Select Lecture");

        if (subject.lectureSchedules != null && !subject.lectureSchedules.isEmpty()) {
            int lectureNumber = 1;
            for (Object lectureObj : subject.lectureSchedules) {
                if (lectureObj instanceof Map) {
                    Map<String, String> lectureMap = (Map<String, String>) lectureObj;
                    LectureItem lecture = new LectureItem();
                    lecture.number = lectureNumber;
                    lecture.date = lectureMap.get("date");
                    lecture.startTime = lectureMap.get("startTime");
                    lecture.endTime = lectureMap.get("endTime");
                    lecture.displayName = "Lecture " + lectureNumber + " - " + lecture.date + " (" + lecture.startTime + " - " + lecture.endTime + ")";

                    lecturesList.add(lecture);
                    lectureNames.add(lecture.displayName);
                    lectureNumber++;
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                lectureNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLecture.setAdapter(adapter);

        setupLectureListener();
    }

    private void setupLectureListener() {
        spinnerLecture.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedLecture = lecturesList.get(position - 1);
                    tvLectureInfo.setText(selectedLecture.displayName);
                    tvLectureInfo.setVisibility(View.VISIBLE);
                    startAutoRefresh();
                } else {
                    layoutQRContainer.setVisibility(View.GONE);
                    stopAutoRefresh();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void startAutoRefresh() {
        stopAutoRefresh(); // Stop any existing refresh

        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                generateQRCode();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };

        // Generate first QR immediately
        generateQRCode();
        // Schedule refresh every 3 minutes
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    private void generateQRCode() {
        try {
            currentSessionId = UUID.randomUUID().toString();
            String timestamp = String.valueOf(System.currentTimeMillis());

            String lectureUniqueId = selectedSchedule.id + "" + selectedSubject.index + "" + selectedLecture.number;

            currentQRData = "TEACHER|" + teacherId + "|" + teacherName + "|" +
                    timestamp + "|" + selectedSchedule.id + "|" + selectedSubject.name + "|" +
                    selectedSubject.index + "|" + selectedLecture.number + "|" +
                    selectedLecture.date + "|" + selectedLecture.startTime + "|" +
                    selectedLecture.endTime + "|" + currentSessionId + "|" + lectureUniqueId;

            tvQRData.setText("Lecture-Specific QR Code for:\n" +
                    selectedSubject.name + " - Lecture " + selectedLecture.number + "\n" +
                    selectedLecture.date + " (" + selectedLecture.startTime + " - " + selectedLecture.endTime + ")");
            updateTimerDisplay();

            currentQRBitmap = generateQRBitmap(currentQRData);
            if (currentQRBitmap != null) {
                ivQRCode.setImageBitmap(currentQRBitmap);
                layoutQRContainer.setVisibility(View.VISIBLE);

                saveAttendanceSession(lectureUniqueId);

                Toast.makeText(this, "QR Code generated for Lecture " + selectedLecture.number, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating QR code: " + e.getMessage());
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAttendanceSession(String lectureUniqueId) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("sessionId", currentSessionId);
        sessionData.put("scheduleId", selectedSchedule.id);
        sessionData.put("scheduleBatch", selectedSchedule.batch);
        sessionData.put("subjectName", selectedSubject.name);
        sessionData.put("subjectIndex", selectedSubject.index);
        sessionData.put("lectureNumber", selectedLecture.number);
        sessionData.put("lectureDate", selectedLecture.date);
        sessionData.put("lectureStartTime", selectedLecture.startTime);
        sessionData.put("lectureEndTime", selectedLecture.endTime);
        sessionData.put("teacherId", teacherId);
        sessionData.put("teacherName", teacherName);
        sessionData.put("timestamp", System.currentTimeMillis());
        sessionData.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        sessionData.put("active", true);
        sessionData.put("attendanceCount", 0);
        sessionData.put("lectureUniqueId", lectureUniqueId);

        db.collection("attendanceSessions")
                .document(currentSessionId)
                .set(sessionData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Attendance session saved successfully with lectureUniqueId: " + lectureUniqueId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving attendance session: " + e.getMessage());
                });
    }

    private void updateTimerDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        tvTimer.setText("Generated at: " + currentTime);
    }

    private Bitmap generateQRBitmap(String text) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            Bitmap bitmap = Bitmap.createBitmap(QR_CODE_SIZE, QR_CODE_SIZE, Bitmap.Config.RGB_565);
            for (int x = 0; x < QR_CODE_SIZE; x++) {
                for (int y = 0; y < QR_CODE_SIZE; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Log.e(TAG, "Error creating QR code: " + e.getMessage());
            return null;
        }
    }

    private void shareQRCode() {
        if (currentQRBitmap == null) {
            Toast.makeText(this, "No QR code to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(file);
            currentQRBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM,
                    androidx.core.content.FileProvider.getUriForFile(this,
                            getApplicationContext().getPackageName() + ".provider", file));
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Attendance QR Code - " + selectedSubject.name + " - Lecture " + selectedLecture.number);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (IOException e) {
            Toast.makeText(this, "Error sharing QR code", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Share error: " + e.getMessage());
        }
    }

    private void saveQRToGallery() {
        if (currentQRBitmap == null) {
            Toast.makeText(this, "No QR code to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "QR_" + selectedSubject.name.replaceAll(" ", "") + "" + timeStamp + ".png";

            File picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
            File qrDir = new File(picturesDir, "AttendanceQR");
            if (!qrDir.exists()) {
                qrDir.mkdirs();
            }

            File imageFile = new File(qrDir, imageFileName);
            FileOutputStream out = new FileOutputStream(imageFile);
            currentQRBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            android.content.Intent mediaScanIntent = new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(android.net.Uri.fromFile(imageFile));
            sendBroadcast(mediaScanIntent);

            Toast.makeText(this, "QR code saved to Gallery", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error saving QR code", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Save error: " + e.getMessage());
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoRefresh();
    }

    // Data Models
    private static class ScheduleItem {
        String id;
        String faculty;
        String programme;
        String batch;
        String displayName;
    }

    private static class SubjectItem {
        int index;
        String name;
        int totalClasses;
        List<Object> lectureSchedules;
    }

    private static class LectureItem {
        int number;
        String date;
        String startTime;
        String endTime;
        String displayName;
    }
}