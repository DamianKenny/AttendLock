package com.nibm.attendancetracker.student;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nibm.attendancetracker.R;

import java.util.Locale;

public class QRScannerActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private String currentStudentId, currentStudentName, currentStudentEmail, currentProgramme, currentBatch;
    private TextView tvStudentName;

    private static final String TAG = "QRScannerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_scanner);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firestore = FirebaseFirestore.getInstance();
        tvStudentName = findViewById(R.id.tvStudentName);

        getCurrentStudentData();
        initQRScanner();
    }

    private void getCurrentStudentData() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        String studentEmail = prefs.getString("current_user_email", "");

        if (!studentEmail.isEmpty()) {
            Log.d(TAG, "Fetching student data for: " + studentEmail);

            firestore.collection("users")
                    .whereEqualTo("email", studentEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);

                            currentStudentId = document.getString("studentId");
                            String firstName = document.getString("firstName");
                            String lastName = document.getString("lastName");
                            currentStudentName = firstName + " " + lastName;
                            currentStudentEmail = document.getString("email");
                            currentProgramme = document.getString("programme");
                            currentBatch = document.getString("batch");

                            tvStudentName.setText("Hello, " + currentStudentName);

                            Log.d(TAG, "Student data loaded - ID: " + currentStudentId +
                                    ", Name: " + currentStudentName +
                                    ", Batch: " + currentBatch);

                        } else {
                            Log.e(TAG, "Error loading student data from Firestore");
                            Toast.makeText(this, "Error loading student profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initQRScanner() {
        findViewById(R.id.btnScanQR).setOnClickListener(v -> {
            if (currentStudentId == null) {
                Toast.makeText(this, "Loading student data...", Toast.LENGTH_SHORT).show();
                getCurrentStudentData();
                return;
            }

            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan Teacher's QR Code");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(false);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            String qrData = result.getContents();
            Log.d(TAG, "QR Code Scanned: " + qrData);
            processQRCode(qrData);
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void processQRCode(String qrData) {
        try {
            String[] parts = qrData.split("\\|");

            // Log everything for debugging
            Log.d(TAG, "========== QR CODE ANALYSIS ==========");
            Log.d(TAG, "Raw QR Data: " + qrData);
            Log.d(TAG, "Number of parts: " + parts.length);

            for (int i = 0; i < parts.length; i++) {
                Log.d(TAG, "Part [" + i + "]: " + parts[i]);
            }
            Log.d(TAG, "=====================================");

            // Check if it starts with TEACHER
            if (parts.length > 0 && "TEACHER".equals(parts[0])) {

                // Handle 13-part format: TEACHER|ID|NAME|TIMESTAMP|SCHEDULE_ID|SUBJECT|INDEX|LECTURE#|DATE|START|END|SESSION_ID|UNIQUE_ID
                if (parts.length == 13) {
                    String teacherId = parts[1];
                    String teacherName = parts[2];
                    String timestamp = parts[3];
                    String scheduleId = parts[4];
                    String subjectName = parts[5];
                    String subjectIndex = parts[6];
                    String lectureNumber = parts[7];
                    String lectureDate = parts[8];
                    String lectureStartTime = parts[9];
                    String lectureEndTime = parts[10];
                    String sessionId = parts[11];
                    String lectureUniqueId = parts[12];

                    Log.d(TAG, "✓ Parsed Successfully (13-part Format):");
                    Log.d(TAG, "  Teacher: " + teacherName + " (ID: " + teacherId + ")");
                    Log.d(TAG, "  Subject: " + subjectName + " (Index: " + subjectIndex + ")");
                    Log.d(TAG, "  Session: " + sessionId);
                    Log.d(TAG, "  Schedule ID: " + scheduleId);

                    if (!isQRCodeValid(timestamp)) {
                        Toast.makeText(this, "QR code expired (older than 5 minutes)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // ✅ VALIDATE BATCH BEFORE PROCEEDING
                    validateBatchAndProceed(qrData, teacherId, teacherName, subjectName, sessionId, timestamp,
                            scheduleId, subjectIndex, lectureNumber, lectureDate, lectureStartTime, lectureEndTime);

                }
                // Handle 10-part format
                else if (parts.length == 10) {
                    String teacherId = parts[1];
                    String teacherName = parts[2];
                    String timestamp = parts[3];
                    String scheduleId = parts[4];
                    String subjectName = parts[5];
                    String subjectIndex = parts[6];
                    String lectureNumber = parts[7];
                    String lectureDate = parts[8];
                    String sessionId = parts[9];

                    Log.d(TAG, "✓ Parsed Successfully (10-part Format)");

                    if (!isQRCodeValid(timestamp)) {
                        Toast.makeText(this, "QR code expired (older than 5 minutes)", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // ✅ VALIDATE BATCH BEFORE PROCEEDING
                    validateBatchAndProceed(qrData, teacherId, teacherName, subjectName, sessionId, timestamp,
                            scheduleId, subjectIndex, lectureNumber, lectureDate, null, null);

                }
                // Handle 6-part format (old)
                else if (parts.length == 6) {
                    String teacherId = parts[1];
                    String teacherName = parts[2];
                    String timestamp = parts[3];
                    String subject = parts[4];
                    String sessionId = parts[5];

                    Log.d(TAG, "✓ Parsed Successfully (6-part Format - Old)");

                    if (!isQRCodeValid(timestamp)) {
                        Toast.makeText(this, "QR code expired", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Old format doesn't have scheduleId - allow without batch validation
                    Toast.makeText(this, "⚠ Old QR format detected - batch validation skipped", Toast.LENGTH_SHORT).show();
                    navigateToFaceScanActivity(qrData, teacherId, teacherName, subject, sessionId, timestamp,
                            null, null, null, null, null, null);
                }
                else {
                    Log.e(TAG, "Unsupported format. Expected 6, 10, or 13 parts, got: " + parts.length);
                    Toast.makeText(this, "Unsupported QR format (" + parts.length + " parts). Please check logs.", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "QR code doesn't start with TEACHER");
                Toast.makeText(this, "Invalid QR code (not a teacher QR code)", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing QR code", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ✅ NEW METHOD: Validate student's batch matches schedule's batch
    private void validateBatchAndProceed(String qrData, String teacherId, String teacherName,
                                         String subject, String sessionId, String timestamp,
                                         String scheduleId, String subjectIndex, String lectureNumber,
                                         String lectureDate, String lectureStartTime, String lectureEndTime) {

        if (scheduleId == null || scheduleId.isEmpty()) {
            Toast.makeText(this, "Invalid QR code - missing schedule information", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Validating batch for schedule: " + scheduleId);
        Log.d(TAG, "Student batch: " + currentBatch);

        // Fetch schedule from Firestore
        firestore.collection("schedules")
                .document(scheduleId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String scheduleBatch = documentSnapshot.getString("batch");
                        String scheduleProgramme = documentSnapshot.getString("programme");

                        Log.d(TAG, "Schedule batch: " + scheduleBatch);
                        Log.d(TAG, "Schedule programme: " + scheduleProgramme);

                        // ✅ CHECK IF BATCHES MATCH
                        if (scheduleBatch != null && scheduleBatch.equalsIgnoreCase(currentBatch)) {
                            Log.d(TAG, "✓ Batch validation PASSED");

                            // Batch matches - proceed to face verification
                            navigateToFaceScanActivity(qrData, teacherId, teacherName, subject,
                                    sessionId, timestamp, scheduleId, subjectIndex, lectureNumber,
                                    lectureDate, lectureStartTime, lectureEndTime);

                        } else {
                            // ❌ BATCH MISMATCH
                            Log.e(TAG, "✗ Batch validation FAILED");
                            Log.e(TAG, "  Your batch: " + currentBatch);
                            Log.e(TAG, "  QR batch: " + scheduleBatch);

                            Toast.makeText(this,
                                    "❌ This QR code is for batch " + scheduleBatch +
                                            "\nYou are in batch " + currentBatch +
                                            "\n\nPlease scan the correct QR code for your batch.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "Schedule not found: " + scheduleId);
                        Toast.makeText(this, "Schedule not found. Please contact admin.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching schedule: " + e.getMessage());
                    Toast.makeText(this, "Error validating QR code. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToFaceScanActivity(String qrData, String teacherId, String teacherName,
                                            String subject, String sessionId, String timestamp,
                                            String scheduleId, String subjectIndex, String lectureNumber,
                                            String lectureDate, String lectureStartTime, String lectureEndTime) {

        Intent intent = new Intent(this, FaceScanLocationActivity.class);

        // QR and Teacher data
        intent.putExtra("qrData", qrData);
        intent.putExtra("teacherId", teacherId);
        intent.putExtra("teacherName", teacherName);
        intent.putExtra("subject", subject);
        intent.putExtra("sessionId", sessionId);
        intent.putExtra("timestamp", timestamp);

        // Student data
        intent.putExtra("studentId", currentStudentId);
        intent.putExtra("studentName", currentStudentName);
        intent.putExtra("studentEmail", currentStudentEmail);
        intent.putExtra("programme", currentProgramme);
        intent.putExtra("batch", currentBatch);

        // Additional lecture data for 10/13-part formats
        if (scheduleId != null) {
            intent.putExtra("scheduleId", scheduleId);
        }
        if (subjectIndex != null) {
            intent.putExtra("subjectIndex", subjectIndex);
        }
        if (lectureNumber != null) {
            intent.putExtra("lectureNumber", lectureNumber);
        }
        if (lectureDate != null) {
            intent.putExtra("lectureDate", lectureDate);
        }
        if (lectureStartTime != null) {
            intent.putExtra("lectureStartTime", lectureStartTime);
        }
        if (lectureEndTime != null) {
            intent.putExtra("lectureEndTime", lectureEndTime);
        }

        startActivity(intent);

        // Show confirmation message
        Toast.makeText(this, "✓ QR Code validated! Proceeding to face verification...", Toast.LENGTH_SHORT).show();
    }

    private boolean isQRCodeValid(String timestamp) {
        try {
            long qrTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long difference = currentTime - qrTime;

            boolean isValid = difference <= (5 * 60 * 1000);

            if (!isValid) {
                Log.d(TAG, "QR Code expired. Age: " + (difference / 1000) + " seconds");
            }

            return isValid;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid timestamp: " + timestamp);
            return false;
        }
    }

}
