package com.nibm.attendancetracker.student;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.student.StudentDashboardActivity;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaceScanLocationActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private FusedLocationProviderClient fusedLocationClient;

    private String qrData, teacherId, teacherName, subject, sessionId, timestamp;
    private String studentId, studentName, studentEmail, programme, batch;

    private String scheduleId, subjectIndex, lectureNumber, lectureDate, lectureStartTime, lectureEndTime;
    private boolean locationVerified = false;
    private boolean faceVerified = false;
    private String capturedFaceImageUrl = "";
    private Location currentLocation;

    private TextView tvClassName, tvRoomTime, tvLocationStatus, tvFaceStatus;
    private static final String TAG = "FaceScanLocationActivity";

    // Camera variables
    private PreviewView cameraPreview;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private boolean isCameraActive = false;
    private boolean isCapturing = false;

    private FaceNetHelper faceNetHelper;
    private String referenceFaceImageUrl = "";
    private float faceSimilarityScore = 0.0f;
    private float lastCosineSimilarity = 0.0f;

    // Permission codes
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int LOCATION_PERMISSION_CODE = 200;

    // Geofencing coordinates
    private static final double TARGET_LATITUDE = 6.835088546298477;
    private static final double TARGET_LONGITUDE = 79.8700881581096 ;
    private static final float GEOFENCE_RADIUS = 100f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_scan_location);

        Log.d(TAG, "=== ACTIVITY STARTED ===");

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvClassName = findViewById(R.id.tvClassName);
        tvRoomTime = findViewById(R.id.tvRoomTime);
        tvLocationStatus = findViewById(R.id.locationStatus);
        tvFaceStatus = findViewById(R.id.faceStatus);
        cameraPreview = findViewById(R.id.camera_preview);

        getIntentData();

        setupClickListeners();

        cameraExecutor = Executors.newSingleThreadExecutor();

        faceNetHelper = new FaceNetHelper(this);

        startRealLocationVerification();

        testNormalization();

    }

    private void getIntentData() {
        try {
            Intent intent = getIntent();
            if (intent != null) {
                // QR and Teacher data
                qrData = intent.getStringExtra("qrData");
                teacherId = intent.getStringExtra("teacherId");
                teacherName = intent.getStringExtra("teacherName");
                subject = intent.getStringExtra("subject");
                sessionId = intent.getStringExtra("sessionId");
                timestamp = intent.getStringExtra("timestamp");

                // Student data
                studentId = intent.getStringExtra("studentId");
                studentName = intent.getStringExtra("studentName");
                studentEmail = intent.getStringExtra("studentEmail");
                programme = intent.getStringExtra("programme");
                batch = intent.getStringExtra("batch");

                // Additional lecture data (for 10/13-part formats)
                scheduleId = intent.getStringExtra("scheduleId");
                subjectIndex = intent.getStringExtra("subjectIndex");
                lectureNumber = intent.getStringExtra("lectureNumber");
                lectureDate = intent.getStringExtra("lectureDate");
                lectureStartTime = intent.getStringExtra("lectureStartTime");
                lectureEndTime = intent.getStringExtra("lectureEndTime");

                Log.d(TAG, "Received data for: " + studentName);
                Log.d(TAG, "Subject: " + subject + ", Session: " + sessionId);

                fetchReferenceFaceImage();

                // Update UI with subject info
                if (tvClassName != null) {
                    tvClassName.setText(subject + " Class");
                }
                if (tvRoomTime != null) {
                    tvRoomTime.setText("Room 203 • " + getCurrentTime());
                }
            } else {
                Toast.makeText(this, "No data received", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getIntentData: " + e.getMessage());
        }
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners...");

        try {
            // Back button
            View backButton = findViewById(R.id.back_button);
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    Log.d(TAG, "Back button clicked");
                    finish();
                });
            }

            // Face scan button
            CardView faceScanCard = findViewById(R.id.face_scan_button);
            if (faceScanCard != null) {
                Log.d(TAG, "Face scan CardView found");

                // Find the inner LinearLayout
                LinearLayout innerLayout = null;
                if (faceScanCard.getChildCount() > 0) {
                    View firstChild = faceScanCard.getChildAt(0);
                    if (firstChild instanceof LinearLayout) {
                        innerLayout = (LinearLayout) firstChild;
                    }
                }

                View.OnClickListener faceScanClickListener = v -> {
                    Log.d(TAG, "Face scan button CLICKED!");
                    handleFaceScanButtonClick();
                };

                faceScanCard.setOnClickListener(faceScanClickListener);

                if (innerLayout != null) {
                    innerLayout.setOnClickListener(faceScanClickListener);
                }
            }

            LinearLayout qrCodeButton = findViewById(R.id.qr_code_button);
            if (qrCodeButton != null) {
                qrCodeButton.setOnClickListener(v -> finish());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in setupClickListeners: " + e.getMessage());
        }
    }

    private void startRealLocationVerification() {
        Log.d(TAG, "Starting REAL location verification...");

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
            return;
        }

        getCurrentLocation();
    }

    private void getCurrentLocation() {
        try {
            runOnUiThread(() -> {
                if (tvLocationStatus != null) {
                    tvLocationStatus.setText("Detecting your location...");
                    tvLocationStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
                }
            });

            fusedLocationClient.getLastLocation()
                    .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                currentLocation = task.getResult();
                                Log.d(TAG, "Current location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                                verifyLocationWithGeofence();
                            } else {
                                Log.e(TAG, "Failed to get location: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                                runOnUiThread(() -> {
                                    Toast.makeText(FaceScanLocationActivity.this,
                                            "Unable to get your location. Please ensure location is enabled.",
                                            Toast.LENGTH_LONG).show();
                                    if (tvLocationStatus != null) {
                                        tvLocationStatus.setText("Location detection failed");
                                        tvLocationStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                                    }
                                });
                            }
                        }
                    });

        } catch (SecurityException e) {
            Log.e(TAG, "Location permission denied: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void verifyLocationWithGeofence() {
        if (currentLocation == null) {
            Log.e(TAG, "No location data available");
            return;
        }

        Location targetLocation = new Location("target");
        targetLocation.setLatitude(TARGET_LATITUDE);
        targetLocation.setLongitude(TARGET_LONGITUDE);

        float distance = currentLocation.distanceTo(targetLocation);
        Log.d(TAG, "Distance from target: " + distance + " meters");
        Log.d(TAG, "Current: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
        Log.d(TAG, "Target: " + TARGET_LATITUDE + ", " + TARGET_LONGITUDE);

        runOnUiThread(() -> {
            if (distance <= GEOFENCE_RADIUS) {
                // Within geofence
                locationVerified = true;
                updateVerificationStatus();
                Toast.makeText(FaceScanLocationActivity.this,
                        "Location verified! You are at the correct location.",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "Location verification SUCCESS - Within " + distance + " meters");
            } else {
                // Outside geofence
                locationVerified = false;
                if (tvLocationStatus != null) {
                    tvLocationStatus.setText("Outside allowed location (" + Math.round(distance) + "m away)");
                    tvLocationStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                }
                Toast.makeText(FaceScanLocationActivity.this,
                        "You are " + Math.round(distance) + " meters away from the required location. Please go to NIBM Colombo.",
                        Toast.LENGTH_LONG).show();
                Log.d(TAG, "Location verification FAILED - " + distance + " meters away");
            }
        });
    }

    private void handleFaceScanButtonClick() {
        if (!locationVerified) {
            Toast.makeText(this, "Please verify your location first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Try again logic
        if (!isCameraActive || cameraPreview.getVisibility() != View.VISIBLE) {
            startRealFaceCapture();
        } else {
            if (!isCapturing) {
                captureFacePhoto();
            } else {
                Toast.makeText(this, "Please wait, capturing in progress...", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void startRealFaceCapture() {
        Log.d(TAG, "Starting real face capture...");

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
            return;
        }

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder()
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                Log.d(TAG, "Display rotation: " + rotation + " (" + getRotationName(rotation) + ")");

                // Image capture with proper rotation
                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(new Size(1080, 1920))
                        .setTargetRotation(rotation)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();

                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                isCameraActive = true;
                Log.d(TAG, "Camera started successfully with rotation: " + rotation);

                // Show camera preview
                runOnUiThread(() -> {
                    cameraPreview.setVisibility(View.VISIBLE);
                    updateUIForCameraReady();
                    Toast.makeText(this, "Camera ready! Click again to capture photo.", Toast.LENGTH_LONG).show();
                });

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private String getRotationName(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0: return "PORTRAIT (0°)";
            case Surface.ROTATION_90: return "LANDSCAPE (90°)";
            case Surface.ROTATION_180: return "REVERSE_PORTRAIT (180°)";
            case Surface.ROTATION_270: return "REVERSE_LANDSCAPE (270°)";
            default: return "UNKNOWN";
        }
    }

    private void updateUIForCameraReady() {
        if (tvFaceStatus != null) {
            tvFaceStatus.setText("Camera ready - Click to capture photo");
            tvFaceStatus.setTextColor(getColor(android.R.color.holo_blue_dark));
        }

        updateButtonText("Capture Face Photo");
    }

    private void updateButtonText(String text) {
        CardView faceScanCard = findViewById(R.id.face_scan_button);
        if (faceScanCard != null && faceScanCard.getChildCount() > 0) {
            View firstChild = faceScanCard.getChildAt(0);
            if (firstChild instanceof LinearLayout) {
                LinearLayout innerLayout = (LinearLayout) firstChild;
                // Find the TextView inside the LinearLayout
                for (int i = 0; i < innerLayout.getChildCount(); i++) {
                    View child = innerLayout.getChildAt(i);
                    if (child instanceof TextView) {
                        TextView textView = (TextView) child;
                        textView.setText(text);
                        break;
                    }
                }
            }
        }

        if (text.equals("Try Again")) {
            isCameraActive = false; // Reset camera
        }
    }


    private void captureFacePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCapturing) {
            Toast.makeText(this, "Already capturing...", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Capturing face photo...");
        isCapturing = true;

        // Update UI
        runOnUiThread(() -> {
            if (tvFaceStatus != null) {
                tvFaceStatus.setText("Capturing photo...");
                tvFaceStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
            }
            updateButtonText("Capturing...");
            Toast.makeText(this, "Capturing face photo...", Toast.LENGTH_SHORT).show();
        });

        // Create time-stamped name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "FACE_" + studentId + "_" + timeStamp + ".jpg";

        // Create output options
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(new File(getFilesDir(), fileName)).build();

        // Take picture
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d(TAG, "Face photo captured successfully: " + fileName);
                        isCapturing = false;

                        // Get the saved file
                        File savedFile = new File(getFilesDir(), fileName);

                        if (savedFile.exists()) {
                            Log.d(TAG, "File exists, size: " + savedFile.length() + " bytes");
                            // Upload to Firebase Storage
                            uploadFaceImageToFirebase(savedFile, fileName);
                        } else {
                            Log.e(TAG, "Saved file does not exist!");
                            runOnUiThread(() -> {
                                Toast.makeText(FaceScanLocationActivity.this,
                                        "Error: Photo not saved", Toast.LENGTH_SHORT).show();
                                updateButtonText("Capture Face Photo");
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Error capturing face photo: " + exception.getMessage(), exception);
                        isCapturing = false;
                        runOnUiThread(() -> {
                            Toast.makeText(FaceScanLocationActivity.this,
                                    "Error capturing photo: " + exception.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            updateButtonText("Capture Face Photo");
                        });
                    }
                }
        );
    }

    private void fetchReferenceFaceImage() {
        Log.d(TAG, "Fetching reference face image for studentId: " + studentId);

        // Changed from "studentimages" to "users" collection
        firestore.collection("users")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Changed field name from "referenceFaceUrl" to "profilePictureUrl"
                        referenceFaceImageUrl = queryDocumentSnapshots.getDocuments().get(0)
                                .getString("profilePictureUrl");

                        if (referenceFaceImageUrl != null && !referenceFaceImageUrl.isEmpty()) {
                            Log.d(TAG, "Reference face URL from users collection: " + referenceFaceImageUrl);
                        } else {
                            Log.e(TAG, "No profile picture found for student: " + studentId);
                            Toast.makeText(this,
                                    "No profile picture found. Please upload one in your profile.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "No user found for student: " + studentId);
                        Toast.makeText(this,
                                "Student record not found. Please contact admin.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user profile: " + e.getMessage());
                    Toast.makeText(this,
                            "Error fetching profile picture: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void verifyFaceWithFaceNet(File capturedImageFile) {
        Log.d(TAG, "Starting FaceNet verification...");

        if (referenceFaceImageUrl == null || referenceFaceImageUrl.isEmpty()) {
            showVerificationError("No profile picture found. Please upload a profile picture first.");
            return;
        }

        runOnUiThread(() -> {
            if (tvFaceStatus != null) {
                tvFaceStatus.setText("Verifying face with AI...");
                tvFaceStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
            }
            updateButtonText("Verifying Face...");
            Toast.makeText(this, "🔍 Verifying your face...", Toast.LENGTH_SHORT).show();
        });

        cameraExecutor.execute(() -> {
            try {
                // 1. Load captured image
                Bitmap capturedBitmap = BitmapFactory.decodeFile(capturedImageFile.getAbsolutePath());
                if (capturedBitmap == null) {
                    showVerificationError("Failed to load captured image");
                    return;
                }
                Log.d(TAG, "Captured image loaded: " + capturedBitmap.getWidth() + "x" + capturedBitmap.getHeight());

                float[] capturedEmbedding = faceNetHelper.getEmbedding(capturedBitmap, capturedImageFile.getAbsolutePath());
                if (capturedEmbedding == null) {
                    showVerificationError("Failed to process captured face");
                    return;
                }

                // 3. Load reference image from users.profilePictureUrl
                Log.d(TAG, "Loading reference image from users.profilePictureUrl: " + referenceFaceImageUrl);
                Bitmap referenceBitmap = loadReferenceImageConsistent(referenceFaceImageUrl);
                if (referenceBitmap == null) {
                    showVerificationError("Failed to load profile picture. Please check your internet connection.");
                    return;
                }
                Log.d(TAG, "Reference image loaded: " + referenceBitmap.getWidth() + "x" + referenceBitmap.getHeight());

                // 4. Generate embedding for reference face
                float[] referenceEmbedding = faceNetHelper.getEmbedding(referenceBitmap);
                if (referenceEmbedding == null) {
                    showVerificationError("Failed to process reference face");
                    return;
                }

                Log.d(TAG, "=== BEFORE NORMALIZATION ===");
                Log.d(TAG, "Captured magnitude: " + faceNetHelper.calculateMagnitude(capturedEmbedding));
                Log.d(TAG, "Reference magnitude: " + faceNetHelper.calculateMagnitude(referenceEmbedding));

                // 5. Create copies and normalize
                float[] capturedNormalized = capturedEmbedding.clone();
                float[] referenceNormalized = referenceEmbedding.clone();

                faceNetHelper.normalizeEmbedding(capturedNormalized);
                faceNetHelper.normalizeEmbedding(referenceNormalized);

                Log.d(TAG, "=== AFTER NORMALIZATION ===");
                Log.d(TAG, "Captured magnitude: " + faceNetHelper.calculateMagnitude(capturedNormalized));
                Log.d(TAG, "Reference magnitude: " + faceNetHelper.calculateMagnitude(referenceNormalized));

                // 6. Calculate both metrics
                faceSimilarityScore = faceNetHelper.calculateDistance(capturedNormalized, referenceNormalized);
                float cosineSim = faceNetHelper.calculateCosineSimilarity(capturedNormalized, referenceNormalized);
                lastCosineSimilarity = cosineSim;

                Log.d(TAG, "=== FINAL RESULT ===");
                Log.d(TAG, "Euclidean distance: " + faceSimilarityScore);
                Log.d(TAG, "Cosine similarity: " + cosineSim);
                Log.d(TAG, "Reference source: users.profilePictureUrl");

                // Thresholds
                final float COSINE_THRESHOLD = 0.70f;
                final float DISTANCE_THRESHOLD = 1.3f;

                boolean passedCosineSimilarity = cosineSim >= COSINE_THRESHOLD;
                boolean passedDistance = faceSimilarityScore <= DISTANCE_THRESHOLD;

                Log.d(TAG, "Cosine check: " + (passedCosineSimilarity ? "PASS" : "FAIL") +
                        " (" + cosineSim + " >= " + COSINE_THRESHOLD + ")");
                Log.d(TAG, "Distance check: " + (passedDistance ? "PASS" : "FAIL") +
                        " (" + faceSimilarityScore + " <= " + DISTANCE_THRESHOLD + ")");

                boolean verified = passedCosineSimilarity;

                runOnUiThread(() -> {
                    if (verified) {
                        faceVerified = true;
                        updateVerificationStatus();

                        String message = String.format(
                                "Face verified!"
                        );

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "VERIFICATION PASSED");

                        if (locationVerified && faceVerified) {
                            saveAttendanceToFirestore();
                        }
                    } else {
                        faceVerified = false;
                        if (tvFaceStatus != null) {
                            tvFaceStatus.setText("Face verification failed");
                            tvFaceStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                        }
                        updateButtonText("Try Again");
                        cameraPreview.setVisibility(View.VISIBLE);

                        String message = String.format(
                                "Face doesn't match! Please Try again"
                        );

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        Log.d(TAG, "VERIFICATION FAILED");
                    }
                });

                saveDebugInfoToFirestore(faceSimilarityScore, cosineSim, verified);

            } catch (Exception e) {
                Log.e(TAG, "Error in face verification: " + e.getMessage(), e);
                showVerificationError("Verification error: " + e.getMessage());
            }
        });
    }

    private void saveDebugInfoToFirestore(float distance, float cosineSim, boolean verified) {
        try {
            Map<String, Object> debugData = new HashMap<>();
            debugData.put("studentId", studentId);
            debugData.put("timestamp", Timestamp.now());
            debugData.put("euclideanDistance", distance);
            debugData.put("cosineSimilarity", cosineSim);
            debugData.put("verificationResult", verified);
            debugData.put("sessionId", sessionId);

            firestore.collection("face_verification_logs")
                    .add(debugData)
                    .addOnSuccessListener(doc -> Log.d(TAG, "Debug info saved"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save debug info"));
        } catch (Exception e) {
            Log.e(TAG, "Error saving debug info: " + e.getMessage());
        }
    }

    // reference image loading with error handling
    private Bitmap loadReferenceImageConsistent(String imageUrl) {
        try {
            Log.d(TAG, "Attempting to load reference image...");
            Log.d(TAG, "URL: " + imageUrl);

            if (imageUrl == null || imageUrl.isEmpty()) {
                Log.e(TAG, "Reference image URL is empty!");
                return null;
            }

            // Try with Glide first (handles caching better)
            try {
                Bitmap originalBitmap = com.bumptech.glide.Glide.with(this)
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get();

                if (originalBitmap != null) {
                    Log.d(TAG, "Glide loaded image: " +
                            originalBitmap.getWidth() + "x" + originalBitmap.getHeight());

                    // Ensure ARGB_8888 format
                    Bitmap rgbBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, false);
                    if (rgbBitmap != originalBitmap) {
                        originalBitmap.recycle();
                    }
                    return rgbBitmap;
                }
            } catch (Exception glideError) {
                Log.w(TAG, "Glide failed, trying HttpURLConnection: " + glideError.getMessage());
            }

            // Fallback: HttpURLConnection
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.connect();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "HTTP Response: " + responseCode);

            if (responseCode == 200) {
                java.io.InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();

                if (bitmap != null) {
                    Log.d(TAG, "HttpURLConnection loaded image: " +
                            bitmap.getWidth() + "x" + bitmap.getHeight());
                }
                return bitmap;
            } else {
                Log.e(TAG, "HTTP error: " + responseCode);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading reference image: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Calculate magnitude
    private float calculateMagnitude(float[] embedding) {
        if (embedding == null) return 0;
        float magnitude = 0.0f;
        for (float val : embedding) {
            magnitude += val * val;
        }
        return (float) Math.sqrt(magnitude);
    }

    // Normalize embedding with logging
    private void normalizeEmbedding(float[] embedding) {
        if (embedding == null) {
            Log.e(TAG, "Cannot normalize null embedding");
            return;
        }

        float magnitude = calculateMagnitude(embedding);
        Log.d(TAG, "Normalizing embedding - Before: magnitude = " + magnitude);

        if (magnitude > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] = embedding[i] / magnitude;
            }
            float newMagnitude = calculateMagnitude(embedding);
            Log.d(TAG, "Normalizing embedding - After: magnitude = " + newMagnitude);
        } else {
            Log.e(TAG, "Cannot normalize - magnitude is 0");
        }
    }

    private void testNormalization() {
        Log.d(TAG, "=== TESTING NORMALIZATION ===");
        float[] testEmbedding = {3.0f, 4.0f, 0.0f}; // Should normalize to magnitude 1.0

        float beforeMagnitude = calculateMagnitude(testEmbedding);
        Log.d(TAG, "Test embedding before: " + beforeMagnitude);

        normalizeEmbedding(testEmbedding);

        float afterMagnitude = calculateMagnitude(testEmbedding);
        Log.d(TAG, "Test embedding after: " + afterMagnitude);

        if (Math.abs(afterMagnitude - 1.0f) < 0.01f) {
            Log.d(TAG, "Normalization test PASSED");
        } else {
            Log.e(TAG, "Normalization test FAILED");
        }
        Log.d(TAG, "=== END TEST ===");
    }

    private Bitmap loadAndProcessReferenceImage(String imageUrl) {
        try {
            // Download the image
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            java.io.InputStream input = connection.getInputStream();

            // Decode to bitmap
            Bitmap originalBitmap = android.graphics.BitmapFactory.decodeStream(input);

            if (originalBitmap != null) {
                Log.d(TAG, "Reference image loaded - Original size: " +
                        originalBitmap.getWidth() + "x" + originalBitmap.getHeight());
            }

            return originalBitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error loading reference image: " + e.getMessage());
            return null;
        }
    }



    private void showVerificationError(String message) {
        Log.e(TAG, message);
        runOnUiThread(() -> {
            Toast.makeText(this, "" + message, Toast.LENGTH_LONG).show();
            if (tvFaceStatus != null) {
                tvFaceStatus.setText("Verification failed");
                tvFaceStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            }
            updateButtonText("Try Again");

            // camera preview again for retry
            cameraPreview.setVisibility(View.VISIBLE);
            isCameraActive = true;
        });
    }
    private void uploadFaceImageToFirebase(File imageFile, String fileName) {
        Log.d(TAG, "Uploading face image to Firebase...");

        runOnUiThread(() -> {
            if (tvFaceStatus != null) {
                tvFaceStatus.setText("Uploading face image...");
                tvFaceStatus.setTextColor(getColor(android.R.color.holo_orange_dark));
            }
            updateButtonText("Uploading...");
        });

        // Create storage reference
        StorageReference faceImagesRef = storage.getReference()
                .child("face_images")
                .child(studentId)
                .child(fileName);

        // Upload file
        UploadTask uploadTask = faceImagesRef.putFile(android.net.Uri.fromFile(imageFile));

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get download URL
            faceImagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                capturedFaceImageUrl = uri.toString();
                Log.d(TAG, "Face image uploaded successfully: " + capturedFaceImageUrl);

                runOnUiThread(() -> {
                    Toast.makeText(FaceScanLocationActivity.this,
                            "Face captured! Now verifying...", Toast.LENGTH_SHORT).show();

                    // Hide camera preview
                    cameraPreview.setVisibility(View.GONE);
                });

                verifyFaceWithFaceNet(imageFile);
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error uploading face image: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(FaceScanLocationActivity.this,
                        "Error uploading face image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                updateButtonText("Capture Face Photo");
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRealLocationVerification();
            } else {
                Toast.makeText(this, "Location permission required for attendance", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRealFaceCapture();
            } else {
                Toast.makeText(this, "Camera permission required for face capture", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateVerificationStatus() {
        try {
            runOnUiThread(() -> {
                if (locationVerified && tvLocationStatus != null) {
                    tvLocationStatus.setText("✓ Location Verified");
                    tvLocationStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                }

                if (faceVerified && tvFaceStatus != null) {
                    tvFaceStatus.setText("✓ Face Captured & Verified");
                    tvFaceStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in updateVerificationStatus: " + e.getMessage());
        }
    }

    private void saveAttendanceToFirestore() {
        Log.d(TAG, "Saving attendance with face verification scores...");
        try {
            Toast.makeText(this, "Saving attendance...", Toast.LENGTH_SHORT).show();

            Map<String, Object> attendanceData = new HashMap<>();

            // Student Information
            attendanceData.put("studentId", studentId);
            attendanceData.put("studentName", studentName);
            attendanceData.put("studentEmail", studentEmail);
            attendanceData.put("programme", programme);
            attendanceData.put("batch", batch);

            // Teacher Information
            attendanceData.put("teacherId", teacherId);
            attendanceData.put("teacherName", teacherName);
            attendanceData.put("subject", subject);
            attendanceData.put("sessionId", sessionId);

            // QR Code Data
            attendanceData.put("qrData", qrData);

            // Timestamps
            attendanceData.put("timestamp", Timestamp.now());
            attendanceData.put("attendanceTime", Timestamp.now());

            // Verification Status
            attendanceData.put("locationVerified", locationVerified);
            attendanceData.put("faceVerified", faceVerified);
            attendanceData.put("status", "present");

            // Face Verification Details
            Map<String, Object> faceVerificationData = new HashMap<>();
            faceVerificationData.put("faceImageUrl", capturedFaceImageUrl);
            faceVerificationData.put("referenceFaceUrl", referenceFaceImageUrl);
            faceVerificationData.put("referenceSource", "users_collection");
            faceVerificationData.put("verificationMethod", "facenet_tflite_512d");
            faceVerificationData.put("captureTime", Timestamp.now());
            faceVerificationData.put("euclideanDistance", faceSimilarityScore);
            faceVerificationData.put("cosineSimilarity", lastCosineSimilarity);
            faceVerificationData.put("distanceThreshold", 1.3f);
            faceVerificationData.put("cosineThreshold", 0.70f);
            faceVerificationData.put("embeddingDimension", 512);
            faceVerificationData.put("verified", faceVerified);

            attendanceData.put("faceVerification", faceVerificationData);

            // Location Data
            Map<String, Object> locationData = new HashMap<>();
            if (currentLocation != null) {
                locationData.put("latitude", currentLocation.getLatitude());
                locationData.put("longitude", currentLocation.getLongitude());
                locationData.put("accuracy", currentLocation.getAccuracy());
                locationData.put("address", "Detected via GPS");
                locationData.put("distanceFromTarget", currentLocation.distanceTo(createTargetLocation()));
            } else {
                locationData.put("latitude", 0.0);
                locationData.put("longitude", 0.0);
                locationData.put("address", "Location not available");
            }
            attendanceData.put("location", locationData);

            // Device Information
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", android.os.Build.MODEL);
            deviceInfo.put("os", "Android " + android.os.Build.VERSION.RELEASE);
            attendanceData.put("deviceInfo", deviceInfo);

            // ✅ SAVE TO MAIN ATTENDANCE COLLECTION (for general records)
            firestore.collection("attendance")
                    .add(attendanceData)
                    .addOnSuccessListener(documentReference -> {
                        String attendanceId = documentReference.getId();
                        Log.d(TAG, "✓ Saved to attendance collection: " + attendanceId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save to attendance collection: " + e.getMessage());
                    });

            // ✅ SAVE TO SCHEDULE ATTENDANCE SUBCOLLECTION (for attendance history)
            if (scheduleId != null && !scheduleId.isEmpty()) {
                // Create schedule-specific attendance data
                Map<String, Object> scheduleAttendanceData = new HashMap<>();
                scheduleAttendanceData.put("studentId", studentId);
                scheduleAttendanceData.put("studentEmail", studentEmail);
                scheduleAttendanceData.put("studentName", studentName);
                scheduleAttendanceData.put("subjectName", subject);
                scheduleAttendanceData.put("status", "present");
                scheduleAttendanceData.put("markedAt", System.currentTimeMillis());

                // Add lecture details if available
                if (lectureNumber != null && !lectureNumber.isEmpty()) {
                    scheduleAttendanceData.put("lectureId", "L" + lectureNumber);
                    scheduleAttendanceData.put("lectureNumber", Integer.parseInt(lectureNumber));
                }
                if (lectureDate != null) {
                    scheduleAttendanceData.put("date", lectureDate);
                }
                if (lectureStartTime != null) {
                    scheduleAttendanceData.put("startTime", lectureStartTime);
                }
                if (lectureEndTime != null) {
                    scheduleAttendanceData.put("endTime", lectureEndTime);
                }

                // Add face verification status
                scheduleAttendanceData.put("faceVerified", faceVerified);
                scheduleAttendanceData.put("locationVerified", locationVerified);
                scheduleAttendanceData.put("cosineSimilarity", lastCosineSimilarity);

                firestore.collection("schedules")
                        .document(scheduleId)
                        .collection("attendance")
                        .add(scheduleAttendanceData)
                        .addOnSuccessListener(docRef -> {
                            Log.d(TAG, "✓ Saved to schedule attendance: " + docRef.getId());
                            Log.d(TAG, "   Schedule ID: " + scheduleId);
                            Log.d(TAG, "   Subject: " + subject);
                            Log.d(TAG, "   Lecture: " + lectureNumber);
                            Log.d(TAG, "   Cosine Similarity: " + lastCosineSimilarity);

                            Toast.makeText(this, "Attendance saved successfully!", Toast.LENGTH_LONG).show();
                            navigateToDashboard();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to save to schedule attendance: " + e.getMessage());
                            Toast.makeText(this, "Attendance saved, but history update failed", Toast.LENGTH_LONG).show();
                            navigateToDashboard();
                        });
            } else {
                // No scheduleId available - still navigate but log warning
                Log.w(TAG, "⚠️ No scheduleId available - attendance saved to main collection only");
                Toast.makeText(this, "Attendance saved (no schedule link)", Toast.LENGTH_LONG).show();
                navigateToDashboard();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in saveAttendanceToFirestore: " + e.getMessage());
            Toast.makeText(this, "Error saving attendance", Toast.LENGTH_SHORT).show();
        }
    }

    private Location createTargetLocation() {
        Location target = new Location("target");
        target.setLatitude(TARGET_LATITUDE);
        target.setLongitude(TARGET_LONGITUDE);
        return target;
    }

    private void navigateToDashboard() {
        try {
            Intent intent = new Intent(this, StudentDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to dashboard: " + e.getMessage());
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown camera executor
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (faceNetHelper != null) {
            faceNetHelper.close();
        }
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
}