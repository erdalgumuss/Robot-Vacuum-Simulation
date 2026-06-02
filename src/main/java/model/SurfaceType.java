package model;

/**
 * Bir zemin hücresinin yüzey tipi (yapısal {@link CellType}'tan ayrı, ortogonal).
 * Robot bu özelliği <b>göremez</b>; yalnızca halıda artan motor direncinden
 * (yük/akım) dolaylı olarak "hisseder" ve kendi belleğine öğrenir.
 */
public enum SurfaceType {
    HARD_FLOOR,
    CARPET
}
