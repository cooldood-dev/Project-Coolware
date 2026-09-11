package com.github.cooldood.utils.alts.microsoft;

import com.github.cooldood.utils.alts.Login;
import com.github.cooldood.utils.alts.SessionUtil;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.NetworkUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Session;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

// stole urls from novoline
public class Cookies {
    public static void authenticate(String cookie) {
        Login.addProgressReport("Logging in with cookies");

        //addProgressReport("Getting xbox redirect.");
        String redirectLocation = getRedirectLocation(cookie);
        if (redirectLocation == null) return;

        String xboxUrl = getXboxUrl(redirectLocation, cookie);
        if (xboxUrl == null) return;

        //addProgressReport("Getting XSTS token and user hashcode from xbox redirect");
        String XSTSTokenAndHashcode = getXSTSTokenAndHashcode(xboxUrl, cookie);
        if (XSTSTokenAndHashcode == null) return;

        //addProgressReport("Getting minecraft access token");
        JsonArray tokensArray = new Gson().fromJson(new String(Base64.getDecoder().decode(XSTSTokenAndHashcode)), JsonArray.class);
        // "Item1": "rp://api.minecraftservices.com/", minecrafts response is the 3rd token in the array for me
        JsonObject minecraftResponse = tokensArray.get(2).getAsJsonObject().get("Item2").getAsJsonObject();
        String userHashcode = minecraftResponse.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString();
        String XSTSaccessToken = minecraftResponse.get("Token").getAsString();

        String minecraftAccessToken = Login.getMinecraftAccessToken(userHashcode, XSTSaccessToken);
        if (minecraftAccessToken == null) return;

        if (ownsMinecraft(minecraftAccessToken)) {
            Session session = SessionUtil.queryPlayerProfile(minecraftAccessToken);
            if (!session.getToken().isEmpty()) {
                Login.loginSession(session);
                Login.saveAccount(Login.AltTypes.Cookie, cookie, C.mc.getSession());
                return;
            }
            Login.setErrorMessage("Failed to log in to account, please try again!");
        }

    }

    private static boolean ownsMinecraft(String accessToken) {
        try {
            HttpGet req = new HttpGet("https://api.minecraftservices.com/entitlements/license?requestId=checker");
            req.setHeader("Authorization", "Bearer " + accessToken);

            JsonObject resJson = NetworkUtil.getJSONObject(req);

            for (JsonElement element : resJson.get("items").getAsJsonArray()) {
                JsonObject jsonObject = element.getAsJsonObject();
                String source = jsonObject.get("source").getAsString();
                if (source.equals("PURCHASE") || source.equals("MC_PURCHASE")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Login.setErrorMessage("No license found!");
        return false;
    }

    private static String getXSTSTokenAndHashcode(String xboxUrl, String cookie) {
        try {
            HttpGet req = new HttpGet(xboxUrl);
            req.setHeader("Cookie", cookie);
            CloseableHttpResponse res = NetworkUtil.getServerResponse(req);

            if (res.getFirstHeader("Location").getValue().contains("https://www.minecraft.net/en-us/login#state=login&accessToken="))
                return res.getFirstHeader("Location").getValue().split("accessToken=")[1];
        } catch (Exception e) {
            e.printStackTrace();
        }

        Login.setErrorMessage("Access token is not present!");
        return null;
    }

    private static String getRedirectLocation(String cookie) {
        try {
            HttpGet req = new HttpGet("https://sisu.xboxlive.com/connect/XboxLive/?state=login&ru=https://www.minecraft.net/en-us/login");
            req.setHeader("Cookie", cookie);

            CloseableHttpResponse res = NetworkUtil.getServerResponse(req);

            if (res.getStatusLine().getStatusCode() == 302 && res.getFirstHeader("Location").getValue().contains("oauth20_authorize.srf"))
                return NetworkUtil.fixURL(res.getFirstHeader("Location").getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Login.setErrorMessage("Redirect location not found");
        return null;
    }

    private static String getXboxUrl(@Nullable String location, String cookie) {
        if (location == null) return null;
        try {
            HttpGet req = new HttpGet(location);
            req.setHeader("Cookie", cookie);
            CloseableHttpResponse res = NetworkUtil.getServerResponse(req);

            if (res.getStatusLine().getStatusCode() == 302 && res.getFirstHeader("Location").getValue().contains("code="))
                return NetworkUtil.fixURL(res.getFirstHeader("Location").getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }

        Login.setErrorMessage("Invalid Cookie, cannot log in to xbox account.");
        return null;
    }

    public static String getCookiesFromFile(String path) throws Exception {
        StringBuilder fileText = new StringBuilder();
        List<String> fileLines = Files.readAllLines(Paths.get(path));

        fileLines.stream().map(e -> e + ";\n").forEach(fileText::append);
        return fileText.toString();
    }

    public static String formatCookies(String fileContent) {
        if (fileContent == null) return null;

        StringBuilder cook = new StringBuilder();
        for (String s : fileContent.split("\n")) {
            String[] strings = s.split("\t");

            if (strings.length < 5) continue;

            cook.append(strings[5]).append("=").append(strings[6]);
        }
        return cook.substring(0, cook.length() - 2);
    }
}