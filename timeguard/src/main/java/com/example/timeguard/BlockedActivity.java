package com.example.timeguard;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import java.util.Locale;

public class BlockedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked);

        TextView blockedView = findViewById(R.id.tvBlocked);
        if (blockedView == null) {
            throw new IllegalStateException("tvBlocked not found in activity_blocked.xml (check layout in :timeguard/res)");
        }

        String reason = getIntent().getStringExtra("reason");
        String packageName = getIntent().getStringExtra("pkg");
        if (reason == null) {
            reason = "Лимит исчерпан";
        }
        if (packageName == null) {
            packageName = "";
        }

        StringBuilder text = new StringBuilder(reason);
        if (!packageName.isBlank()) {
            text.append("\n\nПриложение: ").append(packageName);
        }
        text.append("\n\nРазблокировка: ").append(nextMidnightText());
        blockedView.setText(text.toString());
    }

    private String nextMidnightText() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        return String.format(Locale.US, "%02d.%02d %02d:%02d", day, month, hour, minute);
    }
}
