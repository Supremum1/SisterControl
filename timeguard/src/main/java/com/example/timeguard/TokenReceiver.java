package com.example.timeguard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TokenReceiver extends BroadcastReceiver {
    private static final String TAG = "TG_TOKEN";
    private static final String PREFS = "session";
    private static final String KEY_TOKEN = "token";
    private static final String EXTRA_TOKEN = "token";
    private static final String ACTION = "com.example.timeguard.SET_TOKEN";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) {
            return;
        }
        String token = intent.getStringExtra(EXTRA_TOKEN);
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "Empty token ignored");
            return;
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .apply();

        Log.d(TAG, "Token saved to prefs(session/token)");
    }
}
