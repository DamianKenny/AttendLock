package com.nibm.attendancetracker.common;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nibm.attendancetracker.R;

import java.util.ArrayList;
import java.util.List;

public class SubjectDetailedAttendanceActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvStudentName, tvSubjectName, tvAttendanceStats;
    private RecyclerView rvAttendanceHistory;

    private AttendanceHistoryAdapter attendanceHistoryAdapter;
    private List<AttendanceRecord> attendanceHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_detailed_attendance);

        initViews();
        getIntentData();
        initData();
        setupRecyclerView();
        setupClickListeners();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvStudentName = findViewById(R.id.tv_student_name);
        tvSubjectName = findViewById(R.id.tv_subject_name);
        tvAttendanceStats = findViewById(R.id.tv_attendance_stats);
        rvAttendanceHistory = findViewById(R.id.rv_attendance_history);
    }

    private void getIntentData() {
        String studentName = getIntent().getStringExtra("student_name");
        String subjectName = getIntent().getStringExtra("subject_name");

        tvStudentName.setText(studentName);
        tvSubjectName.setText(subjectName + " Attendance");
    }

    private void initData() {
        // Sample attendance history data
        attendanceHistory = new ArrayList<>();
        attendanceHistory.add(new AttendanceRecord("2024-01-15", "Present", "09:00 AM"));
        attendanceHistory.add(new AttendanceRecord("2024-01-12", "Present", "09:05 AM"));
        attendanceHistory.add(new AttendanceRecord("2024-01-10", "Absent", "-"));
        attendanceHistory.add(new AttendanceRecord("2024-01-08", "Present", "09:02 AM"));
        attendanceHistory.add(new AttendanceRecord("2024-01-05", "Present", "09:00 AM"));
        attendanceHistory.add(new AttendanceRecord("2024-01-03", "Late", "09:15 AM"));
        attendanceHistory.add(new AttendanceRecord("2024-01-01", "Present", "09:03 AM"));

        // Calculate stats
        int present = 0, absent = 0, late = 0;
        for (AttendanceRecord record : attendanceHistory) {
            switch (record.getStatus()) {
                case "Present":
                    present++;
                    break;
                case "Absent":
                    absent++;
                    break;
                case "Late":
                    late++;
                    break;
            }
        }

        int total = attendanceHistory.size();
        double percentage = total > 0 ? (double) (present + late) / total * 100 : 0;

        tvAttendanceStats.setText(String.format("%.0f%% (%d/%d) • %d Absent • %d Late",
                percentage, present + late, total, absent, late));
    }

    private void setupRecyclerView() {
        attendanceHistoryAdapter = new AttendanceHistoryAdapter(attendanceHistory);
        rvAttendanceHistory.setLayoutManager(new LinearLayoutManager(this));
        rvAttendanceHistory.setAdapter(attendanceHistoryAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    // Model class for attendance records
    public static class AttendanceRecord {
        private String date, status, time;

        public AttendanceRecord(String date, String status, String time) {
            this.date = date;
            this.status = status;
            this.time = time;
        }

        public String getDate() { return date; }
        public String getStatus() { return status; }
        public String getTime() { return time; }
    }
}
