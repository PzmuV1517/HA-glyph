package com.PzmuV1517.ha_glyph.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.PzmuV1517.ha_glyph.model.HomeAssistantEntity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeAssistantApiClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final Gson gson;
    private String baseUrl;
    private String accessToken;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public HomeAssistantApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public void configure(String baseUrl, String accessToken) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.accessToken = accessToken;
    }

    public boolean isConfigured() {
        return baseUrl != null && accessToken != null;
    }

    public void testConnection(ApiCallback<Boolean> callback) {
        if (!isConfigured()) {
            callback.onError("Not configured");
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "api/")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Connection failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("HTTP " + response.code() + ": " + response.message());
                }
                response.close();
            }
        });
    }

    public void getStates(ApiCallback<List<HomeAssistantEntity>> callback) {
        if (!isConfigured()) {
            callback.onError("Not configured");
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "api/states")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Failed to get states: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Type listType = new TypeToken<List<HomeAssistantEntity>>(){}.getType();
                    List<HomeAssistantEntity> entities = gson.fromJson(responseBody, listType);
                    callback.onSuccess(entities);
                } else {
                    callback.onError("HTTP " + response.code() + ": " + response.message());
                }
                response.close();
            }
        });
    }

    public void getEntityState(String entityId, ApiCallback<HomeAssistantEntity> callback) {
        if (!isConfigured()) {
            callback.onError("Not configured");
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "api/states/" + entityId)
                .header("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Failed to get entity state: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    HomeAssistantEntity entity = gson.fromJson(responseBody, HomeAssistantEntity.class);
                    callback.onSuccess(entity);
                } else {
                    callback.onError("HTTP " + response.code() + ": " + response.message());
                }
                response.close();
            }
        });
    }

    public void toggleEntity(String entityId, ApiCallback<Boolean> callback) {
        callService("homeassistant", "toggle", entityId, callback);
    }

    public void turnOnEntity(String entityId, ApiCallback<Boolean> callback) {
        callService("homeassistant", "turn_on", entityId, callback);
    }

    public void turnOffEntity(String entityId, ApiCallback<Boolean> callback) {
        callService("homeassistant", "turn_off", entityId, callback);
    }

    private void callService(String domain, String service, String entityId, ApiCallback<Boolean> callback) {
        if (!isConfigured()) {
            callback.onError("Not configured");
            return;
        }

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("entity_id", entityId);

        RequestBody body = RequestBody.create(requestJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "api/services/" + domain + "/" + service)
                .header("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Failed to call service: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess(true);
                } else {
                    callback.onError("HTTP " + response.code() + ": " + response.message());
                }
                response.close();
            }
        });
    }
}
