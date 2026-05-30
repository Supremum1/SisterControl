package com.example.parentapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScreenTimeActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private final List<ScreenTimeRule> rules = new ArrayList<>();
    private SessionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_time);

        store = new SessionStore(this); // this --- конкретный экземпляр экрана, который сейчас создан Android

        EditText etTotal = findViewById(R.id.etTotalLimit);
        EditText etPackage = findViewById(R.id.etPackage);
        EditText etLimit = findViewById(R.id.etLimitMin);
        TextView tvRules = findViewById(R.id.tvRules);
        Button btnAdd = findViewById(R.id.btnAddRule);
        Button btnSave = findViewById(R.id.btnSavePolicy);

        String token = store.getToken();
        if (token == null || token.isBlank()) {
            toast("Нет токена родителя");
            finish();
            return;
        }
        String auth = "Bearer " + token;

        api.getScreenTime(auth).enqueue(new Callback<ScreenTimePolicyResponse>() {
            @Override
            public void onResponse(Call<ScreenTimePolicyResponse> call, Response<ScreenTimePolicyResponse> response) {
                ScreenTimePolicyResponse body = response.body();
                if (body == null) {
                    return;
                }
                etTotal.setText(String.valueOf(body.getTotalLimitMin()));
                rules.clear();
                rules.addAll(body.getRules());
                tvRules.setText(renderRules());
            }

            @Override
            public void onFailure(Call<ScreenTimePolicyResponse> call, Throwable throwable) {
            }
        });

        btnAdd.setOnClickListener(view -> {
            String packageName = etPackage.getText().toString().trim();
            int limit = parseIntOrZero(etLimit.getText().toString().trim());
            if (packageName.isBlank() || limit <= 0) {
                toast("Укажите packageName(название пакета) и лимит по времени в минутах");
                return;
            }

            rules.removeIf(rule -> rule.getPackageName().equals(packageName));
            rules.add(new ScreenTimeRule(packageName, limit));
            tvRules.setText(renderRules());
            etPackage.setText("");
            etLimit.setText("");
        });

        btnSave.setOnClickListener(view -> {
            int total = parseIntOrZero(etTotal.getText().toString().trim());
            ScreenTimePolicyRequest body = new ScreenTimePolicyRequest(Math.max(total, 0), new ArrayList<>(rules));

            api.setScreenTime(auth, body).enqueue(new Callback<BasicResponse>() {
                @Override
                public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                    toast("Сохранено: code=" + response.code());
                }

                @Override
                public void onFailure(Call<BasicResponse> call, Throwable throwable) {
                    toast("Ошибка: " + throwable.getMessage());
                }
            });
        });

        tvRules.setText(renderRules());
    }

    private int parseIntOrZero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String renderRules() {
        if (rules.isEmpty()) {
            return "Правил пока нет.";
        }
        return rules.stream()
                .sorted(Comparator.comparing(ScreenTimeRule::getPackageName))
                .map(rule -> "• " + rule.getPackageName() + " = " + rule.getLimitMin() + " мин/день")
                .collect(Collectors.joining("\n"));
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
