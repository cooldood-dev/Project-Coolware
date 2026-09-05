package com.github.cooldood.utils.alts;

import com.github.cooldood.Main;
import com.github.cooldood.bridge.net.minecraft.client.MinecraftBridge;
import com.github.cooldood.bridge.net.minecraft.util.SessionBridge;
import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.ClientTickEvent;
import com.github.cooldood.events.impl.FileDroppedEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.screens.AltManagerScreen;
import com.github.cooldood.utils.alts.microsoft.AuthServer;
import com.github.cooldood.utils.alts.microsoft.Cookies;
import com.github.cooldood.utils.alts.microsoft.MSAuth;
import com.github.cooldood.utils.alts.microsoft.RefreshTokenAuth;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.FileChooserUtil;
import com.github.cooldood.utils.client.FrameUtil;
import com.github.cooldood.utils.client.NetworkUtil;
import com.github.cooldood.utils.minecraft.ChatUtil;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.util.Session;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class Login {
    @AllArgsConstructor
    public enum AltTypes {
        Cracked("Cracked", "§4", "Creates an account for cracked servers", () -> {
            try {
                loggingInCracked = true;
            } catch (Exception e) {
                setErrorMessage("Invalid clipboard data, please copy a session ID to clipboard!");
            }
        }),
        Session("Session", "§c", "gets session id from clipboard", () -> {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String data = (String) clipboard.getData(DataFlavor.stringFlavor);

                loginSession(data);
            } catch (Exception e) {
                setErrorMessage("Invalid clipboard data, please copy a session ID to clipboard!");
            }
        }),
        Cookie("Cookie", "§a", "picks cookie file or reads clipboard", () -> {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            boolean handled = false;
            try {
                if (clipboard.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) {
                    java.util.List<File> data = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
                    if (data != null && !data.isEmpty()) {
                        for (File file : data) Login.loginCookie(file);
                        handled = true;
                    }
                }
            } catch (Exception ignored) {}

            if (!handled) {
                try {
                    if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                        String str = ((String) clipboard.getData(DataFlavor.stringFlavor)).replaceAll("\"", "").trim();
                        File potentialFile = new File(str);
                        if (potentialFile.exists() && potentialFile.isFile()) {
                            Login.loginCookie(potentialFile);
                            handled = true;
                        } else if (str.contains("=") || str.contains("\t") || str.length() > 50) {
                            Login.loginCookie(str);
                            handled = true;
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (!handled) {
                FileChooserUtil.openFilePicker("Select Cookie File", file -> {
                    if (file != null) {
                        try {
                            Login.loginCookie(file);
                        } catch (Exception e) {
                            Login.setErrorMessage("Failed to read cookie file: " + e.getMessage());
                        }
                    }
                });
            }
        }),
        Microsoft("Microsoft", "§2", "opens microsoft oauth link in browser", Login::loginMicrosoft),
        Refresh_Token("Refresh Token", "§b", "logs in with refresh token from clipboard", () -> {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String data = (String) clipboard.getData(DataFlavor.stringFlavor);

                loginRefreshToken(data);
            } catch (Exception e) {
                setErrorMessage("Invalid clipboard data, please copy a refresh token to clipboard!");
            }
        }),
        Open_Folder("Open Folder", "§4", "opens the alts folder", () -> {
            try {
                Desktop.getDesktop().open(altsPath.toFile());
            } catch (Exception e) {
                setErrorMessage("Invalid clipboard data, please copy email:password to clipboard!");
            }
        });

        public final String name;
        public final String colour;
        public final String description;
        public final Runnable action;
    }

    public static boolean loggingInCracked = false;

    public static Path altsPath = Paths.get(Main.extraSavedFeaturesPath + "alts");

    public static class Alt {
        public String name;
        public String uuid;
        public Login.AltTypes type;
        public HashMap<String, String> json;

        public BufferedImage head;

        Path skinsPath;
        Path headPath;

        public Alt(String name, String uuid, Login.AltTypes type, HashMap<String, String> json) {
            this.name = name;
            this.uuid = uuid;
            this.type = type;
            this.json = json;
            this.skinsPath = Paths.get(altsPath.toString() + "/skins");
            this.headPath = Paths.get(skinsPath + "/" + uuid + ".png");
            this.head = getHead();
        }

        /// use this if updating alts json file
        public HashMap<String, String> getUpdatedJson() {
            return getJson(this.uuid);
        }

        public void updateHeadTexture() {
            try {
                this.head = SessionUtil.getHead(uuid);
                ImageIO.write(this.head, "png", headPath.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                if (this.head == null)
                    this.head = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            }
        }

        public BufferedImage getHead() {
            if (this.head == null) {
                if (!Files.exists(skinsPath)) skinsPath.toFile().mkdirs();
                if (Files.exists(headPath)) {
                    try {
                        this.head = ImageIO.read(headPath.toFile());
                        return this.head;
                    } catch (IOException e) {
                        System.err.println("Failed to load head for alt: " + name + " (" + uuid + ")");
                        e.printStackTrace();
                    }
                }
                else updateHeadTexture();
            }

            return this.head;
        }
    }

    private static HashMap<String, String> getJson(String uuid) {
        if (Files.exists(getAccountPath(uuid))) {
            try {
                String fileText = FileUtils.readFileToString(getAccountPath(uuid).toFile());
                return C.gson.fromJson(fileText, HashMap.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Alt alt = getAlt(uuid);
        if (alt != null) return alt.json;
        return null;
    }

    public static Alt getAlt(String uuid) {
        for (Login.Alt alt : AltManagerScreen.alts) {
            if (alt.uuid.equals(uuid)) {
                return alt;
            }
        }

        return null;
    }

    public static Alt getAlt() {
        return getAlt(C.mc.getSession().getPlayerID());
    }

    @SubscribeEvent
    public static void onFileDrop(FileDroppedEvent event) {
        System.out.println("File dropped: " + event.droppedFiles.get(0).getAbsolutePath());

        for (File file : event.droppedFiles) {
            try {
                loginCookie(file);
            } catch (Exception e) {
                setErrorMessage("Cannot read cookies from file!");
            }
        }
    }

    private static Session lastSession = C.mc.getSession();

    @SubscribeEvent
    public static void onClientTickEvent(ClientTickEvent event) {
        if (!loggingIn && !altsQueue.isEmpty()) {
            AltQueue altQueue = altsQueue.get(0);
            altsQueue.remove(0);

            switch (altQueue.altType) {
                case Microsoft:
                    Login.loginMicrosoft(altQueue.string);
                    break;

                case Refresh_Token:
                    Login.loginRefreshToken(altQueue.string);
                    break;

                case Cookie:
                    Login.loginCookie(altQueue.string);
                    break;

                case Session:
                    Login.loginSession(altQueue.string);
                    break;
            }
        }

        if (lastSession != C.mc.getSession()) {
            // update name change date on login, incase it failed to set before
            Alt currentAlt = getAlt();
            if (currentAlt == null) return;
            currentAlt.json = currentAlt.getUpdatedJson();

            String lastNameChange = SessionUtil.checkLastNameChange(C.mc.getSession().getToken());

            if (!lastNameChange.contains("path")) {
                currentAlt.json.put("nameChangeDate", lastNameChange);
                Login.updateAltJSON(currentAlt.uuid, currentAlt.json);
            }

            // just incase.
            currentAlt.name = C.mc.getSession().getUsername();
            currentAlt.json.put("name", C.mc.getSession().getUsername());
            Login.updateAltJSON(currentAlt.uuid, currentAlt.json);

            currentAlt.updateHeadTexture();

            lastSession = C.mc.getSession();
        }
    }

    // if you get a ban appeal accepted, you will be unbanned early, this is my way of detecting this.
    @SubscribeEvent
    public static void onJoinServer(PacketEvent.Receive event) {
        Alt alt = getAlt();
        if (alt == null) return;

        if (event.packet instanceof S02PacketLoginSuccess) {
            if (C.mc.getCurrentServerData() != null && C.mc.getCurrentServerData().serverIP != null && C.mc.getCurrentServerData().serverIP.endsWith("hypixel.net")) {
                HashMap<String, String> updatedJson = alt.getUpdatedJson();
                if (updatedJson != null) {
                    alt.json = updatedJson;
                    alt.json.put("unbanDate", "now");
                    updateAltJSON(alt.uuid, alt.json);
                }
            }
        }
    }

    public static void loginMicrosoft() {
        StringSelection stringSelection = new StringSelection(AuthServer.URL);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
        NetworkUtil.openBrowser(AuthServer.URL);
    }

    @AllArgsConstructor
    public static class AltQueue {
        private String string;
        private AltTypes altType;
    }
    private static final ArrayList<AltQueue> altsQueue = new ArrayList<>();

    public static void loginMicrosoft(String refreshToken) {
        if (loggingIn) {
            altsQueue.add(new AltQueue(refreshToken, AltTypes.Microsoft));
            return;
        }
        loggingIn = true;

        addProgressReport("Logging in with refresh token");
        ForkJoinPool.commonPool().execute(() -> {
            try {
                loginSession(MSAuth.authWithRefreshToken(refreshToken));
                loggingIn = false;
            } catch (Exception e) {
                setErrorMessage("Failed to login with refresh token!");
                e.printStackTrace();
                loggingIn = false;
            }
        });
    }

    public static void loginRefreshToken(String refreshToken) {
        if (loggingIn) {
            altsQueue.add(new AltQueue(refreshToken, AltTypes.Refresh_Token));
            return;
        }
        loggingIn = true;

        addProgressReport("Logging in with refresh token");
        ForkJoinPool.commonPool().execute(() -> {
            try {
                Session session = RefreshTokenAuth.authenticate(refreshToken);
                saveAccount(AltTypes.Refresh_Token, refreshToken, session);
                loginSession(session);
                loggingIn = false;
            } catch (Exception e) {
                setErrorMessage("Failed to login with refresh token: " + e.getMessage());
                e.printStackTrace();
                loggingIn = false;
            }
        });
    }

    private static boolean loggingIn = false;

    public static void loginCookie(String cookie) {
        if (loggingIn) {
            altsQueue.add(new AltQueue(cookie, AltTypes.Cookie));
            return;
        }
        loggingIn = true;

        ForkJoinPool.commonPool().execute(() -> {
            Cookies.authenticate(cookie);
            loggingIn = false;
        });
    }

    public static void loginCookie(File file) throws Exception {
        String cookie = Cookies.formatCookies(Cookies.getCookiesFromFile(file.getAbsolutePath()));
        if (cookie == null) return;

        loginCookie(cookie);
    }

    protected static final String VALID_NAME_CHARATERS_REGEX = "[a-zA-Z0-9]";
    protected static final String INVALID_NAME_CHARATERS_REGEX = "[^a-zA-Z0-9]";

    public static void addCracked(String name) {
        String invalidCharacters = name.replaceAll(VALID_NAME_CHARATERS_REGEX, "");
        if (!invalidCharacters.isEmpty()) {
            Login.setErrorMessage("Name contains invalid characters: \"" + invalidCharacters + "\" removing them");
            name = name.replaceAll(INVALID_NAME_CHARATERS_REGEX, "");
        }

        MinecraftBridge.from(C.mc).bridge$setSession(SessionBridge.from(new Session(name, "6", "7", "lol")));
        Login.addProgressReport("Username set to: §6" + name + "§f!");
    }

    public static boolean loginSession(String accessToken) {
        Session session = SessionUtil.queryPlayerProfile(accessToken);

        if (session.getToken().isEmpty()) {
            Login.setErrorMessage("Invalid session, cannot log in!");
            return false;
        }
        loginSession(session);

        saveAccount(AltTypes.Session, null, session);
        return true;
    }

    public static void loginSession(Session session) {
        MinecraftBridge.from(C.mc).bridge$setSession(SessionBridge.from(session));
        Login.addProgressReport("Successfully logged in to account: §6" + C.mc.getSession().getUsername() + "§f!");
        saveAccount(AltTypes.Session, null, session);
    }


    public static String getMinecraftAccessToken(String userHashcode, String XSTSaccessToken) {
        try {
            HttpPost req = new HttpPost("https://api.minecraftservices.com/authentication/login_with_xbox");
            req.setHeader("Content-Type", "application/json");
            req.setEntity(new StringEntity("{ \"identityToken\": \"XBL3.0 x="+userHashcode+";"+XSTSaccessToken+"\" }"));

            CloseableHttpResponse res = NetworkUtil.getServerResponse(req);
            JsonObject resJson = NetworkUtil.getAsJSONObject(res);

            // https://http.cat/status/429
            if (res.getStatusLine().getStatusCode() == 429) {
                setErrorMessage("Rate limited, please wait.");
                return null;
            }

            if (resJson.has("access_token"))
                return resJson.get("access_token").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setErrorMessage("No microsoft access token present!");
        return null;
    }

    public static void setErrorMessage(String message) {
        ChatUtil.prefixMessage("&4" + message);
        System.err.println(message);
    }

    public static void addProgressReport(String message) {
        ChatUtil.chat("&a" + message);
        System.out.println(message);
    }

    public static void saveAccount(AltTypes altType, String save, Session account) {
        if (!Files.exists(altsPath)) altsPath.toFile().mkdirs();

        Path accountPath = getAccountPath(account.getPlayerID());

        HashMap<String, String> json = new HashMap<>();
        if (Files.exists(accountPath)) {
            try {
                String fileText = FileUtils.readFileToString(accountPath.toFile());
                json = C.gson.fromJson(fileText, HashMap.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        json.put("name", account.getUsername());
        json.put("uuid", account.getPlayerID());
        json.put("session", account.getToken());

        if (!json.containsKey("unbanDate")) json.put("unbanDate", "now");
        if (!json.containsKey("nameChangeDate")) json.put("nameChangeDate", SessionUtil.checkLastNameChange(account.getToken()));

        if (altType == AltTypes.Microsoft) json.put("refreshToken", save);
        if (altType == AltTypes.Refresh_Token) {
            json.put("refreshToken", save);
            json.put("refreshTokenLogin", "true");
        }
        if (altType == AltTypes.Cookie) json.put("cookie", save);

        if (!Files.exists(accountPath)) {
            if (json.containsKey("refreshTokenLogin")) altType = AltTypes.Refresh_Token;
            else if (json.containsKey("cookie")) altType = AltTypes.Cookie;
            else if (json.containsKey("refreshToken")) altType = AltTypes.Microsoft;

            AltManagerScreen.alts.add(new Alt(account.getUsername(), account.getPlayerID(), altType, json));
        }

        updateAltJSON(account.getPlayerID(), json);
    }

    public static void updateAltJSON(String uuid, HashMap<String, String> json) {
        try {
            Files.write(getAccountPath(uuid), C.gson.toJson(json).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getAccountPath(String uuid) {
        return Paths.get(altsPath + "/" + uuid + ".json");
    }
}
