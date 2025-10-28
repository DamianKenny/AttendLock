package com.nibm.attendancetracker.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nibm.attendancetracker.R;
import com.nibm.attendancetracker.admin.AdminDashboardActivity;
import com.nibm.attendancetracker.admin.ViewStudentsActivity;
import com.nibm.attendancetracker.admin.ViewTeachersActivity;
import com.nibm.attendancetracker.admin.ViewScheduleActivity;
import com.nibm.attendancetracker.student.StudentDashboardActivity;
import com.nibm.attendancetracker.student.ClassScheduleActivity;
import com.nibm.attendancetracker.student.QRScannerActivity;
import com.nibm.attendancetracker.student.AttendanceHistoryActivity;
import com.nibm.attendancetracker.student.StudentProfileActivity;
import com.nibm.attendancetracker.teacher.TeacherDashboardActivity;
import com.nibm.attendancetracker.teacher.TeacherQRActivity;
import com.nibm.attendancetracker.teacher.TeacherViewStudentAttendanceActivity;
import com.nibm.attendancetracker.teacher.ProfileTeacherActivity;

public class NavigationHelper {

    public static void setupNavigation(Activity activity, String userRole) {
        LinearLayout navHome = activity.findViewById(R.id.nav_home);
        LinearLayout navSecond = activity.findViewById(R.id.nav_second);
        LinearLayout navThird = activity.findViewById(R.id.nav_third);
        LinearLayout navFourth = activity.findViewById(R.id.nav_fourth);
        LinearLayout navProfile = activity.findViewById(R.id.nav_profile);


        String currentActivityName = activity.getClass().getSimpleName();

        switch (userRole.toLowerCase()) {
            case "admin":
                setupAdminNavigation(activity, navHome, navSecond, navThird, navFourth, navProfile, currentActivityName);
                break;
            case "student":
                setupStudentNavigation(activity, navHome, navSecond, navThird, navFourth, navProfile, currentActivityName);
                break;
            case "teacher":

                setupTeacherNavigation(activity, navHome, navSecond, navThird, navFourth, currentActivityName);
                break;
        }
    }

    private static void setupAdminNavigation(Activity activity, LinearLayout navHome,
                                             LinearLayout navSecond, LinearLayout navThird,
                                             LinearLayout navFourth, LinearLayout navProfile,
                                             String currentActivity) {

        setNavigationItem(activity, navHome, R.drawable.ic_home, "Dashboard",
                currentActivity.equals("AdminDashboardActivity"));
        setNavigationItem(activity, navSecond, R.drawable.ic_student2, "Students",
                currentActivity.equals("ViewStudentsActivity"));
        setNavigationItem(activity, navThird, R.drawable.ic_teacher, "Teachers",
                currentActivity.equals("ViewTeachersActivity"));
        setNavigationItem(activity, navFourth, R.drawable.ic_schedule3, "Schedules",
                currentActivity.equals("ViewScheduleActivity"));
        setNavigationItem(activity, navProfile, R.drawable.ic_profile, "Profile",
                currentActivity.equals("AdminProfileActivity"));

        navHome.setOnClickListener(v -> navigateTo(activity, AdminDashboardActivity.class));
        navSecond.setOnClickListener(v -> navigateTo(activity, ViewStudentsActivity.class));
        navThird.setOnClickListener(v -> navigateTo(activity, ViewTeachersActivity.class));
        navFourth.setOnClickListener(v -> navigateTo(activity, ViewScheduleActivity.class));
        navProfile.setOnClickListener(v -> navigateTo(activity, AdminDashboardActivity.class));
    }

    private static void setupStudentNavigation(Activity activity, LinearLayout navHome,
                                               LinearLayout navSecond, LinearLayout navThird,
                                               LinearLayout navFourth, LinearLayout navProfile,
                                               String currentActivity) {
        // Configure icons and labels for Student
        setNavigationItem(activity, navHome, R.drawable.ic_home, "Home",
                currentActivity.equals("StudentDashboardActivity"));
        setNavigationItem(activity, navSecond, R.drawable.ic_schedule3, "Schedule",
                currentActivity.equals("ClassScheduleActivity"));
        setNavigationItem(activity, navThird, R.drawable.ic_qr_code, "QR Scan",
                currentActivity.equals("QRScannerActivity"));
        setNavigationItem(activity, navFourth, R.drawable.ic_attendance2, "Attendance",
                currentActivity.equals("AttendanceHistoryActivity"));
        setNavigationItem(activity, navProfile, R.drawable.ic_profile, "Profile",
                currentActivity.equals("StudentProfileActivity"));

        // Set click listeners
        navHome.setOnClickListener(v -> navigateTo(activity, StudentDashboardActivity.class));
        navSecond.setOnClickListener(v -> navigateTo(activity, ClassScheduleActivity.class));
        navThird.setOnClickListener(v -> navigateTo(activity, QRScannerActivity.class));
        navFourth.setOnClickListener(v -> navigateTo(activity, AttendanceHistoryActivity.class));
        navProfile.setOnClickListener(v -> navigateTo(activity, StudentProfileActivity.class));
    }


    private static void setupTeacherNavigation(Activity activity, LinearLayout navHome,
                                               LinearLayout navSecond, LinearLayout navThird,
                                               LinearLayout navFourth,
                                               String currentActivity) {

        setNavigationItem(activity, navHome, R.drawable.ic_profile, "Profile",
                currentActivity.equals("ProfileTeacherActivity"));

        setNavigationItem(activity, navSecond, R.drawable.ic_home, "Home",
                currentActivity.equals("TeacherDashboardActivity"));

        setNavigationItem(activity, navThird, R.drawable.ic_attendance2, "Attendance",
                currentActivity.equals("TeacherViewStudentAttendanceActivity"));

        setNavigationItem(activity, navFourth, R.drawable.ic_qr_code, "QR Code",
                currentActivity.equals("TeacherQRActivity"));

        navHome.setOnClickListener(v -> navigateTo(activity, ProfileTeacherActivity.class));
        navSecond.setOnClickListener(v -> navigateTo(activity, TeacherDashboardActivity.class));
        navThird.setOnClickListener(v -> navigateTo(activity, TeacherViewStudentAttendanceActivity.class));
        navFourth.setOnClickListener(v -> navigateTo(activity, TeacherQRActivity.class));
    }

    private static void setNavigationItem(Activity activity, LinearLayout navItem,
                                          int iconRes, String label, boolean isActive) {
        if (navItem == null) return;

        // Find icon and text by iterating through children
        ImageView icon = null;
        TextView text = null;

        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof ImageView && icon == null) {
                icon = (ImageView) child;
            } else if (child instanceof TextView && text == null) {
                text = (TextView) child;
            }
        }

        if (icon != null) {
            icon.setImageResource(iconRes);
        }
        if (text != null) {
            text.setText(label);
            text.setVisibility(isActive ? View.VISIBLE : View.GONE);
        }

        if (isActive) {
            navItem.setBackgroundResource(R.drawable.nav_button_active);
        } else {
            navItem.setBackgroundResource(R.drawable.nav_button_inactive);
        }
    }

    private static void navigateTo(Activity currentActivity, Class<?> targetActivity) {
        if (!currentActivity.getClass().equals(targetActivity)) {
            Intent intent = new Intent(currentActivity, targetActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            currentActivity.startActivity(intent);
        }
    }

    public static String getUserRole(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE);
        return prefs.getString("user_role", "student");
    }
}