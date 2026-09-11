package com.github.cooldood.modules.impl.client;

import com.github.cooldood.events.SubscribeEvent;
import com.github.cooldood.events.impl.PacketEvent;
import com.github.cooldood.events.impl.RenderTickEvent;
import com.github.cooldood.modules.Category;
import com.github.cooldood.modules.Module;
import com.github.cooldood.modules.RegisterModule;
import com.github.cooldood.modules.RegisterSubModule;
import com.github.cooldood.utils.client.C;
import com.github.cooldood.utils.client.NetworkUtil;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.util.CryptManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.concurrent.ForkJoinPool;

@RegisterModule(
        name = "Anti Rat",
        description = "Provides Anti Rat functionality for the client.",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class AntiRat extends Module {
    @RegisterSubModule(name = "Outgoing Requests", min = 1, max = 10)
    public static int maxOutgoingRequests = 3;

    private static int outgoingRequests = 0;

    @SubscribeEvent
    public static void rateLimitLogin(RenderTickEvent event) {
        if (!C.isInGame() || C.mc.isSingleplayer() || serverPublicKey == null) return;

        // never ending requests.
        rateLimitLogin();
    }

    private static String serverID;
    private static PublicKey serverPublicKey;

    @SubscribeEvent
    public static void onPacketEvent(PacketEvent.Receive event) {
        if (C.mc.isSingleplayer()) return;

        if (event.packet instanceof S01PacketEncryptionRequest) {
            S01PacketEncryptionRequest packet = (S01PacketEncryptionRequest) event.packet;
            serverID = packet.getServerId();
            serverPublicKey = packet.getPublicKey();
        }
    }

    private static int attemptLogin() {
        try {
            if (!C.isInGame()) return 429;

            HttpPost req = new HttpPost("https://sessionserver.mojang.com/session/minecraft/join");
            req.setHeader("Content-Type", "application/json");

            String accessToken = C.mc.getSession().getToken();
            String selectedProfile = C.mc.getSession().getPlayerID();

            String serverId = generateServerId(serverID, serverPublicKey, CryptManager.createNewSharedKey());
            req.setEntity(new StringEntity("{ \"accessToken\": \""+accessToken+"\", \"selectedProfile\": \""+selectedProfile+"\", \"serverId\": \""+serverId+"\"}"));

            CloseableHttpResponse res = NetworkUtil.getServerResponse(req);

            return res.getStatusLine().getStatusCode();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 429;
    }

    public static String generateServerId(String baseServerId,   // Base server ID, usually an empty string""
                                          PublicKey publicKey,   // Server's RSA public key
                                          SecretKey secretKey    // The symmetric AES secret key used between server and client
    ) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(baseServerId.getBytes("ISO_8859_1"));
        messageDigest.update(secretKey.getEncoded());
        messageDigest.update(publicKey.getEncoded());
        byte[] digestData = messageDigest.digest();
        return new BigInteger(digestData).toString(16);
    }

    private static void rateLimitLogin() {
        ForkJoinPool.commonPool().execute(() -> {
            while (C.isInGame()) {
                attemptLogin();
            }
        });
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}
