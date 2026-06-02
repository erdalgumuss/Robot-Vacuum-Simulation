package controller.algorithm;

import model.Direction;
import model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rastgele gezinme. Robot duvara/mobilyaya carptiginda yon degistirir.
 * <p>
 * Kapsamayi artirmak icin: once <b>ziyaret edilmemis</b> komsulari tercih eder
 * ve dogru orantili olarak mevcut yonde devam etme egilimi tasir (titremeyi
 * azaltir). Tum komsular ziyaret edilmisse, sikismamak icin ziyaret edilmis
 * komsulardan rastgele birini secer.
 */
public class RandomStrategy implements CleaningStrategy {

    private final Random random = new Random();

    @Override
    public String name() {
        return "Rastgele";
    }

    @Override
    public Direction chooseDirection(Room room, int row, int col, Direction current) {
        List<Direction> walkable = CleaningStrategy.walkableDirections(room, row, col);
        if (walkable.isEmpty()) {
            return null;
        }

        List<Direction> unvisited = new ArrayList<>();
        for (Direction d : walkable) {
            if (!CleaningStrategy.isVisited(room, row, col, d)) {
                unvisited.add(d);
            }
        }

        if (!unvisited.isEmpty()) {
            // %65 ihtimalle ileri devam (mumkunse), yoksa rastgele ziyaret edilmemis
            if (current != null && unvisited.contains(current) && random.nextDouble() < 0.65) {
                return current;
            }
            return unvisited.get(random.nextInt(unvisited.size()));
        }

        // Tum komsular ziyaretli: rastgele dolasip donguye girmek yerine
        // en yakin ziyaretsiz/kirli hucreye kac (donguyu kir, bitisi garanti et)
        return CleaningStrategy.escapeToNearestUncleaned(room, row, col);
    }
}
