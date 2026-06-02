package controller.perception;

import model.Robot;
import model.Room;
import model.SensorReading;
import util.SimConstants;

/**
 * Robotun sensörlerini simüle eder: gerçekçi modda gerçek ortamı ({@link Room})
 * okuyan <b>tek</b> bileşendir. Robot kararlarını yalnızca buradan dönen
 * {@link SensorReading} üzerinden verir; gerçek odaya doğrudan bakmaz.
 * <p>
 * Modeli: robot merkezinden, heading etrafında {@code SENSOR_ARC} yaya yayılan
 * {@code SENSOR_RAY_COUNT} ışın; her ışın {@code RAY_STEP} adımlarla yürüyüp ilk
 * engelde durur (kızılötesi/lidar benzeri). Ayrıca altındaki hücrede kir ve halı
 * (yüzey direnci) sezilir.
 */
public final class PerceptionService {

    private PerceptionService() { }

    public static SensorReading sense(Room room, Robot robot) {
        double cx = robot.x();
        double cy = robot.y();
        double heading = robot.heading();
        double cell = SimConstants.CELL_SIZE;
        double radius = SimConstants.ROBOT_RADIUS;

        int n = SimConstants.SENSOR_RAY_COUNT;
        double[] angles = new double[n];
        double[] dists = new double[n];
        double half = SimConstants.SENSOR_ARC / 2.0;
        double stepArc = (n > 1) ? SimConstants.SENSOR_ARC / (n - 1) : 0;

        boolean bumpFront = false, bumpLeft = false, bumpRight = false;
        double frontSector = SimConstants.SENSOR_ARC / 6.0;

        for (int i = 0; i < n; i++) {
            double offset = -half + i * stepArc;
            double angle = heading + offset;
            angles[i] = angle;

            boolean hit = false;
            double t = SimConstants.RAY_STEP;
            for (; t <= SimConstants.SENSOR_RANGE; t += SimConstants.RAY_STEP) {
                double px = cx + Math.cos(angle) * t;
                double py = cy + Math.sin(angle) * t;
                int cc = (int) Math.floor(px / cell);
                int rr = (int) Math.floor(py / cell);
                if (!room.inBounds(rr, cc) || room.cell(rr, cc).isObstacle()) {
                    hit = true;
                    break;
                }
            }
            double reported = hit ? Math.max(0.0, t - radius) : SimConstants.SENSOR_RANGE;
            dists[i] = reported;

            if (reported < SimConstants.BUMP_DIST) {
                if (offset < -frontSector) {
                    bumpLeft = true;
                } else if (offset > frontSector) {
                    bumpRight = true;
                } else {
                    bumpFront = true;
                }
            }
        }

        // Altindaki hucre: kir + yuzey
        int row = clamp((int) (cy / cell), room.rows());
        int col = clamp((int) (cx / cell), room.cols());
        boolean dirtDetected = room.inBounds(row, col) && room.cell(row, col).isDirty();
        int dirtRow = dirtDetected ? row : -1;
        int dirtCol = dirtDetected ? col : -1;
        boolean onCarpet = room.inBounds(row, col) && room.cell(row, col).isCarpet();

        return new SensorReading(angles, dists, bumpFront, bumpLeft, bumpRight,
                dirtDetected, dirtRow, dirtCol, onCarpet, robot.battery());
    }

    private static int clamp(int v, int size) {
        if (v < 0) return 0;
        if (v >= size) return size - 1;
        return v;
    }
}
