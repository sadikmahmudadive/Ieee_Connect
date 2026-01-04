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
    private String photoUrl; // Cloudinary URL

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
