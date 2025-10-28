package com.nibm.attendancetracker.teacher;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nibm.attendancetracker.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MarkAttendanceActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvStudentName, tvSelectedDate;
    private Button btnSelectDate, btnSaveAttendance;
    private RecyclerView rvSubjectsToMark;

    private MarkAttendanceAdapter markAttendanceAdapter;
    private List<SubjectMarkAttendance> subjectsToMark;
    private String selectedDate;
    private String studentId, studentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.teacher_mark_attendance);

        initViews();
        getIntentData();
        initData();
        setupRecyclerView();
        setupClickListeners();
        setTodayAsDefault();

        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvStudentName = findViewById(R.id.tv_student_name);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        btnSelectDate = findViewById(R.id.btn_select_date);
        btnSaveAttendance = findViewById(R.id.btn_save_attendance);
        rvSubjectsToMark = findViewById(R.id.rv_subjects_to_mark);
    }

    private void getIntentData() {
        studentId = getIntent().getStringExtra("student_id");
        studentName = getIntent().getStringExtra("student_name");
        tvStudentName.setText("Mark attendance for " + studentName);
    }

    private void initData() {
        subjectsToMark = new ArrayList<>();
        subjectsToMark.add(new SubjectMarkAttendance("1", "Mathematics", "#FB923C", "Not Marked"));
        subjectsToMark.add(new SubjectMarkAttendance("2", "Physics", "#60A5FA", "Not Marked"));
        subjectsToMark.add(new SubjectMarkAttendance("3", "Chemistry", "#34D399", "Not Marked"));
        subjectsToMark.add(new SubjectMarkAttendance("4", "Biology", "#F472B6", "Not Marked"));
        subjectsToMark.add(new SubjectMarkAttendance("5", "English", "#8B5CF6", "Not Marked"));
        subjectsToMark.add(new SubjectMarkAttendance("6", "History", "#F59E0B", "Not Marked"));
    }

    private void setupRecyclerView() {
        markAttendanceAdapter = new MarkAttendanceAdapter(subjectsToMark,
                new MarkAttendanceAdapter.OnAttendanceMarkListener() {
                    @Override
                    public void onAttendanceMark(SubjectMarkAttendance subject, String status) {
                        subject.setAttendanceStatus(status);
                        checkIfAllMarked();
                    }
                });
        rvSubjectsToMark.setLayoutManager(new GridLayoutManager(this, 1));
        rvSubjectsToMark.setAdapter(markAttendanceAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSelectDate.setOnClickListener(v -> showDatePickerDialog());

        btnSaveAttendance.setOnClickListener(v -> saveAttendance());
    }

    private void setTodayAsDefault() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        selectedDate = dateFormat.format(calendar.getTime());
        tvSelectedDate.setText(displayFormat.format(calendar.getTime()));
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                    selectedDate = dateFormat.format(selectedCalendar.getTime());
                    tvSelectedDate.setText(displayFormat.format(selectedCalendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void checkIfAllMarked() {
        boolean allMarked = true;
        for (SubjectMarkAttendance subject : subjectsToMark) {
            if (subject.getAttendanceStatus().equals("Not Marked")) {
                allMarked = false;
                break;
            }
        }
        btnSaveAttendance.setEnabled(allMarked);
    }

    private void saveAttendance() {
        // Here you would save the attendance data to your database
        int presentCount = 0;
        int absentCount = 0;
        int lateCount = 0;

        for (SubjectMarkAttendance subject : subjectsToMark) {
            switch (subject.getAttendanceStatus()) {
                case "Present":
                    presentCount++;
                    break;
                case "Absent":
                    absentCount++;
                    break;
                case "Late":
                    lateCount++;
                    break;
            }
        }

        String message = String.format("Attendance saved successfully!\nPresent: %d, Absent: %d, Late: %d",
                presentCount, absentCount, lateCount);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    // Model class for marking attendance
    public static class SubjectMarkAttendance {
        private String subjectId, subjectName, color, attendanceStatus;

        public SubjectMarkAttendance(String subjectId, String subjectName, String color, String attendanceStatus) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.color = color;
            this.attendanceStatus = attendanceStatus;
        }

        // Getters and setters
        public String getSubjectId() { return subjectId; }
        public String getSubjectName() { return subjectName; }
        public String getColor() { return color; }
        public String getAttendanceStatus() { return attendanceStatus; }
        public void setAttendanceStatus(String attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    }
}