package controller.drive;

import model.Room;
import util.SimConstants;

/**
 * Serbest açılı hareket için sürekli çarpışma testi: robot dairesinin (yarıçap
 * {@code ROBOT_RADIUS}) herhangi bir engel hücresi dikdörtgeniyle (duvar/mobilya)
 * veya grid dışıyla kesişip kesişmediğini söyler.
 * <p>
 * Hareket commit edilmeden önce {@link #overlapsObstacle} ile sınanır; böylece
 * robot asla bir engelin içine giremez.
 */
public final class Physics {

    private Physics() { }

    /** (x,y) merkezli, ROBOT_RADIUS yarıçaplı daire bir engelle çakışıyor mu? */
    public static boolean overlapsObstacle(Room room, double x, double y, double radius) {
        double cell = SimConstants.CELL_SIZE;
        int cMin = (int) Math.floor((x - radius) / cell);
        int cMax = (int) Math.floor((x + radius) / cell);
        int rMin = (int) Math.floor((y - radius) / cell);
        int rMax = (int) Math.floor((y + radius) / cell);

        for (int r = rMin; r <= rMax; r++) {
            for (int c = cMin; c <= cMax; c++) {
                if (!room.inBounds(r, c)) {
                    return true; // grid dışı = engel
                }
                if (!room.cell(r, c).isObstacle()) {
                    continue;
                }
                double rectX = c * cell;
                double rectY = r * cell;
                double closestX = clampD(x, rectX, rectX + cell);
                double closestY = clampD(y, rectY, rectY + cell);
                double dx = x - closestX;
                double dy = y - closestY;
                if (dx * dx + dy * dy < radius * radius) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double clampD(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
