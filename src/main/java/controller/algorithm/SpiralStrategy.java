package controller.algorithm;

import controller.PathFinder;
import model.Direction;
import model.NavGrid;

/**
 * Spiral gezinme — robot süpürgelerin ikonik dışa-doğru sarmalı.
 * <p>
 * İki fazlı çalışır:
 * <ol>
 *   <li><b>Açık alana git (SEEK):</b> en geniş boş (temizlenmemiş) bölgenin
 *       merkezine en kısa yoldan adım adım ilerler. İstasyon köşede olsa bile
 *       sarmal, dağınık değil açık bir noktadan başlar.</li>
 *   <li><b>Dışa-sarmal (SPIRAL):</b> merkeze varınca büyüyen bir kare spiral
 *       çizer: bacak uzunlukları 1,1,2,2,3,3,... artar ve her bacakta sola döner
 *       (1 doğu, 1 kuzey, 2 batı, 2 güney, 3 doğu ...).</li>
 * </ol>
 * Spiral bir engele/duvara ya da temizlenmiş bir hücreye dayanıp kapanınca robot
 * yeni bir açık alan arar ve orada yeni bir spiral başlatır (gerçek Roomba gibi:
 * spiral → engel → başka yerde yeni spiral). Açık alan kalmadığında en yakın
 * temizlenmemiş hücreye yönelerek kalanları toplar; hiç kalmayınca {@code null}
 * döner (controller robotu istasyona döndürüp bitirir).
 */
public class SpiralStrategy implements CleaningStrategy {

    private enum Phase { SEEK, SPIRAL }

    private static final int MAX_CLEARANCE = 4; // spiral merkezi ararken bakılan en büyük yarıçap

    private Phase phase = Phase.SEEK;
    private int centerRow = -1;
    private int centerCol = -1;

    // Dışa-doğru kare spiral durumu
    private Direction dir;
    private int legLength;
    private int stepsLeft;
    private int legParity;

    @Override
    public String name() {
        return "Spiral";
    }

    @Override
    public void reset() {
        phase = Phase.SEEK;
        centerRow = -1;
        centerCol = -1;
    }

    @Override
    public Direction chooseDirection(NavGrid grid, int row, int col, Direction current) {
        // En fazla iki tur: SPIRAL kapanırsa SEEK'e düşüp yeni alan arar.
        for (int attempt = 0; attempt < 2; attempt++) {
            if (phase == Phase.SEEK) {
                // Hedef merkez yok / temizlendi / artık yürünemez -> yeni açık merkez seç
                if (centerRow < 0 || grid.isVisited(centerRow, centerCol)
                        || !grid.isWalkable(centerRow, centerCol)) {
                    pickOpenCenter(grid, row, col);
                }
                if (centerRow < 0) {
                    return CleaningStrategy.escapeToNearestUncleaned(grid, row, col); // açık alan yok
                }
                if (row != centerRow || col != centerCol) {
                    // Merkeze doğru bir adım (her çağrıda yeniden hesaplanır -> tek adım)
                    Direction step = CleaningStrategy.firstStep(
                            PathFinder.shortestPath(grid, row, col, centerRow, centerCol), row, col);
                    if (step != null) {
                        return step;
                    }
                    centerRow = -1; // ulaşılamıyor -> bırak, kalanları topla
                    return CleaningStrategy.escapeToNearestUncleaned(grid, row, col);
                }
                // Merkeze vardık -> dışa-doğru spirale geç
                startSpiral(current);
                phase = Phase.SPIRAL;
            }

            Direction step = spiralStep(grid, row, col);
            if (step != null) {
                return step;
            }
            // Spiral kapandı -> yeni açık alan ara (döngü ikinci turu)
            phase = Phase.SEEK;
            centerRow = -1;
        }
        return CleaningStrategy.escapeToNearestUncleaned(grid, row, col);
    }

    /** Dışa-doğru kare spiralin bir sonraki adımı; spiral kapandıysa {@code null}. */
    private Direction spiralStep(NavGrid grid, int row, int col) {
        if (stepsLeft <= 0) {
            dir = dir.turnLeft();
            if (++legParity == 2) {
                legParity = 0;
                legLength++;
            }
            stepsLeft = legLength;
        }
        if (CleaningStrategy.canGo(grid, row, col, dir)
                && !CleaningStrategy.isVisited(grid, row, col, dir)) {
            stepsLeft--;
            return dir;
        }
        return null;
    }

    private void startSpiral(Direction current) {
        dir = (current != null) ? current : Direction.EAST;
        legLength = 1;
        stepsLeft = 1;
        legParity = 0;
    }

    /**
     * En geniş açık (yürünebilir + temizlenmemiş) bölgenin merkezini seçer:
     * her temizlenmemiş hücre için etrafındaki en büyük tam-boş kareyi (clearance)
     * ölçer, en büyüğü (eşitlikte en yakını) merkez yapar. Aday yoksa center = -1.
     */
    private void pickOpenCenter(NavGrid grid, int row, int col) {
        int bestClear = -1, bestDist = Integer.MAX_VALUE, br = -1, bc = -1;
        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                if (grid.isVisited(r, c) || !grid.isWalkable(r, c)) {
                    continue; // yalnız temizlenmemiş, yürünebilir hücreler aday
                }
                int clr = clearance(grid, r, c);
                int dist = Math.abs(r - row) + Math.abs(c - col);
                if (clr > bestClear || (clr == bestClear && dist < bestDist)) {
                    bestClear = clr;
                    bestDist = dist;
                    br = r;
                    bc = c;
                }
            }
        }
        centerRow = br;
        centerCol = bc;
    }

    /** (r,c) etrafında tamamı yürünebilir + temizlenmemiş olan en büyük kare yarıçapı. */
    private int clearance(NavGrid grid, int r, int c) {
        for (int k = 1; k <= MAX_CLEARANCE; k++) {
            for (int dr = -k; dr <= k; dr++) {
                for (int dc = -k; dc <= k; dc++) {
                    int nr = r + dr, nc = c + dc;
                    if (!grid.inBounds(nr, nc) || !grid.isWalkable(nr, nc) || grid.isVisited(nr, nc)) {
                        return k - 1;
                    }
                }
            }
        }
        return MAX_CLEARANCE;
    }
}
