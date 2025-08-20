package com.PzmuV1517.ha_glyph.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SpriteLoader {
    private static final String TAG = "SpriteLoader";
    private final Context context;
    private final Gson gson;

    public SpriteLoader(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    public Bitmap loadOnSprite() {
        return loadSpriteFromAssets("sprites/HA-on.json");
    }

    public Bitmap loadOffSprite() {
        return loadSpriteFromAssets("sprites/HA-off.json");
    }

    private Bitmap loadSpriteFromAssets(String fileName) {
        try {
            String jsonString = loadJsonFromAssets(fileName);
            return parseSpriteToBitmap(jsonString);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load sprite from " + fileName, e);
            return createErrorBitmap();
        }
    }

    private String loadJsonFromAssets(String fileName) throws IOException {
        InputStream inputStream = context.getAssets().open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }

        reader.close();
        inputStream.close();
        return stringBuilder.toString();
    }

    private Bitmap parseSpriteToBitmap(String jsonString) {
        JsonObject spriteData = gson.fromJson(jsonString, JsonObject.class);

        // Get dimensions
        JsonObject dimensions = spriteData.getAsJsonObject("dimensions");
        int width = dimensions.get("width").getAsInt();
        int height = dimensions.get("height").getAsInt();

        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Initialize with transparent pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bitmap.setPixel(x, y, Color.TRANSPARENT);
            }
        }

        // Get first frame
        JsonArray frames = spriteData.getAsJsonArray("frames");
        if (frames.size() > 0) {
            JsonObject firstFrame = frames.get(0).getAsJsonObject();
            JsonArray pixels = firstFrame.getAsJsonArray("pixels");

            // Parse pixel data directly from JSON coordinates
            for (JsonElement pixelElement : pixels) {
                JsonObject pixel = pixelElement.getAsJsonObject();
                String index = pixel.get("index").getAsString();
                float opacity = pixel.get("opacity").getAsFloat();

                if (opacity > 0) {
                    // Parse the index which should be in format "row-col"
                    String[] coords = index.split("-");
                    if (coords.length == 2) {
                        try {
                            int row = Integer.parseInt(coords[0]);
                            int col = Integer.parseInt(coords[1]);

                            // Ensure coordinates are within bounds
                            if (row >= 0 && row < height && col >= 0 && col < width) {
                                // Convert opacity to alpha and create white pixel
                                int alpha = Math.round(opacity * 255);
                                int color = Color.argb(alpha, 255, 255, 255);
                                bitmap.setPixel(col, row, color);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid pixel index format: " + index);
                        }
                    }
                }
            }
        }

        return bitmap;
    }

    private Bitmap createErrorBitmap() {
        // Create a simple red X pattern for error cases
        Bitmap bitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < 25; i++) {
            for (int j = 0; j < 25; j++) {
                if (i == j || i == 24 - j) {
                    bitmap.setPixel(i, j, Color.RED);
                } else {
                    bitmap.setPixel(i, j, Color.TRANSPARENT);
                }
            }
        }

        return bitmap;
    }
}
