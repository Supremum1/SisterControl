package com.example.parentapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ScreenTimeAdapter extends RecyclerView.Adapter<ScreenTimeAdapter.ViewHolder> {
    private List<ScreenTimeUsageItem> items;

    public ScreenTimeAdapter(List<ScreenTimeUsageItem> items) {
        this.items = items;
    }

    public void submit(List<ScreenTimeUsageItem> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_screen_time, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScreenTimeUsageItem item = items.get(position);
        int usedMin = item.getUsedSec() / 60;
        int limitMin = Math.max(1, item.getLimitMin());

        holder.tvApp.setText(item.getPackageName());
        holder.tvTime.setText("Использовано: " + usedMin + " мин из " + limitMin + " мин");

        int progress = Math.min(1000, (int) (item.getUsedSec() * 1000L / (limitMin * 60L)));
        holder.progressBar.setProgress(progress);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvApp;
        private final TextView tvTime;
        private final ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvApp = itemView.findViewById(R.id.tvApp);
            tvTime = itemView.findViewById(R.id.tvTime);
            progressBar = itemView.findViewById(R.id.pb);
        }
    }
}
