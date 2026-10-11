package coolware.util.render.img;

import net.minecraft.util.ResourceLocation;

import java.util.HashMap;

public final class ImageManager {
    public static HashMap<String, ImageObject> inGameImages = new HashMap<String, ImageObject>(){{
        put("info", new ImageObject(new ResourceLocation("dogclient/icons/info.png")));
    }};

    public static void loadInGameImages() {
        inGameImages.values().forEach(ImageObject::loadAsync);
    }

    public static void unloadInGameImages() {
        inGameImages.values().forEach(ImageObject::unload);
    }
}
