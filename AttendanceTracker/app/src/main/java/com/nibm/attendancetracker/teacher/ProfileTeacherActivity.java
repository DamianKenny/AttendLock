package com.nibm.attendancetracker.teacher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.common.NavigationHelper; // Use the common NavigationHelper
import com.nibm.attendancetracker.student.QRScannerActivity;
import com.nibm.attendancetracker.student.StudentDashboardActivity;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileTeacherActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 1001;
    private static final int GALLERY_REQUEST = 1002;
    private static final int CAMERA_PERMISSION_CODE = 1003;
    private static final int STORAGE_PERMISSION_CODE = 1004;

    private CircleImageView profileImage;
    private ImageView btnBack, btnEditPhoto;
    private EditText etName, etEmail, etPhone, etAddress;
    private TextView tvName;
    private Spinner spinnerSubject;
    private Switch switchNotifications, switchDarkMode;
    private Button btnSaveChanges;

    private SharedPreferences sharedPreferences;
    private FirebaseFirestore firestore;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_teacher);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        NavigationHelper.setupNavigation(this, "teacher");

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
        currentUserEmail = sharedPreferences.getString("current_user_email", null);

        initViews();
        setupClickListeners();

        if (currentUserEmail != null) {
            loadTeacherProfile(currentUserEmail);
        } else {
            Toast.makeText(this, "No user email found. Please log in again.", Toast.LENGTH_SHORT).show();
        }

    }

    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        btnBack = findViewById(R.id.btn_back);
        btnEditPhoto = findViewById(R.id.btn_edit_photo);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        tvName = findViewById(R.id.tv_name);
        spinnerSubject = findViewById(R.id.spinner_subject);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
    }

    // -------------------- LOAD PROFILE --------------------
    private void loadTeacherProfile(String email) {
        firestore.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String department = doc.getString("department");
                        String qualification = doc.getString("qualification");
                        String profileUrl = doc.getString("profilePictureUrl");
                        List<String> assignedSubjects = (List<String>) doc.get("assignedSubjectNames");

                        // Set data
                        if (etName != null) etName.setText(name);
                        if (tvName != null) tvName.setText(name);
                        if (etEmail != null) etEmail.setText(email);
                        if (etPhone != null) etPhone.setText(phone);
                        if (etAddress != null) etAddress.setText(department + " • " + qualification);

                        // Profile image
                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.ic_person_filled)
                                    .error(R.drawable.ic_person_filled)
                                    .into(profileImage);
                        }

                        // Subjects
                        if (assignedSubjects != null && !assignedSubjects.isEmpty()) {
                            setupSubjectSpinner(assignedSubjects);
                        } else {
                            setupSubjectSpinner(null);
                        }
                    } else {
                        Toast.makeText(this, "Teacher profile not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void setupSubjectSpinner(List<String> subjectList) {
        if (subjectList == null || subjectList.isEmpty()) {
            subjectList = java.util.Arrays.asList("No subjects assigned");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                subjectList
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);
    }

    // -------------------- CLICK HANDLERS --------------------
    private void setupClickListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnEditPhoto != null) btnEditPhoto.setOnClickListener(v -> showImagePickerBottomSheet());
        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v ->
                    Toast.makeText(this, "Profile changes saved locally.", Toast.LENGTH_SHORT).show()
            );
        }
    }

    // -------------------- IMAGE PICKER --------------------
    private void showImagePickerBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_image_picker, null);
        sheet.findViewById(R.id.btn_camera).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openCamera();
        });
        sheet.findViewById(R.id.btn_gallery).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openGallery();
        });
        sheet.findViewById(R.id.btn_cancel).setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.setContentView(sheet);
        bottomSheetDialog.show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        } else {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, GALLERY_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && profileImage != null) {
            if (requestCode == CAMERA_REQUEST && data != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                if (bitmap != null) profileImage.setImageBitmap(bitmap);
            } else if (requestCode == GALLERY_REQUEST && data != null) {
                Uri imageUri = data.getData();
                try {
                    InputStream stream = getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    profileImage.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}