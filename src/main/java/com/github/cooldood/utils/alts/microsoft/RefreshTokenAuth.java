package com.github.cooldood.utils.alts.microsoft;

import com.github.cooldood.utils.alts.SessionUtil;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.NetworkUtil;
import com.google.gson.JsonObject;
import net.minecraft.util.Session;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// full logic ported from the refresh token authentication source (org.localts.RefreshTokenAuthentication)
// OAuth refresh -> OAuth access -> XBL -> XSTS -> MC token -> check ownership -> profile
public class RefreshTokenAuth {
    private static final String CLIENT_ID = "00000000402b5328";
    private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    private static final String REFRESH_TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    private static final String XBL_TOKEN_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XBL_RP = "http://auth.xboxlive.com";
    private static final String XSTS_TOKEN_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_TOKEN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String ENTITLEMENTS_URL = "https://api.minecraftservices.com/entitlements/license?requestId=auth";

    // returns the minecraft session for the account, logging in and saving the account
    public static Session authenticate(String refreshToken) throws IOException {
        /* OAuth refresh -> OAuth access -> XBL -> XSTS -> MC */
        MicrosoftTokenResponse oauth = refreshOAuthTokens(refreshToken);
        XboxLiveTokenResponse xboxLive = getXboxLiveToken(oauth.accessToken);
        XstsTokenResponse xsts = getXstsToken(xboxLive.token);
        MinecraftTokenResponse minecraft = getMinecraftAccessToken(xsts.token, xboxLive.userHash);

        /* MC -> Check game ownership -> Fetch profile */
        checkOwnership(minecraft.accessToken);

        return SessionUtil.queryPlayerProfile(minecraft.accessToken);
    }

    private static MicrosoftTokenResponse refreshOAuthTokens(String refreshToken) throws IOException {
        HttpPost req = new HttpPost(REFRESH_TOKEN_URL);
        req.setHeader("Content-Type", "application/x-www-form-urlencoded");

        ArrayList<NameValuePair> payload = new ArrayList<>();
        payload.add(new BasicNameValuePair("client_id", CLIENT_ID));
        payload.add(new BasicNameValuePair("grant_type", "refresh_token"));
        payload.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
        payload.add(new BasicNameValuePair("refresh_token", refreshToken));
        payload.add(new BasicNameValuePair("scope", SCOPE));
        req.setEntity(new UrlEncodedFormEntity(payload));

        String body = execute(req);
        int code = lastResponseCode;

        if (code >= 500) {
            throw new IOException("Microsoft services are unavailable");
        }

        JsonObject json = parseJson(body);
        if (json == null) {
            throw new IOException(String.format("Received no response when trying to refresh oauth tokens (code %s)", code));
        }

        MicrosoftTokenResponse microsoftResponse = MicrosoftTokenResponse.fromJson(json);

        if (!microsoftResponse.isSuccessful()) {
            throw new IOException("Received an error while refreshing oauth tokens: " + microsoftResponse.getError());
        }

        return microsoftResponse;
    }

    private static XboxLiveTokenResponse getXboxLiveToken(String accessToken) throws IOException {
        JsonObject payload = new JsonObject();
        JsonObject properties = new JsonObject();

        payload.add("Properties", properties);

        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", String.format("t=%s", accessToken));

        payload.addProperty("RelyingParty", XBL_RP);
        payload.addProperty("TokenType", "JWT");

        HttpPost req = new HttpPost(XBL_TOKEN_URL);
        req.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

        String body = execute(req);
        int code = lastResponseCode;

        if (code >= 500) {
            throw new IOException("Xbox services are unavailable (XBL)");
        }

        if (code == 401) {
            throw new IOException("OAuth access token is invalid");
        }

        return XboxLiveTokenResponse.fromJson(parseJson(body));
    }

    private static XstsTokenResponse getXstsToken(String accessToken) throws IOException {
        JsonObject payload = new JsonObject();
        JsonObject properties = new JsonObject();
        com.google.gson.JsonArray userTokens = new com.google.gson.JsonArray();

        properties.addProperty("SandboxId", "RETAIL");
        userTokens.add(new com.google.gson.JsonPrimitive(accessToken));
        properties.add("UserTokens", userTokens);

        payload.add("Properties", properties);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        HttpPost req = new HttpPost(XSTS_TOKEN_URL);
        req.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

        String body = execute(req);
        int code = lastResponseCode;

        if (code >= 500) {
            throw new IOException("Xbox services are unavailable (XSTS)");
        }

        XstsTokenResponse xstsResponse = XstsTokenResponse.fromJson(parseJson(body));

        if (!xstsResponse.isSuccessful()) {
            throw new IOException("Received an error while getting XSTS Token: " + xstsResponse.error);
        }

        return xstsResponse;
    }

    private static MinecraftTokenResponse getMinecraftAccessToken(String xstsToken, String userHash) throws IOException {
        JsonObject payload = new JsonObject();

        payload.addProperty("identityToken", String.format("XBL3.0 x=%s;%s", userHash, xstsToken));

        HttpPost req = new HttpPost(MC_TOKEN_URL);
        req.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

        String body = execute(req);
        int code = lastResponseCode;

        if (code >= 500) {
            throw new IOException("Xbox services are unavailable (login_with_xbox)");
        }

        MinecraftTokenResponse minecraftResponse = MinecraftTokenResponse.fromJson(parseJson(body));

        if (!minecraftResponse.isSuccessful()) {
            throw new IOException("Received an error while trying to get Minecraft access token: " + minecraftResponse.error);
        }

        return minecraftResponse;
    }

    private static void checkOwnership(String accessToken) throws IOException {
        HttpGet req = new HttpGet(ENTITLEMENTS_URL);
        req.setHeader("Authorization", String.format("Bearer %s", accessToken));

        String body = execute(req);
        int code = lastResponseCode;

        if (code >= 500) {
            throw new IOException("Minecraft services are unavailable");
        }

        if (code != 200) {
            throw new IOException("Received code " + code + " when trying to check game ownership");
        }

        EntitlementsResponse entitlements = EntitlementsResponse.fromJson(parseJson(body));

        if (!entitlements.checkOwnership()) {
            throw new IOException("Account doesn't own Minecraft!");
        }
    }

    private static int lastResponseCode;

    private static String execute(HttpPost request) throws IOException {
        CloseableHttpResponse response = NetworkUtil.client.execute(request);
        lastResponseCode = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity());
        response.close();
        return body;
    }

    private static String execute(HttpGet request) throws IOException {
        CloseableHttpResponse response = NetworkUtil.client.execute(request);
        lastResponseCode = response.getStatusLine().getStatusCode();
        String body = EntityUtils.toString(response.getEntity());
        response.close();
        return body;
    }

    private static JsonObject parseJson(String body) throws IOException {
        if (body == null || body.isEmpty()) return null;
        try {
            return C.gson.fromJson(body, JsonObject.class);
        } catch (Exception e) {
            throw new IOException("Invalid JSON response from Microsoft services", e);
        }
    }

    public static class MicrosoftTokenResponse {
        public String accessToken;
        public String refreshToken;
        public Long expiresIn;

        public String error;
        public String description;

        public static MicrosoftTokenResponse fromJson(JsonObject json) {
            MicrosoftTokenResponse response = new MicrosoftTokenResponse();

            if (json.has("error")) {
                response.error = json.get("error").getAsString();

                if (json.has("description")) {
                    response.description = json.get("description").getAsString();
                }
            } else if (json.has("access_token") && json.has("refresh_token") && json.has("expires_in")) {
                response.accessToken = json.get("access_token").getAsString();
                response.refreshToken = json.get("refresh_token").getAsString();
                response.expiresIn = json.get("expires_in").getAsLong();
            } else {
                throw new RuntimeException("Received invalid JSON object while trying to refresh oauth tokens");
            }

            return response;
        }

        public String getError() {
            if (description != null) {
                return String.format("%s (%s)", error, description);
            }
            return error;
        }

        public boolean isSuccessful() {
            return error == null;
        }
    }

    public static class XboxLiveTokenResponse {
        public String token;
        public String userHash;

        public static XboxLiveTokenResponse fromJson(JsonObject json) {
            XboxLiveTokenResponse xboxLiveResponse = new XboxLiveTokenResponse();

            if (!json.has("Token") || !json.has("DisplayClaims")) {
                throw new RuntimeException("Missing Token or DisplayClaims when trying to get Xbox live token");
            }

            xboxLiveResponse.token = json.get("Token").getAsString();
            xboxLiveResponse.userHash = json.get("DisplayClaims").getAsJsonObject()
                    .get("xui").getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("uhs").getAsString();

            return xboxLiveResponse;
        }
    }

    public static class XstsTokenResponse {
        private static final Map<Long, String> ERRORS = new HashMap<>();

        static {
            ERRORS.put(2148916227L, "The account is banned from Xbox");
            ERRORS.put(2148916233L, "The account doesn't have an Xbox account (never signed in)");
            ERRORS.put(2148916235L, "The account is from a country where Xbox Live is not available/banned");
            ERRORS.put(2148916236L, "The account needs adult verification on Xbox page. (South Korea)");
            ERRORS.put(2148916237L, "The account needs adult verification on Xbox page. (South Korea)");
            ERRORS.put(2148916238L, "The account is a child (under 18) and cannot proceed unless the account is added to a Family by an adult");
            ERRORS.put(2148916262L, "Unknown error");
        }

        public String token;
        public Long errorCode;
        public String error;

        public static XstsTokenResponse fromJson(JsonObject json) {
            XstsTokenResponse response = new XstsTokenResponse();

            if (json.has("XErr")) {
                response.errorCode = json.get("XErr").getAsLong();
                response.error = ERRORS.containsKey(response.errorCode) ? ERRORS.get(response.errorCode) : "Unknown error";
            } else if (json.has("Token")) {
                response.token = json.get("Token").getAsString();
            } else {
                throw new RuntimeException("XSTS token not found");
            }

            return response;
        }

        public boolean isSuccessful() {
            return error == null;
        }
    }

    public static class MinecraftTokenResponse {
        public String accessToken;
        public Long expiresIn;

        public String error;

        public static MinecraftTokenResponse fromJson(JsonObject json) {
            MinecraftTokenResponse response = new MinecraftTokenResponse();

            if (json.has("path")) {
                if (json.has("error")) {
                    response.error = json.get("error").getAsString();
                } else if (json.has("details") && json.get("details").getAsJsonObject().has("reason")) {
                    response.error = json.get("details").getAsJsonObject().get("reason").getAsString();
                } else {
                    response.error = "You're being rate limited, try again in a moment!";
                }
            } else if (json.has("access_token") && json.has("expires_in")) {
                response.accessToken = json.get("access_token").getAsString();
                response.expiresIn = json.get("expires_in").getAsLong();
            } else {
                throw new RuntimeException("No Minecraft access token found!");
            }

            return response;
        }

        public boolean isSuccessful() {
            return error == null;
        }
    }

    public static class EntitlementsResponse {
        private static final Set<String> SOURCES = new java.util.HashSet<>(java.util.Arrays.asList("GAMEPASS", "PURCHASE", "MC_PURCHASE"));

        public Map<String, String> entitlements;

        public static EntitlementsResponse fromJson(JsonObject json) {
            EntitlementsResponse entitlementsResponse = new EntitlementsResponse();

            if (!json.has("items")) {
                throw new RuntimeException("Couldn't receive entitlements");
            }

            entitlementsResponse.entitlements = new HashMap<>();
            for (com.google.gson.JsonElement element : json.getAsJsonArray("items")) {
                JsonObject item = element.getAsJsonObject();
                entitlementsResponse.entitlements.put(item.get("name").getAsString(), item.get("source").getAsString());
            }

            return entitlementsResponse;
        }

        public boolean checkOwnership() {
            return entitlements.entrySet()
                    .stream()
                    .anyMatch(entry -> entry.getKey().contains("minecraft") && SOURCES.contains(entry.getValue()));
        }
    }
}
