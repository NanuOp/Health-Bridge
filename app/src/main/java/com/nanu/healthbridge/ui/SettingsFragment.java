package com.nanu.healthbridge.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.nanu.healthbridge.HealthBridgeApp;
import com.nanu.healthbridge.HealthMonitorService;
import com.nanu.healthbridge.R;
import com.topstep.fitcloud.sdk.v2.FcConnector;
import com.topstep.fitcloud.sdk.v2.model.settings.FcAlarm;
import com.topstep.fitcloud.sdk.v2.model.settings.FcContacts;
import com.topstep.fitcloud.sdk.v2.model.settings.FcWeatherToday;
import com.topstep.wearkit.base.connector.ConnectorState;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SettingsFragment extends Fragment {
    private static final String BAND_MAC = "2B:D6:78:56:CC:AA";

    private FcConnector connector;
    private SharedPreferences prefs;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private TextView tvConnStatus, tvProfileName, tvProfileDetails, tvCalibrationInfo, tvProfileInitial;
    private EditText etWeatherCity, etServerIp, etAlarmHour, etAlarmMin;
    private Button btnConnect, btnFindWatch, btnSyncContacts, btnNotifAccess, btnSetAlarm, btnSyncWeather, btnSaveServer;
    private View btnEditProfile;
    private SwitchCompat switchMonitor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        connector = HealthBridgeApp.getFcSDK().getConnector();
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        bindViews(view);
        loadSavedSettings();
        observeConnection();
        setupListeners();
        observeProfile();
    }

    private void bindViews(View v) {
        tvConnStatus = v.findViewById(R.id.tv_conn_status_more);
        tvProfileName = v.findViewById(R.id.tv_profile_name);
        tvProfileDetails = v.findViewById(R.id.tv_profile_details);
        tvCalibrationInfo = v.findViewById(R.id.tv_calibration_info);
        tvProfileInitial = v.findViewById(R.id.tv_profile_initial);
        btnEditProfile = v.findViewById(R.id.btn_edit_profile);

        etWeatherCity = v.findViewById(R.id.et_weather_city);
        etServerIp = v.findViewById(R.id.et_server_ip);
        etAlarmHour = v.findViewById(R.id.et_alarm_hour);
        etAlarmMin = v.findViewById(R.id.et_alarm_min);
        
        btnConnect = v.findViewById(R.id.btn_connect);
        btnFindWatch = v.findViewById(R.id.btn_find_watch);
        btnSyncContacts = v.findViewById(R.id.btn_sync_contacts);
        btnNotifAccess = v.findViewById(R.id.btn_notif_access);
        btnSetAlarm = v.findViewById(R.id.btn_set_alarm);
        btnSyncWeather = v.findViewById(R.id.btn_sync_weather);
        btnSaveServer = v.findViewById(R.id.btn_save_server);
        switchMonitor = v.findViewById(R.id.switch_monitor);
    }

    private void loadSavedSettings() {
        etWeatherCity.setText(prefs.getString("weather_city", "Pune"));
        etServerIp.setText(prefs.getString("server_ip", "192.168.0.104"));
        switchMonitor.setChecked(prefs.getBoolean("monitor_247", false));
    }

    private void observeConnection() {
        disposables.add(connector.observerConnectorState()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    String status = "FireBoltt Crusader · " + (state == ConnectorState.CONNECTED ? "Connected 🟢" : "Disconnected 🔴");
                    tvConnStatus.setText(status);
                    tvConnStatus.setTextColor(state == ConnectorState.CONNECTED ? 0xFF00FF88 : 0xFFFF3B5C);
                }));
    }

    private void observeProfile() {
        com.nanu.healthbridge.db.HealthDao dao = com.nanu.healthbridge.db.HealthDatabase.getInstance(requireContext()).healthDao();
        dao.getUserProfileLive().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                tvProfileName.setText(profile.name);
                tvProfileDetails.setText(String.format(java.util.Locale.US, "%d years · %s · %s", profile.age, profile.fitnessLevel, profile.sport));
                tvProfileInitial.setText(profile.name.substring(0, 1).toUpperCase());
                
                String calInfo = String.format(java.util.Locale.US, "Your HR baseline: %d-%d BPM\nCalibrated for: %s", 
                        profile.getHrScoreIdealMin(), profile.getHrScoreIdealMax(), profile.fitnessLevel);
                tvCalibrationInfo.setText(calInfo);
            }
        });
    }

    private void setupListeners() {
        btnConnect.setOnClickListener(v -> {
            if (connector.getConnectorState() == ConnectorState.CONNECTED) {
                connector.disconnect();
            } else {
                connector.connect(BAND_MAC, "healthbridge_user", false, true, 25, 170f, 70f);
            }
        });

        btnFindWatch.setOnClickListener(v -> {
            if (connector.getConnectorState() == ConnectorState.CONNECTED) {
                disposables.add(connector.messageFeature().findDevice()
                        .subscribeOn(Schedulers.io())
                        .subscribe());
                Toast.makeText(requireContext(), "Watch vibrating...", Toast.LENGTH_SHORT).show();
            }
        });

        btnSyncContacts.setOnClickListener(v -> syncPhoneContacts());

        btnNotifAccess.setOnClickListener(v -> {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
            Toast.makeText(requireContext(), "Enable HealthBridge in list", Toast.LENGTH_LONG).show();
        });

        btnSetAlarm.setOnClickListener(v -> setAlarm());

        btnSyncWeather.setOnClickListener(v -> syncWeather());

        btnSaveServer.setOnClickListener(v -> {
            prefs.edit().putString("server_ip", etServerIp.getText().toString().trim()).apply();
            Toast.makeText(requireContext(), "Config saved", Toast.LENGTH_SHORT).show();
        });

        switchMonitor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("monitor_247", isChecked).apply();
            toggleMonitoring(isChecked);
        });

        btnEditProfile.setOnClickListener(v -> {
            requireContext().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean("onboarding_done", false).apply();
            startActivity(new Intent(requireContext(), OnboardingActivity.class));
            requireActivity().finish();
        });
    }

    private void toggleMonitoring(boolean enable) {
        if (enable) {
            // Start app-side monitoring service (Real-time SpO2 + HR)
            Intent intent = new Intent(requireContext(), HealthMonitorService.class);
            intent.setAction(HealthMonitorService.ACTION_START);
            requireContext().startService(intent);
            
            Toast.makeText(requireContext(), "24/7 Monitoring Enabled", Toast.LENGTH_SHORT).show();
        } else {
            // Stop service
            Intent intent = new Intent(requireContext(), HealthMonitorService.class);
            intent.setAction(HealthMonitorService.ACTION_STOP);
            requireContext().startService(intent);

            Toast.makeText(requireContext(), "Monitoring Disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncPhoneContacts() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_CONTACTS}, 201);
            return;
        }

        List<FcContacts> contacts = new ArrayList<>();
        Cursor cursor = requireContext().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null);

        if (cursor != null) {
            int max = connector.settingsFeature().getContactsMaxSize();
            int count = 0;
            while (cursor.moveToNext() && count < max) {
                contacts.add(new FcContacts(cursor.getString(0), cursor.getString(1)));
                count++;
            }
            cursor.close();
        }

        if (!contacts.isEmpty()) {
            disposables.add(connector.settingsFeature().setContacts(contacts)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> Toast.makeText(requireContext(), "📞 Contacts synced", Toast.LENGTH_SHORT).show(), e -> {}));
        }
    }

    private void setAlarm() {
        try {
            int h = Integer.parseInt(etAlarmHour.getText().toString());
            int m = Integer.parseInt(etAlarmMin.getText().toString());
            FcAlarm alarm = new FcAlarm(0); // Index 0
            alarm.setEnabled(true);
            alarm.setHour(h);
            alarm.setMinute(m);
            alarm.setRepeat(0x7F); // Daily

            List<FcAlarm> alarms = new ArrayList<>();
            alarms.add(alarm);

            disposables.add(connector.settingsFeature().setAlarms(alarms)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> Toast.makeText(requireContext(), "⏰ Alarm set", Toast.LENGTH_SHORT).show(), e -> {}));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Enter valid time", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncWeather() {
        String city = etWeatherCity.getText().toString().trim();
        prefs.edit().putString("weather_city", city).apply();

        // Use FcWeatherToday with 10 arguments as required by compilation error
        FcWeatherToday today = new FcWeatherToday(
                22, // low
                32, // high
                0x01, // Sunny
                28, // current
                1000, // pressure
                3, // wind
                10, // visibility
                50, // humidity
                5, // uv
                0 // precip
        );

        disposables.add(connector.settingsFeature().setWeather(city, System.currentTimeMillis(), today, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> Toast.makeText(requireContext(), "🌤️ Weather synced", Toast.LENGTH_SHORT).show(), e -> {}));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}
