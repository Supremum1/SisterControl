package com.example.parentapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        SessionStore store = new SessionStore(context);
        String token = store.getToken();
        if (token != null && !token.trim().isEmpty()) {
            EventPollService.start(context);
        }
    }
}
