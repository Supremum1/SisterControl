package com.example.parentapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScreenTimeStatsActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private SessionStore store;
    private ScreenTimeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_time_stats);

        store = new SessionStore(this);

        RecyclerView recyclerView = findViewById(R.id.rvApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScreenTimeAdapter(List.of());
        recyclerView.setAdapter(adapter);

        String token = store.getToken();
        if (token == null || token.isBlank()) {
            toast("Нет токена родителя");
            finish();
            return;
        }

        api.getScreenTimeUsage("Bearer " + token, todayKey()).enqueue(new Callback<ScreenTimeUsageResponse>() {
            @Override
            public void onResponse(Call<ScreenTimeUsageResponse> call, Response<ScreenTimeUsageResponse> response) {
                if (!response.isSuccessful()) {
                    toast("Ошибка: " + response.code());
                    return;
                }
                ScreenTimeUsageResponse body = response.body();
                adapter.submit(body == null ? List.of() : body.getApps());
            }

            @Override
            public void onFailure(Call<ScreenTimeUsageResponse> call, Throwable throwable) {
                toast("Сеть: " + throwable.getMessage());
            }
        });
    }

    private String todayKey() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
    }
}
