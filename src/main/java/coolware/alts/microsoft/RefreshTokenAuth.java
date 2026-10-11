package coolware.alts.microsoft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Session;
import coolware.alts.SessionUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;

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

    private static final CloseableHttpClient httpClient = HttpClients.custom()
            .disableAuthCaching()
            .disableCookieManagement()
            .disableRedirectHandling()
            .build();

    public static Session authenticate(String refreshToken) throws Exception {
        // Step 1: Refresh OAuth Tokens
        HttpPost req = new HttpPost(REFRESH_TOKEN_URL);
        req.setHeader("Content-Type", "application/x-www-form-urlencoded");

        ArrayList<NameValuePair> payload = new ArrayList<NameValuePair>();
        payload.add(new BasicNameValuePair("client_id", CLIENT_ID));
        payload.add(new BasicNameValuePair("grant_type", "refresh_token"));
        payload.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
        payload.add(new BasicNameValuePair("refresh_token", refreshToken));
        payload.add(new BasicNameValuePair("scope", SCOPE));
        req.setEntity(new UrlEncodedFormEntity(payload));

        CloseableHttpResponse res = httpClient.execute(req);
        String oauthAccessToken;
        try {
            JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();
            if (!json.has("access_token")) {
                throw new IOException("Failed to refresh oauth token: " + json.toString());
            }
            oauthAccessToken = json.get("access_token").getAsString();
        } finally {
            res.close();
        }

        // Step 2: XBL Token
        JsonObject xblPayload = new JsonObject();
        JsonObject xblProps = new JsonObject();
        xblProps.addProperty("AuthMethod", "RPS");
        xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
        xblProps.addProperty("RpsTicket", "t=" + oauthAccessToken);
        xblPayload.add("Properties", xblProps);
        xblPayload.addProperty("RelyingParty", XBL_RP);
        xblPayload.addProperty("TokenType", "JWT");

        HttpPost xblReq = new HttpPost(XBL_TOKEN_URL);
        xblReq.setHeader("Content-Type", "application/json");
        xblReq.setHeader("Accept", "application/json");
        xblReq.setEntity(new StringEntity(xblPayload.toString()));

        CloseableHttpResponse xblRes = httpClient.execute(xblReq);
        String xblToken;
        String userHash;
        try {
            JsonObject xblJson = new JsonParser().parse(EntityUtils.toString(xblRes.getEntity())).getAsJsonObject();
            xblToken = xblJson.get("Token").getAsString();
            userHash = xblJson.get("DisplayClaims").getAsJsonObject()
                    .get("xui").getAsJsonArray().get(0).getAsJsonObject()
                    .get("uhs").getAsString();
        } finally {
            xblRes.close();
        }

        // Step 3: XSTS Token
        JsonObject xstsPayload = new JsonObject();
        JsonObject xstsProps = new JsonObject();
        JsonArray uTokens = new JsonArray();
        uTokens.add(new com.google.gson.JsonPrimitive(xblToken));
        xstsProps.add("UserTokens", uTokens);
        xstsProps.addProperty("SandboxId", "RETAIL");
        xstsPayload.add("Properties", xstsProps);
        xstsPayload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        xstsPayload.addProperty("TokenType", "JWT");

        HttpPost xstsReq = new HttpPost(XSTS_TOKEN_URL);
        xstsReq.setHeader("Content-Type", "application/json");
        xstsReq.setHeader("Accept", "application/json");
        xstsReq.setEntity(new StringEntity(xstsPayload.toString()));

        CloseableHttpResponse xstsRes = httpClient.execute(xstsReq);
        String xstsToken;
        try {
            JsonObject xstsJson = new JsonParser().parse(EntityUtils.toString(xstsRes.getEntity())).getAsJsonObject();
            xstsToken = xstsJson.get("Token").getAsString();
        } finally {
            xstsRes.close();
        }

        // Step 4: Minecraft Token
        HttpPost mcReq = new HttpPost(MC_TOKEN_URL);
        mcReq.setHeader("Content-Type", "application/json");
        mcReq.setEntity(new StringEntity("{ \"identityToken\": \"XBL3.0 x=" + userHash + ";" + xstsToken + "\" }"));

        CloseableHttpResponse mcRes = httpClient.execute(mcReq);
        String mcAccessToken;
        try {
            JsonObject mcJson = new JsonParser().parse(EntityUtils.toString(mcRes.getEntity())).getAsJsonObject();
            if (!mcJson.has("access_token")) {
                throw new IOException("Failed to obtain Minecraft token: " + mcJson.toString());
            }
            mcAccessToken = mcJson.get("access_token").getAsString();
        } finally {
            mcRes.close();
        }

        return SessionUtil.queryPlayerProfile(mcAccessToken);
    }
}
