package org.telegram.messenger.asgram;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DispatchQueue;
import org.telegram.ui.asgram.AsgramUi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public final class AsgramChannelConfig {
    private static final String URL = "https://asbeks.github.io/asgram/feed-channels.json";
    private static final String CACHE_KEY = "feed_channels_v1";
    private static final int MAX_BYTES = 16384;
    private static final DispatchQueue NETWORK = new DispatchQueue("asgramChannelConfig");

    public static List<AsgramDirectory.Entry> current(Context context) {
        try {
            return parse(AsgramUi.prefs(context).getString(CACHE_KEY, ""));
        } catch (JSONException ignored) {
            return AsgramDirectory.PUBLISHERS;
        }
    }

    public static void refresh(Context context, Consumer<List<AsgramDirectory.Entry>> callback) {
        Context app = context.getApplicationContext();
        NETWORK.postRunnable(() -> {
            try {
                String body = download();
                List<AsgramDirectory.Entry> entries = parse(body);
                AsgramUi.prefs(app).edit().putString(CACHE_KEY, body).apply();
                AndroidUtilities.runOnUIThread(() -> callback.accept(entries));
            } catch (IOException | JSONException ignored) {
                List<AsgramDirectory.Entry> entries = current(app);
                AndroidUtilities.runOnUIThread(() -> callback.accept(entries));
            }
        });
    }

    static List<AsgramDirectory.Entry> parse(String value) throws JSONException {
        JSONArray channels = new JSONObject(value).getJSONArray("channels");
        if (channels.length() > 12) throw new JSONException("Too many feed channels");
        List<AsgramDirectory.Entry> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < channels.length(); index++) {
            JSONObject channel = channels.getJSONObject(index);
            String username = channel.getString("username").toLowerCase(Locale.ROOT);
            String title = channel.getString("title").trim();
            String category = channel.getString("category");
            if (!username.matches("[a-z][a-z0-9_]{3,31}") || !seen.add(username)
                    || title.isEmpty() || title.codePointCount(0, title.length()) > 60
                    || title.codePoints().anyMatch(Character::isISOControl)
                    || !AsgramDirectory.CATEGORIES.contains(category)) {
                throw new JSONException("Invalid feed channel");
            }
            result.add(new AsgramDirectory.Entry(username, title, category));
        }
        return result;
    }

    private static String download() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(4000);
        connection.setReadTimeout(4000);
        connection.setRequestProperty("Cache-Control", "no-cache");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Feed config unavailable");
            try (InputStream stream = connection.getInputStream(); ByteArrayOutputStream body = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                for (int count; (count = stream.read(buffer)) != -1; ) {
                    if (body.size() + count > MAX_BYTES) throw new IOException("Feed config too large");
                    body.write(buffer, 0, count);
                }
                return body.toString("UTF-8");
            }
        } finally {
            connection.disconnect();
        }
    }

    private AsgramChannelConfig() {}
}
