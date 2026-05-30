package com.example.childassistant.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.childassistant.R;
import com.example.childassistant.config.ServerConfig;
import com.example.childassistant.net.AiApiService;
import com.example.childassistant.net.AiChatRequest;
import com.example.childassistant.net.AiChatResponse;
import com.example.childassistant.net.AiMessage;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AssistantChatActivity extends AppCompatActivity {
    private final String baseUrl = ServerConfig.apiBaseUrl();
    private final List<ChatItem> items = new ArrayList<>();
    private final List<AiMessage> history = new ArrayList<>();

    private AiApiService api;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private Button sendButton;
    private TextView titleView;
    private ChatAdapter adapter;
    private String token = "";
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_chat);

        String intentToken = getIntent().getStringExtra("token");
        token = intentToken == null ? "" : intentToken;
        childName = getIntent().getStringExtra("childName");

        if (token.isBlank()) {
            Toast.makeText(this, "Нет токена. Войдите заново.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        api = AiApiService.create(baseUrl);
        titleView = findViewById(R.id.tvTitle);
        recyclerView = findViewById(R.id.rvChat);
        messageInput = findViewById(R.id.etMessage);
        sendButton = findViewById(R.id.btnSend);

        titleView.setText("AI-помощник");

        adapter = new ChatAdapter(items);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        String hello = "Привет! Я рядом. Расскажи, что случилось, и я постараюсь помочь.";
        adapter.add(new ChatItem(false, hello));
        history.add(new AiMessage("assistant", hello));

        sendButton.setOnClickListener(view -> {
            String text = messageInput.getText() == null
                    ? ""
                    : messageInput.getText().toString().trim();
            if (text.isBlank()) {
                return;
            }
            messageInput.setText("");
            send(text);
        });
    }

    private void send(String userText) {
        adapter.add(new ChatItem(true, userText));
        recyclerView.scrollToPosition(items.size() - 1);

        history.add(new AiMessage("user", userText));
        List<AiMessage> trimmed = history.size() > 20
                ? new ArrayList<>(history.subList(history.size() - 20, history.size()))
                : new ArrayList<>(history);

        AiChatRequest request = new AiChatRequest(childName, trimmed);
        sendButton.setEnabled(false);

        api.chat("Bearer " + token, request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<AiChatResponse> call, @NonNull Response<AiChatResponse> response) {
                sendButton.setEnabled(true);
                if (!response.isSuccessful()) {
                    addAssistantMessage("Похоже, сейчас не получается ответить. Попробуй ещё раз через минуту.");
                    return;
                }

                AiChatResponse body = response.body();
                String answer = body == null || body.getAnswer() == null ? "" : body.getAnswer().trim();
                String safeAnswer = answer.isBlank()
                        ? "Я не смог сформулировать ответ. Напиши, пожалуйста, чуть подробнее."
                        : answer;
                addAssistantMessage(safeAnswer);
                history.add(new AiMessage("assistant", safeAnswer));
            }

            @Override
            public void onFailure(@NonNull Call<AiChatResponse> call, @NonNull Throwable throwable) {
                sendButton.setEnabled(true);
                addAssistantMessage("Нет связи с сервером помощника. Проверь интернет/Wi-Fi.");
            }
        });
    }

    private void addAssistantMessage(String text) {
        adapter.add(new ChatItem(false, text));
        recyclerView.scrollToPosition(items.size() - 1);
    }
}
