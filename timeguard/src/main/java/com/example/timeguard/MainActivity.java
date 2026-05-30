package com.example.timeguard;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_timeguard);

        TextView statusView = findViewById(R.id.tvStatus);
        Button accessibilityButton = findViewById(R.id.btnOpenAccessibility);

        statusView.setText("TimeGuard установлен.\nВключи Accessibility-сервис TimeGuard в настройках.");
        accessibilityButton.setOnClickListener(view ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
    }
}
