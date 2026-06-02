package model;

/**
 * Bir grid hucresinin temel turu.
 * <ul>
 *   <li>{@link #FLOOR} - robotun gezebildigi, kirlenebilen normal zemin</li>
 *   <li>{@link #WALL} - odanin dis sinirlari; gecilemez</li>
 *   <li>{@link #FURNITURE} - mobilya/masa gibi engeller; gecilemez</li>
 *   <li>{@link #STATION} - sarj istasyonu; robotun donus hedefi</li>
 * </ul>
 */
public enum CellType {
    FLOOR,
    WALL,
    FURNITURE,
    STATION;

    /** WALL ve FURNITURE robot tarafindan gecilemez. */
    public boolean isObstacle() {
        return this == WALL || this == FURNITURE;
    }

    /** Sadece zemin kirlenebilir / temizlenebilir. */
    public boolean isCleanable() {
        return this == FLOOR;
    }
}
