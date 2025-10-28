package com.nibm.attendancetracker.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.student.StudentsAdapter;
import com.nibm.attendancetracker.common.SubjectAttendanceAdapter;
import com.nibm.attendancetracker.common.SubjectDetailedAttendanceActivity;

import java.util.ArrayList;
import java.util.List;

public class StudentAttendanceActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etSearchStudent;
    private RecyclerView rvStudents, rvSubjectAttendance;
    private CardView cardSelectedStudent;
    private TextView tvSelectedStudentName, tvSelectedStudentId, tvSelectedStudentClass;
    private TextView tvAttendancePercentage, tvPresentDays, tvAbsentDays;
    private LinearLayout layoutSubjectsSection, layoutAttendanceSummary;
    private Button btnMarkAttendance;

    private StudentsAdapter studentsAdapter;
    private SubjectAttendanceAdapter subjectAttendanceAdapter;
    private List<Student> studentsList;
    private List<SubjectAttendance> subjectAttendanceList;
    private Student selectedStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.student_attendance_checker);

        initViews();
        initData();
//        setupRecyclerViews();
        setupClickListeners();
//        setupSearch();
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etSearchStudent = findViewById(R.id.et_search_student);
        rvStudents = findViewById(R.id.rv_students);
        rvSubjectAttendance = findViewById(R.id.rv_subject_attendance);
        cardSelectedStudent = findViewById(R.id.card_selected_student);
        tvSelectedStudentName = findViewById(R.id.tv_selected_student_name);
        tvSelectedStudentId = findViewById(R.id.tv_selected_student_id);
        tvSelectedStudentClass = findViewById(R.id.tv_selected_student_class);
        tvAttendancePercentage = findViewById(R.id.tv_attendance_percentage);
        tvPresentDays = findViewById(R.id.tv_present_days);
        tvAbsentDays = findViewById(R.id.tv_absent_days);
        layoutSubjectsSection = findViewById(R.id.layout_subjects_section);
        layoutAttendanceSummary = findViewById(R.id.layout_attendance_summary);
        btnMarkAttendance = findViewById(R.id.btn_mark_attendance);
    }

    private void initData() {
        // Initialize students list
        studentsList = new ArrayList<>();
        studentsList.add(new Student("1", "Chanul Liyanage", "STU001", "Grade 10-A", 85.0));
        studentsList.add(new Student("2", "Damian Kenny", "STU002", "Grade 10-A", 92.5));
        studentsList.add(new Student("3", "Lehara Abeysundera", "STU003", "Grade 10-B", 78.3));
        studentsList.add(new Student("4", "Shuaib Naufel", "STU004", "Grade 10-A", 95.2));
        studentsList.add(new Student("5", "Paveesha Perera", "STU005", "Grade 10-B", 67.8));
        studentsList.add(new Student("6", "Gi-Hun", "STU006", "Grade 10-A", 89.1));

        // Initialize subject attendance data
        subjectAttendanceList = new ArrayList<>();
        subjectAttendanceList.add(new SubjectAttendance("1", "Statistics", 18, 22, "#FB923C"));
        subjectAttendanceList.add(new SubjectAttendance("2", "Machine Learning", 16, 20, "#60A5FA"));
        subjectAttendanceList.add(new SubjectAttendance("3", "Robotics", 19, 21, "#34D399"));
        subjectAttendanceList.add(new SubjectAttendance("4", "DMW", 15, 19, "#F472B6"));
        subjectAttendanceList.add(new SubjectAttendance("5", "IoT", 20, 23, "#8B5CF6"));
        subjectAttendanceList.add(new SubjectAttendance("6", "PDSA", 17, 20, "#F59E0B"));
    }

//    private void setupRecyclerViews() {
//        // Students RecyclerView
//        studentsAdapter = new StudentsAdapter(studentsList, new StudentsAdapter.OnStudentClickListener() {
//            @Override
//            public void onStudentClick(Student student) {
//                selectStudent(student);
//            }
//        });
//        rvStudents.setLayoutManager(new LinearLayoutManager(this));
//        rvStudents.setAdapter(studentsAdapter);
//
//        // Subject Attendance RecyclerView
//        subjectAttendanceAdapter = new SubjectAttendanceAdapter(subjectAttendanceList,
//                new SubjectAttendanceAdapter.OnSubjectClickListener() {
//                    @Override
//                    public void onSubjectClick(SubjectAttendance subject) {
//                        showSubjectDetailedAttendance(subject);
//                    }
//                });
//        rvSubjectAttendance.setLayoutManager(new GridLayoutManager(this, 2));
//        rvSubjectAttendance.setAdapter(subjectAttendanceAdapter);
//    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMarkAttendance.setOnClickListener(v -> {
            if (selectedStudent != null) {
                Intent intent = new Intent(this, MarkAttendanceActivity.class);
                intent.putExtra("student_id", selectedStudent.getId());
                intent.putExtra("student_name", selectedStudent.getName());
                startActivity(intent);
            }
        });
    }

//    private void setupSearch() {
//        etSearchStudent.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                studentsAdapter.filter(s.toString());
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {}
//        });
//    }

    private void selectStudent(Student student) {
        selectedStudent = student;

        // Update selected student card
        tvSelectedStudentName.setText(student.getName());
        tvSelectedStudentId.setText("Student ID: " + student.getStudentId());
        tvSelectedStudentClass.setText(student.getClassName());
        tvAttendancePercentage.setText(String.format("%.0f%%", student.getOverallAttendance()));

        // Show student sections
        cardSelectedStudent.setVisibility(View.VISIBLE);
        layoutSubjectsSection.setVisibility(View.VISIBLE);
        layoutAttendanceSummary.setVisibility(View.VISIBLE);

        // Update summary
        updateAttendanceSummary();

        // Update subject attendance for selected student
        updateSubjectAttendanceForStudent(student.getId());
    }

    private void updateAttendanceSummary() {
        int totalPresent = 0;
        int totalClasses = 0;

        for (SubjectAttendance subject : subjectAttendanceList) {
            totalPresent += subject.getPresentDays();
            totalClasses += subject.getTotalClasses();
        }

        int totalAbsent = totalClasses - totalPresent;

        tvPresentDays.setText(String.valueOf(totalPresent));
        tvAbsentDays.setText(String.valueOf(totalAbsent));
    }

    private void updateSubjectAttendanceForStudent(String studentId) {
        // In a real app, you would fetch this data from your database based on studentId
        // For now, we'll use the sample data
        subjectAttendanceAdapter.notifyDataSetChanged();
    }

    private void showSubjectDetailedAttendance(SubjectAttendance subject) {
        Intent intent = new Intent(this, SubjectDetailedAttendanceActivity.class);
        intent.putExtra("student_id", selectedStudent.getId());
        intent.putExtra("student_name", selectedStudent.getName());
        intent.putExtra("subject_id", subject.getSubjectId());
        intent.putExtra("subject_name", subject.getSubjectName());
        startActivity(intent);
    }

    // Model Classes
    public static class Student {
        private String id, name, studentId, className;
        private double overallAttendance;

        public Student(String id, String name, String studentId, String className, double overallAttendance) {
            this.id = id;
            this.name = name;
            this.studentId = studentId;
            this.className = className;
            this.overallAttendance = overallAttendance;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getStudentId() { return studentId; }
        public String getClassName() { return className; }
        public double getOverallAttendance() { return overallAttendance; }
    }

    public static class SubjectAttendance {
        private String subjectId, subjectName, color;
        private int presentDays, totalClasses;

        public SubjectAttendance(String subjectId, String subjectName, int presentDays, int totalClasses, String color) {
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.presentDays = presentDays;
            this.totalClasses = totalClasses;
            this.color = color;
        }

        // Getters
        public String getSubjectId() { return subjectId; }
        public String getSubjectName() { return subjectName; }
        public String getColor() { return color; }
        public int getPresentDays() { return presentDays; }
        public int getTotalClasses() { return totalClasses; }
        public double getAttendancePercentage() {
            return totalClasses > 0 ? (double) presentDays / totalClasses * 100 : 0;
        }
    }
}
