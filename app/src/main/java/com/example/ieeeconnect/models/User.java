package com.example.ieeeconnect.models;

public class User {
    private String uid;
    private String firstName;
    private String lastName;
    private String email;
    private String profileImageUrl;
    private String bio;
    private String department;
    private String studentId;
    private String ieeeId;
    private String role;
    private int eventsAttendedCount;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String uid, String firstName, String lastName, String email, String profileImageUrl) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.role = "MEMBER"; // Default role
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getIeeeId() {
        return ieeeId;
    }

    public void setIeeeId(String ieeeId) {
        this.ieeeId = ieeeId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getEventsAttendedCount() {
        return eventsAttendedCount;
    }

    public void setEventsAttendedCount(int eventsAttendedCount) {
        this.eventsAttendedCount = eventsAttendedCount;
    }
}
