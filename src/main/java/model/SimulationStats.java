package model;

/**
 * Simulasyonun canli istatistikleri. View katmani bu degerleri okuyup
 * gercek zamanli gosterir (temizlenen alan %, kalan kirli alan, gecen sure,
 * toplanan kir vb.).
 * <p>
 * Degerler controller tarafindan her tikte guncellenir.
 */
public class SimulationStats {

    private int totalFloorCells;     // temizlenebilir toplam hucre
    private int initialDirtyCells;   // simulasyon basindaki kirli hucre sayisi
    private int cleanedDirtCount;    // simdiye kadar temizlenen kir adedi
    private long elapsedMillis;      // gecen sure (ms)

    public void reset(int totalFloorCells, int initialDirtyCells) {
        this.totalFloorCells = totalFloorCells;
        this.initialDirtyCells = initialDirtyCells;
        this.cleanedDirtCount = 0;
        this.elapsedMillis = 0;
    }

    public void incrementCleaned() {
        cleanedDirtCount++;
    }

    public void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
    }

    public int totalFloorCells() { return totalFloorCells; }
    public int initialDirtyCells() { return initialDirtyCells; }
    public int cleanedDirtCount() { return cleanedDirtCount; }
    public long elapsedMillis() { return elapsedMillis; }

    public int remainingDirtyCount() {
        return Math.max(0, initialDirtyCells - cleanedDirtCount);
    }

    /** Baslangictaki kirin temizlenen yuzdesi (0..100). */
    public double cleanedPercentage() {
        if (initialDirtyCells == 0) {
            return 100.0;
        }
        return 100.0 * cleanedDirtCount / initialDirtyCells;
    }

    /** "mm:ss" biciminde gecen sure. */
    public String elapsedFormatted() {
        long totalSeconds = elapsedMillis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
