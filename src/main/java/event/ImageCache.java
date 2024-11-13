package event;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final Map<String, WeakReference<Image>> cache = new ConcurrentHashMap<>();

    public static Image getImage(String path) {
        WeakReference<Image> ref = cache.get(path);
        Image img = (ref != null) ? ref.get() : null;

        if (img == null) {
            try {
                img = ImageIO.read(ImageCache.class.getResource(path));
                cache.put(path, new WeakReference<>(img));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return img;
    }

    public static void cleanup() {
        cache.clear();
    }
}