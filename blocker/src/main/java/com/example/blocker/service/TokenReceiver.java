package com.example.blocker.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TokenReceiver extends BroadcastReceiver {
    public static final String ACTION_SET_TOKEN = "com.example.blocker.SET_TOKEN";
    public static final String EXTRA_TOKEN = "token";

    private static final String PREFS = "session";
    private static final String KEY_TOKEN = "token";
    private static final String TAG = "Blocker";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_SET_TOKEN.equals(intent.getAction())) {
            return;
        }

        String token = intent.getStringExtra(EXTRA_TOKEN);
        if (token == null || token.trim().isEmpty()) {
            Log.w(TAG, "TokenReceiver: empty token, ignored");
            return;
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .apply();

        Log.d(TAG, "TokenReceiver: token saved to Blocker prefs (" + PREFS + "/" + KEY_TOKEN + ")");
    }
}
