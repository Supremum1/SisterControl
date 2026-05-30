package com.example.childapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChildParentMessagesActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private SessionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_parent_messages);

        store = new SessionStore(this);
        TextView tvParentMessages = findViewById(R.id.tvParentMsgs);

        String token = store.getToken();
        if (token == null || token.isBlank()) {
            tvParentMessages.setText("Нет токена");
            return;
        }

        api.getParentMessages("Bearer " + token, 50).enqueue(new Callback<ParentMessagesResponse>() {
            @Override
            public void onResponse(Call<ParentMessagesResponse> call, Response<ParentMessagesResponse> response) {
                if (!response.isSuccessful()) {
                    tvParentMessages.setText("Ошибка загрузки: " + response.code());
                    return;
                }

                ParentMessagesResponse body = response.body();
                List<ParentMsgItem> messages = body == null ? List.of() : body.getMessages();
                if (messages.isEmpty()) {
                    tvParentMessages.setText("Пока нет сообщений от родителя.");
                    return;
                }

                StringBuilder text = new StringBuilder();
                for (ParentMsgItem message : messages) {
                    text.append("• ").append(message.getCreatedAt()).append("\n");
                    text.append(message.getMessage()).append("\n\n");
                }
                tvParentMessages.setText(text.toString());
            }

            @Override
            public void onFailure(Call<ParentMessagesResponse> call, Throwable throwable) {
                tvParentMessages.setText("Нет связи с сервером");
            }
        });
    }
}
