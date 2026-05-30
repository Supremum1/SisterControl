package com.example.childapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChildHomeActivity extends AppCompatActivity {
    private static final String TAG = "ChildHome";
    private static final long POLL_MS = 2500L;

    private final ApiService api = ApiService.create();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ConstraintSet constraintsOriginal = new ConstraintSet();
    private final ConstraintSet constraintsToTarget = new ConstraintSet();
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            pollCommands();
            handler.postDelayed(this, POLL_MS);
        }
    };

    private SessionStore store;
    private boolean inFlight;
    private ConstraintLayout root;
    private ImageButton btnSettings;
    private ImageView mascot;
    private ImageView mascotSettings;
    private boolean animating;
    private String childDisplayName;

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> playExitAnimation());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_home);

        store = new SessionStore(this);
        TextView tvHello = findViewById(R.id.tvChildHello);
        TextView tvParent = findViewById(R.id.tvParentEmail);

        String token = store.getToken();
        if (token == null) {
            return;
        }
        String auth = "Bearer " + token;

        api.pairStatus(auth).enqueue(new Callback<PairStatusResponse>() {
            @Override
            public void onResponse(Call<PairStatusResponse> call, Response<PairStatusResponse> response) {
                PairStatusResponse body = response.body();
                boolean connected = response.isSuccessful() && body != null && body.getConnected();
                if (!connected) {
                    startActivity(new Intent(ChildHomeActivity.this, CodeActivity.class));
                    finish();
                }
            }

            @Override
            public void onFailure(Call<PairStatusResponse> call, Throwable throwable) {
                startActivity(new Intent(ChildHomeActivity.this, CodeActivity.class));
                finish();
            }
        });

        String name = store.getDisplayName();
        String fallback = store.getUserEmail() == null ? "друг" : store.getUserEmail();
        childDisplayName = name == null ? fallback : name;
        tvHello.setText("Привет, " + childDisplayName + "!");

        api.childConnectionInfo(auth).enqueue(new Callback<ChildConnectionInfo>() {
            @Override
            public void onResponse(Call<ChildConnectionInfo> call, Response<ChildConnectionInfo> response) {
                ChildConnectionInfo body = response.body();
                if (response.isSuccessful() && body != null && body.getParentEmail() != null) {
                    tvParent.setText("Родитель: " + body.getParentEmail());
                } else {
                    tvParent.setText("Родитель: пока нет подключения");
                }
            }

            @Override
            public void onFailure(Call<ChildConnectionInfo> call, Throwable throwable) {
                tvParent.setText("Родитель: ошибка запроса");
            }
        });

        Button screenTimeButton = findViewById(R.id.btnOpenScreenTimeMenu);
        Button assistantButton = findViewById(R.id.btnOpenAssistant);
        Button trustLetterButton = findViewById(R.id.btnTrustLetter);
        Button parentMessagesButton = findViewById(R.id.btnParentMessages);

        screenTimeButton.setOnClickListener(view -> startActivity(new Intent(this, ChildScreenTimeStatusActivity.class)));
        assistantButton.setOnClickListener(view -> openAssistant());
        trustLetterButton.setOnClickListener(view -> openTrustLetterDialog());
        parentMessagesButton.setOnClickListener(view -> startActivity(new Intent(this, ChildParentMessagesActivity.class)));

        root = findViewById(R.id.constraintLayout);
        btnSettings = findViewById(R.id.btnSettings);
        mascot = findViewById(R.id.ivMascot);
        mascotSettings = findViewById(R.id.ivMascotSettings);

        mascot.setAlpha(1f);
        mascotSettings.setVisibility(View.GONE);
        mascotSettings.setAlpha(0f);

        constraintsOriginal.clone(root);
        buildTargetConstraintSet();
        btnSettings.setOnClickListener(view -> {
            if (!animating) {
                playEnterAnimation();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(pollRunnable);
    }

    @Override
    protected void onStop() {
        handler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    private void openAssistant() {
        String token = store.getToken();
        if (token == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setClassName(this, "com.example.childassistant.ui.AssistantChatActivity");
        intent.putExtra("token", token);
        intent.putExtra("childName", childDisplayName);
        startActivity(intent);
    }

    private void pollCommands() {
        if (inFlight) {
            return;
        }
        String token = store.getToken();
        if (token == null) {
            return;
        }
        inFlight = true;

        api.getCommands("Bearer " + token, 10).enqueue(new Callback<CommandsResponse>() {
            @Override
            public void onResponse(Call<CommandsResponse> call, Response<CommandsResponse> response) {
                inFlight = false;
                if (!response.isSuccessful()) {
                    return;
                }

                CommandsResponse body = response.body();
                List<CommandItem> commands = body == null ? List.of() : body.getCommands();
                if (commands.isEmpty()) {
                    return;
                }

                for (CommandItem command : commands) {
                    try {
                        handleCommand(command);
                    } catch (Exception e) {
                        Log.e(TAG, "handleCommand failed: " + command.getCommandType(), e);
                    }

                    api.commandDone("Bearer " + token, command.getId()).enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable throwable) {
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<CommandsResponse> call, Throwable throwable) {
                inFlight = false;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void handleCommand(CommandItem command) {
        switch (command.getCommandType()) {
            case "REQUEST_GPS" -> startActivity(new Intent(this, GpsPermissionActivity.class));
            case "SET_SAFE_ZONE" -> {
                Object payload = command.getPayload();
                if (!(payload instanceof Map<?, ?> payloadMap)) {
                    Log.w(TAG, "SAFE_ZONE payload invalid: " + payload);
                    return;
                }
                Double lat = numberToDouble(payloadMap.get("lat"));
                Double lon = numberToDouble(payloadMap.get("lon"));
                Double radiusM = numberToDouble(payloadMap.get("radiusM"));

                if (lat != null && lon != null && radiusM != null && radiusM > 0) {
                    store.saveSafeZone(lat, lon, radiusM);
                    Log.d(TAG, "SAFE_ZONE saved lat=" + lat + " lon=" + lon + " r=" + radiusM);
                } else {
                    Log.w(TAG, "SAFE_ZONE payload invalid: " + payload);
                }
            }
            default -> {
            }
        }
    }

    private Double numberToDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private void openTrustLetterDialog() {
        String token = store.getToken();
        if (token == null) {
            return;
        }
        String auth = "Bearer " + token;

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 16, 24, 0);

        TextView info = new TextView(this);
        info.setText("Иногда бывает тяжело держать всё в себе. Это письмо --- безопасный способ рассказать родителю о том, что ты чувствуешь. Родители всегда выслушат и поддержат тебя.");
        info.setTextSize(14f);

        EditText input = new EditText(this);
        input.setHint("Напиши своё письмо...");
        input.setMinLines(4);
        input.setMaxLines(10);
        input.setSingleLine(false);
        input.setGravity(Gravity.TOP | Gravity.START);

        container.addView(info);
        container.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Письмо доверия")
                .setView(container)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Отправить", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String text = input.getText() == null ? "" : input.getText().toString().trim();
            if (text.isBlank()) {
                Toast.makeText(this, "Напиши текст письма", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            api.sendTrustLetter(auth, new TrustLetterRequest(text)).enqueue(new Callback<TrustLetterResponse>() {
                @Override
                public void onResponse(Call<TrustLetterResponse> call, Response<TrustLetterResponse> response) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    TrustLetterResponse body = response.body();
                    if (response.isSuccessful() && body != null && body.getOk()) {
                        Toast.makeText(ChildHomeActivity.this, "Отправлено родителю!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(ChildHomeActivity.this, "Не удалось отправить", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TrustLetterResponse> call, Throwable throwable) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(ChildHomeActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void buildTargetConstraintSet() {
        constraintsToTarget.clone(root);
        constraintsToTarget.clear(R.id.btnSettings, ConstraintSet.START);
        constraintsToTarget.clear(R.id.btnSettings, ConstraintSet.END);
        constraintsToTarget.clear(R.id.btnSettings, ConstraintSet.TOP);
        constraintsToTarget.clear(R.id.btnSettings, ConstraintSet.BOTTOM);
        constraintsToTarget.connect(R.id.btnSettings, ConstraintSet.START, R.id.settingsTarget, ConstraintSet.START);
        constraintsToTarget.connect(R.id.btnSettings, ConstraintSet.END, R.id.settingsTarget, ConstraintSet.END);
        constraintsToTarget.connect(R.id.btnSettings, ConstraintSet.TOP, R.id.settingsTarget, ConstraintSet.TOP);
        constraintsToTarget.connect(R.id.btnSettings, ConstraintSet.BOTTOM, R.id.settingsTarget, ConstraintSet.BOTTOM);
    }

    private void playEnterAnimation() {
        animating = true;
        btnSettings.setEnabled(false);

        ChangeBounds move = new ChangeBounds();
        move.setDuration(380);
        move.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                mascot.setVisibility(View.GONE);
                mascotSettings.setVisibility(View.VISIBLE);
                mascotSettings.setAlpha(1f);
                root.postDelayed(
                        () -> settingsLauncher.launch(new Intent(ChildHomeActivity.this, ChildSettingsActivity.class)),
                        700
                );
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });

        TransitionManager.beginDelayedTransition(root, move);
        constraintsToTarget.applyTo(root);
    }

    private void playExitAnimation() {
        mascot.setVisibility(View.VISIBLE);
        mascot.setAlpha(1f);
        mascotSettings.setVisibility(View.GONE);
        mascotSettings.setAlpha(0f);

        ChangeBounds move = new ChangeBounds();
        move.setDuration(320);
        TransitionManager.beginDelayedTransition(root, move);
        constraintsOriginal.applyTo(root);

        btnSettings.setEnabled(true);
        animating = false;
    }
}
