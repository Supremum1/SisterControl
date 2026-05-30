package com.example.childapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
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
    private static final String BLOCKER_APP_ID = "com.example.blocker";
    private static final String ACTION_SET_TOKEN = "com.example.blocker.SET_TOKEN";
    private static final String EXTRA_TOKEN = "token";
    private static final String TAG = "ChildApp";

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

        String saved = store.getToken();
        if (saved != null && !saved.isBlank()) {
            sendTokenToServices(saved);
            routeAfterPairStatus(saved);
            return;
        }

        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnLogin = findViewById(R.id.btnLogin);

        email.addTextChangedListener(watcher);
        password.addTextChangedListener(watcher);

        btnRegister.setOnClickListener(view -> register(email.getText().toString(), password.getText().toString()));
        btnLogin.setOnClickListener(view -> login(email.getText().toString(), password.getText().toString()));
    }

    @Override
    protected void onStop() {
        if (idleRunnable != null) {
            uiHandler.removeCallbacks(idleRunnable);
        }
        super.onStop();
    }

    private void register(String email, String password) {
        api.register(new RegisterRequest(email, password, "child")).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Register failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                AuthResponse body = response.body();
                String token = body == null ? null : body.getToken();
                if (token != null && !token.isBlank()) {
                    store.saveToken(token);
                    sendTokenToServices(token);
                    askNameAfterRegister(true);

                    if (!store.isPermissionsOnboardingDone()) {
                        startActivity(new Intent(MainActivity.this, ChildPermissionsOnboardingActivity.class));
                        finish();
                    }
                }
                Toast.makeText(MainActivity.this, "Registered", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void login(String email, String password) {
        api.login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                AuthResponse authResponse = response.body();
                if (!response.isSuccessful() || authResponse == null) {
                    Toast.makeText(MainActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                String token = authResponse.getToken();
                User user = authResponse.getUser();
                if (token == null || token.isBlank() || user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                    Toast.makeText(MainActivity.this, "Login: bad response", Toast.LENGTH_SHORT).show();
                    return;
                }

                store.saveToken(token);
                store.saveUser(user.getEmail());
                sendTokenToServices(token);
                routeAfterPairStatus(token);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void routeAfterPairStatus(String token) {
        api.pairStatus("Bearer " + token).enqueue(new Callback<PairStatusResponse>() {
            @Override
            public void onResponse(Call<PairStatusResponse> call, Response<PairStatusResponse> response) {
                PairStatusResponse pairStatus = response.body();
                boolean connected = response.isSuccessful() && pairStatus != null && pairStatus.getConnected();
                startActivity(new Intent(MainActivity.this, connected ? ChildHomeActivity.class : CodeActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<PairStatusResponse> call, Throwable throwable) {
                startActivity(new Intent(MainActivity.this, CodeActivity.class));
                finish();
            }
        });
    }

    private void sendTokenToServices(String token) {
        sendTokenToBlocker(token);
        sendTokenToBlur(token);
        sendTokenToTimeGuard(token);
    }

    private void sendTokenToBlocker(String token) {
        try {
            Intent intent = new Intent(ACTION_SET_TOKEN);
            intent.setPackage(BLOCKER_APP_ID);
            intent.putExtra(EXTRA_TOKEN, token);
            sendBroadcast(intent);
            Log.d(TAG, "Token broadcast sent to " + BLOCKER_APP_ID);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send token to Blocker", e);
        }
    }

    private void sendTokenToBlur(String token) {
        Intent intent = new Intent("com.example.overlaydetector.SET_TOKEN");
        intent.setPackage("com.example.overlaydetector");
        intent.putExtra("token", token);
        sendBroadcast(intent);
        Log.d(TAG, "Token broadcast sent to Blur");
    }

    private void sendTokenToTimeGuard(String token) {
        Intent intent = new Intent("com.example.timeguard.SET_TOKEN");
        intent.setPackage("com.example.timeguard");
        intent.putExtra("token", token);
        sendBroadcast(intent);
        Log.d(TAG, "Token broadcast sent to TimeGuard");
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
                .setMessage("Так будет удобнее обращаться в приложении.")
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
