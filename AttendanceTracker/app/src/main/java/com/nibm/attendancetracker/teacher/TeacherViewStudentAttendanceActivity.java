package com.nibm.attendancetracker.teacher;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.NavigationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherViewStudentAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "TeacherViewStudent";

    private ImageView btnBack;
    private EditText etSearchStudent;
    private RecyclerView rvStudentList;
    private LinearLayout layoutStudentDetails, layoutAttendanceStats, subjectListContainer;
    private TextView tvStudentName, tvStudentId, tvStudentBatch, tvStudentEmail;
    private TextView tvPresentCount, tvAbsentCount, tvLateCount, tvTotalClasses;
    private ProgressBar overallProgressBar, searchProgressBar;

    private FirebaseFirestore db;
    private StudentSearchAdapter searchAdapter;
    private List<StudentItem> studentsList;
    private List<StudentItem> filteredStudents;

    private StudentItem selectedStudent;
    private ScheduleData scheduleData;
    private Map<String, List<AttendanceRecord>> attendanceBySubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.teacher_view_student_attendance);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupListeners();

        // Setup navigation with role
        NavigationHelper.setupNavigation(this, "teacher");

        loadAllStudents();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etSearchStudent = findViewById(R.id.et_search_student);
        rvStudentList = findViewById(R.id.rv_student_list);
        layoutStudentDetails = findViewById(R.id.layout_student_details);
        layoutAttendanceStats = findViewById(R.id.layout_attendance_stats);
        subjectListContainer = findViewById(R.id.subjectListContainer);
        tvStudentName = findViewById(R.id.tv_student_name);
        tvStudentId = findViewById(R.id.tv_student_id);
        tvStudentBatch = findViewById(R.id.tv_student_batch);
        tvStudentEmail = findViewById(R.id.tv_student_email);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvLateCount = findViewById(R.id.tvLateCount);
        tvTotalClasses = findViewById(R.id.tvTotalClasses);
        overallProgressBar = findViewById(R.id.overallProgressBar);
        searchProgressBar = findViewById(R.id.search_progress_bar);

        db = FirebaseFirestore.getInstance();
        studentsList = new ArrayList<>();
        filteredStudents = new ArrayList<>();

        // Initially hide student details
        layoutStudentDetails.setVisibility(View.GONE);
        layoutAttendanceStats.setVisibility(View.GONE);

        // Setup RecyclerView
        searchAdapter = new StudentSearchAdapter(filteredStudents, this::onStudentSelected);
        rvStudentList.setLayoutManager(new LinearLayoutManager(this));
        rvStudentList.setAdapter(searchAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllStudents() {
        searchProgressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentsList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        StudentItem student = new StudentItem();
                        student.id = document.getId();
                        student.studentId = document.getString("studentId");
                        student.name = document.getString("name");
                        student.firstName = document.getString("firstName");
                        student.lastName = document.getString("lastName");
                        student.email = document.getString("email");
                        student.batch = document.getString("batch");
                        student.assignedBatch = document.getString("assignedBatch");
                        student.programme = document.getString("programme");
                        student.faculty = document.getString("faculty");
                        student.assignedScheduleId = document.getString("assignedScheduleId");
                        student.profilePictureUrl = document.getString("profilePictureUrl");

                        studentsList.add(student);
                    }

                    filteredStudents.clear();
                    filteredStudents.addAll(studentsList);
                    searchAdapter.notifyDataSetChanged();

                    searchProgressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Loaded " + studentsList.size() + " students");
                })
                .addOnFailureListener(e -> {
                    searchProgressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading students: " + e.getMessage());
                    Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show();
                });
    }

    private void filterStudents(String query) {
        filteredStudents.clear();

        if (query.isEmpty()) {
            filteredStudents.addAll(studentsList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (StudentItem student : studentsList) {
                if ((student.studentId != null && student.studentId.toLowerCase().contains(lowerQuery)) ||
                        (student.name != null && student.name.toLowerCase().contains(lowerQuery)) ||
                        (student.email != null && student.email.toLowerCase().contains(lowerQuery)) ||
                        (student.batch != null && student.batch.toLowerCase().contains(lowerQuery))) {
                    filteredStudents.add(student);
                }
            }
        }

        searchAdapter.notifyDataSetChanged();
    }

    private void onStudentSelected(StudentItem student) {
        selectedStudent = student;

        // Hide search results, show student details
        rvStudentList.setVisibility(View.GONE);
        etSearchStudent.setText("");

        displayStudentInfo();
        loadStudentAttendance();
    }

    private void displayStudentInfo() {
        layoutStudentDetails.setVisibility(View.VISIBLE);

        tvStudentName.setText(selectedStudent.name != null ? selectedStudent.name : "N/A");
        tvStudentId.setText("ID: " + (selectedStudent.studentId != null ? selectedStudent.studentId : "N/A"));
        tvStudentBatch.setText("Batch: " + (selectedStudent.batch != null ? selectedStudent.batch : "N/A"));
        tvStudentEmail.setText(selectedStudent.email != null ? selectedStudent.email : "N/A");
    }

    private void loadStudentAttendance() {
        if (selectedStudent.assignedScheduleId == null || selectedStudent.assignedScheduleId.isEmpty()) {
            layoutAttendanceStats.setVisibility(View.VISIBLE);
            showNoScheduleMessage();
            return;
        }

        // Load schedule data first
        db.collection("schedules")
                .document(selectedStudent.assignedScheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        scheduleData = parseScheduleData(documentSnapshot);
                        loadAttendanceData();
                    } else {
                        Toast.makeText(this, "Schedule not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading schedule: " + e.getMessage());
                    Toast.makeText(this, "Error loading schedule", Toast.LENGTH_SHORT).show();
                });
    }

    private ScheduleData parseScheduleData(DocumentSnapshot document) {
        ScheduleData schedule = new ScheduleData();
        schedule.id = document.getId();
        schedule.batch = document.getString("batch");
        schedule.programme = document.getString("programme");
        schedule.faculty = document.getString("faculty");
        schedule.subjects = new ArrayList<>();

        List<Object> subjectsData = (List<Object>) document.get("subjects");
        if (subjectsData != null) {
            int subjectIndex = 0;
            for (Object subjectObj : subjectsData) {
                if (subjectObj instanceof Map) {
                    Map<String, Object> subjectMap = (Map<String, Object>) subjectObj;

                    SubjectData subject = new SubjectData();
                    subject.index = subjectIndex++;
                    subject.name = (String) subjectMap.get("subjectName");
                    subject.totalClasses = getIntValue(subjectMap.get("totalClasses"));
                    subject.credits = getIntValue(subjectMap.get("credits"));
                    subject.lectures = new ArrayList<>();

                    List<Object> lecturesData = (List<Object>) subjectMap.get("lectureSchedules");
                    if (lecturesData != null) {
                        for (int i = 0; i < lecturesData.size(); i++) {
                            if (lecturesData.get(i) instanceof Map) {
                                Map<String, String> lectureMap = (Map<String, String>) lecturesData.get(i);

                                LectureData lecture = new LectureData();
                                lecture.id = "L" + (i + 1); // Set lectureId
                                lecture.number = i + 1;
                                lecture.date = lectureMap.get("date");
                                lecture.startTime = lectureMap.get("startTime");
                                lecture.endTime = lectureMap.get("endTime");

                                subject.lectures.add(lecture);
                            }
                        }
                    }

                    schedule.subjects.add(subject);
                }
            }
        }

        return schedule;
    }

    private void loadAttendanceData() {
        // Load attendance records from the schedules subcollection
        db.collection("schedules")
                .document(selectedStudent.assignedScheduleId)
                .collection("attendance")
                .whereEqualTo("studentEmail", selectedStudent.email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceBySubject = new HashMap<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        AttendanceRecord record = new AttendanceRecord();
                        record.studentId = document.getString("studentId");
                        record.studentEmail = document.getString("studentEmail");
                        record.subjectName = document.getString("subjectName");
                        record.lectureId = document.getString("lectureId"); // Add lectureId
                        record.lectureNumber = getIntValue(document.get("lectureNumber"));
                        record.lectureDate = document.getString("lectureDate");
                        record.lectureStartTime = document.getString("lectureStartTime");
                        record.lectureEndTime = document.getString("lectureEndTime");
                        record.status = document.getString("status");
                        record.markedAt = getLongValue(document.get("markedAt"));
                        record.markedAtFormatted = document.getString("markedAtFormatted");

                        // Group by subject
                        if (!attendanceBySubject.containsKey(record.subjectName)) {
                            attendanceBySubject.put(record.subjectName, new ArrayList<>());
                        }
                        attendanceBySubject.get(record.subjectName).add(record);
                    }

                    Log.d(TAG, "Loaded " + queryDocumentSnapshots.size() + " attendance records");
                    calculateAndDisplayStats();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading attendance: " + e.getMessage());
                    Toast.makeText(this, "Error loading attendance data", Toast.LENGTH_SHORT).show();
                    calculateAndDisplayStats(); // Still show schedule even if no attendance
                });
    }

    private void calculateAndDisplayStats() {
        if (scheduleData == null || scheduleData.subjects == null) {
            showNoDataMessage();
            return;
        }

        layoutAttendanceStats.setVisibility(View.VISIBLE);

        int totalLectures = 0;
        int totalAttended = 0;
        int totalAbsent = 0;

        // Calculate totals across all subjects
        for (SubjectData subject : scheduleData.subjects) {
            int subjectTotalLectures = subject.lectures.size();
            totalLectures += subjectTotalLectures;

            List<AttendanceRecord> subjectAttendance = attendanceBySubject.get(subject.name);
            if (subjectAttendance != null) {
                for (AttendanceRecord record : subjectAttendance) {
                    if ("present".equalsIgnoreCase(record.status)) {
                        totalAttended++;
                    }
                }
            }
        }

        totalAbsent = totalLectures - totalAttended;

        // Calculate percentages
        int presentPercentage = totalLectures > 0 ? (totalAttended * 100) / totalLectures : 0;
        int absentPercentage = totalLectures > 0 ? (totalAbsent * 100) / totalLectures : 0;

        // Update overall stats UI
        updateOverallStatsUI(presentPercentage, absentPercentage, 0, totalAttended, totalLectures);

        // Display subject-wise breakdown
        displaySubjectBreakdown();
    }

    private void updateOverallStatsUI(int presentPercentage, int absentPercentage,
                                      int latePercentage, int attendedCount, int totalCount) {
        runOnUiThread(() -> {
            tvPresentCount.setText(presentPercentage + "%");
            tvAbsentCount.setText(absentPercentage + "%");
            tvLateCount.setText(latePercentage + "%");
            tvTotalClasses.setText(attendedCount + " of " + totalCount + " classes attended");
            overallProgressBar.setProgress(presentPercentage);
        });
    }

    private void displaySubjectBreakdown() {
        runOnUiThread(() -> {
            subjectListContainer.removeAllViews();

            if (scheduleData.subjects.isEmpty()) {
                showNoSubjectsMessage();
                return;
            }

            for (SubjectData subject : scheduleData.subjects) {
                createSubjectCard(subject);
            }
        });
    }

    private void createSubjectCard(SubjectData subject) {
        // Calculate attendance for this subject
        int totalLectures = subject.lectures.size();
        int attendedLectures = 0;

        List<AttendanceRecord> subjectAttendance = attendanceBySubject.get(subject.name);
        if (subjectAttendance != null) {
            for (AttendanceRecord record : subjectAttendance) {
                if ("present".equalsIgnoreCase(record.status)) {
                    attendedLectures++;
                }
            }
        }

        int percentage = totalLectures > 0 ? (attendedLectures * 100) / totalLectures : 0;

        // Create card view
        CardView subjectCard = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        subjectCard.setLayoutParams(cardParams);
        subjectCard.setCardBackgroundColor(0xFF2A2A2A);
        subjectCard.setRadius(dpToPx(12));
        subjectCard.setCardElevation(0);

        // Main container
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        mainLayout.setOrientation(LinearLayout.HORIZONTAL);
        mainLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Color indicator
        View colorIndicator = new View(this);
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                dpToPx(8),
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        indicatorParams.setMargins(0, 0, dpToPx(16), 0);
        colorIndicator.setLayoutParams(indicatorParams);
        colorIndicator.setBackgroundColor(getSubjectColor(percentage));

        // Content container
        LinearLayout contentLayout = new LinearLayout(this);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        contentLayout.setLayoutParams(contentParams);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // Subject name
        TextView subjectNameView = new TextView(this);
        subjectNameView.setText(subject.name);
        subjectNameView.setTextColor(0xFFFFFFFF);
        subjectNameView.setTextSize(16);
        subjectNameView.setTypeface(getResources().getFont(R.font.cascadiacode));
        subjectNameView.setPadding(0, 0, 0, dpToPx(4));

        // Class count
        TextView classCountView = new TextView(this);
        classCountView.setText(attendedLectures + " of " + totalLectures + " classes");
        classCountView.setTextColor(0xFF999999);
        classCountView.setTextSize(14);
        classCountView.setTypeface(getResources().getFont(R.font.cascadiacode));
        classCountView.setPadding(0, 0, 0, dpToPx(8));

        // Progress bar
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                dpToPx(120),
                dpToPx(4)
        );
        progressBar.setLayoutParams(progressParams);
        progressBar.setProgress(percentage);
        progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.mini_progress_bar));

        contentLayout.addView(subjectNameView);
        contentLayout.addView(classCountView);
        contentLayout.addView(progressBar);

        // Percentage container
        LinearLayout percentageLayout = new LinearLayout(this);
        percentageLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        percentageLayout.setOrientation(LinearLayout.VERTICAL);
        percentageLayout.setGravity(android.view.Gravity.END);

        // Percentage text
        TextView percentageView = new TextView(this);
        percentageView.setText(percentage + "%");
        percentageView.setTextColor(getAttendanceColor(percentage));
        percentageView.setTextSize(18);
        percentageView.setTypeface(getResources().getFont(R.font.cascadiacode), android.graphics.Typeface.BOLD);
        percentageView.setPadding(0, 0, 0, dpToPx(4));

        // Arrow icon
        ImageView arrowIcon = new ImageView(this);
        arrowIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)));
        arrowIcon.setImageResource(R.drawable.ic_arrow_forward);
        arrowIcon.setColorFilter(0xFF666666);

        percentageLayout.addView(percentageView);
        percentageLayout.addView(arrowIcon);

        mainLayout.addView(colorIndicator);
        mainLayout.addView(contentLayout);
        mainLayout.addView(percentageLayout);

        subjectCard.addView(mainLayout);
        subjectListContainer.addView(subjectCard);

        // Click listener for subject details
        int finalAttendedLectures = attendedLectures;
        int finalTotalLectures = totalLectures;
        subjectCard.setOnClickListener(v -> showSubjectDetails(subject, finalAttendedLectures, finalTotalLectures));
    }

    private void showSubjectDetails(SubjectData subject, int attended, int total) {
        StringBuilder details = new StringBuilder();
        details.append(subject.name).append("\n\n");
        details.append("Total Lectures: ").append(total).append("\n");
        details.append("Attended: ").append(attended).append("\n");
        details.append("Absent: ").append(total - attended).append("\n");
        details.append("Credits: ").append(subject.credits).append("\n\n");

        details.append("Lecture Details:\n");
        for (LectureData lecture : subject.lectures) {
            details.append("Lecture ").append(lecture.number)
                    .append(" - ").append(lecture.date)
                    .append(" (").append(lecture.startTime)
                    .append(" - ").append(lecture.endTime).append(")");

            // Check if attended
            boolean attended_lecture = false;
            List<AttendanceRecord> subjectAttendance = attendanceBySubject.get(subject.name);
            if (subjectAttendance != null) {
                for (AttendanceRecord record : subjectAttendance) {
                    if (record.lectureId != null && record.lectureId.equals(lecture.id)) {
                        attended_lecture = true;
                        details.append(" - Marked at: ").append(record.markedAtFormatted);
                        break;
                    }
                }
            }

            details.append(attended_lecture ? " ✓" : " ✗").append("\n");
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Subject Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showNoScheduleMessage() {
        TextView noScheduleText = new TextView(this);
        noScheduleText.setText("No schedule assigned to this student");
        noScheduleText.setTextColor(0xFFFFFFFF);
        noScheduleText.setTextSize(14);
        noScheduleText.setTypeface(getResources().getFont(R.font.cascadiacode));
        noScheduleText.setGravity(android.view.Gravity.CENTER);
        noScheduleText.setPadding(0, dpToPx(20), 0, 0);

        subjectListContainer.removeAllViews();
        subjectListContainer.addView(noScheduleText);
    }

    private void showNoDataMessage() {
        TextView noDataText = new TextView(this);
        noDataText.setText("No attendance data available");
        noDataText.setTextColor(0xFFFFFFFF);
        noDataText.setTextSize(14);
        noDataText.setTypeface(getResources().getFont(R.font.cascadiacode));
        noDataText.setGravity(android.view.Gravity.CENTER);
        noDataText.setPadding(0, dpToPx(20), 0, 0);

        subjectListContainer.removeAllViews();
        subjectListContainer.addView(noDataText);
    }

    private void showNoSubjectsMessage() {
        TextView noSubjectsText = new TextView(this);
        noSubjectsText.setText("No subjects found in schedule");
        noSubjectsText.setTextColor(0xFFFFFFFF);
        noSubjectsText.setTextSize(14);
        noSubjectsText.setTypeface(getResources().getFont(R.font.cascadiacode));
        noSubjectsText.setGravity(android.view.Gravity.CENTER);
        noSubjectsText.setPadding(0, dpToPx(20), 0, 0);

        subjectListContainer.addView(noSubjectsText);
    }

    private int getSubjectColor(int percentage) {
        if (percentage >= 80) return 0xFF4CAF50;
        else if (percentage >= 60) return 0xFFFF9800;
        else return 0xFFF44336;
    }

    private int getAttendanceColor(int percentage) {
        if (percentage >= 80) return 0xFF4CAF50;
        else if (percentage >= 60) return 0xFFFF9800;
        else return 0xFFF44336;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int getIntValue(Object value) {
        if (value instanceof Long) {
            return ((Long) value).intValue();
        } else if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    private long getLongValue(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return 0;
    }

    // Data Classes
    private static class StudentItem {
        String id;
        String studentId;
        String name;
        String firstName;
        String lastName;
        String email;
        String batch;
        String assignedBatch;
        String programme;
        String faculty;
        String assignedScheduleId;
        String profilePictureUrl;
    }

    private static class ScheduleData {
        String id;
        String batch;
        String programme;
        String faculty;
        List<SubjectData> subjects;
    }

    private static class SubjectData {
        int index;
        String name;
        int totalClasses;
        int credits;
        List<LectureData> lectures;
    }

    private static class LectureData {
        String id;
        int number;
        String date;
        String startTime;
        String endTime;
    }

    private static class AttendanceRecord {
        String studentId;
        String studentEmail;
        String subjectName;
        String lectureId;
        int subjectIndex;
        int lectureNumber;
        String lectureDate;
        String lectureStartTime;
        String lectureEndTime;
        String status;
        long markedAt;
        String markedAtFormatted;
    }

    private static class StudentSearchAdapter extends RecyclerView.Adapter<StudentSearchAdapter.ViewHolder> {
        private List<StudentItem> students;
        private OnStudentClickListener listener;
        private android.content.Context context;

        interface OnStudentClickListener {
            void onStudentClick(StudentItem student);
        }

        StudentSearchAdapter(List<StudentItem> students, OnStudentClickListener listener) {
            this.students = students;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            context = parent.getContext();
            View view = android.view.LayoutInflater.from(context)
                    .inflate(R.layout.item_student_search, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            StudentItem student = students.get(position);
            holder.tvStudentName.setText(student.name != null ? student.name : "N/A");
            holder.tvStudentId.setText("ID: " + (student.studentId != null ? student.studentId : "N/A"));
            holder.tvStudentBatch.setText("Batch: " + (student.batch != null ? student.batch : "N/A"));

            // Load profile picture
            if (student.profilePictureUrl != null && !student.profilePictureUrl.isEmpty()) {
                // Using Glide to load image
                try {
                    com.bumptech.glide.Glide.with(context)
                            .load(student.profilePictureUrl)
                            .placeholder(R.drawable.ic_person_filled)
                            .error(R.drawable.ic_person_filled)
                            .circleCrop()
                            .into(holder.ivStudentProfile);
                } catch (Exception e) {
                    // Fallback to default icon if Glide not available
                    holder.ivStudentProfile.setImageResource(R.drawable.ic_person_filled);
                    Log.e("StudentSearchAdapter", "Error loading profile image: " + e.getMessage());
                }
            } else {
                // Set default icon
                holder.ivStudentProfile.setImageResource(R.drawable.ic_person_filled);
            }

            holder.itemView.setOnClickListener(v -> listener.onStudentClick(student));
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvStudentId, tvStudentBatch;
            ImageView ivStudentProfile;

            ViewHolder(View itemView) {
                super(itemView);
                tvStudentName = itemView.findViewById(R.id.tv_student_name);
                tvStudentId = itemView.findViewById(R.id.tv_student_id);
                tvStudentBatch = itemView.findViewById(R.id.tv_student_batch);
                ivStudentProfile = itemView.findViewById(R.id.iv_student_profile);
            }
        }
    }
}