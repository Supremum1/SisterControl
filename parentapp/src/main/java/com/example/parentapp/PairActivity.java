package com.example.parentapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PairActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private SessionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pair);

        store = new SessionStore(this);

        EditText code = findViewById(R.id.code);
        Button button = findViewById(R.id.btnSubmit);

        button.setOnClickListener(view -> {
            String token = store.getToken();
            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "No token", Toast.LENGTH_SHORT).show();
                return;
            }

            String auth = "Bearer " + token;
            PairRequest body = new PairRequest(code.getText().toString());

            api.pair(auth, body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (!response.isSuccessful()) {
                        Toast.makeText(PairActivity.this, "Pair failed", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(PairActivity.this, "соединение установлено", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(PairActivity.this, ParentHomeActivity.class));
                    finish();
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable throwable) {
                    Toast.makeText(PairActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
