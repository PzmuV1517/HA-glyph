package com.PzmuV1517.ha_glyph.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "ha_glyph_prefs";
    private static final String KEY_HA_URL = "ha_url";
    private static final String KEY_HA_TOKEN = "ha_token";
    private static final String KEY_SELECTED_ENTITY = "selected_entity";
    private static final String KEY_SELECTED_ENTITY_NAME = "selected_entity_name";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setHomeAssistantUrl(String url) {
        prefs.edit().putString(KEY_HA_URL, url).apply();
    }

    public String getHomeAssistantUrl() {
        return prefs.getString(KEY_HA_URL, null);
    }

    public void setHomeAssistantToken(String token) {
        prefs.edit().putString(KEY_HA_TOKEN, token).apply();
    }

    public String getHomeAssistantToken() {
        return prefs.getString(KEY_HA_TOKEN, null);
    }

    public void setSelectedEntity(String entityId, String entityName) {
        prefs.edit()
                .putString(KEY_SELECTED_ENTITY, entityId)
                .putString(KEY_SELECTED_ENTITY_NAME, entityName)
                .apply();
    }

    public String getSelectedEntity() {
        return prefs.getString(KEY_SELECTED_ENTITY, null);
    }

    public String getSelectedEntityName() {
        return prefs.getString(KEY_SELECTED_ENTITY_NAME, null);
    }

    public boolean isConfigured() {
        return getHomeAssistantUrl() != null && getHomeAssistantToken() != null;
    }

    public boolean hasSelectedEntity() {
        return getSelectedEntity() != null;
    }

    public void clearConfiguration() {
        prefs.edit().clear().apply();
    }
}
