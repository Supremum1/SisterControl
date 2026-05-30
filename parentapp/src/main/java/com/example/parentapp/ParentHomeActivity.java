package com.example.parentapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParentHomeActivity extends AppCompatActivity {
    private final ApiService api = ApiService.create();
    private final ConstraintSet constraintsOriginal = new ConstraintSet();
    private final ConstraintSet constraintsToTarget = new ConstraintSet();

    private SessionStore store;
    private ConstraintLayout root;
    private ImageButton btnSettings;
    private ImageView mascot;
    private ImageView mascotSettings;
    private boolean animating;

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> playExitAnimation());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_home);

        store = new SessionStore(this);

        TextView tvHello = findViewById(R.id.tvParentHello);
        TextView tvChild = findViewById(R.id.tvConnectedChild);
        Button btnControl = findViewById(R.id.btnControl);
        Button btnGps = findViewById(R.id.btnGps);
        Button btnScreenTime = findViewById(R.id.btnScreenTime);
        Button btnMessages = findViewById(R.id.btnMessages);

        String token = store.getToken();
        if (token == null) {
            return;
        }

        String name = store.getDisplayName();
        String fallback = store.getUserEmail() == null ? "друг" : store.getUserEmail();
        tvHello.setText("Привет, " + (name == null ? fallback : name) + "!");

        api.getConnections("Bearer " + token).enqueue(new Callback<ParentConnectionsResponse>() {
            @Override
            public void onResponse(Call<ParentConnectionsResponse> call, Response<ParentConnectionsResponse> response) {
                ParentConnectionsResponse body = response.body();
                if (response.isSuccessful() && body != null && body.getChildEmail() != null) {
                    tvChild.setText("Ребёнок: " + body.getChildEmail());
                } else {
                    tvChild.setText("Ребёнок пока не подключен");
                }
            }

            @Override
            public void onFailure(Call<ParentConnectionsResponse> call, Throwable throwable) {
                tvChild.setText("Ребёнок: ошибка запроса");
            }
        });

        btnControl.setOnClickListener(view -> startActivity(new Intent(this, ControlLogsActivity.class)));
        btnGps.setOnClickListener(view -> startActivity(new Intent(this, GpsTrackingActivity.class)));
        btnScreenTime.setOnClickListener(view -> startActivity(new Intent(this, ParentScreenTimeMenuActivity.class)));
        btnMessages.setOnClickListener(view -> startActivity(new Intent(this, MessagesActivity.class)));

        root = findViewById(R.id.constraintLayout);
        btnSettings = root.findViewById(R.id.btnParentSettings);
        mascot = root.findViewById(R.id.ivMascot);
        mascotSettings = root.findViewById(R.id.ivMascotSettings);

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

    private void buildTargetConstraintSet() {
        constraintsToTarget.clone(root);
        constraintsToTarget.clear(R.id.btnParentSettings, ConstraintSet.START);
        constraintsToTarget.clear(R.id.btnParentSettings, ConstraintSet.END);
        constraintsToTarget.clear(R.id.btnParentSettings, ConstraintSet.TOP);
        constraintsToTarget.clear(R.id.btnParentSettings, ConstraintSet.BOTTOM);
        constraintsToTarget.connect(R.id.btnParentSettings, ConstraintSet.START, R.id.settingsTarget, ConstraintSet.START);
        constraintsToTarget.connect(R.id.btnParentSettings, ConstraintSet.END, R.id.settingsTarget, ConstraintSet.END);
        constraintsToTarget.connect(R.id.btnParentSettings, ConstraintSet.TOP, R.id.settingsTarget, ConstraintSet.TOP);
        constraintsToTarget.connect(R.id.btnParentSettings, ConstraintSet.BOTTOM, R.id.settingsTarget, ConstraintSet.BOTTOM);
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
                        () -> settingsLauncher.launch(new Intent(ParentHomeActivity.this, ParentSettingsActivity.class)),
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
