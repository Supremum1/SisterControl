package com.example.parentapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessagesActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private SessionStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        store = new SessionStore(this);

        Button childMessagesButton = findViewById(R.id.btnChildMessages);
        Button sendToChildButton = findViewById(R.id.btnSendToChild);

        childMessagesButton.setOnClickListener(view -> startActivity(new Intent(this, ParentTrustLettersActivity.class)));
        sendToChildButton.setOnClickListener(view -> openMessageToChildDialog());
    }

    private void openMessageToChildDialog() {
        String token = store.getToken();
        if (token == null) {
            return;
        }
        String auth = "Bearer " + token;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 16, 24, 0);

        TextView info = new TextView(this);
        info.setText("Сообщение ребёнку должно быть коротким и понятным. Можно попросить о делах по дому, поддержать или напомнить о важном.");
        info.setTextSize(14f);

        EditText input = new EditText(this);
        input.setHint("Например: Вынеси мусор, пожалуйста");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(4);
        input.setMaxLines(10);
        input.setGravity(Gravity.TOP | Gravity.START);

        container.addView(info);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Сообщение ребёнку")
                .setView(container)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Отправить", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String text = input.getText() == null ? "" : input.getText().toString().trim();
            if (text.isBlank()) {
                Toast.makeText(this, "Введите текст сообщения", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            api.sendMessageToChild(auth, new ParentMessageToChildRequest(text))
                    .enqueue(new Callback<ParentMessageToChildResponse>() {
                        @Override
                        public void onResponse(Call<ParentMessageToChildResponse> call, Response<ParentMessageToChildResponse> response) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            ParentMessageToChildResponse body = response.body();
                            if (response.isSuccessful() && body != null && body.getOk()) {
                                Toast.makeText(MessagesActivity.this, "Отправлено!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(MessagesActivity.this, "Не удалось отправить", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ParentMessageToChildResponse> call, Throwable throwable) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            Toast.makeText(MessagesActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
