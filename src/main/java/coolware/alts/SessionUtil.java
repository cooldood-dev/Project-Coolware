package coolware.alts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Session;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class SessionUtil {
    private static final CloseableHttpClient httpClient = HttpClients.custom()
            .disableAuthCaching()
            .disableCookieManagement()
            .disableRedirectHandling()
            .build();

    public static JsonObject getJsonObject(CloseableHttpResponse response) throws IOException {
        String json = EntityUtils.toString(response.getEntity());
        return new JsonParser().parse(json).getAsJsonObject();
    }

    public static Session queryPlayerProfile(String accessToken) {
        try {
            HttpGet req = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
            req.addHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse res = httpClient.execute(req);
            try {
                if (res.getStatusLine().getStatusCode() == 200) {
                    JsonObject obj = getJsonObject(res);
                    if (obj.has("id") && obj.has("name")) {
                        String uuid = obj.get("id").getAsString();
                        String username = obj.get("name").getAsString();
                        return new Session(username, uuid, accessToken, Session.Type.MOJANG.name());
                    }
                }
            } finally {
                res.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Session("Unknown", "Unknown", "", Session.Type.MOJANG.name());
    }

    public static String checkLastNameChange(String accessToken) {
        try {
            HttpGet req = new HttpGet("https://api.minecraftservices.com/minecraft/profile/namechange");
            req.addHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse res = httpClient.execute(req);
            try {
                if (res.getStatusLine().getStatusCode() == 200) {
                    JsonObject obj = getJsonObject(res);
                    if (obj.has("nameChangeAllowed")) {
                        if (obj.get("nameChangeAllowed").getAsBoolean()) {
                            return "ALLOWED";
                        }
                        if (obj.has("changedAt")) return obj.get("changedAt").getAsString();
                        if (obj.has("createdAt")) return obj.get("createdAt").getAsString();
                    }
                }
            } finally {
                res.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public static String checkNameAvailability(String accessToken, String name) {
        if (name.length() > 16) return "Name is too long (>16 chars)";
        if (name.length() < 3) return "Name is too short (<3 chars)";
        try {
            HttpGet req = new HttpGet("https://api.minecraftservices.com/minecraft/profile/name/" + name + "/available");
            req.addHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse res = httpClient.execute(req);
            try {
                if (res.getStatusLine().getStatusCode() == 200) {
                    JsonObject obj = getJsonObject(res);
                    if (obj.has("status")) return obj.get("status").getAsString();
                }
            } finally {
                res.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }

    public static String changeName(String accessToken, String newName) {
        try {
            HttpPut req = new HttpPut("https://api.minecraftservices.com/minecraft/profile/name/" + newName);
            req.addHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse res = httpClient.execute(req);
            try {
                int code = res.getStatusLine().getStatusCode();
                if (code == 200) return "Name changed to " + newName + "!";
                if (code == 400) return "Invalid name format.";
                if (code == 403) return "Name change cooldown active.";
                if (code == 429) return "Rate limited, try again later.";
                return "Failed with HTTP status: " + code;
            } finally {
                res.close();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public static String changeSkin(String accessToken, String skinURL, boolean slim) {
        try {
            HttpPost req = new HttpPost("https://api.minecraftservices.com/minecraft/profile/skins");
            req.addHeader("Authorization", "Bearer " + accessToken);
            req.setHeader("Content-Type", "application/json");
            req.setEntity(new StringEntity("{ \"variant\": \"" + (slim ? "slim" : "classic") + "\", \"url\": \"" + skinURL + "\"}"));

            CloseableHttpResponse res = httpClient.execute(req);
            try {
                int code = res.getStatusLine().getStatusCode();
                if (code == 200) return "Skin updated successfully!";
                if (code == 400) return "Invalid skin URL.";
                return "Failed with status code " + code;
            } finally {
                res.close();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
