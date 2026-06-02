package model;

/**
 * Desteklenen kir turleri ve her birinin temizleme maliyeti.
 * <p>
 * Odev geregi en az uc tip olmali ve farkli surelerde temizlenmeli:
 * toz hizli, sivi ve leke daha yavas. Ayrica her tip farkli miktarda
 * batarya tuketir.
 *
 * @param cleaningSeconds robotun bu kiri temizlemek icin hucrede gecirdigi sure (sn)
 * @param batteryCost     temizleme sirasinda harcanan ek batarya (%)
 * @param colorHex        view katmaninda kullanilacak renk (model UI'a bagimli degil,
 *                        sadece bir ipucu olarak hex string tutulur)
 */
public enum DirtType {
    DUST("Toz", 0.8, 1.0, "#C9A227"),
    LIQUID("Sıvı", 2.5, 3.0, "#3B82F6"),
    STAIN("Leke", 4.0, 5.0, "#7C3AED");

    private final String label;
    private final double cleaningSeconds;
    private final double batteryCost;
    private final String colorHex;

    DirtType(String label, double cleaningSeconds, double batteryCost, String colorHex) {
        this.label = label;
        this.cleaningSeconds = cleaningSeconds;
        this.batteryCost = batteryCost;
        this.colorHex = colorHex;
    }

    public String label() { return label; }
    public double cleaningSeconds() { return cleaningSeconds; }
    public double batteryCost() { return batteryCost; }
    public String colorHex() { return colorHex; }
}
