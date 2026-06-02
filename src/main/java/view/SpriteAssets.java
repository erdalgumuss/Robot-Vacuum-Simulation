package view;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import model.DirtType;

/**
 * Optional PNG asset loader for the premium visual layer.
 * Missing files are expected during development; callers receive null and keep
 * using Canvas fallbacks.
 */
final class SpriteAssets {

    private final Map<String, Image> cache = new HashMap<>();
    private final EnumMap<DirtType, Image> dirtSprites = new EnumMap<>(DirtType.class);

    SpriteAssets() {
        dirtSprites.put(DirtType.DUST, load("/png/dirt/dirt_dust.png"));
        dirtSprites.put(DirtType.LIQUID, load("/png/dirt/dirt_liquid.png"));
        dirtSprites.put(DirtType.STAIN, load("/png/dirt/dirt_stain.png"));
    }

    Image floorTile() {
        return load("/png/floor/floor_tile.png");
    }

    Image wallTile() {
        return load("/png/floor/wall_tile.png");
    }

    Image rug() {
        return load("/png/floor/rug.png");
    }

    Image robot(boolean cleaning) {
        if (cleaning) {
            Image cleaningSprite = load("/png/robot/robot_cleaning.png");
            if (cleaningSprite != null) {
                return cleaningSprite;
            }
        }
        return load("/png/robot/robot.png");
    }

    Image chargingStation() {
        return load("/png/charging_station.png");
    }

    Image dirt(DirtType type) {
        return dirtSprites.get(type);
    }

    /** İsimli mobilya sprite'ı (örn. "sofa" -> /png/furniture/sofa.png). Yoksa null. */
    Image furnitureByName(String asset) {
        return load("/png/furniture/" + asset + ".png");
    }

    private Image load(String path) {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }
        Image image = null;
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in != null) {
                image = new Image(in);
            }
        } catch (Exception ignored) {
            image = null;
        }
        cache.put(path, image);
        return image;
    }
}
