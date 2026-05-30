package com.example.parentapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentSettingsActivity extends AppCompatActivity {
    private SessionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_settings);

        store = new SessionStore(this);

        Button logoutButton = findViewById(R.id.btnLogout);
        Button deleteAccountButton = findViewById(R.id.btnDeleteAccount);

        logoutButton.setOnClickListener(view -> logoutToMain());
        deleteAccountButton.setOnClickListener(view -> confirmDeleteAccount());
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
                        ParentSettingsActivity.this,
                        "Не удалось удалить на сервере: " + throwable.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
                logoutToMain();
            }
        });
    }

    private void logoutToMain() {
        store.clearSession();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
