package org.telegram.messenger.asgram;

import android.util.Base64;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;
import org.telegram.messenger.BuildConfig;

/** Sends an ephemeral Telegram QR token. The review account session stays on the gateway. */
public final class AsgramReviewAccess {
    private AsgramReviewAccess() {}

    public static boolean configured() {
        return !BuildConfig.ASGRAM_REVIEW_ACCESS_URL.isEmpty() && BuildConfig.ASGRAM_REVIEW_TELEGRAM_ID > 0;
    }

    public static boolean validCode(String code) {
        return code != null && code.matches("[A-Za-z0-9_-]{32,64}");
    }

    public static int approve(String code, byte[] token) {
        if (!configured() || !validCode(code) || token == null) return 400;
        HttpsURLConnection connection = null;
        try {
            URI uri = URI.create(BuildConfig.ASGRAM_REVIEW_ACCESS_URL);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null || !"/v1/asgram/reviewer/authorize".equals(uri.getPath())) return 503;
            byte[] body = new JSONObject().put("token", Base64.encodeToString(token,
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING)).toString().getBytes(StandardCharsets.UTF_8);
            connection = (HttpsURLConnection) uri.toURL().openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(12_000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + code);
            connection.setFixedLengthStreamingMode(body.length);
            connection.setDoOutput(true);
            try (java.io.OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            return connection.getResponseCode();
        } catch (Exception unavailable) {
            return 503;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
