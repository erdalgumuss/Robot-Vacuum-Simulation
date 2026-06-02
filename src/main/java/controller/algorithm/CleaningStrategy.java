package controller.algorithm;

import controller.PathFinder;
import model.Direction;
import model.Room;

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
     * @param room       oda modeli (komsu yuruyebilirlik/ziyaret sorgusu icin)
     * @param row        robotun bulundugu hucre satiri
     * @param col        robotun bulundugu hucre sutunu
     * @param current    robotun mevcut yonu (ilk adimda null olabilir)
     * @return secilen yon; yuruyebilen komsu yoksa {@code null}
     */
    Direction chooseDirection(Room room, int row, int col, Direction current);

    /** Algoritmanin ic durumunu sifirlar (yeni simulasyon basinda cagrilir). */
    default void reset() { }

    // --- Ortak yardimcilar ---

    /** Belirtilen yondeki komsu hucre yuruyebilir mi? */
    static boolean canGo(Room room, int row, int col, Direction d) {
        return room.isWalkable(row + d.dRow(), col + d.dCol());
    }

    /** Belirtilen yondeki komsu hucre daha once ziyaret edilmis mi? */
    static boolean isVisited(Room room, int row, int col, Direction d) {
        int nr = row + d.dRow();
        int nc = col + d.dCol();
        return room.inBounds(nr, nc) && room.cell(nr, nc).isVisited();
    }

    /** Yuruyebilen tum komsu yonler. */
    static List<Direction> walkableDirections(Room room, int row, int col) {
        List<Direction> result = new ArrayList<>(4);
        for (Direction d : Direction.values()) {
            if (canGo(room, row, col, d)) {
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
    static Direction escapeToNearestUncleaned(Room room, int row, int col) {
        return firstStep(PathFinder.pathToNearestUncleaned(room, row, col), row, col);
    }
}
