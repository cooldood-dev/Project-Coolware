package coolware.alts.microsoft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Session;
import coolware.alts.SessionUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

public class MSAuth {
    public static final String CLIENT_ID = "54fd49e4-2103-4044-9603-2b028c814ec3";
    public static final String PORT = "59125";
    public static final String SCOPE = "XboxLive.signin XboxLive.offline_access";

    private static final CloseableHttpClient httpClient = HttpClients.custom()
            .disableAuthCaching()
            .disableCookieManagement()
            .disableRedirectHandling()
            .build();

    public static class AccessRefreshToken {
        public final String accessToken;
        public final String refreshToken;

        public AccessRefreshToken(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    private static class XBLTokenUhs {
        public final String xblToken;
        public final String uhs;

        public XBLTokenUhs(String xblToken, String uhs) {
            this.xblToken = xblToken;
            this.uhs = uhs;
        }
    }

    public static AccessRefreshToken authCode(String code) throws Exception {
        HttpPost req = new HttpPost("https://login.live.com/oauth20_token.srf");
        req.addHeader("Content-Type", "application/x-www-form-urlencoded");

        ArrayList<NameValuePair> payload = new ArrayList<NameValuePair>();
        payload.add(new BasicNameValuePair("client_id", CLIENT_ID));
        payload.add(new BasicNameValuePair("code", code));
        payload.add(new BasicNameValuePair("grant_type", "authorization_code"));
        payload.add(new BasicNameValuePair("redirect_uri", "http://localhost:" + PORT + "/"));
        payload.add(new BasicNameValuePair("scope", SCOPE));
        req.setEntity(new UrlEncodedFormEntity(payload));

        CloseableHttpResponse res = httpClient.execute(req);
        try {
            JsonObject resJson = new Gson().fromJson(EntityUtils.toString(res.getEntity()), JsonObject.class);
            String accessToken = resJson.get("access_token").getAsString();
            String refreshToken = resJson.get("refresh_token").getAsString();
            return new AccessRefreshToken(accessToken, refreshToken);
        } finally {
            res.close();
        }
    }

    public static AccessRefreshToken refreshToken(String refreshToken) throws Exception {
        HttpPost req = new HttpPost("https://login.live.com/oauth20_token.srf");
        req.addHeader("Content-Type", "application/x-www-form-urlencoded");

        ArrayList<NameValuePair> payload = new ArrayList<NameValuePair>();
        payload.add(new BasicNameValuePair("client_id", CLIENT_ID));
        payload.add(new BasicNameValuePair("refresh_token", refreshToken));
        payload.add(new BasicNameValuePair("grant_type", "refresh_token"));
        payload.add(new BasicNameValuePair("redirect_uri", "http://localhost:" + PORT + "/"));
        req.setEntity(new UrlEncodedFormEntity(payload));

        CloseableHttpResponse res = httpClient.execute(req);
        try {
            JsonObject resJson = new Gson().fromJson(EntityUtils.toString(res.getEntity()), JsonObject.class);
            String accessToken = resJson.get("access_token").getAsString();
            String newRefreshToken = resJson.get("refresh_token").getAsString();
            return new AccessRefreshToken(accessToken, newRefreshToken);
        } finally {
            res.close();
        }
    }

    public static String authXBL(String authToken) throws Exception {
        HttpPost req = new HttpPost("https://user.auth.xboxlive.com/user/authenticate");
        req.addHeader("Content-Type", "application/json");
        req.addHeader("Accept", "application/json");

        JsonObject payload = new JsonObject();
        JsonObject payloadProps = new JsonObject();
        payloadProps.addProperty("AuthMethod", "RPS");
        payloadProps.addProperty("SiteName", "user.auth.xboxlive.com");
        payloadProps.addProperty("RpsTicket", "d=" + authToken);
        payload.add("Properties", payloadProps);
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        req.setEntity(new StringEntity(payload.toString()));

        CloseableHttpResponse res = httpClient.execute(req);
        try {
            JsonObject resJson = new Gson().fromJson(EntityUtils.toString(res.getEntity()), JsonObject.class);
            return resJson.get("Token").getAsString();
        } finally {
            res.close();
        }
    }

    public static XBLTokenUhs authXSTS(String xblToken) throws Exception {
        HttpPost req = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");
        req.addHeader("Content-Type", "application/json");
        req.addHeader("Accept", "application/json");

        JsonObject payload = new JsonObject();
        JsonObject payloadProps = new JsonObject();
        JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(xblToken));
        payloadProps.add("UserTokens", userTokens);
        payloadProps.addProperty("SandboxId", "RETAIL");
        payload.add("Properties", payloadProps);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        req.setEntity(new StringEntity(payload.toString()));

        CloseableHttpResponse res = httpClient.execute(req);
        try {
            JsonObject resJson = new Gson().fromJson(EntityUtils.toString(res.getEntity()), JsonObject.class);
            String token = resJson.get("Token").getAsString();
            String uhs = resJson.get("DisplayClaims").getAsJsonObject()
                    .get("xui").getAsJsonArray().get(0).getAsJsonObject()
                    .get("uhs").getAsString();
            return new XBLTokenUhs(token, uhs);
        } finally {
            res.close();
        }
    }

    public static String getMinecraftAccessToken(String userHashcode, String xstsAccessToken) throws Exception {
        HttpPost req = new HttpPost("https://api.minecraftservices.com/authentication/login_with_xbox");
        req.setHeader("Content-Type", "application/json");
        req.setEntity(new StringEntity("{ \"identityToken\": \"XBL3.0 x=" + userHashcode + ";" + xstsAccessToken + "\" }"));

        CloseableHttpResponse res = httpClient.execute(req);
        try {
            JsonObject resJson = new Gson().fromJson(EntityUtils.toString(res.getEntity()), JsonObject.class);
            if (resJson.has("access_token")) {
                return resJson.get("access_token").getAsString();
            }
        } finally {
            res.close();
        }
        return null;
    }

    public static Session authWithAccessRefreshToken(AccessRefreshToken accessRefreshToken) throws Exception {
        String xblToken = authXBL(accessRefreshToken.accessToken);
        XBLTokenUhs xblTokenUhs = authXSTS(xblToken);
        String mcAccessToken = getMinecraftAccessToken(xblTokenUhs.uhs, xblTokenUhs.xblToken);
        if (mcAccessToken == null) {
            throw new RuntimeException("Could not obtain Minecraft Access Token from Xbox auth.");
        }
        return SessionUtil.queryPlayerProfile(mcAccessToken);
    }

    public static Session authWithCode(String code) throws Exception {
        return authWithAccessRefreshToken(authCode(code));
    }

    public static Session authWithRefreshToken(String refreshToken) throws Exception {
        return authWithAccessRefreshToken(refreshToken(refreshToken));
    }
}
