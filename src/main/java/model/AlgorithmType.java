package model;

/**
 * Kullanicinin secebilecegi temizlik (gezinme) algoritmalari.
 * Asil davranis controller.algorithm paketindeki Strategy siniflarinda
 * yer alir; bu enum sadece UI secimi ve fabrika anahtari olarak kullanilir.
 */
public enum AlgorithmType {
    RANDOM("Rastgele"),
    SPIRAL("Spiral"),
    WALL_FOLLOW("Duvar Takip"),
    SMART("Akıllı");

    private final String label;

    AlgorithmType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
