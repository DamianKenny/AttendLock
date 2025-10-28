package com.nibm.attendancetracker.models;

import java.util.ArrayList;
import java.util.List;

public class Teacher {
    private String id;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String employeeId;
    private String phone;
    private String dob;
    private String joinDate;
    private String qualification;
    private String profilePictureUrl;
    private String password;
    private List<String> assignedSubjects;
    private List<String> assignedSubjectNames;

    // Default constructor for Firestore
    public Teacher() {
        this.assignedSubjects = new ArrayList<>();
        this.assignedSubjectNames = new ArrayList<>();
    }

    // Constructor for minimal data (used in AddSubjectsToTeacherActivity)
    public Teacher(String id, String name, String department, String email, String profilePictureUrl) {
        this.id = id;
        this.name = name;
        this.department = department != null ? department : "Unknown";
        this.email = email != null ? email : "No email";
        this.profilePictureUrl = profilePictureUrl != null ? profilePictureUrl : "";
        this.assignedSubjects = new ArrayList<>();
        this.assignedSubjectNames = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getProfilePictureUrl() { return profilePictureUrl; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<String> getAssignedSubjects() { return assignedSubjects; }
    public void setAssignedSubjects(List<String> assignedSubjects) {
        this.assignedSubjects = assignedSubjects != null ? assignedSubjects : new ArrayList<>();
    }

    public List<String> getAssignedSubjectNames() { return assignedSubjectNames; }
    public void setAssignedSubjectNames(List<String> assignedSubjectNames) {
        this.assignedSubjectNames = assignedSubjectNames != null ? assignedSubjectNames : new ArrayList<>();
    }
}
