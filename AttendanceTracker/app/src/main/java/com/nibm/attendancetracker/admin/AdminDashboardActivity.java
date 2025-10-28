package com.nibm.attendancetracker.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private TextView totalStudents, totalTeachers, avgAttendance;

    private LinearLayout navHome, navDocuments, navChat, navMenu, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_dashboard);

        db = FirebaseFirestore.getInstance();

        totalStudents = findViewById(R.id.text_students_count);
        totalTeachers = findViewById(R.id.text_teachers_count);
        avgAttendance = findViewById(R.id.text_attendance_avg);

        navHome = findViewById(R.id.nav_home);
        navDocuments = findViewById(R.id.nav_documents);
        navChat = findViewById(R.id.nav_chat);
        navMenu = findViewById(R.id.nav_menu);
        navProfile = findViewById(R.id.nav_profile);

        setupCardListeners();
        setupBottomNav();
        loadDashboardStats();

        if (getSupportActionBar()!= null) {
            getSupportActionBar().hide();
        }
    }

    private void loadDashboardStats() {
        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int studentCount = querySnapshot.size();
                    updateTextView(R.id.text_students_count, String.valueOf(studentCount));
                })
                .addOnFailureListener(e -> {
                    updateTextView(R.id.text_students_count, "0");
                });

        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int teacherCount = querySnapshot.size();
                    updateTextView(R.id.text_teachers_count, String.valueOf(teacherCount));
                })
                .addOnFailureListener(e -> {
                    updateTextView(R.id.text_teachers_count, "0");
                });

        db.collection("attendance_stats").document("overall")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String avg = documentSnapshot.getString("average");
                        updateTextView(R.id.text_attendance_avg, (avg != null ? avg : "0") + "%");
                    } else {
                        updateTextView(R.id.text_attendance_avg, "—");
                    }
                })
                .addOnFailureListener(e -> {
                    updateTextView(R.id.text_attendance_avg, "—");
                });
    }


    private void updateTextView(int id, String text) {
        runOnUiThread(() -> {
            TextView textView = findViewById(id);
            if (textView != null) {
                textView.setText(text);
            }
        });
    }

    private void setupCardListeners() {
        // Find CardView elements locally (don't declare as class fields)
        CardView cardAddTeacher = findViewById(R.id.card_user_management);
        CardView cardAddStudent = findViewById(R.id.card_subject_management);
        CardView cardViewSchedule = findViewById(R.id.card_view_schedule);
        CardView cardViewStudents = findViewById(R.id.card_view_students);
        CardView cardViewTeachers = findViewById(R.id.card_view_teachers);
        CardView cardSubjectToTeachers = findViewById(R.id.card_subject_to_teachers);
        CardView cardScheduleToStudents = findViewById(R.id.card_schedule_to_student);
        CardView cardSettings = findViewById(R.id.card_settings);

        cardAddTeacher.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateTeacherActivity.class));
        });

        cardAddStudent.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateStudentActivity.class));
        });

        cardViewTeachers.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewTeachersActivity.class));
        });

        cardViewStudents.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewStudentsActivity.class));
        });

        cardViewSchedule.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewScheduleActivity.class));
        });

        cardSubjectToTeachers.setOnClickListener(v -> {
            startActivity(new Intent(this, AddSubjectsToTeacherActivity.class));
        });

        cardScheduleToStudents.setOnClickListener(v -> {
            startActivity(new Intent(this, AddScheduleToStudentActivity.class));
        });

        cardSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show();
        });

    }

    /**
     * Handles bottom navigation
     */
    private void setupBottomNav() {
        navHome.setOnClickListener(v -> Toast.makeText(this, "Already on Home 🦇", Toast.LENGTH_SHORT).show());
        navDocuments.setOnClickListener(v -> startActivity(new Intent(this, ViewTeachersActivity.class)));
        navChat.setOnClickListener(v -> startActivity(new Intent(this, ViewStudentsActivity.class)));
        navMenu.setOnClickListener(v -> startActivity(new Intent(this, ViewScheduleActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, AddSubjectsToTeacherActivity.class)));
    }
}