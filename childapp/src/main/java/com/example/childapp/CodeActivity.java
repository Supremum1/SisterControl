package com.example.childapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CodeActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SessionStore store;
    private Runnable poll;
    private TextView tvCode;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);

        store = new SessionStore(this);
        tvCode = findViewById(R.id.tvCode);
        tvStatus = findViewById(R.id.tvStatus);

        String token = store.getToken();
        if (token == null) {
            Toast.makeText(this, "No token", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String auth = "Bearer " + token;
        api.createCode(auth).enqueue(new Callback<CreateCodeResponse>() {
            @Override
            public void onResponse(Call<CreateCodeResponse> call, Response<CreateCodeResponse> response) {
                CreateCodeResponse body = response.body();
                if (response.isSuccessful() && body != null) {
                    tvCode.setText("Pair code: " + body.getCode());
                    startPolling(auth);
                } else {
                    Toast.makeText(CodeActivity.this, "Create code failed", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<CreateCodeResponse> call, Throwable throwable) {
                Toast.makeText(CodeActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (poll != null) {
            handler.removeCallbacks(poll);
        }
    }

    private void startPolling(String auth) {
        poll = new Runnable() {
            @Override
            public void run() {
                api.pairStatus(auth).enqueue(new Callback<PairStatusResponse>() {
                    @Override
                    public void onResponse(Call<PairStatusResponse> call, Response<PairStatusResponse> response) {
                        PairStatusResponse body = response.body();
                        if (response.isSuccessful() && body != null && body.getConnected()) {
                            handler.removeCallbacks(poll);
                            askNameIfMissingThenGoNext();
                        } else {
                            tvStatus.setText("Waiting for parent...");
                            handler.postDelayed(poll, 2000);
                        }
                    }

                    @Override
                    public void onFailure(Call<PairStatusResponse> call, Throwable throwable) {
                        handler.postDelayed(poll, 2000);
                    }
                });
            }
        };
        handler.post(poll);
    }

    private void askNameIfMissingThenGoNext() {
        String existing = store.getDisplayName();
        if (existing != null && !existing.isBlank()) {
            goNextAfterPair();
            return;
        }

        EditText input = new EditText(this);
        input.setHint("Например: Артём");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Как тебя зовут?")
                .setMessage("Так будет удобнее обращаться к тебе в приложении.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (!name.isBlank()) {
                        store.saveDisplayName(name);
                        goNextAfterPair();
                    } else {
                        Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show();
                        askNameIfMissingThenGoNext();
                    }
                })
                .show();
    }

    private void goNextAfterPair() {
        Class<?> next = store.isPermissionsOnboardingDone()
                ? ChildHomeActivity.class
                : ChildPermissionsOnboardingActivity.class;
        startActivity(new Intent(this, next));
        finish();
    }
}
