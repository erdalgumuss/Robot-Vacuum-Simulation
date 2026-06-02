package controller.drive;

import controller.PathFinder;
import controller.algorithm.CleaningStrategy;
import model.Cell;
import model.Direction;
import model.DirtType;
import model.Robot;
import model.RobotState;
import model.Room;
import model.SimulationStats;
import util.SimConstants;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * "Tanrı Modu" sürücüsü: robotun tüm odayı bildiği (omniscient) verimli temizlik.
 * <p>
 * Robot konumu <b>sürekli</b> (piksel) olsa da kararlar grid hücre merkezlerinde
 * alınır: robot daima yürünebilen bir komşu hücre merkezine doğru ilerler; böylece
 * duvar/mobilyaya asla giremez. Gezinme {@link CleaningStrategy} ile, istasyona
 * dönüş {@link PathFinder#aStar} ile yapılır.
 * <p>
 * Bu sınıf, daha önce {@code RobotController} içinde yer alan mantığın birebir
 * taşınmış halidir (davranış değişmez).
 */
public class OmniscientDriver implements Driver {

    private static final double CELL = SimConstants.CELL_SIZE;
    private static final double ARRIVE_EPS = 0.5;

    private final Room room;
    private final Robot robot;
    private final SimulationStats stats;
    private CleaningStrategy strategy;

    private double speedMultiplier = 1.5;

    // Hareket hedefi (komsu hucre)
    private boolean hasTarget;
    private int targetRow;
    private int targetCol;
    private Direction currentDir;

    // Donus (BFS/A*) durumu
    private final Deque<int[]> returnPath = new ArrayDeque<>();
    private boolean returningToFinish;

    public OmniscientDriver(Room room, Robot robot, SimulationStats stats, CleaningStrategy strategy) {
        this.room = room;
        this.robot = robot;
        this.stats = stats;
        this.strategy = strategy;
    }

    // --- Disari acilan kontrol ---

    public void setStrategy(CleaningStrategy strategy) {
        this.strategy = strategy;
        this.strategy.reset();
        this.hasTarget = false;
    }

    @Override
    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    @Override
    public Direction currentDirection() {
        return currentDir;
    }

    @Override
    public void beginCleaning() {
        if (robot.state() == RobotState.IDLE || robot.state() == RobotState.FINISHED
                || robot.state() == RobotState.STUCK) {
            robot.setState(RobotState.CLEANING);
        }
    }

    @Override
    public void requestReturnToStation() {
        if (robot.state() == RobotState.CHARGING || robot.state() == RobotState.RETURNING) {
            return;
        }
        beginReturn(false);
    }

    @Override
    public void resetToStation() {
        robot.setPosition(centerX(room.stationCol()), centerY(room.stationRow()));
        robot.setHeading(Direction.EAST.angleRadians());
        robot.setBattery(SimConstants.BATTERY_FULL);
        robot.setState(RobotState.IDLE);
        robot.clearTrail();
        hasTarget = false;
        returningToFinish = false;
        returnPath.clear();
        currentDir = null;
        strategy.reset();
        room.cell(room.stationRow(), room.stationCol()).markVisited();
    }

    // --- Ana tik ---

    @Override
    public void step(double dt) {
        switch (robot.state()) {
            case IDLE, STUCK, FINISHED -> { /* hareket yok */ }
            case CHARGING -> charge(dt);
            case RETURNING -> returnStep(dt);
            case CLEANING -> cleaningStep(dt);
        }
    }

    // --- Temizlik modu ---

    private void cleaningStep(double dt) {
        // Saglamlik: hedef hucre bu arada engele donustuyse hedefi iptal et.
        if (hasTarget && !room.isWalkable(targetRow, targetCol)) {
            hasTarget = false;
        }

        int r = cellRow();
        int c = cellCol();
        Cell here = room.cell(r, c);

        // 1) Bulundugu hucre kirliyse, temizlenene kadar burada bekle
        if (here.isDirty()) {
            cleanHere(dt, here);
            return;
        }

        // 2) Batarya dusukse istasyona don
        if (robot.battery() <= SimConstants.LOW_BATTERY_THRESHOLD) {
            beginReturn(false);
            return;
        }

        // 3) Erisilebilir kir kalmadiysa eve donup bitir
        if (reachableDirtyCount() == 0) {
            beginReturn(true);
            return;
        }

        // 4) Hedef yoksa strateji ile yeni yon sec
        if (!hasTarget) {
            Direction d = strategy.chooseDirection(room, r, c, currentDir);
            if (d == null) {
                robot.setState(RobotState.STUCK);
                return;
            }
            currentDir = d;
            setTarget(r + d.dRow(), c + d.dCol());
        }

        moveTowardTarget(dt);
    }

    private void cleanHere(double dt, Cell cell) {
        DirtType dirt = cell.dirt();
        robot.setState(RobotState.CLEANING);
        double progress = dt / dirt.cleaningSeconds();
        robot.drainBattery(dirt.batteryCost() * progress);
        boolean finished = cell.applyCleaning(progress);
        if (finished) {
            stats.incrementCleaned();
            hasTarget = false;
        }
    }

    // --- Donus modu ---

    private void beginReturn(boolean finishAfter) {
        this.returningToFinish = finishAfter;
        robot.setState(RobotState.RETURNING);
        hasTarget = false;
        computeReturnPath();
        if (returnPath.isEmpty()) {
            if (cellRow() == room.stationRow() && cellCol() == room.stationCol()) {
                reachedStation();
            } else {
                robot.setState(RobotState.STUCK);
            }
        }
    }

    private void computeReturnPath() {
        returnPath.clear();
        List<int[]> path = PathFinder.aStar(
                room, cellRow(), cellCol(), room.stationRow(), room.stationCol());
        returnPath.addAll(path);
    }

    private void returnStep(double dt) {
        if (hasTarget && !room.isWalkable(targetRow, targetCol)) {
            hasTarget = false;
            returnPath.clear();
        }
        if (!hasTarget) {
            if (returnPath.isEmpty()) {
                if (cellRow() == room.stationRow() && cellCol() == room.stationCol()) {
                    reachedStation();
                    return;
                }
                computeReturnPath();
                if (returnPath.isEmpty()) {
                    robot.setState(RobotState.STUCK);
                    return;
                }
            }
            int[] next = returnPath.poll();
            currentDir = directionTo(next[0], next[1]);
            setTarget(next[0], next[1]);
        }
        moveTowardTarget(dt);
    }

    private void reachedStation() {
        robot.setPosition(centerX(room.stationCol()), centerY(room.stationRow()));
        hasTarget = false;
        returnPath.clear();
        robot.setState(returningToFinish ? RobotState.FINISHED : RobotState.CHARGING);
    }

    // --- Sarj ---

    private void charge(double dt) {
        robot.setBattery(robot.battery() + SimConstants.CHARGE_RATE_PER_SEC * dt);
        if (robot.battery() >= SimConstants.BATTERY_FULL) {
            robot.setBattery(SimConstants.BATTERY_FULL);
            robot.setState(reachableDirtyCount() == 0 ? RobotState.FINISHED : RobotState.CLEANING);
        }
    }

    private int reachableDirtyCount() {
        boolean[][] reach = PathFinder.reachable(room, cellRow(), cellCol());
        int count = 0;
        for (int r = 0; r < room.rows(); r++) {
            for (int c = 0; c < room.cols(); c++) {
                if (reach[r][c] && room.cell(r, c).isDirty()) {
                    count++;
                }
            }
        }
        return count;
    }

    // --- Surekli hareket ---

    private void moveTowardTarget(double dt) {
        double tx = centerX(targetCol);
        double ty = centerY(targetRow);
        double dx = tx - robot.x();
        double dy = ty - robot.y();
        double dist = Math.hypot(dx, dy);

        double stepLen = SimConstants.BASE_SPEED * speedMultiplier * dt;

        if (dist <= stepLen + ARRIVE_EPS) {
            robot.setPosition(tx, ty);
            onArrive();
        } else {
            robot.setPosition(robot.x() + dx / dist * stepLen,
                              robot.y() + dy / dist * stepLen);
            smoothTurn(Math.atan2(dy, dx), dt);
        }

        robot.drainBattery(SimConstants.BATTERY_MOVE_COST_PER_SEC * dt);
        robot.recordTrail();
    }

    private void onArrive() {
        hasTarget = false;
        room.cell(cellRow(), cellCol()).markVisited();
    }

    private void smoothTurn(double targetAngle, double dt) {
        double diff = normalizeAngle(targetAngle - robot.heading());
        double maxStep = SimConstants.TURN_SPEED * dt;
        if (Math.abs(diff) <= maxStep) {
            robot.setHeading(targetAngle);
        } else {
            robot.setHeading(robot.heading() + Math.signum(diff) * maxStep);
        }
    }

    // --- Yardimcilar ---

    private Direction directionTo(int row, int col) {
        int dr = row - cellRow();
        int dc = col - cellCol();
        for (Direction d : Direction.values()) {
            if (d.dRow() == dr && d.dCol() == dc) {
                return d;
            }
        }
        return currentDir;
    }

    private void setTarget(int row, int col) {
        this.targetRow = row;
        this.targetCol = col;
        this.hasTarget = true;
    }

    private int cellRow() { return clamp((int) (robot.y() / CELL), room.rows()); }
    private int cellCol() { return clamp((int) (robot.x() / CELL), room.cols()); }

    private static int clamp(int value, int size) {
        if (value < 0) return 0;
        if (value >= size) return size - 1;
        return value;
    }

    private static double centerX(int col) { return col * CELL + CELL / 2.0; }
    private static double centerY(int row) { return row * CELL + CELL / 2.0; }

    private static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }
}
