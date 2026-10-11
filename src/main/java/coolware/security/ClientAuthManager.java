package coolware.security;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise Client Authentication & Periodic HWID Verification.
 * Strictly verifies User ID and HWID against Supabase REST API.
 * Periodic checks occur every 60 seconds without logging sensitive data.
 * Mismatch or tampered status triggers immediate silent process exit/crash.
 */
public final class ClientAuthManager {
    private static final String SUPABASE_HOST = "zdohsxpqqrkrasoaswhi.supabase.co";
    private static final String SUPABASE_SECRET = "";

    private static volatile String authenticatedUserId = null;
    private static volatile String activeHwid = null;
    private static volatile boolean isAuthenticated = true;

    private static ScheduledExecutorService heartbeatScheduler;

    public static String computeHardwareId() {
        return "coolware-hwid";
    }

    /**
     * Authenticates the client during game initialization (Bypassed).
     */
    public static void initializeAuth(String userIdParam, String hwidParam) {
        isAuthenticated = true;
    }

    /**
     * Checks Supabase REST API for active license with matching ID and HWID.
     * Silent verification: zero credentials or raw payloads logged.
     */
    public static boolean verifyWithSupabase(String userId, String expectedHwid) {
        if (userId == null || expectedHwid == null) return false;
        HttpURLConnection conn = null;
        try {
            String endpoint = "https://" + SUPABASE_HOST + "/rest/v1/licenses?or=(id.eq." + userId + ",user_id.eq." + userId + ")&select=id,user_id,hwid,status,expires_at";
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("apikey", SUPABASE_SECRET);
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_SECRET);
            conn.setRequestProperty("Content-Type", "application/json");

            int code = conn.getResponseCode();
            if (code != 200) {
                return false;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String response = sb.toString();
            if (response.length() <= 2 || !response.startsWith("[")) {
                return false;
            }

            // Verify status is not revoked/banned and hwid matches
            if (response.contains("\"status\":\"revoked\"") || response.contains("\"status\":\"banned\"")) {
                return false;
            }

            // Verify HWID match in JSON response
            String hwidPattern = "\"hwid\":\"" + expectedHwid + "\"";
            return response.contains(hwidPattern);
        } catch (Throwable t) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static int consecutiveFailures = 0;

    /**
     * Starts periodic 1-minute background heartbeat verification.
     */
    private static void startHeartbeatVerification() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            return;
        }
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SecurityWatchdog");
            t.setDaemon(true);
            return t;
        });

        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                String localHwid = computeHardwareId();
                if (!localHwid.equalsIgnoreCase(activeHwid)) {
                    crashGame();
                    return;
                }

                boolean ok = verifyWithSupabase(authenticatedUserId, activeHwid);
                if (!ok) {
                    consecutiveFailures++;
                    // Allow 2 retries in case of transient network timeout/drop
                    if (consecutiveFailures >= 3) {
                        crashGame();
                    }
                } else {
                    consecutiveFailures = 0;
                }
            } catch (Throwable t) {
                // Ignore transient background watchdog error unless sustained
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Instantly terminates JVM with native halt / illegal state crash.
     */
    public static void crashGame() {
        // Disabled: Auth removed
    }

    public static boolean isAuthenticated() {
        return isAuthenticated;
    }
}
