package com.example.parentapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentTrustLettersActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private SessionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_trust_letters);

        store = new SessionStore(this);

        TextView tvLetters = findViewById(R.id.tvLetters);
        String token = store.getToken();
        if (token == null) {
            tvLetters.setText("Нет токена");
            return;
        }

        api.getTrustLetters("Bearer " + token, 50).enqueue(new Callback<TrustLettersResponse>() {
            @Override
            public void onResponse(Call<TrustLettersResponse> call, Response<TrustLettersResponse> response) {
                if (!response.isSuccessful()) {
                    tvLetters.setText("Ошибка загрузки: " + response.code());
                    return;
                }

                TrustLettersResponse body = response.body();
                List<TrustLetterItem> letters = body == null ? List.of() : body.getLetters();
                if (letters.isEmpty()) {
                    tvLetters.setText("Пока нет сообщений от ребёнка.");
                    return;
                }

                StringBuilder text = new StringBuilder();
                for (TrustLetterItem letter : letters) {
                    text.append("• ").append(letter.getCreatedAt()).append("\n");
                    text.append(letter.getMessage()).append("\n\n");
                }
                tvLetters.setText(text.toString());
            }

            @Override
            public void onFailure(Call<TrustLettersResponse> call, Throwable throwable) {
                tvLetters.setText("Нет связи с сервером");
            }
        });
    }
}
