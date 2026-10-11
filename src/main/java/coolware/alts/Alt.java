package coolware.alts;

import java.util.HashMap;
import java.util.Map;

public class Alt {
    private String username;
    private String uuid;
    private AltType type;
    private String token; // Access Token, Refresh Token, or Cookie
    private Map<String, String> data;

    public Alt(String username, String uuid, AltType type) {
        this(username, uuid, type, "", new HashMap<String, String>());
    }

    public Alt(String username, String uuid, AltType type, String token, Map<String, String> data) {
        this.username = username;
        this.uuid = uuid;
        this.type = type;
        this.token = token;
        this.data = data != null ? data : new HashMap<String, String>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public AltType getType() {
        return type;
    }

    public void setType(AltType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
