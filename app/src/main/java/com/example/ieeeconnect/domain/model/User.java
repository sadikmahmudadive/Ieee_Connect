package com.example.ieeeconnect.domain.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class User {
    @NonNull
    private String id;
    @NonNull
    private String name;
    @NonNull
    private String email;
    @Nullable
    private String bio;
    @Nullable
    private String photoUrl;
    @Nullable
    private String coverUrl;
    @Nullable
    private String username;
    @Nullable
    private String dept;
    @Nullable
    private String phone;
    @Nullable
    private String gender;

    // --- ADMIN FIELDS ---
    private boolean isAdmin; // true if user is admin
    private String role; // e.g., SUPER_ADMIN, ADMIN, EXCOM, MEMBER

    public User(@NonNull String id, @NonNull String name, @NonNull String email, @Nullable String bio, @Nullable String photoUrl) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.bio = bio;
        this.photoUrl = photoUrl;
    }

    public User() {
        // Default constructor for Firebase
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    @Nullable
    public String getBio() {
        return bio;
    }

    public void setBio(@Nullable String bio) {
        this.bio = bio;
    }

    @Nullable
    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(@Nullable String photoUrl) {
        this.photoUrl = photoUrl;
    }

    @Nullable
    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(@Nullable String coverUrl) {
        this.coverUrl = coverUrl;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    @Nullable
    public String getDept() {
        return dept;
    }

    public void setDept(@Nullable String dept) {
        this.dept = dept;
    }

    @Nullable
    public String getPhone() {
        return phone;
    }

    public void setPhone(@Nullable String phone) {
        this.phone = phone;
    }

    @Nullable
    public String getGender() {
        return gender;
    }

    public void setGender(@Nullable String gender) {
        this.gender = gender;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
