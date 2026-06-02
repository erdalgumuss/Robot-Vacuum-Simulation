package controller;

import controller.algorithm.StrategyFactory;
import model.AlgorithmType;
import model.CellType;
import model.DirtType;
import model.FurnitureType;
import model.LayoutType;
import model.Robot;
import model.RobotState;
import model.Room;
import model.SimulationMode;
import model.SimulationStats;
import util.SimConstants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulasyonun ana orkestratoru. Model nesnelerinin (oda, robot, istatistik)
 * sahibidir ve zaman dongusunu yonetir. View katmani yalnizca bu sinifla
 * konusur; boylece UI ile mantik birbirinden ayrik kalir (MVC).
 */
public class SimulationManager {

    private final Room room;
    private final Robot robot;
    private final SimulationStats stats;
    private final RobotController controller;

    private AlgorithmType algorithm = AlgorithmType.SPIRAL;
    private LayoutType layout = LayoutType.LIVING_ROOM;
    private boolean running;

    private long lastNanos;
    private double elapsedSeconds;

    // Istasyondan erisilebilirlik haritasi (ulasilamaz alan tespiti - bonus)
    private boolean[][] reachableFromStation;

    // Senaryo kir duzeni: kir bir kez yerlestiginde hatirlanir; Sifirla bunu geri yukler
    private final Map<Integer, DirtType> dirtBlueprint = new LinkedHashMap<>();

    public SimulationManager(int rows, int cols) {
        this.room = new Room(rows, cols);
        this.robot = new Robot(0, 0, SimConstants.BATTERY_FULL);
        this.stats = new SimulationStats();
        this.controller = new RobotController(room, robot, stats, StrategyFactory.create(algorithm));
        // Bos oda ile baslar; somut bir oda duzeni view tarafindan loadLayout ile yuklenir.
        this.controller.resetToStation();
        this.stats.reset(room.totalFloorCells(), room.dirtyCellCount());
        recomputeReachability();
    }

    /**
     * Hazır bir oda düzenini yükler ("çoklu oda düzeni" bonusu): mevcut mobilya
     * ve kiri temizler, düzenin mobilyalarını yerleştirir, varsayılan kirini
     * uygular (Sıfırla'da geri yüklenir) ve robotu istasyona alır.
     */
    public void loadLayout(LayoutType type) {
        this.layout = type;
        running = false;
        lastNanos = 0;
        elapsedSeconds = 0;
        room.clearAllFurniture();
        room.resetSurfaces();
        room.resetDirtAndVisits();
        dirtBlueprint.clear();
        for (LayoutFactory.DirtSpec spec : LayoutFactory.apply(room, type)) {
            addDirt(spec.row(), spec.col(), spec.type());
        }
        controller.resetToStation();
        stats.reset(room.totalFloorCells(), room.dirtyCellCount());
        recomputeReachability();
    }

    /**
     * "Mobilya Ekle" aracı: var olana tıklanırsa kaldırır, boş zemine seçili
     * türü yerleştirir (robotun üstüne yerleştirmez).
     */
    public void placeFurniture(FurnitureType type, int row, int col) {
        if (room.removeFurnitureAt(row, col)) {
            recomputeReachability();
            return;
        }
        int robotRow = (int) (robot.y() / SimConstants.CELL_SIZE);
        int robotCol = (int) (robot.x() / SimConstants.CELL_SIZE);
        for (int r = row; r < row + type.rows(); r++) {
            for (int c = col; c < col + type.cols(); c++) {
                if (r == robotRow && c == robotCol) {
                    return; // robotu gömme
                }
            }
        }
        if (room.placeFurniture(type, row, col)) {
            recomputeReachability();
        }
    }

    public LayoutType layout() {
        return layout;
    }

    private void recomputeReachability() {
        reachableFromStation = PathFinder.reachable(room, room.stationRow(), room.stationCol());
    }

    public boolean[][] reachableFromStation() {
        return reachableFromStation;
    }

    /** Istasyondan erisilemeyen kirli hucre sayisi (bonus gostergesi). */
    public int unreachableDirtCount() {
        int count = 0;
        for (int r = 0; r < room.rows(); r++) {
            for (int c = 0; c < room.cols(); c++) {
                if (room.cell(r, c).isDirty() && !reachableFromStation[r][c]) {
                    count++;
                }
            }
        }
        return count;
    }

    // --- Zaman dongusu (view'in AnimationTimer'i her karede cagirir) ---

    public void update(long now) {
        if (lastNanos == 0) {
            lastNanos = now;
            return;
        }
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        if (dt > 0.05) {
            dt = 0.05; // buyuk sicramalari engelle (ornegin pencere suruklenirken)
        }
        if (!running) {
            return;
        }
        elapsedSeconds += dt;
        stats.setElapsedMillis((long) (elapsedSeconds * 1000));
        controller.step(dt);
    }

    // --- Kontroller ---

    public void start() {
        if (robot.state() == RobotState.IDLE) {
            stats.reset(room.totalFloorCells(), room.dirtyCellCount());
            elapsedSeconds = 0;
        }
        controller.beginCleaning();
        running = true;
    }

    public void pause() {
        running = false;
    }

    public void reset() {
        running = false;
        lastNanos = 0;
        elapsedSeconds = 0;
        room.resetDirtAndVisits();
        restoreBlueprintDirt();           // senaryodaki kiri geri yukle (aninda "tamamlandi" olmasin)
        controller.resetToStation();
        stats.reset(room.totalFloorCells(), room.dirtyCellCount());
        recomputeReachability();
    }

    /** Kaydedilmis kir duzenini, halen zemin olan hucrelere yeniden uygular. */
    private void restoreBlueprintDirt() {
        int cols = room.cols();
        for (Map.Entry<Integer, DirtType> entry : dirtBlueprint.entrySet()) {
            int row = entry.getKey() / cols;
            int col = entry.getKey() % cols;
            room.addDirt(row, col, entry.getValue());
        }
    }

    public void returnToStation() {
        running = true;
        controller.requestReturnToStation();
    }

    public void setAlgorithm(AlgorithmType algorithm) {
        this.algorithm = algorithm;
        controller.setStrategy(StrategyFactory.create(algorithm));
    }

    /** Tanrı/Gerçekçi mod arasında geçiş yapar (robotu istasyona alır, duraklatır). */
    public void setMode(SimulationMode mode) {
        running = false;
        controller.setMode(mode);
        recomputeReachability();
    }

    public SimulationMode mode() {
        return controller.mode();
    }

    public void setSpeedMultiplier(double multiplier) {
        controller.setSpeedMultiplier(multiplier);
    }

    /** Kullanici bataryayi elle ayarlar (gereksinim). */
    public void setBattery(double value) {
        robot.setBattery(value);
    }

    public void addDirt(int row, int col, DirtType type) {
        if (!room.inBounds(row, col)) {
            return;
        }
        room.addDirt(row, col, type);
        if (room.cell(row, col).isDirty()) {
            // Basariyla eklendi (zemin hucresi) -> senaryoya kaydet (Sifirla geri yukler)
            dirtBlueprint.put(row * room.cols() + col, type);
        }
        if (robot.state() == RobotState.FINISHED) {
            robot.setState(RobotState.IDLE); // yeni kir eklendi, tekrar baslatilirsa calissin
        }
    }

    public void toggleFurniture(int row, int col) {
        if (!room.inBounds(row, col)) {
            return;
        }
        // Robotun uzerinde durdugu hucreye mobilya konamaz (kendini hapsetmesin)
        int robotRow = (int) (robot.y() / SimConstants.CELL_SIZE);
        int robotCol = (int) (robot.x() / SimConstants.CELL_SIZE);
        if (row == robotRow && col == robotCol && room.cell(row, col).type() == CellType.FLOOR) {
            return;
        }
        switch (room.cell(row, col).type()) {
            case FLOOR -> room.setFurniture(row, col);
            case FURNITURE -> room.clearObstacle(row, col);
            default -> { /* duvar/istasyon degistirilemez */ }
        }
        recomputeReachability();
    }

    // --- Erisim ---

    public Room room() { return room; }
    public Robot robot() { return robot; }
    public SimulationStats stats() { return stats; }
    public RobotController controller() { return controller; }
    public boolean isRunning() { return running; }
    public AlgorithmType algorithm() { return algorithm; }
}
