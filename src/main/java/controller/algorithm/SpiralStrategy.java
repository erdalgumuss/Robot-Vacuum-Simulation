package controller.algorithm;

import model.Direction;
import model.NavGrid;

/**
 * Spiral gezinme. Tutarli bir donme egilimiyle (saga yonelme onceligi) ice
 * dogru sarmal bir kapsama deseni olusturur.
 * <p>
 * Tercih sirasi: once <b>saga don</b>, sonra <b>duz</b>, sonra <b>sola don</b>,
 * en son <b>geri</b>. Her adimda once ziyaret edilmemis yonler denenir; tum
 * yonler ziyaret edilmisse ayni sirayla yuruyebilen ilk yon secilir (sikismayi
 * onler ve sarmal disina cikip yeni alana gecer).
 */
public class SpiralStrategy implements CleaningStrategy {

    @Override
    public String name() {
        return "Spiral";
    }

    @Override
    public Direction chooseDirection(NavGrid grid, int row, int col, Direction current) {
        Direction base = (current != null) ? current : Direction.EAST;
        Direction[] preference = {
                base.turnRight(),
                base,
                base.turnLeft(),
                base.opposite()
        };

        // 1) Ziyaret edilmemis ve yuruyebilen ilk yon (sarmal karakteri korunur)
        for (Direction d : preference) {
            if (CleaningStrategy.canGo(grid, row, col, d)
                    && !CleaningStrategy.isVisited(grid, row, col, d)) {
                return d;
            }
        }
        // 2) Yerel olarak sikisildi: en yakin ziyaretsiz/kirli hucreye kac (donguyu kir)
        return CleaningStrategy.escapeToNearestUncleaned(grid, row, col);
    }
}
