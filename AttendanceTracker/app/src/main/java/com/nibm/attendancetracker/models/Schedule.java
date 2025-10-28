package com.nibm.attendancetracker.models;

import java.util.List;
import java.util.Map;

public class Schedule {
    private String id;
    private String batch;
    private long createdAt;
    private int credits;
    private String faculty;
    private String programme;
    private List<Map<String, String>> scheduleTimes;
    private String subjectName;
    private int totalClasses;

    // Default constructor (required for Firebase)
    public Schedule() {}

    // Constructor with all fields
    public Schedule(String id, String batch, long createdAt, int credits, String faculty,
                    String programme, List<Map<String, String>> scheduleTimes,
                    String subjectName, int totalClasses) {
        this.id = id;
        this.batch = batch;
        this.createdAt = createdAt;
        this.credits = credits;
        this.faculty = faculty;
        this.programme = programme;
        this.scheduleTimes = scheduleTimes;
        this.subjectName = subjectName;
        this.totalClasses = totalClasses;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getProgramme() { return programme; }
    public void setProgramme(String programme) { this.programme = programme; }

    public List<Map<String, String>> getScheduleTimes() { return scheduleTimes; }
    public void setScheduleTimes(List<Map<String, String>> scheduleTimes) { this.scheduleTimes = scheduleTimes; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public int getTotalClasses() { return totalClasses; }
    public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }

    // Helper method to get formatted schedule info
    public String getScheduleInfo() {
        if (scheduleTimes == null || scheduleTimes.isEmpty()) {
            return "No schedule times";
        }

        StringBuilder info = new StringBuilder();
        for (Map<String, String> time : scheduleTimes) {
            String date = time.get("date");
            String startTime = time.get("startTime");
            String endTime = time.get("endTime");

            if (date != null && startTime != null && endTime != null) {
                info.append(date).append(": ").append(startTime).append("-").append(endTime).append("\n");
            }
        }
        return info.toString();
    }

    // Helper method to get next class date
    public String getNextClassDate() {
        if (scheduleTimes != null && !scheduleTimes.isEmpty()) {
            Map<String, String> firstSchedule = scheduleTimes.get(0);
            return firstSchedule.get("date");
        }
        return "No schedule";
    }

    // Helper method to get first class time
    public String getFirstClassTime() {
        if (scheduleTimes != null && !scheduleTimes.isEmpty()) {
            Map<String, String> firstSchedule = scheduleTimes.get(0);
            String startTime = firstSchedule.get("startTime");
            String endTime = firstSchedule.get("endTime");
            return startTime + "-" + endTime;
        }
        return "No time";
    }
}
