package com.PzmuV1517.ha_glyph;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphMatrixManager;
import com.nothing.ketchum.GlyphMatrixFrame;
import com.nothing.ketchum.GlyphMatrixObject;
import com.nothing.ketchum.GlyphMatrixUtils;
import com.nothing.ketchum.GlyphToy;

import com.PzmuV1517.ha_glyph.api.HomeAssistantApiClient;
import com.PzmuV1517.ha_glyph.model.HomeAssistantEntity;
import com.PzmuV1517.ha_glyph.util.PreferencesManager;
import com.PzmuV1517.ha_glyph.util.SpriteLoader;

public class HomeAssistantToyService extends Service {
    private static final String TAG = "HAGlyphToy";

    private GlyphMatrixManager mGM;
    private GlyphMatrixManager.Callback mCallback;

    private HomeAssistantApiClient apiClient;
    private PreferencesManager prefsManager;
    private Handler mainHandler;
    private Handler updateHandler;
    private Runnable updateRunnable;

    private boolean isDeviceOn = false;
    private String selectedEntityId;

    // Bitmap sprites for on/off states
    private Bitmap onSprite;
    private Bitmap offSprite;

    private SpriteLoader spriteLoader;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");
        init();
        return serviceMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service unbound");
        cleanup();
        return false;
    }

    private void init() {
        Log.d(TAG, "Initializing HomeAssistant Glyph Toy");

        prefsManager = new PreferencesManager(this);
        apiClient = new HomeAssistantApiClient();
        mainHandler = new Handler(Looper.getMainLooper());
        updateHandler = new Handler(Looper.getMainLooper());
        spriteLoader = new SpriteLoader(this);

        // Load configuration
        String url = prefsManager.getHomeAssistantUrl();
        String token = prefsManager.getHomeAssistantToken();
        selectedEntityId = prefsManager.getSelectedEntity();

        if (url != null && token != null) {
            apiClient.configure(url, token);
        }

        // Initialize sprites from JSON files
        initSpritesFromJson();

        mGM = GlyphMatrixManager.getInstance(getApplicationContext());
        mCallback = new GlyphMatrixManager.Callback() {
            @Override
            public void onServiceConnected(ComponentName componentName) {
                Log.d(TAG, "Glyph Matrix service connected");
                mGM.register(Glyph.DEVICE_23112);
                startDeviceMonitoring();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(TAG, "Glyph Matrix service disconnected");
            }
        };
        mGM.init(mCallback);
    }

    private void cleanup() {
        Log.d(TAG, "Cleaning up service");
        stopDeviceMonitoring();
        if (mGM != null) {
            mGM.unInit();
            mGM = null;
        }
        mCallback = null;
    }

    private void initSpritesFromJson() {
        try {
            // Load your custom sprites from JSON
            onSprite = spriteLoader.loadOnSprite();
            offSprite = spriteLoader.loadOffSprite();
            Log.d(TAG, "Successfully loaded custom sprites from JSON");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load custom sprites, using defaults", e);
            // Fall back to default sprites
            initDefaultSprites();
        }
    }

    private void initDefaultSprites() {
        // Create default 25x25 bitmaps for on/off states (fallback)
        onSprite = createDefaultOnSprite();
        offSprite = createDefaultOffSprite();
    }

    private Bitmap createDefaultOnSprite() {
        Bitmap bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setAntiAlias(true);

        // Draw a filled circle for "ON" state
        canvas.drawCircle(12.5f, 12.5f, 10f, paint);
        return bitmap;
    }

    private Bitmap createDefaultOffSprite() {
        Bitmap bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(2f);

        // Draw an X for "OFF" state
        canvas.drawLine(5, 5, 20, 20, paint);
        canvas.drawLine(20, 5, 5, 20, paint);
        return bitmap;
    }

    private void startDeviceMonitoring() {
        if (selectedEntityId == null || !apiClient.isConfigured()) {
            Log.w(TAG, "Cannot start monitoring - not configured or no device selected");
            displayErrorState();
            return;
        }

        // Update immediately
        updateDeviceState();

        // Set up periodic updates every 30 seconds
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDeviceState();
                updateHandler.postDelayed(this, 30000); // 30 seconds
            }
        };
        updateHandler.post(updateRunnable);
    }

    private void stopDeviceMonitoring() {
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    private void updateDeviceState() {
        if (selectedEntityId == null || !apiClient.isConfigured()) {
            return;
        }

        apiClient.getEntityState(selectedEntityId, new HomeAssistantApiClient.ApiCallback<HomeAssistantEntity>() {
            @Override
            public void onSuccess(HomeAssistantEntity entity) {
                mainHandler.post(() -> {
                    boolean newState = entity.isOn();
                    if (newState != isDeviceOn) {
                        isDeviceOn = newState;
                        Log.d(TAG, "Device state changed to: " + (isDeviceOn ? "ON" : "OFF"));
                    }
                    displayCurrentState();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to get device state: " + error);
                mainHandler.post(() -> displayErrorState());
            }
        });
    }

    private void displayCurrentState() {
        if (mGM == null) return;

        try {
            Bitmap spriteToShow = isDeviceOn ? onSprite : offSprite;

            GlyphMatrixObject.Builder objectBuilder = new GlyphMatrixObject.Builder();
            GlyphMatrixObject matrixObject = objectBuilder
                    .setImageSource(spriteToShow)
                    .setPosition(0, 0)
                    .setScale(100)
                    .setBrightness(255)
                    .build();

            GlyphMatrixFrame.Builder frameBuilder = new GlyphMatrixFrame.Builder();
            GlyphMatrixFrame frame = frameBuilder.addTop(matrixObject).build(this);

            mGM.setMatrixFrame(frame.render());
            Log.d(TAG, "Displayed " + (isDeviceOn ? "ON" : "OFF") + " state");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying state", e);
        }
    }

    private void displayErrorState() {
        if (mGM == null) return;

        try {
            // Create a red error pattern
            Bitmap errorBitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(errorBitmap);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            canvas.drawRect(0, 0, 25, 25, paint);

            GlyphMatrixObject.Builder objectBuilder = new GlyphMatrixObject.Builder();
            GlyphMatrixObject matrixObject = objectBuilder
                    .setImageSource(errorBitmap)
                    .setPosition(0, 0)
                    .build();

            GlyphMatrixFrame.Builder frameBuilder = new GlyphMatrixFrame.Builder();
            GlyphMatrixFrame frame = frameBuilder.addTop(matrixObject).build(this);

            mGM.setMatrixFrame(frame.render());
            Log.d(TAG, "Displayed error state");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying error state", e);
        }
    }

    private void toggleDevice() {
        if (selectedEntityId == null || !apiClient.isConfigured()) {
            Log.w(TAG, "Cannot toggle - not configured or no device selected");
            return;
        }

        Log.d(TAG, "Toggling device: " + selectedEntityId);

        apiClient.toggleEntity(selectedEntityId, new HomeAssistantApiClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.d(TAG, "Device toggle successful");
                // Update state after a short delay to allow HA to process
                mainHandler.postDelayed(() -> updateDeviceState(), 1000);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to toggle device: " + error);
            }
        });
    }

    private final Handler serviceHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GlyphToy.MSG_GLYPH_TOY: {
                    Bundle bundle = msg.getData();
                    String event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA);
                    Log.d(TAG, "Received event: " + event);

                    if (GlyphToy.EVENT_CHANGE.equals(event)) {
                        // Long press - toggle the device
                        toggleDevice();
                    } else if (GlyphToy.EVENT_AOD.equals(event)) {
                        // AOD update - refresh device state
                        updateDeviceState();
                    }
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private final Messenger serviceMessenger = new Messenger(serviceHandler);

    // Method to update sprites from your JSON data
    public void updateSprites(Bitmap newOnSprite, Bitmap newOffSprite) {
        if (newOnSprite != null) {
            this.onSprite = newOnSprite;
        }
        if (newOffSprite != null) {
            this.offSprite = newOffSprite;
        }
        // Refresh display with new sprites
        displayCurrentState();
    }
}
