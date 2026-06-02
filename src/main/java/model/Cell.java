package model;

/**
 * Oda grid'indeki tek bir hucre. Saf veri + davranis; JavaFX'e bagimli degil.
 * <p>
 * Bir hucre bir {@link CellType} (zemin, duvar, mobilya, istasyon) tasir ve
 * zemin ise uzerinde bir {@link DirtType} kir bulundurabilir. Sivi/leke gibi
 * kirler tek seferde degil, belli bir sure boyunca temizlendigi icin hucre
 * temizlik ilerlemesini ({@link #cleaningProgress}) de tutar.
 */
public class Cell {

    private final int row;
    private final int col;
    private CellType type;

    private DirtType dirt;             // null => temiz
    private double cleaningProgress;   // 0.0 .. 1.0 (kismi temizlik durumu)
    private boolean visited;           // robotun en az bir kez ustunden gectigi
    private int visitCount;            // kac kez ustunden gecildi (kapsama isi haritasi)
    private SurfaceType surface = SurfaceType.HARD_FLOOR; // zemin yuzeyi (hali vb.)

    public Cell(int row, int col, CellType type) {
        this.row = row;
        this.col = col;
        this.type = type;
    }

    public int row() { return row; }
    public int col() { return col; }

    public CellType type() { return type; }
    public void setType(CellType type) { this.type = type; }

    public boolean isObstacle() { return type.isObstacle(); }
    public boolean isWalkable() { return !type.isObstacle(); }

    public DirtType dirt() { return dirt; }
    public boolean isDirty() { return dirt != null; }

    public double cleaningProgress() { return cleaningProgress; }
    public boolean isVisited() { return visited; }
    public int visitCount() { return visitCount; }
    public void markVisited() {
        this.visited = true;
        this.visitCount++;
    }

    public SurfaceType surface() { return surface; }
    public void setSurface(SurfaceType surface) { this.surface = surface; }
    public boolean isCarpet() { return surface == SurfaceType.CARPET; }

    /** Bu hucreye kir ekler (yalnizca zemin kirlenebilir). */
    public void setDirt(DirtType dirt) {
        if (type.isCleanable()) {
            this.dirt = dirt;
            this.cleaningProgress = 0.0;
        }
    }

    /**
     * Kiri kismi olarak temizler. {@code amount} 0..1 arasi bir orandir
     * (gecen sure / kirin toplam temizleme suresi).
     *
     * @return kir tamamen temizlendiyse true
     */
    public boolean applyCleaning(double amount) {
        if (dirt == null) {
            return false;
        }
        cleaningProgress = Math.min(1.0, cleaningProgress + amount);
        if (cleaningProgress >= 1.0) {
            dirt = null;
            cleaningProgress = 0.0;
            return true;
        }
        return false;
    }

    /** Hucreyi baslangic durumuna dondurur (kiri ve ilerlemeyi siler). */
    public void resetDirt() {
        this.dirt = null;
        this.cleaningProgress = 0.0;
        this.visited = false;
        this.visitCount = 0;
    }
}
