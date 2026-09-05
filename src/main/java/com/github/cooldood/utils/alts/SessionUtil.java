package com.github.cooldood.utils.alts;

import com.github.cooldood.bridge.net.minecraft.client.MinecraftBridge;
import com.github.cooldood.bridge.net.minecraft.util.SessionBridge;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.NetworkUtil;
import com.google.gson.JsonObject;
import net.minecraft.util.Session;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

// https://minecraft.wiki/w/Mojang_API
public class SessionUtil {
    // https://minecraft.wiki/w/Mojang_API#Query_player_profile
    public static Session queryPlayerProfile(String accessToken) {
        try {
            HttpGet req = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
            req.addHeader("Authorization", "Bearer " + accessToken);

            JsonObject res = NetworkUtil.getJSONObject(req);

            if (res.has("id") && res.has("name")) {
                String uuid = res.get("id").getAsString();
                String username = res.get("name").getAsString();

                return new Session(username, uuid, accessToken, Session.Type.MOJANG.name());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Session("Unknown", "Unknown", "", Session.Type.MOJANG.name());
    }

    // https://minecraft.wiki/w/Mojang_API#Check_name_availability
    public static String checkNameAvailability(String accessToken, String name) {
        if (name.length() > 16) return "Name is too long: " + name.length() + " > 16";
        if (name.length() <= 3) return "Name is too short: " + name.length() + " < 3";
        try {
            HttpGet req = new HttpGet("https://api.minecraftservices.com/minecraft/profile/name/"+name+"/available");
            req.addHeader("Authorization", "Bearer " + accessToken);

            JsonObject res = NetworkUtil.getJSONObject(req);
            if (!res.has("status")) return "Access token is invalid.";

            return res.get("status").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // https://minecraft.wiki/w/Mojang_API#Query_player's_name_change_information
    public static String checkLastNameChange(String accessToken) {
        try {
            HttpGet req = new HttpGet("https://api.minecraftservices.com/minecraft/profile/namechange");
            req.addHeader("Authorization", "Bearer " + accessToken);

            JsonObject res = NetworkUtil.getJSONObject(req);

            if (res.has("nameChangeAllowed")) {
                boolean allowed = res.get("nameChangeAllowed").getAsBoolean();

                if (allowed) return "ALLOWED";

                if (res.has("changedAt"))
                    return res.get("changedAt").getAsString();

                if (res.has("createdAt"))
                    return res.get("createdAt").getAsString();
            }

            return res.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // https://minecraft.wiki/w/Mojang_API#Change_name
    public static String changeName(String accessToken, String newName) {
        String lastChange = checkLastNameChange(accessToken);
        if (!lastChange.equals("ALLOWED")) {
            if (lastChange.isEmpty() || lastChange.contains("path")) return "Cannot change name, access token is invalid.";

            return "Cannot change name, last name change was: " + lastChange;
        }

        String invalidCharacters = newName.replaceAll(Login.VALID_NAME_CHARATERS_REGEX, "");
        if (!invalidCharacters.isEmpty()) return "Name contains invalid characters: \"" + invalidCharacters + "\"";
        if (newName.length() > 16) return "Name is " + newName.length() + " characters, 16 is the limit.";

        String nameAvailability = checkNameAvailability(accessToken, newName);
        switch (nameAvailability) {
            case "AVAILABLE":
                break;
            case "DUPLICATE":
                return "Name is already taken.";
            case "NOT_ALLOWED":
                return "Name is BANNED by microsoft.";
            default:
                // shouldnt appear, maybe if server doesnt respond? normally only replies if character limit reached or invalid charaters
                return nameAvailability;
        }

        try {
            HttpPut req = new HttpPut("https://api.minecraftservices.com/minecraft/profile/name/"+newName);
            req.addHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse res = HttpClients.createDefault().execute(req);

            switch (res.getStatusLine().getStatusCode()) {
                case 400:
                    return "Name does not meet requirement. The name must have less than or equal to 16 characters and must consist of alphanumericals and underscores.";
                case 403:
                    return "Cannot change name, last name change was: ";
                case 429:
                    return "Too many rename requests sent.";
                case 200:
                    Session newSession = new Session(newName, C.mc.getSession().getPlayerID(), C.mc.getSession().getToken(), C.mc.getSession().getSessionType().name());
                    MinecraftBridge.from(C.mc).bridge$setSession(SessionBridge.from(newSession));

                    return "Name changed successfully to: " + newName + "!";
                default:
                    return "Unknown error, what happened.";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String changeSkin(String accessToken, String skinURL) {
        boolean slim = true;
        try {
            BufferedImage skinImage = ImageIO.read(new URL(skinURL));

            // slim skins SHOULDNT have a coloured pixel here, because it wont be drawn
            slim = skinImage.getRGB(54, 20) == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return changeSkin(accessToken, skinURL, slim ? "slim" : "classic");
    }

    // https://minecraft.wiki/w/Mojang_API#Change_skin
    public static String changeSkin(String accessToken, String skinURL, String skinVariant) {
        try {
            HttpPost req = new HttpPost("https://api.minecraftservices.com/minecraft/profile/skins");
            req.addHeader("Authorization", "Bearer " + accessToken);
            req.setHeader("Content-Type", "application/json");
            req.setEntity(new StringEntity("{ \"variant\": \""+skinVariant+"\", \"url\": \""+skinURL+"\"}"));

            CloseableHttpResponse res = NetworkUtil.getServerResponse(req);
            switch (res.getStatusLine().getStatusCode()) {
                case 400:
                    return "Request failed, skin URL could be invalid.";
                case 403:
                    return "Cannot change skin.";
                case 429:
                    return "Too many skin change requests sent.";
                case 200:
                    try {
                        Login.getAlt().head = SessionUtil.getHead(ImageIO.read(new URL(skinURL)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return "Skin changed successfully!";
                default:
                    return "Unknown error, what happened.";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // https://minecraft.wiki/w/Mojang_API#Query_player's_skin_and_cape
    public static String getSkin(String uuid) {
        try {
            HttpGet req = new HttpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);

            JsonObject res = NetworkUtil.getJSONObject(req);

            String value = res.get("properties").getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("value").getAsString();

            String valueDecoded = new String(Base64.getDecoder().decode(value));

            JsonObject valueObject = C.gson.fromJson(valueDecoded, JsonObject.class);

            return valueObject.get("textures").getAsJsonObject()
                    .get("SKIN").getAsJsonObject()
                    .get("url").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            // the api claims if you have a steve skin it returns an empty SKIN field, didn't test.
            return "https://s.namemc.com/i/12b92a9206470fe2.png";
        }
    }

    // great name, well done scale
    public static BufferedImage getHead(String uuid) {
        try {
            BufferedImage skinImage = ImageIO.read(new URL(getSkin(uuid)));

            return getHead(skinImage);
        } catch (IOException e) {
            e.printStackTrace();
            return new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public static BufferedImage getHead(BufferedImage skinImage) {
        BufferedImage head = skinImage.getSubimage(8, 8, 8, 8);
        BufferedImage secondaryLayer = skinImage.getSubimage(40, 8, 8, 8);

        head.getGraphics().drawImage(secondaryLayer, 0, 0, null);

        return head;
    }
}