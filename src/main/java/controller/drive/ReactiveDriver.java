package controller.drive;

import controller.perception.BeliefUpdater;
import controller.perception.PerceptionService;
import model.Cell;
import model.Direction;
import model.DirtType;
import model.KnownMap;
import model.Robot;
import model.RobotState;
import model.Room;
import model.SensorReading;
import model.SimulationStats;
import util.SimConstants;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;

/**
 * "Gerçekçi Mod" sürücüsü: robot ortamı bilmez. Her tikte sensörlerini okur,
 * yalnızca bunlardan kendi iç haritasını ({@link KnownMap}) öğrenir ve serbest
 * açılı (sürekli) hareketle gezer.
 * <p>
 * Davranış: belief haritası üzerinde en yakın <i>henüz üstünden geçilmemiş</i>
 * hücreye (frontier) yönelir, yolda kir sezerse durup temizler, gerçek bir engele
 * çarptığında (sensörle) sekerek yön değiştirir (bump-and-turn). Batarya düşünce
 * veya keşif bitince belief üzerinden istasyona döner. Karar girdisi olarak gerçek
 * {@link Room}'a asla bakmaz; tek istisna {@link PerceptionService} (sensör).
 */
public class ReactiveDriver implements Driver {

    private static final double CELL = SimConstants.CELL_SIZE;
    private static final double R = SimConstants.ROBOT_RADIUS;
    private static final int STUCK_TICKS = 120;

    private final Room room;
    private final Robot robot;
    private final SimulationStats stats;
    private final Random random = new Random();

    private double speedMultiplier = 1.5;
    private final Deque<int[]> path = new ArrayDeque<>();
    private boolean returningToFinish;
    private int noProgressTicks;
    private double lastDistToWp = Double.MAX_VALUE;
    private static final double ALIGN_TOLERANCE = Math.toRadians(55); // önce yönel, sonra ilerle

    // Ulaşamadığı keşif hedefini bir süre erteler (rastgele debelenme yerine başka yere yönelir)
    private long tick;
    private int goalRow = -1;
    private int goalCol = -1;
    private final Map<Integer, Long> deferred = new HashMap<>();
    private static final long DEFER_TICKS = 400;

    // Mekik (boustrophedon) yön durumu: +1 doğu, -1 batı
    private int sweepDirX = 1;

    // Odometri: robotun tahmin ettiği konum (gerçek hareket + küçük hata birikimi)
    private double estX;
    private double estY;
    private static final double DRIFT_RATE = 0.05; // hareket başına hata oranı

    // Tik içi telemetri girdileri
    private boolean tickCleaning;
    private double tickDirtResistance;
    private double tickMoved;

    public ReactiveDriver(Room room, Robot robot, SimulationStats stats) {
        this.room = room;
        this.robot = robot;
        this.stats = stats;
    }

    @Override
    public void setSpeedMultiplier(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    @Override
    public Direction currentDirection() {
        return headingToDirection(robot.heading());
    }

    @Override
    public void beginCleaning() {
        if (robot.state() == RobotState.IDLE || robot.state() == RobotState.FINISHED
                || robot.state() == RobotState.STUCK) {
            robot.setState(RobotState.CLEANING);
            path.clear();
            noProgressTicks = 0;
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
        // Robot her şeyi unutur: boş bir belief map ile sıfırdan öğrenmeye başlar.
        robot.setKnownMap(new KnownMap(room.rows(), room.cols()));
        robot.setLastReading(SensorReading.empty());
        robot.setMotorTelemetry(0, 0, 0);
        path.clear();
        deferred.clear();
        goalRow = -1;
        goalCol = -1;
        sweepDirX = 1;
        returningToFinish = false;
        noProgressTicks = 0;
        lastDistToWp = Double.MAX_VALUE;
        estX = robot.x();
        estY = robot.y();
        robot.setPoseEstimate(estX, estY, 1.0);
    }

    // --- Ana tik ---

    @Override
    public void step(double dt) {
        tick++;
        robot.decayContact();
        KnownMap map = robot.knownMap();
        if (map == null) {
            return; // gerçekçi mod kurulmadıysa
        }

        // 1) Algıla ve öğren (gerçek odaya tek bakış burada)
        SensorReading reading = PerceptionService.sense(room, robot);
        robot.setLastReading(reading);
        BeliefUpdater.integrate(map, robot.x(), robot.y(), reading);

        // 2) Tik girdilerini sıfırla, davranışı işlet
        tickCleaning = false;
        tickDirtResistance = 0;
        tickMoved = 0;
        double headingBefore = robot.heading();
        double prevX = robot.x();
        double prevY = robot.y();

        switch (robot.state()) {
            case IDLE, STUCK, FINISHED -> { /* hareket yok */ }
            case CHARGING -> charge(dt, map);
            case RETURNING -> returnStep(dt, reading, map);
            case CLEANING -> cleaningStep(dt, reading, map);
        }

        // 3) Motor telemetrisi + odometri (drift)
        updateTelemetry(dt, headingBefore, reading);
        updateOdometry(prevX, prevY);
    }

    // --- Temizlik modu ---

    private void cleaningStep(double dt, SensorReading reading, KnownMap map) {
        int r = cellRow();
        int c = cellCol();
        map.markFree(r, c);
        map.markCleaned(r, c);
        room.cell(r, c).markVisited(); // görsel heatmap/stats için (karar girdisi değil)

        // 1) Altında kir varsa, temizlenene kadar bekle (linger)
        if (reading.dirtDetected()) {
            Cell d = room.cell(reading.dirtRow(), reading.dirtCol());
            if (d.isDirty()) {
                DirtType dirt = d.dirt();
                tickCleaning = true;
                tickDirtResistance = MotorModel.dirtResistance(dirt);
                double progress = dt / dirt.cleaningSeconds();
                robot.drainBattery(dirt.batteryCost() * progress);
                if (d.applyCleaning(progress)) {
                    stats.incrementCleaned();
                    map.markCleaned(reading.dirtRow(), reading.dirtCol());
                    map.clearDirtSeen(reading.dirtRow(), reading.dirtCol());
                }
                noProgressTicks = 0;
                return; // temizlerken ilerleme yok
            }
        }

        // 2) Batarya düşükse istasyona dön
        if (robot.battery() <= SimConstants.LOW_BATTERY_THRESHOLD) {
            beginReturn(false);
            return;
        }

        // 3) Hedef yoksa mekik (boustrophedon) planlamasıyla yeni hedef seç
        if (path.isEmpty()) {
            List<int[]> p = beliefPathBoustrophedon(map);
            if (p.isEmpty()) {
                // Belki tüm hedefler ertelendi: ertelemeleri temizleyip tekrar dene
                deferred.clear();
                p = beliefPathBoustrophedon(map);
                if (p.isEmpty()) {
                    beginReturn(true); // keşif gerçekten bitti, eve dön ve tamamla
                    return;
                }
            }
            path.addAll(p);
            int[] goal = p.get(p.size() - 1);
            goalRow = goal[0];
            goalCol = goal[1];
        }

        followPath(dt, reading);
    }

    // --- Dönüş modu (belief üzerinde) ---

    private void beginReturn(boolean finishAfter) {
        this.returningToFinish = finishAfter;
        robot.setState(RobotState.RETURNING);
        path.clear();
        noProgressTicks = 0;
    }

    private void returnStep(double dt, SensorReading reading, KnownMap map) {
        int sr = cellRow();
        int sc = cellCol();
        if (sr == room.stationRow() && sc == room.stationCol()) {
            reachedStation(map);
            return;
        }
        if (path.isEmpty()) {
            path.addAll(beliefPathTo(map,
                    (rr, cc) -> rr == room.stationRow() && cc == room.stationCol()));
            if (path.isEmpty()) {
                robot.setState(RobotState.STUCK);
                return;
            }
        }
        followPath(dt, reading);
    }

    private void reachedStation(KnownMap map) {
        path.clear();
        if (returningToFinish) {
            robot.setState(RobotState.FINISHED);
        } else {
            robot.setState(RobotState.CHARGING);
        }
    }

    private void charge(double dt, KnownMap map) {
        robot.setBattery(robot.battery() + SimConstants.CHARGE_RATE_PER_SEC * dt);
        if (robot.battery() >= SimConstants.BATTERY_FULL) {
            robot.setBattery(SimConstants.BATTERY_FULL);
            boolean more = !beliefPathTo(map, (rr, cc) -> !map.isCleaned(rr, cc)).isEmpty();
            robot.setState(more ? RobotState.CLEANING : RobotState.FINISHED);
        }
    }

    // --- Serbest açılı hareket ---

    /**
     * Mevcut yol hedefine doğru gider. Yay çizip orbit'e girmemek için ÖNCE
     * hedefe yeterince yönelir, SONRA ilerler. Bir hücreye "vardım" sayması o
     * hücreye merkezi gerçekten girdiğinde olur (erken atlama yok).
     */
    private void followPath(double dt, SensorReading reading) {
        int[] wp = path.peekFirst();
        double tx = centerX(wp[1]);
        double ty = centerY(wp[0]);
        double dx = tx - robot.x();
        double dy = ty - robot.y();
        double distToWp = Math.hypot(dx, dy);

        double desired = Math.atan2(dy, dx);
        smoothTurn(desired, dt);
        double angleErr = Math.abs(normalizeAngle(desired - robot.heading()));
        boolean aligned = angleErr < ALIGN_TOLERANCE;

        boolean moved = aligned && attemptMove(dt, reading.onCarpet());

        if (cellRow() == wp[0] && cellCol() == wp[1]) {
            // Hücreye gerçekten girildi -> vardı (markPass burayı temizledi)
            path.pollFirst();
            noProgressTicks = 0;
            lastDistToWp = Double.MAX_VALUE;
        } else if (aligned && !moved) {
            // Hizalıyım ama ilerleyemedim = haritada olmayan gerçek engele ÇARPTIM.
            robot.triggerContact();   // tampon animasyonu + tık sesi için temas sinyali
            deferGoal();
            path.clear();
            bumpTurn(reading);
            noProgressTicks++;
        } else if (distToWp < lastDistToWp - 0.05) {
            noProgressTicks = 0;        // hedefe yaklaşıyorum
        } else if (aligned) {
            noProgressTicks++;          // hizalı + ilerliyorum ama yaklaşmıyorum (orbit emaresi)
        }
        lastDistToWp = distToWp;

        if (noProgressTicks > STUCK_TICKS) {
            // Sıkıştı: hedefi ertele, bırak ve rastgele yöne sek (kurtulma davranışı)
            deferGoal();
            path.clear();
            robot.setHeading(random.nextDouble() * 2 * Math.PI - Math.PI);
            noProgressTicks = 0;
            lastDistToWp = Double.MAX_VALUE;
        }
    }

    /** Ulaşılamayan keşif hedefini bir süre erteler (yalnız temizlik modunda). */
    private void deferGoal() {
        if (robot.state() == RobotState.CLEANING && goalRow >= 0) {
            deferred.put(goalRow * room.cols() + goalCol, tick + DEFER_TICKS);
        }
    }

    private boolean isDeferred(int row, int col) {
        Long expiry = deferred.get(row * room.cols() + col);
        return expiry != null && expiry > tick;
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

    /**
     * Heading boyunca ilerler; çarpışma testini geçen her mikro-adımı commit eder
     * (tünelleme yok). Hiç ilerleyemezse false döner.
     */
    private boolean attemptMove(double dt, boolean onCarpet) {
        double speed = SimConstants.BASE_SPEED * speedMultiplier * (onCarpet ? SimConstants.CARPET_SLOW : 1.0);
        double stepLen = speed * dt;
        int sub = Math.max(1, (int) Math.ceil(stepLen / (R * 0.5)));
        double seg = stepLen / sub;
        double h = robot.heading();
        boolean any = false;

        for (int i = 0; i < sub; i++) {
            double nx = robot.x() + Math.cos(h) * seg;
            double ny = robot.y() + Math.sin(h) * seg;
            if (Physics.overlapsObstacle(room, nx, ny, R)) {
                break;
            }
            robot.setPosition(nx, ny);
            tickMoved += seg;
            any = true;
            markPass();
        }

        robot.drainBattery(SimConstants.BATTERY_MOVE_COST_PER_SEC * dt);
        robot.recordTrail();
        return any;
    }

    private void markPass() {
        int r = cellRow();
        int c = cellCol();
        KnownMap map = robot.knownMap();
        if (map != null) {
            map.markFree(r, c);
            map.markCleaned(r, c);
        }
        room.cell(r, c).markVisited();
    }

    /** Sensör ışınlarından en açık yönü bulup ona (rastgele sapmayla) döner. */
    private void bumpTurn(SensorReading reading) {
        double bestAngle = robot.heading() + Math.PI; // varsayılan: geri dön
        double bestDist = -1;
        for (int i = 0; i < reading.rayCount(); i++) {
            if (reading.rayDistances()[i] > bestDist) {
                bestDist = reading.rayDistances()[i];
                bestAngle = reading.rayAngles()[i];
            }
        }
        double jitter = (random.nextDouble() * 2 - 1) * SimConstants.BUMP_JITTER;
        robot.setHeading(normalizeAngle(bestAngle + jitter));
    }

    // --- Telemetri ---

    private void updateTelemetry(double dt, double headingBefore, SensorReading reading) {
        RobotState s = robot.state();
        if (s == RobotState.IDLE || s == RobotState.STUCK || s == RobotState.FINISHED) {
            robot.setMotorTelemetry(0, MotorModel.voltage(0, robot.battery()), 0);
            return;
        }
        double turnRate = Math.abs(normalizeAngle(robot.heading() - headingBefore)) / Math.max(dt, 1e-6);
        double load = MotorModel.load(turnRate, tickCleaning, tickDirtResistance, reading.onCarpet());
        double voltage = MotorModel.voltage(load, robot.battery());
        double rpm = MotorModel.rpm(tickMoved / Math.max(dt, 1e-6));
        robot.setMotorTelemetry(load, voltage, rpm);
    }

    /**
     * Odometri: robotun tahmini konumu gerçek harekete küçük rastgele hata
     * eklenerek ilerler; bu yüzden zamanla gerçeklikten sapar (drift). İstasyona
     * varınca yeniden konumlanır (re-localization) ve güven 1'e döner.
     * NOT: yalnızca gösterim amaçlıdır; navigasyon gerçek konumu kullanır.
     */
    private void updateOdometry(double prevX, double prevY) {
        double dx = robot.x() - prevX;
        double dy = robot.y() - prevY;
        double moved = Math.hypot(dx, dy);
        if (moved > 0) {
            estX += dx + random.nextGaussian() * DRIFT_RATE * moved;
            estY += dy + random.nextGaussian() * DRIFT_RATE * moved;
        }
        if (cellRow() == room.stationRow() && cellCol() == room.stationCol()) {
            estX = robot.x();   // dock'ta yeniden konumlan
            estY = robot.y();
        }
        double drift = Math.hypot(estX - robot.x(), estY - robot.y());
        double confidence = 1.0 / (1.0 + drift / SimConstants.CELL_SIZE);
        robot.setPoseEstimate(estX, estY, confidence);
    }

    // --- Belief üzerinde BFS (en yakın hedef hücreye yol) ---

    private List<int[]> beliefPathTo(KnownMap map, BiPredicate<Integer, Integer> isTarget) {
        int sr = cellRow();
        int sc = cellCol();
        int rows = map.rows();
        int cols = map.cols();
        boolean[][] visited = new boolean[rows][cols];
        int[][] pr = new int[rows][cols];
        int[][] pc = new int[rows][cols];
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sr, sc});
        visited[sr][sc] = true;
        int[][] moves = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1];
            if (!(r == sr && c == sc) && isTarget.test(r, c)) {
                return reconstruct(pr, pc, sr, sc, r, c);
            }
            for (int[] m : moves) {
                int nr = r + m[0];
                int nc = c + m[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]
                        && map.at(nr, nc) != KnownMap.Belief.OBSTACLE) {
                    visited[nr][nc] = true;
                    pr[nr][nc] = r;
                    pc[nr][nc] = c;
                    q.add(new int[]{nr, nc});
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Mekik (boustrophedon) planlaması: belief üzerinde tüm erişilebilir alanı
     * tarar; mevcut satırda <b>tarama yönünde en uzaktaki</b> temizlenmemiş hücreyi
     * hedef alır (satırı baştan sona süpür). O satır bitince en yakın temizlenmemiş
     * hücreye geçer ve yönü çevirir (serpantin). Güvenli fallback: en yakın hücre.
     */
    private List<int[]> beliefPathBoustrophedon(KnownMap map) {
        int sr = cellRow();
        int sc = cellCol();
        int rows = map.rows();
        int cols = map.cols();
        boolean[][] visited = new boolean[rows][cols];
        int[][] pr = new int[rows][cols];
        int[][] pc = new int[rows][cols];
        int[][] dist = new int[rows][cols];
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sr, sc});
        visited[sr][sc] = true;
        int[][] moves = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        int rowTargetCol = Integer.MIN_VALUE; // aynı satırda, tarama yönünde en uzak
        int rowTargetRow = -1;
        int nearestRow = -1, nearestCol = -1, nearestDist = Integer.MAX_VALUE;

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int r = cur[0], c = cur[1];
            boolean candidate = !(r == sr && c == sc) && !map.isCleaned(r, c) && !isDeferred(r, c);
            if (candidate) {
                if (dist[r][c] < nearestDist) {
                    nearestDist = dist[r][c];
                    nearestRow = r;
                    nearestCol = c;
                }
                if (r == sr && (c - sc) * sweepDirX > 0) {
                    int ahead = (c - sc) * sweepDirX;
                    if (ahead > rowTargetCol) {
                        rowTargetCol = ahead;
                        rowTargetRow = r;
                    }
                }
            }
            for (int[] m : moves) {
                int nr = r + m[0];
                int nc = c + m[1];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !visited[nr][nc]
                        && map.at(nr, nc) != KnownMap.Belief.OBSTACLE) {
                    visited[nr][nc] = true;
                    pr[nr][nc] = r;
                    pc[nr][nc] = c;
                    dist[nr][nc] = dist[r][c] + 1;
                    q.add(new int[]{nr, nc});
                }
            }
        }

        if (rowTargetRow >= 0) {
            // Aynı satırı süpürmeye devam
            int targetCol = sc + rowTargetCol * sweepDirX;
            return reconstruct(pr, pc, sr, sc, rowTargetRow, targetCol);
        }
        if (nearestRow >= 0) {
            // Satır bitti: en yakın temizlenmemiş hücreye geç ve tarama yönünü çevir
            sweepDirX = -sweepDirX;
            return reconstruct(pr, pc, sr, sc, nearestRow, nearestCol);
        }
        return Collections.emptyList();
    }

    private static List<int[]> reconstruct(int[][] pr, int[][] pc, int sr, int sc, int gr, int gc) {
        java.util.ArrayList<int[]> path = new java.util.ArrayList<>();
        int r = gr, c = gc;
        while (!(r == sr && c == sc)) {
            path.add(new int[]{r, c});
            int prr = pr[r][c];
            int pcc = pc[r][c];
            r = prr;
            c = pcc;
        }
        Collections.reverse(path);
        return path;
    }

    // --- Yardımcılar ---

    private int cellRow() { return clamp((int) (robot.y() / CELL), room.rows()); }
    private int cellCol() { return clamp((int) (robot.x() / CELL), room.cols()); }

    private static int clamp(int v, int size) {
        if (v < 0) return 0;
        if (v >= size) return size - 1;
        return v;
    }

    private static double centerX(int col) { return col * CELL + CELL / 2.0; }
    private static double centerY(int row) { return row * CELL + CELL / 2.0; }

    private static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    private static Direction headingToDirection(double heading) {
        double a = normalizeAngle(heading);
        if (a >= -Math.PI / 4 && a < Math.PI / 4) return Direction.EAST;
        if (a >= Math.PI / 4 && a < 3 * Math.PI / 4) return Direction.SOUTH;
        if (a >= -3 * Math.PI / 4 && a < -Math.PI / 4) return Direction.NORTH;
        return Direction.WEST;
    }
}
