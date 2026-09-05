package com.github.cooldood.utils.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class NetworkUtil {
    public static final CloseableHttpClient client =
            HttpClients.custom()
                    .disableAuthCaching()
                    .disableCookieManagement()
                    .disableRedirectHandling()
                    .build();

    // minecraft code i stole from GuiStreamUnavailable.func_152320_a
    public static void openBrowser(String websiteURL) {
        try {
            Desktop.getDesktop().browse(new URI(websiteURL));
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static CloseableHttpResponse getServerResponse(HttpRequestBase request) throws IOException {
        CloseableHttpResponse response = client.execute(request);
        response.close();

        return response;
    }

    public static JsonObject getJSONObject(HttpRequestBase request) throws IOException {
        return C.gson.fromJson(EntityUtils.toString(getServerResponse(request).getEntity()), JsonElement.class).getAsJsonObject();
    }

    public static JsonObject getAsJSONObject(CloseableHttpResponse request) throws IOException {
        return C.gson.fromJson(EntityUtils.toString(request.getEntity()), JsonElement.class).getAsJsonObject();
    }

    // the cookie login redirects to a link with a space in it.
    public static String fixURL(String url) {
        return url.replaceAll(" ", "%20");
    }
}
