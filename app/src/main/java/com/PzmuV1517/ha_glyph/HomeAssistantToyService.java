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
    private Bitmap errorSprite;

    private SpriteLoader spriteLoader;

    private boolean initialized = false;
    private int consecutiveErrors = 0;
    private static final int MAX_CONSECUTIVE_ERRORS_BEFORE_ERROR_SPRITE = 3;
    private final Runnable reconnectRunnable = new Runnable() {
        @Override public void run() {
            if (mGM != null && mCallback != null) {
                try {
                    Log.d(TAG, "Attempting GlyphMatrixManager re-init");
                    mGM.init(mCallback);
                } catch (Exception e) {
                    Log.e(TAG, "Re-init failed, scheduling retry", e);
                    mainHandler.postDelayed(this, 5000);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Ensure still initialized if system restarted service
        if (!initialized) {
            init();
        }
        return START_STICKY; // request restart after being killed
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bound");
        if (!initialized) {
            init();
        }
        return serviceMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Service unbound");
        // Do NOT cleanup fully; keep monitoring so glyph continues while app UI gone
        return true; // allow onRebind
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "Service rebound");
    }

    private void init() {
        if (initialized) return;
        Log.d(TAG, "Initializing HomeAssistant Glyph Toy");

        prefsManager = new PreferencesManager(this);
        apiClient = new HomeAssistantApiClient();
        mainHandler = new Handler(Looper.getMainLooper());
        updateHandler = new Handler(Looper.getMainLooper());
        spriteLoader = new SpriteLoader(this);
        consecutiveErrors = 0;

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
                try { mGM.register(Glyph.DEVICE_23112); } catch (Exception e) { Log.e(TAG, "Register failed", e); }
                startDeviceMonitoring();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(TAG, "Glyph Matrix service disconnected, scheduling re-init");
                stopDeviceMonitoring();
                mainHandler.removeCallbacks(reconnectRunnable);
                mainHandler.postDelayed(reconnectRunnable, 3000);
            }
        };
        try { mGM.init(mCallback); } catch (Exception e) { Log.e(TAG, "Initial mGM.init failed", e); mainHandler.postDelayed(reconnectRunnable, 3000); }
        initialized = true;
    }

    private void cleanup() {
        Log.d(TAG, "Cleaning up service");
        stopDeviceMonitoring();
        mainHandler.removeCallbacks(reconnectRunnable);
        if (mGM != null) {
            try { mGM.unInit(); } catch (Exception ignore) {}
            mGM = null;
        }
        mCallback = null;
        initialized = false;
    }

    private void initSpritesFromJson() {
        try {
            // Load your custom sprites from JSON
            onSprite = spriteLoader.loadOnSprite();
            offSprite = spriteLoader.loadOffSprite();
            errorSprite = spriteLoader.loadErrorSprite();
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
        errorSprite = spriteLoader.loadErrorSprite(); // Try to load error sprite even if others fail
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

        // Set up periodic updates every 2 seconds for real-time monitoring
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDeviceState();
                updateHandler.postDelayed(this, 2000); // 2 seconds
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
                    consecutiveErrors = 0; // reset error streak
                    boolean newState = entity.isOn();
                    if (newState != isDeviceOn) {
                        isDeviceOn = newState;
                        Log.d(TAG, "Device state changed to: " + (isDeviceOn ? "ON" : "OFF"));
                        displayCurrentState();
                    } else if (onSprite != null && offSprite != null) {
                        // Periodically re-push frame to guard against glyph clearing
                        displayCurrentState();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Transient device state fetch error: " + error);
                mainHandler.post(() -> {
                    consecutiveErrors++;
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS_BEFORE_ERROR_SPRITE) {
                        displayErrorState();
                    } else {
                        // Keep last known state visible instead of flashing error
                        displayCurrentState();
                    }
                });
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
            // Use the error sprite from HA-err.json instead of creating shapes
            Bitmap spriteToShow = errorSprite != null ? errorSprite : spriteLoader.loadErrorSprite();

            if (spriteToShow == null) {
                // Fallback to a simple error pattern if sprite loading fails
                spriteToShow = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(spriteToShow);
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                canvas.drawRect(0, 0, 25, 25, paint);
            }

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
            Log.d(TAG, "Displayed error state using HA-err.json sprite");
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
