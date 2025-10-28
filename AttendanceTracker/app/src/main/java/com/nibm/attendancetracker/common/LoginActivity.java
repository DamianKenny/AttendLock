package com.nibm.attendancetracker.common;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.MainActivity;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.admin.AdminDashboardActivity;
import com.nibm.attendancetracker.student.StudentDashboardActivity;
import com.nibm.attendancetracker.teacher.TeacherDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewForgotPassword;
    private AutoCompleteTextView roleDropdown;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page); // Make sure the layout is updated

        initViews();
        setClickListeners();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firestore = FirebaseFirestore.getInstance();

        String[] roles = new String[]{"Admin", "Teacher", "Student"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, roles);
        roleDropdown.setAdapter(adapter);
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        roleDropdown = findViewById(R.id.roleDropdown);
    }

    private void setClickListeners() {
        buttonLogin.setOnClickListener(v -> loginUser());
        textViewForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String selectedRole = roleDropdown.getText().toString().trim().toLowerCase();

        if (!validateInput(email, password, selectedRole)) return;

        buttonLogin.setEnabled(false);
        buttonLogin.setText("Logging in...");

        if (selectedRole.equals("admin") &&
                email.equals("admin123@gmail.com") &&
                password.equals("admin123")) {

            // Store email in SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("current_user_email", email);
            editor.apply();

            Toast.makeText(this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    buttonLogin.setEnabled(true);
                    buttonLogin.setText("Log In");

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        boolean foundUser = false;
                        for (DocumentSnapshot doc : task.getResult()) {
                            String dbPassword = doc.getString("password");
                            String dbRole = doc.getString("role");

                            if (dbRole != null && dbRole.equalsIgnoreCase(selectedRole) && dbPassword != null && dbPassword.equals(password)) {
                                foundUser = true;

                                // Store email in SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("current_user_email", email);
                                editor.apply();

                                Toast.makeText(LoginActivity.this, "Welcome " + dbRole + "!", Toast.LENGTH_SHORT).show();

                                Intent intent;
                                switch (dbRole.toLowerCase()) {
                                    case "student":
                                        intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                                        intent.putExtra("email", email);
                                        break;
                                    case "teacher":
                                        intent = new Intent(LoginActivity.this, TeacherDashboardActivity.class);
                                        intent.putExtra("email", email);
                                        break;
                                    default:
                                        intent = new Intent(LoginActivity.this, MainActivity.class);
                                        break;
                                }

                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                intent.putExtra("firstName", doc.getString("firstName"));
                                startActivity(intent);
                                finish();
                            }
                        }

                        if (!foundUser) {
                            Toast.makeText(LoginActivity.this, "Invalid password or role mismatch.", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(LoginActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    buttonLogin.setEnabled(true);
                    buttonLogin.setText("Log In");
                    Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInput(String email, String password, String role) {
        if (TextUtils.isEmpty(role)) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void handleForgotPassword() {
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            editTextEmail.requestFocus();
            return;
        }

        Toast.makeText(this, "Password reset link sent to " + email, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
