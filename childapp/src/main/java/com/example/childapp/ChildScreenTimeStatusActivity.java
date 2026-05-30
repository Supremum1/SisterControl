package com.example.childapp;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.timeguard.service.AppTimeGuardService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChildScreenTimeStatusActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ChildScreenTimeAdapter adapter;
    private TextView tvHint;
    private Button btnOpenAccessibility;
    private Button btnRefresh;
    private SharedPreferences prefs;

    private final BroadcastReceiver policyUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AppTimeGuardService.ACTION_POLICY_UPDATED.equals(intent.getAction())) {
                render();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_screen_time_status);

        prefs = getSharedPreferences(AppTimeGuardService.PREFS, MODE_PRIVATE);
        recyclerView = findViewById(R.id.rvApps);
        tvHint = findViewById(R.id.tvHint);
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility);
        btnRefresh = findViewById(R.id.btnRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChildScreenTimeAdapter(List.of());
        recyclerView.setAdapter(adapter);

        btnOpenAccessibility.setOnClickListener(view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        btnRefresh.setOnClickListener(view -> render());
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AppTimeGuardService.ACTION_POLICY_UPDATED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(policyUpdatedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(policyUpdatedReceiver, filter);
        }
        render();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(policyUpdatedReceiver);
        super.onStop();
    }

    private boolean isTimeGuardEnabled() {
        String expected = getPackageName() + "/" + AppTimeGuardService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabledServices == null) {
            return false;
        }

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            String componentName = splitter.next();
            if (componentName.equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    private void render() {
        boolean enabled = isTimeGuardEnabled();
        tvHint.setText(enabled
                ? "Экранное время сегодня (обновляется автоматически после изменения политики родителем)."
                : "Включи сервис TimeGuard в Accessibility, чтобы считать экранное время.");

        String raw = prefs.getString(AppTimeGuardService.KEY_POLICY_JSON, null);
        if (raw == null || raw.isBlank()) {
            adapter.submit(List.of());
            return;
        }

        List<ChildScreenTimeItem> items = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(raw);
            JSONArray rules = json.optJSONArray("rules");
            if (rules == null) {
                rules = new JSONArray();
            }

            for (int i = 0; i < rules.length(); i++) {
                JSONObject rule = rules.getJSONObject(i);
                String packageName = rule.optString("packageName", "").trim();
                int limitMin = rule.optInt("limitMin", 0);
                if (packageName.isBlank() || limitMin <= 0) {
                    continue;
                }

                int usedSec = prefs.getInt(AppTimeGuardService.KEY_APP_SEC_PREFIX + packageName, 0);
                items.add(new ChildScreenTimeItem(packageName, usedSec, limitMin));
            }
        } catch (Exception e) {
            adapter.submit(List.of());
            return;
        }

        items.sort(Comparator.comparing(ChildScreenTimeItem::getPackageName));
        adapter.submit(items);
    }

    public static class ChildScreenTimeItem {
        private final String packageName;
        private final int usedSec;
        private final int limitMin;

        public ChildScreenTimeItem(String packageName, int usedSec, int limitMin) {
            this.packageName = packageName;
            this.usedSec = usedSec;
            this.limitMin = limitMin;
        }

        public String getPackageName() {
            return packageName;
        }

        public int getUsedSec() {
            return usedSec;
        }

        public int getLimitMin() {
            return limitMin;
        }
    }

    public static class ChildScreenTimeAdapter extends RecyclerView.Adapter<ChildScreenTimeAdapter.ViewHolder> {
        private List<ChildScreenTimeItem> items;

        public ChildScreenTimeAdapter(List<ChildScreenTimeItem> items) {
            this.items = items;
        }

        public void submit(List<ChildScreenTimeItem> newItems) {
            items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_screen_time, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChildScreenTimeItem item = items.get(position);
            int usedMin = item.getUsedSec() / 60;
            int limitMin = Math.max(1, item.getLimitMin());

            holder.tvApp.setText(item.getPackageName());
            holder.tvTime.setText("Использовано: " + usedMin + " мин из " + limitMin + " мин");

            int progress = Math.min(1000, (int) (item.getUsedSec() * 1000L / (limitMin * 60L)));
            holder.progressBar.setMax(1000);
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
}
