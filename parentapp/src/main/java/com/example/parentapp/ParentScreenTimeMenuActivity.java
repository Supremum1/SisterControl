package com.example.parentapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ParentScreenTimeMenuActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_screen_time_menu);

        Button manageButton = findViewById(R.id.btnScreenTimeManage);
        Button statsButton = findViewById(R.id.btnScreenTimeStats);

        manageButton.setOnClickListener(view -> startActivity(new Intent(this, ScreenTimeActivity.class)));
        statsButton.setOnClickListener(view -> startActivity(new Intent(this, ScreenTimeStatsActivity.class)));
    }
}
