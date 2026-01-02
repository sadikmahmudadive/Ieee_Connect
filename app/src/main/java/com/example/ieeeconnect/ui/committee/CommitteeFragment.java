package com.example.ieeeconnect.ui.committee;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ieeeconnect.R;

public class CommitteeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_placeholder, container, false);
        TextView title = root.findViewById(R.id.title);
        TextView subtitle = root.findViewById(R.id.subtitle);
        title.setText("Committee");
        subtitle.setText("Committee placeholder");
        return root;
    }
}
