package controller.algorithm;

import controller.PathFinder;
import model.Direction;
import model.NavGrid;

import java.util.ArrayList;
import java.util.List;

/**
 * Robotun gezinme davranisini soyutlayan Strategy arayuzu.
 * <p>
 * Her algoritma (Rastgele, Spiral, Duvar Takip) bu arayuzu uygular. Robot bir
 * hucre merkezine vardiginda, bir sonraki hareket yonunu secmek icin
 * {@link #chooseDirection} cagrilir. Bu sayede yeni bir algoritma eklemek tek
 * bir sinif yazmak kadar kolaydir (Acik/Kapali ilkesi).
 */
public interface CleaningStrategy {

    String name();

    /**
     * Robotun bulundugu hucreden sonraki gidecegi yonu secer.
     *
     * @param grid       gezilen grid: gercek oda (Tanri Modu) veya robotun ic
     *                   haritasi (Gercekci Mod) — bkz. {@link NavGrid}
     * @param row        robotun bulundugu hucre satiri
     * @param col        robotun bulundugu hucre sutunu
     * @param current    robotun mevcut yonu (ilk adimda null olabilir)
     * @return secilen yon; yuruyebilen komsu yoksa {@code null}
     */
    Direction chooseDirection(NavGrid grid, int row, int col, Direction current);

    /** Algoritmanin ic durumunu sifirlar (yeni simulasyon basinda cagrilir). */
    default void reset() { }

    // --- Ortak yardimcilar ---

    /** Belirtilen yondeki komsu hucre yuruyebilir mi? */
    static boolean canGo(NavGrid grid, int row, int col, Direction d) {
        return grid.isWalkable(row + d.dRow(), col + d.dCol());
    }

    /** Belirtilen yondeki komsu hucre daha once ziyaret edilmis mi? */
    static boolean isVisited(NavGrid grid, int row, int col, Direction d) {
        int nr = row + d.dRow();
        int nc = col + d.dCol();
        return grid.inBounds(nr, nc) && grid.isVisited(nr, nc);
    }

    /** Yuruyebilen tum komsu yonler. */
    static List<Direction> walkableDirections(NavGrid grid, int row, int col) {
        List<Direction> result = new ArrayList<>(4);
        for (Direction d : Direction.values()) {
            if (canGo(grid, row, col, d)) {
                result.add(d);
            }
        }
        return result;
    }

    /** Bir BFS yolunun ilk adimini {@link Direction}'a cevirir (kacis icin). */
    static Direction firstStep(List<int[]> path, int row, int col) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        int[] s = path.get(0);
        int dr = s[0] - row;
        int dc = s[1] - col;
        for (Direction d : Direction.values()) {
            if (d.dRow() == dr && d.dCol() == dc) {
                return d;
            }
        }
        return null;
    }

    /**
     * Ortak kacis: yerel olarak sikisinca (tum komsular ziyaretli) en yakin
     * ziyaretsiz/kirli hucreye BFS ile yonelir. Donguyu kirar, bitisi garanti eder.
     */
    static Direction escapeToNearestUncleaned(NavGrid grid, int row, int col) {
        return firstStep(PathFinder.pathToNearestUncleaned(grid, row, col), row, col);
    }
}
