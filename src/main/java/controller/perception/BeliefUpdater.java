package controller.perception;

import model.KnownMap;
import model.SensorReading;
import util.SimConstants;

/**
 * Sensör okumasını robotun iç haritasına (belief map) işler.
 * <p>
 * Tek kural: harita YALNIZCA bir ışının dokunduğu hücreler için güncellenir.
 * Bir ışın boyunca, çarpışmadan önceki hücreler {@code FREE}, çarptığı hücre
 * {@code OBSTACLE} işaretlenir; robotun altındaki hücre {@code FREE} olur.
 * Hiçbir ışının uğramadığı hücre {@code UNKNOWN} kalır (robot orayı görmedi).
 */
public final class BeliefUpdater {

    private BeliefUpdater() { }

    public static void integrate(KnownMap map, double robotX, double robotY, SensorReading reading) {
        if (map == null || reading == null) {
            return;
        }
        double cell = SimConstants.CELL_SIZE;
        double radius = SimConstants.ROBOT_RADIUS;
        double step = SimConstants.RAY_STEP;
        double range = SimConstants.SENSOR_RANGE;

        // Robotun altindaki hucre serbest
        map.markFree((int) (robotY / cell), (int) (robotX / cell));

        for (int i = 0; i < reading.rayCount(); i++) {
            double angle = reading.rayAngles()[i];
            double reported = reading.rayDistances()[i];
            boolean hit = reported < range - 1e-9;
            double hitDist = reported + radius; // robot merkezinden engele mesafe

            for (double t = step; t < hitDist - 1e-9; t += step) {
                int c = (int) Math.floor((robotX + Math.cos(angle) * t) / cell);
                int r = (int) Math.floor((robotY + Math.sin(angle) * t) / cell);
                map.markFree(r, c);
            }
            if (hit) {
                int c = (int) Math.floor((robotX + Math.cos(angle) * hitDist) / cell);
                int r = (int) Math.floor((robotY + Math.sin(angle) * hitDist) / cell);
                map.markObstacle(r, c);
            }
        }

        // Kir ve hali, yalnizca robotun altinda sezildiyse
        if (reading.dirtDetected()) {
            map.markDirtSeen(reading.dirtRow(), reading.dirtCol());
        }
        if (reading.onCarpet()) {
            map.markCarpet((int) (robotY / cell), (int) (robotX / cell));
        }
    }
}
