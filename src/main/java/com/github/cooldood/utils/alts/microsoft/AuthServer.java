package com.github.cooldood.utils.alts.microsoft;

import com.github.cooldood.utils.alts.Login;
import com.github.cooldood.utils.minecraft.ChatUtil;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://login.microsoftonline.com/consumers/oauth2/v2.0/logout
public class AuthServer {
    public static final String URL = "https://login.live.com/oauth20_authorize.srf" +
            "?client_id=" + MSAuth.clientId +
            "&response_type=code" +
            "&scope=XboxLive.signin%20XboxLive.offline_access" +
            "&prompt=select_account" +
            "&redirect_uri=http://localhost:"+MSAuth.port+"/";

    private final Pattern codePattern = Pattern.compile("\\?code=(.+?)$", 0);

    public AuthServer() {
        new Thread(this::start).start();
    }

    private void start() {
        try {
            HttpServer s = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", 59125),
                    0
            );
            s.createContext("/", exchange -> {
                OutputStream out = exchange.getResponseBody();
                exchange.sendResponseHeaders(200, "".getBytes().length);
                out.write("".getBytes());
                out.flush();
                out.close();

                Matcher m = codePattern.matcher(
                        exchange.getRequestURI().toString()
                );
                if (m.find()) {
                    String code = m.group(1);
                    try {
                        Login.addProgressReport("Logging into Microsoft account");
                        Login.loginSession(MSAuth.authWithCode(code));
                    } catch (Exception ex) {
                        ChatUtil.prefixMessage("&cFailed to login with Microsoft account.");
                        ex.printStackTrace();
                    }
                }
            });
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}