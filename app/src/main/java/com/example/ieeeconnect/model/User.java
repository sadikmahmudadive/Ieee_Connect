package com.example.ieeeconnect.model;

import androidx.annotation.Nullable;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private String photoUrl;
    private String bio;

    public User() { }

    public User(String uid, String displayName, String email, String photoUrl, String bio) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.bio = bio;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof User)) return false;
        return ((User) obj).uid != null && ((User) obj).uid.equals(uid);
    }
}

