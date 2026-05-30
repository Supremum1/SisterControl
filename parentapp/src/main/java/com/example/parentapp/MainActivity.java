package com.example.parentapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            showTypingMascot();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private SessionStore store;
    private ImageView ivMascot;
    private ImageView ivMascotTyping;
    private Runnable idleRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        store = new SessionStore(this);
        ivMascot = findViewById(R.id.ivMascot);
        ivMascotTyping = findViewById(R.id.ivMascotTyping);
        ivMascotTyping.setVisibility(View.GONE);

        String savedToken = store.getToken();
        if (savedToken != null && !savedToken.isEmpty()) {
            navigateAfterAuth();
            return;
        }

        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnPair = findViewById(R.id.btnPair);

        email.addTextChangedListener(watcher);
        password.addTextChangedListener(watcher);

        btnRegister.setOnClickListener(view -> register(email.getText().toString(), password.getText().toString()));
        btnLogin.setOnClickListener(view -> login(email.getText().toString(), password.getText().toString()));
        btnPair.setOnClickListener(view -> {
            String token = store.getToken();
            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "Сначала войдите", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, PairActivity.class));
        });
    }

    private void register(String email, String password) {
        RegisterRequest body = new RegisterRequest(email, password, "parent");
        
        //ApiService.java
        //@POST("api/register")
        //Call<AuthResponse> register(@Body RegisterRequest body);
        api.register(body).
        // отправить запрос без блокировки UI-потока; вызвать сallback по окончании вызова
        enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                AuthResponse authResponse = response.body();
                if (!response.isSuccessful() || authResponse == null) {
                    Toast.makeText(MainActivity.this, "Register failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                store.saveToken(authResponse.getToken());
                store.saveUser(authResponse.getUser().getEmail());
                askNameAfterRegister(false);
                Toast.makeText(MainActivity.this, "Registered", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void login(String email, String password) {
        LoginRequest body = new LoginRequest(email, password);
        api.login(body).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                AuthResponse authResponse = response.body();
                if (!response.isSuccessful() || authResponse == null) {
                    Toast.makeText(MainActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                store.saveToken(authResponse.getToken());
                store.saveUser(authResponse.getUser().getEmail());
                navigateAfterAuth();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateAfterAuth() {
        String token = store.getToken();
        if (token == null) {
            return;
        }

        api.getConnections("Bearer " + token).enqueue(new Callback<ParentConnectionsResponse>() {
            @Override
            public void onResponse(Call<ParentConnectionsResponse> call, Response<ParentConnectionsResponse> response) {
                ParentConnectionsResponse body = response.body();
                String child = body == null ? null : body.getChildEmail();
                if (response.isSuccessful() && child != null && !child.isEmpty()) {
                    startActivity(new Intent(MainActivity.this, ParentHomeActivity.class));
                    EventPollService.start(MainActivity.this);
                } else {
                    startActivity(new Intent(MainActivity.this, PairActivity.class));
                }
                finish();
            }

            @Override
            public void onFailure(Call<ParentConnectionsResponse> call, Throwable throwable) {
                startActivity(new Intent(MainActivity.this, PairActivity.class));
                finish();
            }
        });
    }

    private void askNameAfterRegister(boolean isChild) {
        String displayName = store.getDisplayName();
        if (displayName != null && !displayName.isBlank()) {
            return;
        }

        EditText input = new EditText(this);
        input.setHint(isChild ? "Например: Артём" : "Например: Екатерина");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle(isChild ? "Как тебя зовут?" : "Как Вас зовут?")
                .setMessage("Так будет удобнее обращаться к вам в приложении.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (!name.isBlank()) {
                        store.saveDisplayName(name);
                        Toast.makeText(this, "Имя сохранено", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show();
                        askNameAfterRegister(isChild);
                    }
                })
                .show();
    }

    private void showTypingMascot() {
        ivMascot.setVisibility(View.GONE);
        ivMascotTyping.setVisibility(View.VISIBLE);

        if (idleRunnable != null) {
            uiHandler.removeCallbacks(idleRunnable);
        }
        idleRunnable = () -> {
            ivMascotTyping.setVisibility(View.GONE);
            ivMascot.setVisibility(View.VISIBLE);
        };
        uiHandler.postDelayed(idleRunnable, 1000L);
    }
}
