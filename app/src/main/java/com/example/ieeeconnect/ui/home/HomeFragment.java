package com.example.ieeeconnect.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ieeeconnect.databinding.FragmentHomeBinding;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        List<String> headlines = Arrays.asList("Welcome to IEEE Connect","Upcoming AGM on Friday","Check out new event rooms");
        binding.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recycler.setAdapter(new SimpleStringAdapter(headlines));
    }

    private static class SimpleStringAdapter extends RecyclerView.Adapter<SimpleStringAdapter.VH> {
        private final List<String> items;
        SimpleStringAdapter(List<String> items) { this.items = items; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            int pad = (int) (16 * parent.getResources().getDisplayMetrics().density);
            tv.setPadding(pad, pad, pad, pad);
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
            return new VH(tv);
        }

        @Override public void onBindViewHolder(@NonNull VH holder, int position) { holder.tv.setText(items.get(position)); }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            final TextView tv;
            VH(@NonNull TextView itemView) { super(itemView); this.tv = itemView; }
        }
    }
}
