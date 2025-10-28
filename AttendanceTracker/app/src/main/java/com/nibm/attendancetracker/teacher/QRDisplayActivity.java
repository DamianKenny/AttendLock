package com.nibm.attendancetracker.teacher;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.nibm.attendancetracker.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QRDisplayActivity extends AppCompatActivity {

    private MaterialButton btnBack, btnShare, btnSaveGallery;
    private ImageView ivQRCode;
    private TextView tvStudentName, tvStudentId, tvQRData, tvTimer;

    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 3 * 60 * 1000; // 3 minutes in milliseconds
    private Bitmap currentQRBitmap;
    private String currentQRData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_display);

        initializeViews();
        setupClickListeners();
        startAutoRefresh();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        btnSaveGallery = findViewById(R.id.btn_save_gallery);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvQRData = findViewById(R.id.tvQRData);
        tvTimer = findViewById(R.id.tv_timer);

        refreshHandler = new Handler();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> shareQRCode());
        btnSaveGallery.setOnClickListener(v -> saveQRToGallery());
    }

    private void startAutoRefresh() {
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

    private void generateQRCode() {
        // Generate QR data for teacher with timestamp for security
        String teacherName = "Chanul Liyanage";
        String teacherId = "TCH001";
        String currentTime = String.valueOf(System.currentTimeMillis());

        // NEW: Use pipe | as delimiter to avoid issues with spaces in names
        // Format: TEACHER|ID|NAME|TIMESTAMP|SUBJECT|SESSION_ID
        String sessionId = generateSessionId();
        currentQRData = "TEACHER|" + teacherId + "|" + teacherName + "|" +
                currentTime + "|Mathematics|" + sessionId;

        // Update UI
        tvStudentName.setText("Teacher: " + teacherName);
        tvStudentId.setText("ID: " + teacherId);
        tvQRData.setText("QR Data: " + currentQRData);

        // Update timer
        updateTimerDisplay();

        // Generate QR bitmap
        try {
            currentQRBitmap = generateQRBitmap(currentQRData);
            if (currentQRBitmap != null) {
                ivQRCode.setImageBitmap(currentQRBitmap);
                Toast.makeText(this, "QR Code refreshed!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateSessionId() {
        // Generate a unique session ID based on current time and random element
        SimpleDateFormat sdf = new SimpleDateFormat("ddHHmmss", Locale.getDefault());
        String timePart = sdf.format(new Date());
        String randomPart = String.valueOf((int)(Math.random() * 1000));
        return timePart + randomPart;
    }

    private void updateTimerDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        if (tvTimer != null) {
            tvTimer.setText("Generated at: " + currentTime);
        }
    }

    private Bitmap generateQRBitmap(String text) {
        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void shareQRCode() {
        if (currentQRBitmap == null) {
            Toast.makeText(this, "No QR code to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Save bitmap temporarily to share
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(file);
            currentQRBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Share the image
            android.content.Intent shareIntent = new android.content.Intent();
            shareIntent.setAction(android.content.Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM,
                    androidx.core.content.FileProvider.getUriForFile(this,
                            getApplicationContext().getPackageName() + ".provider", file));
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                    "Attendance QR Code - " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(android.content.Intent.createChooser(shareIntent, "Share QR Code"));

        } catch (IOException e) {
            Toast.makeText(this, "Error sharing QR code", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveQRToGallery() {
        if (currentQRBitmap == null) {
            Toast.makeText(this, "No QR code to save", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "QR_Code_" + timeStamp + ".png";

            // Save to Pictures directory
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

            // Notify gallery
            android.content.Intent mediaScanIntent = new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(android.net.Uri.fromFile(imageFile));
            sendBroadcast(mediaScanIntent);

            Toast.makeText(this, "QR code saved to Gallery", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error saving QR code", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the auto-refresh when activity is destroyed
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
}