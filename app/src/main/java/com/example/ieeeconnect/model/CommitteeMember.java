package com.example.ieeeconnect.model;

import androidx.annotation.Nullable;

/**
 * Represents a committee member fetched from Firestore.
 * Expected Firestore collection: "committee_members"
 */
public class CommitteeMember {
    private String uid;
    private String name;
    private String designation;
    private String department;
    private String email;
    private String phone;
    private String photoUrl;
    private String role;         // CHAIRMAN, VICE_CHAIRMAN, SECRETARY, TREASURER, MEMBER
    private String committee;    // Executive, Academic, Finance, Event, Technical, etc.
    private int sortOrder;       // for ordering within a committee

    public CommitteeMember() { }

    public CommitteeMember(String uid, String name, String designation, String department,
                           String email, String phone, String photoUrl, String role,
                           String committee, int sortOrder) {
        this.uid = uid;
        this.name = name;
        this.designation = designation;
        this.department = department;
        this.email = email;
        this.phone = phone;
        this.photoUrl = photoUrl;
        this.role = role;
        this.committee = committee;
        this.sortOrder = sortOrder;
    }

    // ── Getters & Setters ──────────────────────────────────────────

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCommittee() { return committee; }
    public void setCommittee(String committee) { this.committee = committee; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    /**
     * Returns a user-friendly display name for the role.
     */
    public String getRoleDisplayName() {
        if (role == null) return "Member";
        switch (role.toUpperCase()) {
            case "CHAIRMAN":       return "Chairman";
            case "VICE_CHAIRMAN":  return "Vice Chairman";
            case "SECRETARY":      return "Secretary";
            case "TREASURER":      return "Treasurer";
            case "JOINT_SECRETARY":return "Joint Secretary";
            default:               return "Member";
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof CommitteeMember)) return false;
        CommitteeMember other = (CommitteeMember) obj;
        return uid != null && uid.equals(other.uid);
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }
}

