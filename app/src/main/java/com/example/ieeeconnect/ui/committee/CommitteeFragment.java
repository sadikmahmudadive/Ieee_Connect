package com.example.ieeeconnect.ui.committee;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ieeeconnect.R;
import com.example.ieeeconnect.model.CommitteeMember;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class CommitteeFragment extends Fragment {

    private CommitteeViewModel viewModel;
    private CommitteeMemberAdapter adapter;

    private RecyclerView rvCommitteeMembers;
    private ChipGroup chipGroupFilter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout emptyStateLayout;
    private EditText etSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_committee, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CommitteeViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupSwipeRefresh();
        observeViewModel();
    }

    private void initViews(View view) {
        rvCommitteeMembers = view.findViewById(R.id.rvCommitteeMembers);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        etSearch = view.findViewById(R.id.etSearch);
    }

    private void setupRecyclerView() {
        adapter = new CommitteeMemberAdapter(new CommitteeMemberAdapter.OnMemberActionListener() {
            @Override
            public void onCallClick(CommitteeMember member) {
                makePhoneCall(member);
            }

            @Override
            public void onEmailClick(CommitteeMember member) {
                sendEmail(member);
            }

            @Override
            public void onItemClick(CommitteeMember member) {
                showMemberDetails(member);
            }
        });

        int spanCount = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spanCount);
        rvCommitteeMembers.setLayoutManager(layoutManager);
        rvCommitteeMembers.setAdapter(adapter);
        rvCommitteeMembers.setHasFixedSize(false);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.secondary,
                R.color.nav_icon_active
        );
        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void observeViewModel() {
        // Categories → build chips
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::buildFilterChips);

        // Filtered members
        viewModel.getFilteredMembers().observe(getViewLifecycleOwner(), members -> {
            adapter.submitList(members);
            boolean isEmpty = members == null || members.isEmpty();
            emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvCommitteeMembers.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        // Loading
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            swipeRefreshLayout.setRefreshing(false);
            if (loading != null && loading) {
                shimmerLayout.setVisibility(View.VISIBLE);
                shimmerLayout.startShimmer();
                rvCommitteeMembers.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.GONE);
            } else {
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
            }
        });

        // Errors
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && getView() != null) {
                Snackbar.make(getView(), error, Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> viewModel.refresh())
                        .show();
            }
        });
    }

    private void buildFilterChips(List<String> cats) {
        chipGroupFilter.removeAllViews();
        String selected = viewModel.getSelectedCategory().getValue();
        if (selected == null) selected = "All";

        for (String cat : cats) {
            Chip chip = new Chip(requireContext());
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColorResource(R.color.surfaceVariant);
            chip.setTextColor(getResources().getColor(R.color.onSurface, null));
            chip.setChecked(cat.equals(selected));

            if (chip.isChecked()) {
                chip.setChipBackgroundColorResource(R.color.primary);
                chip.setTextColor(getResources().getColor(R.color.white, null));
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    viewModel.setSelectedCategory(cat);
                    // Update chip colors
                    for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
                        Chip c = (Chip) chipGroupFilter.getChildAt(i);
                        if (c.isChecked()) {
                            c.setChipBackgroundColorResource(R.color.primary);
                            c.setTextColor(getResources().getColor(R.color.white, null));
                        } else {
                            c.setChipBackgroundColorResource(R.color.surfaceVariant);
                            c.setTextColor(getResources().getColor(R.color.onSurface, null));
                        }
                    }
                }
            });

            chipGroupFilter.addView(chip);
        }
    }

    private void makePhoneCall(CommitteeMember member) {
        String phone = member.getPhone();
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to make call", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail(CommitteeMember member) {
        String email = member.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(requireContext(), "Email not available", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "IEEE BUBT SB - Committee Inquiry");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMemberDetails(CommitteeMember member) {
        String message = "Designation: " + nullSafe(member.getDesignation()) + "\n"
                + "Department: " + nullSafe(member.getDepartment()) + "\n"
                + "Committee: " + nullSafe(member.getCommittee()) + "\n"
                + "Role: " + member.getRoleDisplayName() + "\n\n"
                + "\uD83D\uDCE7 " + nullSafe(member.getEmail()) + "\n"
                + "\uD83D\uDCDE " + nullSafe(member.getPhone());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(member.getName())
                .setMessage(message)
                .setPositiveButton("Call", (d, w) -> makePhoneCall(member))
                .setNeutralButton("Email", (d, w) -> sendEmail(member))
                .setNegativeButton("Close", null)
                .show();
    }

    private String nullSafe(String s) {
        return s != null ? s : "N/A";
    }
}
