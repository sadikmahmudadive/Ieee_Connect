package com.example.ieeeconnect.model;

import androidx.annotation.Nullable;

public class User {
    private String uid;
    private String displayName;
    private String username;
    private String email;
    private String photoUrl;
    private String coverUrl;
    private String bio;
    private String role;
    private String dept;
    private String phone;
    private String gender;

    public User() { }

    public User(String uid, String displayName, String username, String email, String photoUrl, String coverUrl, String bio, String role, String dept, String phone, String gender) {
        this.uid = uid;
        this.displayName = displayName;
        this.username = username;
        this.email = email;
        this.photoUrl = photoUrl;
        this.coverUrl = coverUrl;
        this.bio = bio;
        this.role = role;
        this.dept = dept;
        this.phone = phone;
        this.gender = gender;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDept() { return dept; }
    public void setDept(String dept) { this.dept = dept; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof User)) return false;
        return ((User) obj).uid != null && ((User) obj).uid.equals(uid);
    }
}
