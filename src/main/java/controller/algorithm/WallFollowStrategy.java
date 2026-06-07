package controller.algorithm;

import model.Direction;
import model.NavGrid;

/**
 * Duvar takibi (sol-el kurali). Robot daima sol tarafindaki engeli/duvari
 * takip eder; bu klasik labirent gezme yontemi odanin cevresini ve girintilerini
 * sistematik olarak kapsar.
 * <p>
 * Tercih sirasi: once <b>sola don</b> (duvari yokla), sonra <b>duz</b>, sonra
 * <b>saga don</b>, en son <b>geri</b>. Ilk yuruyebilen yon secilir.
 */
public class WallFollowStrategy implements CleaningStrategy {

    @Override
    public String name() {
        return "Duvar Takip";
    }

    @Override
    public Direction chooseDirection(NavGrid grid, int row, int col, Direction current) {
        Direction base = (current != null) ? current : Direction.EAST;
        Direction[] preference = {
                base.turnLeft(),
                base,
                base.turnRight(),
                base.opposite()
        };
        // 1) Sol-el kuralina gore ziyaretsiz ilk yon (duvari takip ederek yeni alana)
        for (Direction d : preference) {
            if (CleaningStrategy.canGo(grid, row, col, d)
                    && !CleaningStrategy.isVisited(grid, row, col, d)) {
                return d;
            }
        }
        // 2) Yerel olarak sikisildi: en yakin ziyaretsiz/kirli hucreye kac
        return CleaningStrategy.escapeToNearestUncleaned(grid, row, col);
    }
}
