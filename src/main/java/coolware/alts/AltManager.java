package coolware.alts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import coolware.alts.microsoft.AuthServer;
import coolware.alts.microsoft.Cookies;
import coolware.alts.microsoft.MSAuth;
import coolware.alts.microsoft.RefreshTokenAuth;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AltManager {
    private static AltManager instance;
    private final List<Alt> alts = new ArrayList<Alt>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File altsDir = new File(Minecraft.getMinecraft().mcDataDir, "coolware/alts");
    private final File altsFile = new File(altsDir, "alts.json");

    private String statusMessage = "\u00a77Idle";
    private boolean isError = false;
    private long statusTime = 0;

    public static synchronized AltManager getInstance() {
        if (instance == null) {
            instance = new AltManager();
        }
        return instance;
    }

    public AltManager() {
        loadAlts();
    }

    public List<Alt> getAlts() {
        return alts;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public boolean isError() {
        return isError;
    }

    public void setStatus(String message, boolean error) {
        this.statusMessage = message;
        this.isError = error;
        this.statusTime = System.currentTimeMillis();
    }

    public void loadAlts() {
        alts.clear();
        if (!altsDir.exists()) {
            altsDir.mkdirs();
        }
        if (altsFile.exists()) {
            try {
                String json = FileUtils.readFileToString(altsFile, "UTF-8");
                if (json != null && !json.trim().isEmpty()) {
                    Alt[] loaded = gson.fromJson(json, Alt[].class);
                    if (loaded != null) {
                        for (Alt a : loaded) {
                            if (a != null) alts.add(a);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAlts() {
        try {
            if (!altsDir.exists()) altsDir.mkdirs();
            FileUtils.writeStringToFile(altsFile, gson.toJson(alts.toArray(new Alt[0])), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addOrUpdateAlt(Alt alt) {
        for (int i = 0; i < alts.size(); i++) {
            if (alts.get(i).getUsername().equalsIgnoreCase(alt.getUsername())) {
                alts.set(i, alt);
                saveAlts();
                return;
            }
        }
        alts.add(alt);
        saveAlts();
    }

    public void removeAlt(Alt alt) {
        alts.remove(alt);
        saveAlts();
    }

    // 1. Cracked Login
    public void loginCracked(final String username) {
        if (username == null || username.trim().isEmpty()) {
            setStatus("\u00a7cUsername cannot be empty!", true);
            return;
        }
        final String cleanName = username.trim().replaceAll("[^a-zA-Z0-9_]", "");
        if (cleanName.isEmpty()) {
            setStatus("\u00a7cInvalid characters in username!", true);
            return;
        }

        Session session = new Session(cleanName, "0", "-", "legacy");
        Minecraft.getMinecraft().setSession(session);
        addOrUpdateAlt(new Alt(cleanName, "0", AltType.CRACKED));
        setStatus("\u00a7aLogged in as Cracked: " + cleanName, false);
    }

    // 2. Session Token Login
    public void loginSessionToken(final String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            setStatus("\u00a7cSession token cannot be empty!", true);
            return;
        }
        setStatus("\u00a7eValidating Session Token...", false);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Session session = SessionUtil.queryPlayerProfile(sessionToken.trim());
                    if (session != null && !session.getToken().isEmpty()) {
                        Minecraft.getMinecraft().setSession(session);
                        addOrUpdateAlt(new Alt(session.getUsername(), session.getPlayerID(), AltType.SESSION, sessionToken.trim(), null));
                        setStatus("\u00a7aLogged in as " + session.getUsername() + "!", false);
                    } else {
                        setStatus("\u00a7cInvalid session token / profile not found.", true);
                    }
                } catch (Exception e) {
                    setStatus("\u00a7cSession login error: " + e.getMessage(), true);
                    e.printStackTrace();
                }
            }
        });
    }

    // 3. Refresh Token Login
    public void loginRefreshToken(final String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            setStatus("\u00a7cRefresh token cannot be empty!", true);
            return;
        }
        setStatus("\u00a7eAuthenticating Refresh Token...", false);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Session session = RefreshTokenAuth.authenticate(refreshToken.trim());
                    if (session != null && !session.getToken().isEmpty()) {
                        Minecraft.getMinecraft().setSession(session);
                        addOrUpdateAlt(new Alt(session.getUsername(), session.getPlayerID(), AltType.REFRESH_TOKEN, refreshToken.trim(), null));
                        setStatus("\u00a7aLogged in as " + session.getUsername() + "!", false);
                    } else {
                        setStatus("\u00a7cCould not authenticate with refresh token.", true);
                    }
                } catch (Exception e) {
                    setStatus("\u00a7cRefresh token error: " + e.getMessage(), true);
                    e.printStackTrace();
                }
            }
        });
    }

    // 4. Microsoft Login
    public void loginMicrosoft() {
        AuthServer.startAndOpenBrowser();
    }

    // 5. Cookie Login
    public void loginCookie(final String cookieText) {
        if (cookieText == null || cookieText.trim().isEmpty()) {
            setStatus("\u00a7cCookie data cannot be empty!", true);
            return;
        }
        setStatus("\u00a7eAuthenticating with Cookie...", false);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Session session = Cookies.authenticate(cookieText.trim());
                    if (session != null && !session.getToken().isEmpty()) {
                        Minecraft.getMinecraft().setSession(session);
                        addOrUpdateAlt(new Alt(session.getUsername(), session.getPlayerID(), AltType.COOKIE, cookieText.trim(), null));
                        setStatus("\u00a7aLogged in as " + session.getUsername() + "!", false);
                    } else {
                        setStatus("\u00a7cCookie authentication failed.", true);
                    }
                } catch (Exception e) {
                    setStatus("\u00a7cCookie login error: " + e.getMessage(), true);
                    e.printStackTrace();
                }
            }
        });
    }

    // Log in with existing alt
    public void loginAlt(final Alt alt) {
        if (alt == null) return;
        switch (alt.getType()) {
            case CRACKED:
                loginCracked(alt.getUsername());
                break;
            case SESSION:
                loginSessionToken(alt.getToken());
                break;
            case REFRESH_TOKEN:
                loginRefreshToken(alt.getToken());
                break;
            case COOKIE:
                loginCookie(alt.getToken());
                break;
            case MICROSOFT:
                if (alt.getToken() != null && !alt.getToken().isEmpty()) {
                    loginRefreshToken(alt.getToken());
                } else {
                    loginMicrosoft();
                }
                break;
        }
    }
}
