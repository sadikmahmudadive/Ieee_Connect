package com.example.ieeeconnect.ui.committee;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ieeeconnect.model.CommitteeMember;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CommitteeViewModel extends ViewModel {

    private static final String TAG = "CommitteeViewModel";
    private static final String COLLECTION = "committee_members";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<CommitteeMember>> allMembers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("All");
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MediatorLiveData<List<CommitteeMember>> filteredMembers = new MediatorLiveData<>();
    private final MutableLiveData<List<String>> categories = new MutableLiveData<>(new ArrayList<>());

    public CommitteeViewModel() {
        filteredMembers.addSource(allMembers, m -> applyFilters());
        filteredMembers.addSource(selectedCategory, c -> applyFilters());
        filteredMembers.addSource(searchQuery, q -> applyFilters());
        loadCommitteeMembers();
    }

    public LiveData<List<CommitteeMember>> getFilteredMembers() { return filteredMembers; }
    public LiveData<List<String>> getCategories() { return categories; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSelectedCategory() { return selectedCategory; }

    public void setSelectedCategory(String category) { selectedCategory.setValue(category); }
    public void setSearchQuery(String query) { searchQuery.setValue(query); }
    public void refresh() { loadCommitteeMembers(); }

    public void loadCommitteeMembers() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        firestore.collection(COLLECTION)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<CommitteeMember> members = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        CommitteeMember m = doc.toObject(CommitteeMember.class);
                        if (m != null) {
                            if (m.getUid() == null || m.getUid().isEmpty()) m.setUid(doc.getId());
                            members.add(m);
                        }
                    }
                    Collections.sort(members, (a, b) -> {
                        int pa = getRolePriority(a.getRole()), pb = getRolePriority(b.getRole());
                        if (pa != pb) return pa - pb;
                        return Integer.compare(a.getSortOrder(), b.getSortOrder());
                    });
                    allMembers.setValue(members);
                    extractCategories(members);
                    if (members.isEmpty()) loadSampleData();
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load committee members", e);
                    errorMessage.setValue("Failed to load committee members");
                    isLoading.setValue(false);
                    loadSampleData();
                });
    }

    private void applyFilters() {
        List<CommitteeMember> source = allMembers.getValue();
        if (source == null) { filteredMembers.setValue(new ArrayList<>()); return; }
        String category = selectedCategory.getValue();
        String query = searchQuery.getValue();
        List<CommitteeMember> result = new ArrayList<>();
        for (CommitteeMember m : source) {
            if (category != null && !"All".equals(category)) {
                if (m.getCommittee() == null || !m.getCommittee().equalsIgnoreCase(category)) continue;
            }
            if (query != null && !query.trim().isEmpty()) {
                String q = query.toLowerCase().trim();
                boolean match = (m.getName() != null && m.getName().toLowerCase().contains(q))
                        || (m.getDesignation() != null && m.getDesignation().toLowerCase().contains(q))
                        || (m.getDepartment() != null && m.getDepartment().toLowerCase().contains(q))
                        || m.getRoleDisplayName().toLowerCase().contains(q);
                if (!match) continue;
            }
            result.add(m);
        }
        filteredMembers.setValue(result);
    }

    private void extractCategories(List<CommitteeMember> members) {
        Set<String> cats = new LinkedHashSet<>();
        cats.add("All");
        for (CommitteeMember m : members) {
            if (m.getCommittee() != null && !m.getCommittee().isEmpty()) cats.add(m.getCommittee());
        }
        categories.setValue(new ArrayList<>(cats));
    }

    private int getRolePriority(String role) {
        if (role == null) return 99;
        switch (role.toUpperCase()) {
            case "CHAIRMAN": return 0;
            case "VICE_CHAIRMAN": return 1;
            case "SECRETARY": return 2;
            case "JOINT_SECRETARY": return 3;
            case "TREASURER": return 4;
            default: return 10;
        }
    }

    private void loadSampleData() {
        List<CommitteeMember> s = new ArrayList<>();
        s.add(mm("1","Dr. Ahmed Rahman","Professor","CSE Department","ahmed@ieee.org","+8801711111111","CHAIRMAN","Executive Committee",1));
        s.add(mm("2","Dr. Fatima Begum","Associate Professor","EEE Department","fatima@ieee.org","+8801722222222","VICE_CHAIRMAN","Executive Committee",2));
        s.add(mm("3","Md. Karim Uddin","Assistant Professor","CSE Department","karim@ieee.org","+8801733333333","SECRETARY","Executive Committee",3));
        s.add(mm("4","Ayesha Siddiqua","Lecturer","EEE Department","ayesha@ieee.org","+8801744444444","TREASURER","Executive Committee",4));
        s.add(mm("5","Md. Tanvir Hasan","Student","CSE Department","tanvir@ieee.org","+8801755555555","MEMBER","Executive Committee",5));
        s.add(mm("6","Dr. Nasreen Akter","Professor","CSE Department","nasreen@ieee.org","+8801766666666","CHAIRMAN","Technical Committee",1));
        s.add(mm("7","Md. Rafiq Islam","Lecturer","CSE Department","rafiq@ieee.org","+8801777777777","SECRETARY","Technical Committee",2));
        s.add(mm("8","Sanjida Alam","Student","CSE Department","sanjida@ieee.org","+8801788888888","MEMBER","Technical Committee",3));
        s.add(mm("9","Dr. Shahidul Haque","Associate Professor","BBA Department","shahidul@ieee.org","+8801799999999","CHAIRMAN","Finance Committee",1));
        s.add(mm("10","Tahira Khatun","Assistant Professor","BBA Department","tahira@ieee.org","+8801700000001","TREASURER","Finance Committee",2));
        s.add(mm("11","Sadia Jahan","Lecturer","EEE Department","sadia@ieee.org","+8801700000002","CHAIRMAN","Event Committee",1));
        s.add(mm("12","Md. Zahir Hossain","Student","CSE Department","zahir@ieee.org","+8801700000003","SECRETARY","Event Committee",2));
        s.add(mm("13","Nusrat Jahan","Student","EEE Department","nusrat@ieee.org","+8801700000004","MEMBER","Event Committee",3));
        s.add(mm("14","Dr. Mahbub Alam","Professor","CSE Department","mahbub@ieee.org","+8801700000005","CHAIRMAN","Publicity Committee",1));
        s.add(mm("15","Farhan Ahmed","Student","CSE Department","farhan@ieee.org","+8801700000006","MEMBER","Publicity Committee",2));
        allMembers.setValue(s);
        extractCategories(s);
    }

    private CommitteeMember mm(String uid, String name, String desig, String dept, String email, String phone, String role, String committee, int order) {
        return new CommitteeMember(uid, name, desig, dept, email, phone, "", role, committee, order);
    }
}

