package com.example.childapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChildSettingsActivity extends AppCompatActivity {
    private SessionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_settings);

        store = new SessionStore(this);

        Button blurButton = findViewById(R.id.btnBlur);
        Button blockButton = findViewById(R.id.btnBlock);
        Button enableTimeGuardButton = findViewById(R.id.btnEnableTimeGuard);
        Button logoutButton = findViewById(R.id.btnLogout);
        Button deleteAccountButton = findViewById(R.id.btnDeleteAccount);

        blurButton.setOnClickListener(view -> startActivity(new Intent(this, com.example.overlaydetector.MainActivity.class)));
        blockButton.setOnClickListener(view -> startActivity(new Intent(this, com.example.blocker.ui.MainActivity.class)));
        enableTimeGuardButton.setOnClickListener(view -> {
            pushTokenToTimeGuard();
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        logoutButton.setOnClickListener(view -> logoutToMain());
        deleteAccountButton.setOnClickListener(view -> confirmDeleteAccount());
    }

    private void logoutToMain() {
        store.clearSession();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void pushTokenToTimeGuard() {
        String token = store.getToken();
        if (token == null) {
            return;
        }
        Intent intent = new Intent("com.example.timeguard.SET_TOKEN");
        intent.setPackage("com.example.timeguard");
        intent.putExtra("token", token);
        try {
            sendBroadcast(intent);
        } catch (Exception ignored) {
        }
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Удалить учётную запись?")
                .setMessage("Это действие нельзя отменить. Аккаунт и данные будут удалены с сервера.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (dialog, which) -> deleteAccount())
                .show();
    }

    private void deleteAccount() {
        String token = store.getToken();
        if (token == null || token.isBlank()) {
            logoutToMain();
            return;
        }

        ApiService.create().deleteMe("Bearer " + token).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                logoutToMain();
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable throwable) {
                Toast.makeText(
                        ChildSettingsActivity.this,
                        "Не удалось удалить на сервере: " + throwable.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
                logoutToMain();
            }
        });
    }
}
