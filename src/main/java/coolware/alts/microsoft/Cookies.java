package coolware.alts.microsoft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.Session;
import coolware.alts.SessionUtil;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class Cookies {
    private static final CloseableHttpClient httpClient = HttpClients.custom()
            .disableAuthCaching()
            .disableCookieManagement()
            .disableRedirectHandling()
            .build();

    public static Session authenticate(String cookie) throws Exception {
        String redirectLocation = getRedirectLocation(cookie);
        if (redirectLocation == null) {
            throw new RuntimeException("Could not get redirect location from cookie.");
        }

        String xboxUrl = getXboxUrl(redirectLocation, cookie);
        if (xboxUrl == null) {
            throw new RuntimeException("Could not get xbox URL.");
        }

        String xstsTokenAndHashcode = getXSTSTokenAndHashcode(xboxUrl, cookie);
        if (xstsTokenAndHashcode == null) {
            throw new RuntimeException("Could not get XSTS token and hashcode.");
        }

        JsonArray tokensArray = new JsonParser().parse(new String(Base64.getDecoder().decode(xstsTokenAndHashcode))).getAsJsonArray();
        JsonObject minecraftResponse = tokensArray.get(2).getAsJsonObject().get("Item2").getAsJsonObject();
        String userHashcode = minecraftResponse.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
        String xstsAccessToken = minecraftResponse.get("Token").getAsString();

        String mcAccessToken = MSAuth.getMinecraftAccessToken(userHashcode, xstsAccessToken);
        if (mcAccessToken == null) {
            throw new RuntimeException("Could not obtain Minecraft access token.");
        }

        return SessionUtil.queryPlayerProfile(mcAccessToken);
    }

    public static Session authenticate(File cookieFile) throws Exception {
        List<String> lines = FileUtils.readLines(cookieFile);
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\t");
            if (parts.length >= 7) {
                sb.append(parts[5]).append("=").append(parts[6]).append("; ");
            } else if (line.contains("=")) {
                sb.append(line).append("; ");
            }
        }
        return authenticate(sb.toString().trim());
    }

    private static String getRedirectLocation(String cookie) {
        try {
            HttpGet req = new HttpGet("https://sisu.xboxlive.com/connect/XboxLive/?state=login&ru=https://www.minecraft.net/en-us/login");
            req.setHeader("Cookie", cookie);
            CloseableHttpResponse res = httpClient.execute(req);
            try {
                if (res.containsHeader("Location")) {
                    return res.getFirstHeader("Location").getValue();
                }
            } finally {
                res.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getXboxUrl(String redirectLocation, String cookie) {
        try {
            HttpGet req = new HttpGet(redirectLocation.replaceAll(" ", "%20"));
            req.setHeader("Cookie", cookie);
            CloseableHttpResponse res = httpClient.execute(req);
            try {
                if (res.containsHeader("Location")) {
                    return res.getFirstHeader("Location").getValue();
                }
            } finally {
                res.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getXSTSTokenAndHashcode(String xboxUrl, String cookie) {
        try {
            HttpGet req = new HttpGet(xboxUrl);
            req.setHeader("Cookie", cookie);
            CloseableHttpResponse res = httpClient.execute(req);
            try {
                if (res.containsHeader("Location")) {
                    String loc = res.getFirstHeader("Location").getValue();
                    if (loc.contains("accessToken=")) {
                        return loc.split("accessToken=")[1];
                    }
                }
            } finally {
                res.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
