package com.example.blocker.ui;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.blocker.databinding.ActivityBlockedBinding;

public class BlockedActivity extends AppCompatActivity {
    private ActivityBlockedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlockedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonBackToBrowser.setOnClickListener(view -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }
}
