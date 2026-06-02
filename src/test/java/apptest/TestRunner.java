    package apptest;

import controller.PathFinder;
import controller.SimulationManager;
import controller.algorithm.CleaningStrategy;
import controller.algorithm.SmartStrategy;
import controller.algorithm.SpiralStrategy;
import controller.drive.MotorModel;
import controller.drive.Physics;
import controller.perception.BeliefUpdater;
import controller.perception.PerceptionService;
import model.AlgorithmType;
import model.Cell;
import model.CellType;
import model.Direction;
import model.DirtType;
import model.KnownMap;
import model.Robot;
import model.RobotState;
import model.Room;
import model.SensorReading;
import model.SimulationMode;
import util.SimConstants;

import java.util.List;

/**
 * Sifir-bagimlilik test kosucusu (JUnit KULLANILMAZ - odev 3. parti kutuphane
 * yasagina uyar). model/ ve controller/ katmanlari JavaFX'siz oldugu icin bu
 * testler arayuz olmadan, sadece is mantigini dogrular.
 * <p>
 * Calistirma (JavaFX gerektirmez):
 *   mvn test-compile
 *   java -cp "target/classes;target/test-classes" apptest.TestRunner
 * <p>
 * Tum testler gecerse cikis kodu 0, aksi halde 1 doner.
 */
public class TestRunner {

    private static int passed;
    private static int failed;
    private static final double CELL = SimConstants.CELL_SIZE;

    public static void main(String[] args) {
        System.out.println("=== Robot Süpürge - Çekirdek Testleri ===\n");

        testDirectionEnum();
        testDirtTypeDurations();
        testCellCleaning();
        testRoomStructure();
        testRoomDirtAndReset();
        testBfsShortestPath();
        testAStarMatchesBfs();
        testReachabilityAndUnreachablePocket();
        testNearestDirtAndUncleaned();
        testStrategiesReturnWalkable();
        testSmartCleansAllReachable();
        testSpiralNoInfiniteLoop();
        testLowBatteryTriggersReturn();
        testBatteryDrainsWhileMoving();
        testResetRestoresDirt();
        testUnreachableDirtDoesNotBlockFinish();
        testFurnitureNotPlacedOnRobot();
        testFurnitureNotPlacedOnDirt();
        testPerceptionRaycast();
        testDirtSensor();
        testBeliefOnlyFromRays();
        testRealisticNoCollision();
        testRealisticCleansAndFinishes();
        testMotorModel();
        testModeSwitchRegression();

        System.out.println("\n=== Sonuç: " + passed + " geçti, " + failed + " kaldı ===");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ model

    private static void testDirectionEnum() {
        section("Direction enum");
        check("EAST sağa dönünce SOUTH", Direction.EAST.turnRight() == Direction.SOUTH);
        check("NORTH sola dönünce WEST", Direction.NORTH.turnLeft() == Direction.WEST);
        check("EAST tersi WEST", Direction.EAST.opposite() == Direction.WEST);
        check("NORTH dRow -1", Direction.NORTH.dRow() == -1 && Direction.NORTH.dCol() == 0);
        check("SOUTH dRow +1", Direction.SOUTH.dRow() == 1);
    }

    private static void testDirtTypeDurations() {
        section("DirtType süreleri (toz < sıvı < leke)");
        check("toz < sıvı", DirtType.DUST.cleaningSeconds() < DirtType.LIQUID.cleaningSeconds());
        check("sıvı < leke", DirtType.LIQUID.cleaningSeconds() < DirtType.STAIN.cleaningSeconds());
        check("her tipte batarya maliyeti > 0",
                DirtType.DUST.batteryCost() > 0 && DirtType.STAIN.batteryCost() > 0);
    }

    private static void testCellCleaning() {
        section("Cell temizleme ilerlemesi");
        Cell c = new Cell(1, 1, CellType.FLOOR);
        c.setDirt(DirtType.LIQUID);
        check("kir eklendi", c.isDirty());
        boolean done1 = c.applyCleaning(0.5);
        check("yarı temizlik henüz bitmez", !done1 && c.isDirty());
        boolean done2 = c.applyCleaning(0.6); // toplam > 1.0
        check("tam temizlenince biter", done2 && !c.isDirty());

        Cell wall = new Cell(0, 0, CellType.WALL);
        wall.setDirt(DirtType.DUST);
        check("duvara kir eklenemez", !wall.isDirty());
    }

    private static void testRoomStructure() {
        section("Room yapısı");
        Room room = new Room(8, 10);
        check("köşe duvar", room.cell(0, 0).type() == CellType.WALL);
        check("iç hücre zemin (istasyon hariç)", room.cell(2, 2).type() == CellType.FLOOR);
        check("istasyon var", room.stationCell().type() == CellType.STATION);
        // iç alan = (8-2)*(10-2) = 48; istasyon 1 hücreyi kaplar -> 47 zemin
        check("zemin sayısı doğru", room.totalFloorCells() == 6 * 8 - 1);
        check("istasyon yürünebilir", room.isWalkable(room.stationRow(), room.stationCol()));
        check("duvar yürünemez", !room.isWalkable(0, 0));
    }

    private static void testRoomDirtAndReset() {
        section("Room kir & reset");
        Room room = new Room(8, 10);
        room.addDirt(3, 3, DirtType.DUST);
        room.addDirt(4, 4, DirtType.STAIN);
        check("kirli sayısı 2", room.dirtyCellCount() == 2);
        room.cell(3, 3).markVisited();
        check("ziyaret sayısı >=1", room.visitedFloorCells() >= 1);
        room.resetDirtAndVisits();
        check("reset kiri siler", room.dirtyCellCount() == 0);
        check("reset ziyaretleri siler", room.visitedFloorCells() == 0);
    }

    // -------------------------------------------------------------- pathfinder

    private static void testBfsShortestPath() {
        section("BFS en kısa yol");
        Room room = new Room(9, 9); // engelsiz iç alan
        List<int[]> path = PathFinder.shortestPath(room, 2, 2, 5, 6);
        int manhattan = Math.abs(5 - 2) + Math.abs(6 - 2);
        check("yol uzunluğu Manhattan mesafesi", path.size() == manhattan);
        check("yol başlangıcı hariç, hedef dahil",
                path.get(path.size() - 1)[0] == 5 && path.get(path.size() - 1)[1] == 6);
        check("yol adımları bitişik", stepsAreContiguous(2, 2, path));
    }

    private static void testAStarMatchesBfs() {
        section("A* = BFS (uzunluk)");
        Room room = new Room(10, 12);
        room.setFurniture(4, 4);
        room.setFurniture(4, 5);
        room.setFurniture(4, 6);
        int sr = 2, sc = 2, gr = 7, gc = 8;
        int bfs = PathFinder.shortestPath(room, sr, sc, gr, gc).size();
        int astar = PathFinder.aStar(room, sr, sc, gr, gc).size();
        check("A* ve BFS aynı en kısa uzunluğu verir", bfs == astar && bfs > 0);
    }

    private static void testReachabilityAndUnreachablePocket() {
        section("Erişilebilirlik & ulaşılamaz cep");
        Room room = new Room(9, 9);
        // (3,3) hücresini mobilyayla çevrele
        room.setFurniture(2, 3);
        room.setFurniture(4, 3);
        room.setFurniture(3, 2);
        room.setFurniture(3, 4);
        room.addDirt(3, 3, DirtType.STAIN);
        boolean[][] reach = PathFinder.reachable(room, room.stationRow(), room.stationCol());
        check("cep istasyondan erişilemez", !reach[3][3]);
        check("normal iç hücre erişilebilir", reach[5][5]);
        check("isReachable cep için false",
                !PathFinder.isReachable(room, room.stationRow(), room.stationCol(), 3, 3));
    }

    private static void testNearestDirtAndUncleaned() {
        section("En yakın kir / temizlenmemiş");
        Room room = new Room(9, 9);
        room.addDirt(2, 6, DirtType.DUST);
        List<int[]> toDirt = PathFinder.pathToNearestDirt(room, 2, 2);
        check("en yakın kire yol bulunur", !toDirt.isEmpty());
        check("yol kirli hücrede biter",
                room.cell(toDirt.get(toDirt.size() - 1)[0], toDirt.get(toDirt.size() - 1)[1]).isDirty());
        List<int[]> toUncleaned = PathFinder.pathToNearestUncleaned(room, 2, 2);
        check("temizlenmemiş hücreye yol bulunur (kaçış)", !toUncleaned.isEmpty());
    }

    private static void testStrategiesReturnWalkable() {
        section("Stratejiler yürünebilir yön döner");
        Room room = new Room(9, 9);
        room.addDirt(5, 5, DirtType.DUST);
        CleaningStrategy spiral = new SpiralStrategy();
        Direction d = spiral.chooseDirection(room, 2, 2, null);
        check("Spiral yürünebilir yön verir",
                d != null && room.isWalkable(2 + d.dRow(), 2 + d.dCol()));
        CleaningStrategy smart = new SmartStrategy();
        Direction ds = smart.chooseDirection(room, 2, 2, null);
        check("Smart kire doğru yürünebilir yön verir",
                ds != null && room.isWalkable(2 + ds.dRow(), 2 + ds.dCol()));
    }

    // ------------------------------------------------------------- integration

    private static void testSmartCleansAllReachable() {
        section("Smart tüm erişilebilir kiri temizler -> FINISHED");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.setAlgorithm(AlgorithmType.SMART);
        sim.addDirt(2, 3, DirtType.DUST);
        sim.addDirt(3, 8, DirtType.LIQUID);
        sim.addDirt(6, 5, DirtType.STAIN);
        sim.start();
        boolean finished = runUntilSettled(sim, 200_000);
        check("simülasyon tamamlandı", finished && sim.robot().state() == RobotState.FINISHED);
        check("hiç kir kalmadı", sim.room().dirtyCellCount() == 0);
        check("robot istasyonda", robotAtStation(sim));
    }

    private static void testSpiralNoInfiniteLoop() {
        section("Spiral sonsuz döngüye girmez (kaçış çalışır)");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.setAlgorithm(AlgorithmType.SPIRAL);
        sim.addDirt(2, 9, DirtType.DUST);
        sim.addDirt(6, 2, DirtType.DUST);
        sim.start();
        boolean finished = runUntilSettled(sim, 400_000);
        check("Spiral de tamamlanır", finished && sim.robot().state() == RobotState.FINISHED);
        check("Spiral tüm kiri temizler", sim.room().dirtyCellCount() == 0);
    }

    private static void testLowBatteryTriggersReturn() {
        section("Düşük batarya -> istasyona dönüş");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.setAlgorithm(AlgorithmType.SMART);
        sim.addDirt(6, 10, DirtType.DUST); // uzakta
        sim.start();
        step(sim, 5); // hareket başlasın
        sim.setBattery(SimConstants.LOW_BATTERY_THRESHOLD - 5);
        step(sim, 3);
        RobotState s = sim.robot().state();
        check("düşük bataryada dönüş/şarj durumu",
                s == RobotState.RETURNING || s == RobotState.CHARGING);
    }

    private static void testBatteryDrainsWhileMoving() {
        section("Hareket bataryayı tüketir");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.setAlgorithm(AlgorithmType.SMART);
        sim.addDirt(6, 10, DirtType.DUST);
        sim.start();
        double before = sim.robot().battery();
        step(sim, 20);
        check("batarya azaldı", sim.robot().battery() < before);
    }

    private static void testResetRestoresDirt() {
        section("Sıfırla kir düzenini geri yükler (anında tamamlanmaz)");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.setAlgorithm(AlgorithmType.SMART);
        sim.addDirt(2, 3, DirtType.DUST);
        sim.addDirt(3, 8, DirtType.LIQUID);
        int initialDirty = sim.room().dirtyCellCount();
        sim.start();
        runUntilSettled(sim, 200_000);
        check("temizlik sonrası kir 0", sim.room().dirtyCellCount() == 0);
        sim.reset();
        check("reset kiri geri yükler", sim.room().dirtyCellCount() == initialDirty);
        check("reset sonrası robot IDLE", sim.robot().state() == RobotState.IDLE);
    }

    private static void testUnreachableDirtDoesNotBlockFinish() {
        section("Ulaşılamaz kir bitişi engellemez");
        SimulationManager sim = new SimulationManager(10, 12);
        sim.setAlgorithm(AlgorithmType.SMART);
        sim.addDirt(2, 3, DirtType.DUST); // erişilebilir
        // (5,8) cebini mobilyayla çevrele + içine kir
        sim.toggleFurniture(4, 8);
        sim.toggleFurniture(6, 8);
        sim.toggleFurniture(5, 7);
        sim.toggleFurniture(5, 9);
        sim.addDirt(5, 8, DirtType.STAIN);
        check("ulaşılamaz kir sayısı 1", sim.unreachableDirtCount() == 1);
        sim.start();
        boolean finished = runUntilSettled(sim, 200_000);
        check("ulaşılamaz kire rağmen tamamlanır",
                finished && sim.robot().state() == RobotState.FINISHED);
        check("sadece ulaşılamaz kir kalır", sim.room().dirtyCellCount() == 1);
    }

    private static void testFurnitureNotPlacedOnRobot() {
        section("Robotun hücresine mobilya konamaz");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.setAlgorithm(AlgorithmType.SMART);
        sim.addDirt(2, 6, DirtType.DUST);
        sim.start();
        step(sim, 40); // robot istasyondan çıkıp zemine geçsin
        int rr = (int) (sim.robot().y() / CELL);
        int rc = (int) (sim.robot().x() / CELL);
        CellType before = sim.room().cell(rr, rc).type();
        sim.toggleFurniture(rr, rc);
        CellType after = sim.room().cell(rr, rc).type();
        check("robotun zeminine mobilya eklenmedi",
                !(before == CellType.FLOOR && after == CellType.FURNITURE));
    }

    private static void testFurnitureNotPlacedOnDirt() {
        section("Kirli hücreye mobilya konamaz");

        SimulationManager sim = new SimulationManager(9, 12);
        sim.addDirt(3, 3, DirtType.DUST);
        sim.toggleFurniture(3, 3);
        check("tek hücre mobilya kiri kapatmaz",
                sim.room().cell(3, 3).type() == CellType.FLOOR
                        && sim.room().cell(3, 3).isDirty());

        sim.placeFurniture(model.FurnitureType.SOFA, 3, 3);
        check("footprint mobilya kirli hücreye yerleşmez",
                sim.room().cell(3, 3).type() == CellType.FLOOR
                        && sim.room().cell(3, 4).type() == CellType.FLOOR
                        && sim.room().cell(3, 3).isDirty());
    }

    // -------------------------------------------------- gerçekçi mod: algı

    private static Robot robotAtCell(int row, int col, double heading) {
        Robot robot = new Robot(col * CELL + CELL / 2.0, row * CELL + CELL / 2.0, 100);
        robot.setHeading(heading);
        return robot;
    }

    private static void testPerceptionRaycast() {
        section("Sensör ışını mesafesi");
        int mid = SimConstants.SENSOR_RAY_COUNT / 2;

        // Açık oda: doğu ışını menzil içinde engel görmez -> SENSOR_RANGE
        Room open = new Room(9, 9);
        Robot r1 = robotAtCell(4, 4, Direction.EAST.angleRadians());
        SensorReading rOpen = PerceptionService.sense(open, r1);
        check("engelsiz ışın SENSOR_RANGE döner",
                Math.abs(rOpen.rayDistances()[mid] - SimConstants.SENSOR_RANGE) < 1e-6);

        // Doğuda (1 hücre ötede) mobilya: orta ışın mesafesi ~ kenar-yarıçap (±RAY_STEP)
        Room blocked = new Room(9, 9);
        blocked.setFurniture(4, 5);
        Robot r2 = robotAtCell(4, 4, Direction.EAST.angleRadians());
        SensorReading rHit = PerceptionService.sense(blocked, r2);
        double edge = 5 * CELL - (4 * CELL + CELL / 2.0); // merkez->engel ön kenarı
        double expected = edge - SimConstants.ROBOT_RADIUS;
        double d = rHit.rayDistances()[mid];
        check("engel ışını yaklaşık doğru mesafe verir",
                d < SimConstants.SENSOR_RANGE
                        && d >= expected - SimConstants.RAY_STEP
                        && d <= expected + SimConstants.RAY_STEP);
    }

    private static void testDirtSensor() {
        section("Kir sensörü yalnız kir üstünde");
        Room room = new Room(9, 9);
        Robot robot = robotAtCell(4, 4, Direction.EAST.angleRadians());
        check("kir yokken algılanmaz", !PerceptionService.sense(room, robot).dirtDetected());

        room.addDirt(4, 4, DirtType.DUST);
        SensorReading over = PerceptionService.sense(room, robot);
        check("kir üstünde algılanır", over.dirtDetected()
                && over.dirtRow() == 4 && over.dirtCol() == 4);

        Robot off = robotAtCell(4, 5, Direction.EAST.angleRadians());
        check("kir dışında algılanmaz", !PerceptionService.sense(room, off).dirtDetected());
    }

    private static void testBeliefOnlyFromRays() {
        section("Belief yalnız ışınla güncellenir");
        Room room = new Room(9, 9);
        room.addDirt(1, 1, DirtType.STAIN); // robotun arkasında, menzil dışında
        Robot robot = robotAtCell(4, 4, Direction.EAST.angleRadians());
        KnownMap map = new KnownMap(9, 9);

        SensorReading reading = PerceptionService.sense(room, robot);
        BeliefUpdater.integrate(map, robot.x(), robot.y(), reading);

        check("robot altı FREE", map.at(4, 4) == KnownMap.Belief.FREE);
        check("doğudaki açık hücre FREE", map.at(4, 5) == KnownMap.Belief.FREE);
        check("görülmeyen hücre UNKNOWN kalır", map.at(1, 1) == KnownMap.Belief.UNKNOWN);
        check("görülmeyen kir dirtSeen olmaz", !map.isDirtSeen(1, 1));
    }

    // ----------------------------------------------- gerçekçi mod: sürüş

    private static void testRealisticNoCollision() {
        section("Gerçekçi mod: engele asla girmez");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.toggleFurniture(4, 4);
        sim.toggleFurniture(4, 5);
        sim.toggleFurniture(4, 6);
        sim.toggleFurniture(6, 8);
        sim.toggleFurniture(2, 9);
        sim.addDirt(3, 3, DirtType.DUST);
        sim.addDirt(7, 10, DirtType.LIQUID);
        sim.addDirt(2, 2, DirtType.STAIN);
        sim.setMode(SimulationMode.REALISTIC);
        sim.start();

        boolean overlap = false;
        for (int i = 0; i < 5000 && !overlap; i++) {
            sim.controller().step(0.05);
            overlap = Physics.overlapsObstacle(sim.room(),
                    sim.robot().x(), sim.robot().y(), SimConstants.ROBOT_RADIUS);
        }
        check("5000 tikte engelle çakışma yok", !overlap);
    }

    private static void testRealisticCleansAndFinishes() {
        section("Gerçekçi mod: keşfeder, temizler, biter");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.addDirt(2, 3, DirtType.DUST);
        sim.addDirt(5, 8, DirtType.LIQUID);
        sim.addDirt(7, 5, DirtType.DUST);
        sim.setMode(SimulationMode.REALISTIC);
        sim.start();

        boolean finished = false;
        for (int i = 0; i < 600_000; i++) {
            sim.controller().step(0.05);
            RobotState s = sim.robot().state();
            if (s == RobotState.FINISHED) {
                finished = true;
                break;
            }
            if (s == RobotState.STUCK) {
                break;
            }
        }
        check("gerçekçi mod tamamlanır (FINISHED)", finished);
        check("tüm erişilebilir kir temizlendi", sim.room().dirtyCellCount() == 0);
    }

    private static void testMotorModel() {
        section("Motor modeli (yük/voltaj/devir)");
        double straight = MotorModel.load(0, false, 0, false);
        double turning = MotorModel.load(SimConstants.TURN_SPEED, false, 0, false);
        double carpet = MotorModel.load(0, false, 0, true);
        double stainClean = MotorModel.load(0, true, MotorModel.dirtResistance(DirtType.STAIN), false);

        check("dönüş yükü düz gidişten büyük", turning > straight);
        check("halı yükü düz zeminden büyük", carpet > straight);
        check("leke temizleme yükü düz gidişten büyük", stainClean > straight);
        check("yük [0,1] aralığında", straight >= 0 && stainClean <= 1.0);

        double vLow = MotorModel.voltage(0.1, 100);
        double vHigh = MotorModel.voltage(0.9, 100);
        check("voltaj nominali aşmaz", vLow <= SimConstants.MOTOR_VOLTAGE_NOMINAL + 1e-9);
        check("voltaj pozitif", vHigh > 0);
        check("yük arttıkça voltaj düşer (sag)", vHigh < vLow);
        check("devir negatif olamaz", MotorModel.rpm(0) >= 0 && MotorModel.rpm(120) > 0);
    }

    private static void testModeSwitchRegression() {
        section("Mod geçişi sonrası God mode regresyonu");
        SimulationManager sim = new SimulationManager(9, 12);
        sim.setMode(SimulationMode.REALISTIC);
        sim.setMode(SimulationMode.GOD);
        sim.setAlgorithm(AlgorithmType.SMART);
        sim.addDirt(2, 3, DirtType.DUST);
        sim.addDirt(4, 8, DirtType.LIQUID);
        sim.start();
        boolean finished = runUntilSettled(sim, 300_000);
        check("God mode hâlâ tamamlanır", finished && sim.robot().state() == RobotState.FINISHED);
        check("tüm kir temizlendi", sim.room().dirtyCellCount() == 0);
    }

    // ---------------------------------------------------------------- helpers

    /** Robot FINISHED veya STUCK olana dek adımla; settled olursa true. */
    private static boolean runUntilSettled(SimulationManager sim, int maxSteps) {
        for (int i = 0; i < maxSteps; i++) {
            sim.controller().step(0.05);
            RobotState s = sim.robot().state();
            if (s == RobotState.FINISHED || s == RobotState.STUCK) {
                return s == RobotState.FINISHED;
            }
        }
        return false;
    }

    private static void step(SimulationManager sim, int n) {
        for (int i = 0; i < n; i++) {
            sim.controller().step(0.05);
        }
    }

    private static boolean robotAtStation(SimulationManager sim) {
        int rr = (int) (sim.robot().y() / CELL);
        int rc = (int) (sim.robot().x() / CELL);
        return rr == sim.room().stationRow() && rc == sim.room().stationCol();
    }

    private static boolean stepsAreContiguous(int startRow, int startCol, List<int[]> path) {
        int pr = startRow, pc = startCol;
        for (int[] step : path) {
            int d = Math.abs(step[0] - pr) + Math.abs(step[1] - pc);
            if (d != 1) {
                return false;
            }
            pr = step[0];
            pc = step[1];
        }
        return true;
    }

    private static void section(String title) {
        System.out.println("-- " + title);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("   [PASS] " + name);
        } else {
            failed++;
            System.out.println("   [FAIL] " + name);
        }
    }
}
