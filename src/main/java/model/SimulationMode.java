package model;

/**
 * Simülasyonun çalışma modu.
 * <ul>
 *   <li>{@link #GOD} — Tanrı Modu: robot tüm odayı bilir (omniscient), strateji
 *       seçimiyle verimli temizler.</li>
 *   <li>{@link #REALISTIC} — Gerçekçi Mod: robot ortamı bilmez; yalnızca
 *       sensörlerinden öğrenip kendi iç haritasını kurar ve serbest açılı gezer.</li>
 * </ul>
 */
public enum SimulationMode {
    GOD("Tanrı Modu"),
    REALISTIC("Gerçekçi Mod");

    private final String label;

    SimulationMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
