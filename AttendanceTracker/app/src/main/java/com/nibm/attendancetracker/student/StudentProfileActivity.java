package com.nibm.attendancetracker.student;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nibm.attendancetracker.common.BlurNavigationHelper;
import com.nibm.attendancetracker.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentProfileActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 1001;
    private static final int GALLERY_REQUEST = 1002;
    private static final int CAMERA_PERMISSION_CODE = 1003;
    private static final int STORAGE_PERMISSION_CODE = 1004;

    // Profile Views
    private CircleImageView profileImage;
    private ImageView btnBack, btnEditPhoto;
    private EditText etName, etEmail, etPhone, etAddress, etProgram, etBatch;
    private TextView tvName, tvStudentId;
    private Switch switchNotifications;
    private Button btnSaveChanges;

    // Bottom Navigation Views
    private LinearLayout navHome, navDocuments, navChat, navMenu, navProfile;
    private TextView homeText, documentsText, chatText, menuText, profileText;
    private ImageView homeIcon, documentsIcon, chatIcon, menuIcon, profileIcon;

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String currentUserEmail;
    private String currentUserId;

    private SharedPreferences sharedPreferences;
    private int currentSelectedTab = 4; // Profile tab is selected (index 4)

    // Temporary storage for selected image
    private Bitmap selectedBitmap;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        initViews();
        initFirebase();
        initBottomNavigation();
        setupClickListeners();
        setupBottomNavigationListeners();
        loadUserProfileFromFirestore();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set profile tab as selected
        selectTab(4);
    }

    private void initViews() {
        // Profile views
        profileImage = findViewById(R.id.profile_image);
        btnBack = findViewById(R.id.btn_back);
        btnEditPhoto = findViewById(R.id.btn_edit_photo);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        etProgram = findViewById(R.id.et_program);
        etBatch = findViewById(R.id.et_batch);
        tvName = findViewById(R.id.tv_name);
        tvStudentId = findViewById(R.id.student_id);
        switchNotifications = findViewById(R.id.switch_notifications);
        btnSaveChanges = findViewById(R.id.btn_save_changes);

        sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
        currentUserEmail = sharedPreferences.getString("current_user_email", "");

        // Make program and batch non-editable
        etProgram.setEnabled(false);
        etProgram.setFocusable(false);
        etProgram.setClickable(false);

        etBatch.setEnabled(false);
        etBatch.setFocusable(false);
        etBatch.setClickable(false);

        // Set different background for disabled fields to indicate they're read-only
        etProgram.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        etBatch.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
    }

    private void initFirebase() {
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    private void loadUserProfileFromFirestore() {
        if (TextUtils.isEmpty(currentUserEmail)) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users")
                .whereEqualTo("email", currentUserEmail)
                .whereEqualTo("role", "student")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        currentUserId = document.getId();
                        populateUserData(document);
                    } else {
                        Toast.makeText(StudentProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                        // Load from SharedPreferences as fallback
                        loadFromSharedPreferences();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadFromSharedPreferences();
                });
    }

    private void populateUserData(DocumentSnapshot document) {
        try {
            // Name fields
            String firstName = document.getString("firstName");
            String lastName = document.getString("lastName");
            String fullName = document.getString("name");

            if (fullName == null && firstName != null && lastName != null) {
                fullName = firstName + " " + lastName;
            }

            if (etName != null) etName.setText(fullName != null ? fullName : "");
            if (tvName != null) tvName.setText(fullName != null ? fullName : "");

            // Email
            String email = document.getString("email");
            if (etEmail != null) etEmail.setText(email != null ? email : "");

            // Phone
            String phone = document.getString("phone");
            if (etPhone != null) etPhone.setText(phone != null ? phone : "");

            // Address
            String address = document.getString("address");
            if (etAddress != null) etAddress.setText(address != null ? address : "");

            // Program
            String program = document.getString("programme");
            if (etProgram != null) etProgram.setText(program != null ? program : "");

            // Batch
            String batch = document.getString("batch");
            if (etBatch != null) etBatch.setText(batch != null ? batch : "");

            // Student ID
            String studentId = document.getString("studentId");
            if (tvStudentId != null) tvStudentId.setText(studentId != null ? studentId : "");

            // Profile Picture - Load from profilePictureUrl
            String profilePictureUrl = document.getString("profilePictureUrl");
            if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                loadProfilePicture(profilePictureUrl);
            } else {
                // Set default profile picture if no URL is available
                profileImage.setImageResource(R.drawable.default_profile);
            }

            // Save to SharedPreferences for offline use
            saveToSharedPreferences(document);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing user data", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfilePicture(String profilePictureUrl) {
        // Use Glide to load the profile picture from URL
        Glide.with(this)
                .load(profilePictureUrl)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .placeholder(R.drawable.default_profile) // Default image while loading
                .error(R.drawable.default_profile) // Default image if error occurs
                .into(profileImage);
    }

    private void saveToSharedPreferences(DocumentSnapshot document) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("name", document.getString("name"));
        editor.putString("email", document.getString("email"));
        editor.putString("phone", document.getString("phone"));
        editor.putString("address", document.getString("address"));
        editor.putString("programme", document.getString("programme"));
        editor.putString("batch", document.getString("batch"));
        editor.putString("studentId", document.getString("studentId"));
        editor.putString("firstName", document.getString("firstName"));
        editor.putString("lastName", document.getString("lastName"));
        editor.putString("profilePictureUrl", document.getString("profilePictureUrl"));

        editor.apply();
    }

    private void loadFromSharedPreferences() {
        String name = sharedPreferences.getString("name", "");
        String email = sharedPreferences.getString("email", "");
        String phone = sharedPreferences.getString("phone", "");
        String address = sharedPreferences.getString("address", "");
        String program = sharedPreferences.getString("programme", "");
        String batch = sharedPreferences.getString("batch", "");
        String studentId = sharedPreferences.getString("studentId", "");
        String profilePictureUrl = sharedPreferences.getString("profilePictureUrl", "");

        if (etName != null) etName.setText(name);
        if (tvName != null) tvName.setText(name);
        if (etEmail != null) etEmail.setText(email);
        if (etPhone != null) etPhone.setText(phone);
        if (etAddress != null) etAddress.setText(address);
        if (etProgram != null) etProgram.setText(program);
        if (etBatch != null) etBatch.setText(batch);
        if (tvStudentId != null) tvStudentId.setText(studentId);

        // Load profile picture from SharedPreferences URL
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            loadProfilePicture(profilePictureUrl);
        } else {
            profileImage.setImageResource(R.drawable.default_profile);
        }
    }

    // -------------------- GLASSY NAVIGATION BAR --------------------

    private void initBottomNavigation() {
        // Initialize navigation layouts
        navHome = findViewById(R.id.nav_home);
        navDocuments = findViewById(R.id.nav_documents);
        navChat = findViewById(R.id.nav_chat);
        navMenu = findViewById(R.id.nav_menu);
        navProfile = findViewById(R.id.nav_profile);

        // Initialize all text views
        homeText = findViewById(R.id.home_text);
        documentsText = findViewById(R.id.documents_text);
        chatText = findViewById(R.id.chat_text);
        menuText = findViewById(R.id.menu_text);
        profileText = findViewById(R.id.profile_text);

        // Initialize all icons
        homeIcon = findViewById(R.id.home_icon);
        documentsIcon = findViewById(R.id.documents_icon);
        chatIcon = findViewById(R.id.chat_icon);
        menuIcon = findViewById(R.id.menu_icon);
        profileIcon = findViewById(R.id.profile_icon);
    }

    private void setupBottomNavigationListeners() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                selectTab(0);
                Intent intent = new Intent(this, StudentDashboardActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (navDocuments != null) {
            navDocuments.setOnClickListener(v -> {
                selectTab(1);
                Intent intent = new Intent(this, QRScannerActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (navChat != null) {
            navChat.setOnClickListener(v -> {
                selectTab(2);
                Intent intent = new Intent(this, QRScannerActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (navMenu != null) {
            navMenu.setOnClickListener(v -> {
                selectTab(3);
                Toast.makeText(this, "Menu selected", Toast.LENGTH_SHORT).show();
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                selectTab(4);
                // Already on Profile, just update selection
            });
        }
    }

    private void selectTab(int tabIndex) {
        resetAllTabs();
        currentSelectedTab = tabIndex;

        switch (tabIndex) {
            case 0: // Home
                setTabSelected(navHome, homeText, "Home");
                break;
            case 1: // Documents
                setTabSelected(navDocuments, documentsText, "Documents");
                break;
            case 2: // Chat
                setTabSelected(navChat, chatText, "Chat");
                break;
            case 3: // Menu
                setTabSelected(navMenu, menuText, "Menu");
                break;
            case 4: // Profile
                setTabSelected(navProfile, profileText, "Profile");
                break;
        }
    }

    private void resetAllTabs() {
        setTabInactive(navHome, homeText);
        setTabInactive(navDocuments, documentsText);
        setTabInactive(navChat, chatText);
        setTabInactive(navMenu, menuText);
        setTabInactive(navProfile, profileText);
    }

    private void setTabSelected(LinearLayout tabLayout, TextView textView, String text) {
        if (tabLayout != null && textView != null) {
            tabLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.nav_button_active));
            textView.setText(text);
            textView.setVisibility(TextView.VISIBLE);
            animateTabExpansion(tabLayout, textView, true);
        }
    }

    private void setTabInactive(LinearLayout tabLayout, TextView textView) {
        if (tabLayout != null && textView != null) {
            tabLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.nav_button_inactive));
            textView.setVisibility(TextView.GONE);
            animateTabExpansion(tabLayout, textView, false);
        }
    }

    private void animateTabExpansion(LinearLayout tabLayout, TextView textView, boolean expand) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tabLayout.getLayoutParams();

        if (expand) {
            params.weight = 2.0f;
        } else {
            params.weight = 1.0f;
        }

        tabLayout.setLayoutParams(params);

        if (expand) {
            textView.setAlpha(0f);
            textView.setVisibility(TextView.VISIBLE);
            textView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        } else {
            textView.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> textView.setVisibility(TextView.GONE))
                    .start();
        }
    }

    // -------------------- PROFILE FUNCTIONALITY --------------------

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnEditPhoto != null) {
            btnEditPhoto.setOnClickListener(v -> showImagePickerBottomSheet());
        }

        View changePasswordLayout = findViewById(R.id.layout_change_password);
        if (changePasswordLayout != null) {
            changePasswordLayout.setOnClickListener(v -> {
                Toast.makeText(this, "Change Password clicked", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSaveChanges != null) {
            btnSaveChanges.setOnClickListener(v -> {
                // If there's a new profile picture selected, upload it first
                if (selectedBitmap != null || selectedImageUri != null) {
                    uploadProfilePicture();
                } else {
                    saveUserProfileToFirestore();
                }
            });
        }
    }

    private void showImagePickerBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image_picker, null);

        bottomSheetView.findViewById(R.id.btn_camera).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openCamera();
        });

        bottomSheetView.findViewById(R.id.btn_gallery).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openGallery();
        });

        bottomSheetView.findViewById(R.id.btn_cancel).setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            launchCameraIntent();
        }
    }

    private void launchCameraIntent() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) uses READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);
            } else {
                launchGalleryIntent();
            }
        } else {
            // Android 12 and below uses READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            } else {
                launchGalleryIntent();
            }
        }
    }

    private void launchGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && profileImage != null) {
            if (requestCode == CAMERA_REQUEST && data != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                if (bitmap != null) {
                    selectedBitmap = bitmap;
                    profileImage.setImageBitmap(bitmap);
                    // Clear the URI since we're using bitmap from camera
                    selectedImageUri = null;
                    Toast.makeText(this, "Profile picture selected. Click 'Save Changes' to update.", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == GALLERY_REQUEST && data != null) {
                selectedImageUri = data.getData();
                try {
                    InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                    selectedBitmap = bitmap;
                    profileImage.setImageBitmap(bitmap);
                    Toast.makeText(this, "Profile picture selected. Click 'Save Changes' to update.", Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == CAMERA_PERMISSION_CODE) {
                launchCameraIntent();
            } else if (requestCode == STORAGE_PERMISSION_CODE) {
                // Permission granted, now launch the gallery
                launchGalleryIntent();
            }
        } else {
            // Permission denied
            if (requestCode == STORAGE_PERMISSION_CODE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES)) {
                        // User selected "Don't ask again", show explanation
                        showPermissionExplanationDialog("Gallery access is required to select profile pictures. Please enable it in Settings.");
                    } else {
                        Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // User selected "Don't ask again", show explanation
                        showPermissionExplanationDialog("Storage access is required to select profile pictures. Please enable it in Settings.");
                    } else {
                        Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (requestCode == CAMERA_PERMISSION_CODE) {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPermissionExplanationDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage(message)
                .setPositiveButton("Settings", (dialog, which) -> {
                    // Open app settings so user can enable permissions
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadProfilePicture() {
        if (TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "User not logged in properly", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Uploading...");

        // Create a reference to the profile picture in Firebase Storage
        StorageReference profileImageRef = storageReference
                .child("profile_pictures")
                .child(currentUserId + "_" + System.currentTimeMillis() + ".jpg");

        UploadTask uploadTask;

        if (selectedImageUri != null) {
            // Upload from URI (gallery selection)
            uploadTask = profileImageRef.putFile(selectedImageUri);
        } else if (selectedBitmap != null) {
            // Upload from Bitmap (camera capture)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageData = baos.toByteArray();
            uploadTask = profileImageRef.putBytes(imageData);
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText("Save Changes");
            return;
        }

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Image uploaded successfully, now get the download URL
            profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();

                // Update Firestore with the new profile picture URL
                updateProfilePictureUrl(downloadUrl);

            }).addOnFailureListener(e -> {
                Toast.makeText(StudentProfileActivity.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnSaveChanges.setEnabled(true);
                btnSaveChanges.setText("Save Changes");
            });

        }).addOnFailureListener(e -> {
            Toast.makeText(StudentProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText("Save Changes");
        });
    }

    private void updateProfilePictureUrl(String imageUrl) {
        if (TextUtils.isEmpty(currentUserId)) {
            return;
        }

        // Update Firestore document
        Map<String, Object> updates = new HashMap<>();
        updates.put("profilePictureUrl", imageUrl);

        firestore.collection("users").document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("profilePictureUrl", imageUrl);
                    editor.apply();

                    // Now save the rest of the profile data
                    saveUserProfileToFirestore();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(StudentProfileActivity.this, "Failed to update profile picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                });
    }

    private void saveUserProfileToFirestore() {
        if (TextUtils.isEmpty(currentUserEmail) || TextUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "User not logged in properly", Toast.LENGTH_SHORT).show();
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText("Save Changes");
            return;
        }

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText("Save Changes");
            return;
        }

        // Split name into first and last name
        String[] nameParts = name.split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Create update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("address", address);

        // Update Firestore
        firestore.collection("users").document(currentUserId)
                .update(updates)
                .addOnCompleteListener(task -> {
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");

                    if (task.isSuccessful()) {
                        // Update local display
                        if (tvName != null) {
                            tvName.setText(name);
                        }

                        // Save to SharedPreferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("name", name);
                        editor.putString("email", email);
                        editor.putString("phone", phone);
                        editor.putString("address", address);
                        editor.putString("firstName", firstName);
                        editor.putString("lastName", lastName);
                        editor.apply();

                        // Clear selected image data
                        selectedBitmap = null;
                        selectedImageUri = null;

                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentSelectedTab == 4) {
            selectTab(4);
        }
    }

    public int getCurrentSelectedTab() {
        return currentSelectedTab;
    }
}
