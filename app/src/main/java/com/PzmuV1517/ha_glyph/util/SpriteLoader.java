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
import java.util.HashMap;
import java.util.Map;

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

        // Get shape array (defines the outline of the matrix)
        JsonArray shapeArray = spriteData.getAsJsonArray("shape");
        int[] shape = new int[shapeArray.size()];
        for (int i = 0; i < shapeArray.size(); i++) {
            shape[i] = shapeArray.get(i).getAsInt();
        }

        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Get first frame (assuming static sprites for now)
        JsonArray frames = spriteData.getAsJsonArray("frames");
        if (frames.size() > 0) {
            JsonObject firstFrame = frames.get(0).getAsJsonObject();
            JsonArray pixels = firstFrame.getAsJsonArray("pixels");

            // Create a map to store pixel data
            Map<String, Float> pixelOpacities = new HashMap<>();

            // Parse pixel data
            for (JsonElement pixelElement : pixels) {
                JsonObject pixel = pixelElement.getAsJsonObject();
                String index = pixel.get("index").getAsString();
                float opacity = pixel.get("opacity").getAsFloat();
                pixelOpacities.put(index, opacity);
            }

            // Fill bitmap based on pixel data and shape
            fillBitmapFromSpriteData(bitmap, shape, pixelOpacities, width, height);
        }

        return bitmap;
    }

    private void fillBitmapFromSpriteData(Bitmap bitmap, int[] shape, Map<String, Float> pixelOpacities, int width, int height) {
        // Initialize with transparent pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bitmap.setPixel(x, y, Color.TRANSPARENT);
            }
        }

        // Fill pixels based on sprite data
        for (int row = 0; row < Math.min(shape.length, height); row++) {
            int maxCol = shape[row];
            int startCol = (width - maxCol) / 2; // Center the shape

            for (int col = 0; col < maxCol && (startCol + col) < width; col++) {
                String pixelIndex = row + "-" + col;
                Float opacity = pixelOpacities.get(pixelIndex);

                if (opacity != null && opacity > 0) {
                    // Convert opacity to alpha and create white pixel
                    int alpha = Math.round(opacity * 255);
                    int color = Color.argb(alpha, 255, 255, 255); // White with varying opacity
                    bitmap.setPixel(startCol + col, row, color);
                }
            }
        }
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

    // Method to handle animated sprites (if needed in the future)
    public Bitmap[] loadAnimatedSprite(String fileName) {
        try {
            String jsonString = loadJsonFromAssets(fileName);
            JsonObject spriteData = gson.fromJson(jsonString, JsonObject.class);
            JsonArray frames = spriteData.getAsJsonArray("frames");

            Bitmap[] bitmaps = new Bitmap[frames.size()];

            for (int i = 0; i < frames.size(); i++) {
                // Parse each frame - implementation would be similar to parseSpriteToBitmap
                // but handling individual frames
                bitmaps[i] = parseFrameToBitmap(spriteData, i);
            }

            return bitmaps;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load animated sprite from " + fileName, e);
            return new Bitmap[]{createErrorBitmap()};
        }
    }

    private Bitmap parseFrameToBitmap(JsonObject spriteData, int frameIndex) {
        // Similar to parseSpriteToBitmap but for a specific frame
        // This is a placeholder for future animation support
        return parseSpriteToBitmap(spriteData.toString());
    }
}
