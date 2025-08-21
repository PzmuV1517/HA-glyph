package com.PzmuV1517.ha_glyph;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.PzmuV1517.ha_glyph.api.HomeAssistantApiClient;
import com.PzmuV1517.ha_glyph.model.HomeAssistantEntity;
import com.PzmuV1517.ha_glyph.util.PreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private EditText etUrl, etToken, etSearch; // added etSearch
    private Button btnConnect, btnDisconnect;
    private TextView btnPrivacy;
    private TextView tvStatus, tvSelectedDevice;
    private RecyclerView rvDevices;
    private ProgressBar progressBar;

    private HomeAssistantApiClient apiClient;
    private PreferencesManager prefsManager;
    private DeviceAdapter deviceAdapter;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initData();
        loadSavedConfiguration();
    }

    private void initViews() {
        etUrl = findViewById(R.id.et_url);
        etToken = findViewById(R.id.et_token);
        etSearch = findViewById(R.id.et_search); // init search
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnPrivacy = findViewById(R.id.tv_privacy_link);
        tvStatus = findViewById(R.id.tv_status);
        tvSelectedDevice = findViewById(R.id.tv_selected_device);
        rvDevices = findViewById(R.id.rv_devices);
        progressBar = findViewById(R.id.progress_bar);

        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(new ArrayList<>(), this::onDeviceSelected);
        rvDevices.setAdapter(deviceAdapter);

        btnConnect.setOnClickListener(v -> connectToHomeAssistant());
        btnDisconnect.setOnClickListener(v -> disconnectFromHomeAssistant());
        btnPrivacy.setOnClickListener(v -> startActivity(new Intent(this, PrivacyPolicyActivity.class)));

        // Search filtering
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                deviceAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initData() {
        apiClient = new HomeAssistantApiClient();
        prefsManager = new PreferencesManager(this);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void loadSavedConfiguration() {
        String savedUrl = prefsManager.getHomeAssistantUrl();
        String savedToken = prefsManager.getHomeAssistantToken();
        String selectedEntity = prefsManager.getSelectedEntityName();
        String selectedEntityId = prefsManager.getSelectedEntity(); // id

        if (savedUrl != null) {
            etUrl.setText(savedUrl);
        }
        if (savedToken != null) {
            etToken.setText(savedToken);
        }
        if (selectedEntity != null) {
            tvSelectedDevice.setText("Selected: " + selectedEntity);
            if (selectedEntityId != null) {
                deviceAdapter.setSelectedEntityId(selectedEntityId);
            }
        }

        if (prefsManager.isConfigured()) {
            apiClient.configure(savedUrl, savedToken);
            updateUIState(true);
            loadDevices();
        } else {
            updateUIState(false);
        }
    }

    private void connectToHomeAssistant() {
        String url = etUrl.getText().toString().trim();
        String token = etToken.getText().toString().trim();

        if (url.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "Please enter both URL and token", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add http:// if no protocol specified
        final String finalUrl = url.startsWith("http://") || url.startsWith("https://") ? url : "http://" + url;
        final String finalToken = token;

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Connecting...");

        apiClient.configure(finalUrl, finalToken);

        apiClient.testConnection(new HomeAssistantApiClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    prefsManager.setHomeAssistantUrl(finalUrl);
                    prefsManager.setHomeAssistantToken(finalToken);
                    updateUIState(true);
                    loadDevices();
                    Toast.makeText(MainActivity.this, "Connected successfully!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Connection failed: " + error);
                    Toast.makeText(MainActivity.this, "Connection failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void disconnectFromHomeAssistant() {
        prefsManager.clearConfiguration();
        deviceAdapter.updateDevices(new ArrayList<>());
        updateUIState(false);
        tvSelectedDevice.setText("No device selected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    private void loadDevices() {
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Loading devices...");

        apiClient.getStates(new HomeAssistantApiClient.ApiCallback<List<HomeAssistantEntity>>() {
            @Override
            public void onSuccess(List<HomeAssistantEntity> entities) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);

                    // Filter for controllable entities (lights, switches, etc.)
                    List<HomeAssistantEntity> controllableEntities = entities.stream()
                            .filter(entity -> {
                                String entityId = entity.getEntityId();
                                return entityId.startsWith("light.") ||
                                       entityId.startsWith("switch.") ||
                                       entityId.startsWith("fan.") ||
                                       entityId.startsWith("input_boolean.") ||
                                       entityId.startsWith("automation.");
                            })
                            .collect(Collectors.toList());

                    deviceAdapter.updateDevices(controllableEntities);
                    tvStatus.setText("Found " + controllableEntities.size() + " controllable devices");
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Failed to load devices: " + error);
                    Toast.makeText(MainActivity.this, "Failed to load devices: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void onDeviceSelected(HomeAssistantEntity entity) {
        prefsManager.setSelectedEntity(entity.getEntityId(), entity.getFriendlyName());
        tvSelectedDevice.setText("Selected: " + entity.getFriendlyName());
        deviceAdapter.setSelectedEntityId(entity.getEntityId()); // update highlight
        Toast.makeText(this, "Selected: " + entity.getFriendlyName() + "\nYou can now use the Glyph toy!", Toast.LENGTH_LONG).show();
    }

    private void updateUIState(boolean connected) {
        if (connected) {
            etUrl.setEnabled(false);
            etToken.setEnabled(false);
            btnConnect.setEnabled(false);
            btnDisconnect.setEnabled(true);
            tvStatus.setText("Connected to Home Assistant");
        } else {
            etUrl.setEnabled(true);
            etToken.setEnabled(true);
            btnConnect.setEnabled(true);
            btnDisconnect.setEnabled(false);
            tvStatus.setText("Not connected");
        }
    }
}
