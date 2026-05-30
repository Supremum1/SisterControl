package com.example.overlaydetector;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.overlaydetector.databinding.ActivityBlurBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityBlurBinding binding;
    private MediaProjectionManager mediaProjectionManager;

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ignored -> {
            });

    private final ActivityResultLauncher<Intent> requestProjection =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    toast("Разрешение на захват экрана не выдано");
                    return;
                }

                Intent service = new Intent(this, ScreenCaptureService.class);
                service.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.getResultCode());
                service.putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.getData());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(service);
                } else {
                    startService(service);
                }
                toast("Мониторинг запущен");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityBlurBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        binding.btnRequestPermission.setOnClickListener(view -> requestOverlayPermission());
        binding.btnShowOverlay.setOnClickListener(view -> {
            if (!Settings.canDrawOverlays(this)) {
                toast("Сначала дай разрешение «Показывать поверх других приложений».");
                requestOverlayPermission();
                return;
            }

            if (Build.VERSION.SDK_INT >= 33) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
            requestProjection.launch(mediaProjectionManager.createScreenCaptureIntent());
        });

        binding.btnHideOverlay.setOnClickListener(view -> {
            stopService(new Intent(this, ScreenCaptureService.class));
            toast("Мониторинг остановлен");
        });
    }

    private void requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            toast("Разрешение уже выдано.");
            return;
        }

        try {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            toast("Откройте настройки наложений вручную.");
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
