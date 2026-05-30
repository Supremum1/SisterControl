package com.example.childapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GpsPermissionActivity extends AppCompatActivity {
    private static final int REQ = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_permission);

        TextView tvInfo = findViewById(R.id.tvInfo);
        tvInfo.setText("Родитель беспокоится о тебе и хочет знать, где ты находишься.Пожалуйста, выдай следующе  разрешение.");

        Button allowButton = findViewById(R.id.btnAllow);
        Button laterButton = findViewById(R.id.btnLater);
        allowButton.setOnClickListener(view -> ensurePermission());
        laterButton.setOnClickListener(view -> finish());
    }

    private boolean hasFineLocation() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void ensurePermission() {
        if (hasFineLocation()) {
            startGpsService();
            Toast.makeText(this, "GPS разрешён. Отправка началась.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        boolean canShowDialog = ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        if (!canShowDialog) {
            openAppSettings();
            Toast.makeText(this, "Открой настройки и включи Разрешение: Местоположение", Toast.LENGTH_LONG).show();
            return;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQ
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ) {
            return;
        }

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }

        if (granted) {
            startGpsService();
            Toast.makeText(this, "GPS разрешён. Отправка началась.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            openAppSettings();
            Toast.makeText(this, "Без разрешения GPS отправка невозможна. Включи в настройках.", Toast.LENGTH_LONG).show();
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void startGpsService() {
        if (!isLocationEnabled()) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            Toast.makeText(this, "Включи геолокацию в настройках, потом вернись.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, LocationUploadService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}
