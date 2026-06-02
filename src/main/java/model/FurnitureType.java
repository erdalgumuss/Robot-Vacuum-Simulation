package model;

/**
 * İsimli mobilya türleri: her biri bir <b>footprint</b> (satır×sütun hücre) ve
 * bir sprite asset adı taşır. Böylece kullanıcı/oda düzeni belirli bir parçayı
 * (kanepe, yemek masası vb.) tam konumuna yerleştirebilir; çizim katmanı doğru
 * görseli footprint'e oturtur.
 * <p>
 * footprint (rows×cols), sprite dosyasının en-boy oranıyla uyumludur:
 * örn. kanepe 256×128 → 1×2 (yatay), yemek masası 256×256 → 2×2.
 */
public enum FurnitureType {
    SOFA(1, 2, "sofa", "Kanepe"),
    COFFEE_TABLE(1, 2, "coffee_table", "Sehpa"),
    DINING_TABLE(2, 2, "dining_table", "Yemek Masası"),
    BED(2, 2, "bed", "Yatak"),
    ARMCHAIR(1, 1, "armchair", "Koltuk"),
    PLANT(1, 1, "plant_large", "Bitki");

    private final int rows;
    private final int cols;
    private final String asset;
    private final String label;

    FurnitureType(int rows, int cols, String asset, String label) {
        this.rows = rows;
        this.cols = cols;
        this.asset = asset;
        this.label = label;
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
    public String asset() { return asset; }
    public String label() { return label; }
}
