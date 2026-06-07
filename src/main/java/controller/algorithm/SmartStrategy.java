package controller.algorithm;

import controller.PathFinder;
import model.Direction;
import model.NavGrid;

import java.util.List;

/**
 * Akilli temizlik: robot her adimda <b>en yakin kirli hucreye</b> giden en kisa
 * yolu (BFS) hesaplar ve o yonun ilk adimini izler. Boylece kiri verimli sekilde,
 * gereksiz dolasmadan toplar. Erisilebilir kir kalmadiginda {@code null} doner;
 * bu durumda controller robotu istasyona donderip bitirir.
 */
public class SmartStrategy implements CleaningStrategy {

    @Override
    public String name() {
        return "Akıllı";
    }

    @Override
    public Direction chooseDirection(NavGrid grid, int row, int col, Direction current) {
        List<int[]> path = PathFinder.pathToNearestDirt(grid, row, col);
        if (path.isEmpty()) {
            return null;
        }
        int[] firstStep = path.get(0);
        int dr = firstStep[0] - row;
        int dc = firstStep[1] - col;
        for (Direction d : Direction.values()) {
            if (d.dRow() == dr && d.dCol() == dc) {
                return d;
            }
        }
        return null;
    }
}
