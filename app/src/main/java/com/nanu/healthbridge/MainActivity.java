package com.nanu.healthbridge;

import android.Manifest;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.nanu.healthbridge.ui.DashboardFragment;
import com.nanu.healthbridge.ui.VitalsFragment;
import com.nanu.healthbridge.ui.SettingsFragment;
import com.nanu.healthbridge.ui.SleepFragment;
import com.nanu.healthbridge.ui.WorkoutFragment;
import com.nanu.healthbridge.ui.views.CustomBottomNavView;
import com.topstep.fitcloud.sdk.v2.FcConnector;
import com.topstep.wearkit.base.connector.ConnectorState;

public class MainActivity extends AppCompatActivity {
    private static final String BAND_MAC = "2B:D6:78:56:CC:AA";

    private FcConnector connector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Onboarding Check
        boolean onboardingDone = getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("onboarding_done", false);
        if (!onboardingDone) {
            startActivity(new android.content.Intent(this, com.nanu.healthbridge.ui.OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Request permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.POST_NOTIFICATIONS
        }, 100);

        connector = HealthBridgeApp.getFcSDK().getConnector();

        // Setup bottom navigation
        CustomBottomNavView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(id -> {
            bottomNav.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Fragment fragment = null;
            if (id == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (id == R.id.nav_sleep) {
                fragment = new SleepFragment();
            } else if (id == R.id.nav_recovery) {
                fragment = new VitalsFragment();
            } else if (id == R.id.nav_workout) {
                fragment = new WorkoutFragment();
            } else if (id == R.id.nav_settings) {
                fragment = new SettingsFragment();
            }
            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }
        });

        // Start with dashboard
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
        }

        // Initialize Day Cycle
        DayManager.getInstance().scheduleMidnightReset(this);
        DayManager.getInstance().getOrCreateToday(
                com.nanu.healthbridge.db.HealthDatabase.getInstance(this).healthDayDao(),
                day -> {}
        );

        // Auto-connect to watch on startup
        if (connector.getConnectorState() != ConnectorState.CONNECTED) {
            connector.connect(BAND_MAC, "healthbridge_user", false, true, 25, 170f, 70f);
        }

        // AUTO-START 24/7 MONITORING (Non-negotiable)
        android.content.Intent monitorIntent = new android.content.Intent(this, HealthMonitorService.class);
        monitorIntent.setAction(HealthMonitorService.ACTION_START);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(monitorIntent);
        } else {
            startService(monitorIntent);
        }
    }
}
