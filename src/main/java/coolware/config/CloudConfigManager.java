package coolware.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Online Configs Client:
 * Fetches and downloads configurations from the GitHub repository:
 * https://github.com/cooldood-dev/coolware-configs
 * All files in this repository are treated as configs.
 */
public final class CloudConfigManager {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/cooldood-dev/coolware-configs/contents";
    private static final String GITHUB_RAW_BASE = "https://raw.githubusercontent.com/cooldood-dev/coolware-configs/main/";

    private static final List<String> cachedConfigs = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean fetching = false;
    private static volatile String lastStatus = "Click to refresh";

    public static List<String> getCachedConfigs() {
        return new ArrayList<>(cachedConfigs);
    }

    public static String getLastStatus() {
        return lastStatus;
    }

    public static boolean isFetching() {
        return fetching;
    }

    public static void refreshAsync() {
        if (fetching) return;
        fetching = true;
        lastStatus = "Fetching online configs...";
        new Thread(() -> {
            try {
                List<String> list = fetchCloudConfigList();
                cachedConfigs.clear();
                cachedConfigs.addAll(list);
                lastStatus = "Found " + list.size() + " online configs";
            } catch (Throwable t) {
                lastStatus = "Failed to load online configs";
            } finally {
                fetching = false;
            }
        }, "OnlineConfig-Fetch").start();
    }

    public static List<String> fetchCloudConfigList() throws Exception {
        List<String> result = new ArrayList<>();
        URL url = new URL(GITHUB_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "Coolware-Client");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("HTTP " + code);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(sb.toString());
        if (root.isJsonArray()) {
            JsonArray arr = root.getAsJsonArray();
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    // Check for type == "file" or simply include all files from the repo
                    if (obj.has("name")) {
                        String type = obj.has("type") ? obj.get("type").getAsString() : "file";
                        if ("file".equalsIgnoreCase(type)) {
                            String name = obj.get("name").getAsString();
                            // Exclude common git/repo files if present (e.g. .gitignore, README.md) if not intended,
                            // but all files in this repo are configs per prompt.
                            if (!name.startsWith(".")) {
                                result.add(name);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Downloads config directly from GitHub repo raw URL, writes to local configs folder, and immediately loads it.
     */
    public static boolean downloadAndLoad(String configFileName) {
        try {
            lastStatus = "Downloading " + configFileName + "...";
            String rawUrl = GITHUB_RAW_BASE + configFileName;
            URL url = new URL(rawUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "Coolware-Client");

            if (conn.getResponseCode() != 200) {
                lastStatus = "Error: HTTP " + conn.getResponseCode();
                return false;
            }

            java.io.File target = new java.io.File(ConfigManager.getConfigDir(), configFileName);
            try (java.io.InputStream is = conn.getInputStream();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(target)) {
                byte[] buf = new byte[4096];
                int read;
                while ((read = is.read(buf)) != -1) {
                    fos.write(buf, 0, read);
                }
            }

            boolean loaded = ConfigManager.loadConfig(configFileName);
            if (loaded) {
                lastStatus = "Loaded " + configFileName.replace(".cfg", "");
            } else {
                lastStatus = "Failed to apply config";
            }
            return loaded;
        } catch (Throwable t) {
            lastStatus = "Error: " + t.getMessage();
            return false;
        }
    }
}
