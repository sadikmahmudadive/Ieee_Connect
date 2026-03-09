package com.example.ieeeconnect.domain.model;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

@IgnoreExtraProperties
public class User {
    private String id;
    private String name;
    private String email;
    private String bio;
    private String photoUrl;
    private String coverUrl;
    private String username;
    private String dept;
    private String phone;
    private String gender;
    private boolean isAdmin;
    private String role;
    private String fcmToken;

    @PropertyName("displayName")
    private String displayName;
    
    @PropertyName("profileImageUrl")
    private String profileImageUrl;

    public User() {
        // Default constructor for Firebase
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("name")
    public String getName() {
        if (name != null && !name.isEmpty()) return name;
        if (displayName != null && !displayName.isEmpty()) return displayName;
        return "Member";
    }
    
    @PropertyName("name")
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    @PropertyName("photoUrl")
    public String getPhotoUrl() {
        if (photoUrl != null && !photoUrl.isEmpty()) return photoUrl;
        return profileImageUrl;
    }
    
    @PropertyName("photoUrl")
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDept() { return dept; }
    public void setDept(String dept) { this.dept = dept; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    @PropertyName("displayName")
    public String getDisplayName() { return displayName; }
    @PropertyName("displayName")
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    @PropertyName("profileImageUrl")
    public String getProfileImageUrl() { return profileImageUrl; }
    @PropertyName("profileImageUrl")
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
