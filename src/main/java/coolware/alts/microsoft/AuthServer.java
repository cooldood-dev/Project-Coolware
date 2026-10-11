package coolware.alts.microsoft;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import coolware.alts.Alt;
import coolware.alts.AltManager;
import coolware.alts.AltType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthServer {
    public static final String AUTH_URL = "https://login.live.com/oauth20_authorize.srf" +
            "?client_id=" + MSAuth.CLIENT_ID +
            "&response_type=code" +
            "&scope=XboxLive.signin%20XboxLive.offline_access" +
            "&prompt=select_account" +
            "&redirect_uri=http://localhost:" + MSAuth.PORT + "/";

    private static final Pattern CODE_PATTERN = Pattern.compile("code=([^&]+)");
    private static HttpServer server;

    public static synchronized void startAndOpenBrowser() {
        stop();

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", Integer.parseInt(MSAuth.PORT)), 0);
            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String query = exchange.getRequestURI().getQuery();
                    String responseText = "<!DOCTYPE html><html><head><title>Coolware Login</title><style>body{background:#140e12;color:#fff;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}.card{background:#1e1218;padding:40px;border-radius:12px;border:1px solid #7d0a1e;text-align:center;box-shadow:0 8px 32px rgba(0,0,0,0.5);}h1{color:#e53935;margin-bottom:10px;}</style></head><body><div class=\"card\"><h1>Authentication Successful!</h1><p>You can now return to Minecraft.</p></div></body></html>";

                    exchange.sendResponseHeaders(200, responseText.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(responseText.getBytes());
                    os.flush();
                    os.close();

                    if (query != null) {
                        Matcher m = CODE_PATTERN.matcher(query);
                        if (m.find()) {
                            final String code = m.group(1);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        AltManager.getInstance().setStatus("\u00a7eAuthenticating with Microsoft...", false);
                                        Session session = MSAuth.authWithCode(code);
                                        if (session != null && !session.getToken().isEmpty()) {
                                            Minecraft.getMinecraft().setSession(session);
                                            AltManager.getInstance().addOrUpdateAlt(new Alt(session.getUsername(), session.getPlayerID(), AltType.MICROSOFT, session.getToken(), null));
                                            AltManager.getInstance().setStatus("\u00a7aLogged in as " + session.getUsername() + "!", false);
                                        } else {
                                            AltManager.getInstance().setStatus("\u00a7cInvalid session from Microsoft.", true);
                                        }
                                    } catch (Exception e) {
                                        AltManager.getInstance().setStatus("\u00a7cMicrosoft login failed: " + e.getMessage(), true);
                                        e.printStackTrace();
                                    } finally {
                                        stop();
                                    }
                                }
                            }).start();
                        }
                    }
                }
            });
            server.start();
            AltManager.getInstance().setStatus("\u00a7eBrowser opened for Microsoft Login...", false);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(AUTH_URL));
            } else {
                Runtime.getRuntime().exec("cmd /c start " + AUTH_URL);
            }
        } catch (Exception e) {
            AltManager.getInstance().setStatus("\u00a7cFailed to launch OAuth server: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception ignored) {}
            server = null;
        }
    }
}
