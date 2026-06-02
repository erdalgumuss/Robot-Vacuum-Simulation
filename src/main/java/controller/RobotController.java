package controller;

import controller.algorithm.CleaningStrategy;
import controller.drive.Driver;
import controller.drive.OmniscientDriver;
import controller.drive.ReactiveDriver;
import model.Direction;
import model.Robot;
import model.Room;
import model.SimulationMode;
import model.SimulationStats;

/**
 * Robotu süren beyin için ince bir façade. İki sürücü tutar — {@link OmniscientDriver}
 * (Tanrı Modu) ve {@link ReactiveDriver} (Gerçekçi Mod) — ve aktif olana delege eder.
 * <p>
 * Bu sayede mod değişimi tek noktadan ({@link #setMode}) yönetilir ve mevcut çağrı
 * yerleri ({@code controller().step()}, vb.) değişmeden çalışır.
 */
public class RobotController {

    private final Robot robot;

    private final OmniscientDriver godDriver;
    private final ReactiveDriver reactiveDriver;

    private SimulationMode mode = SimulationMode.GOD;
    private Driver active;

    public RobotController(Room room, Robot robot, SimulationStats stats, CleaningStrategy strategy) {
        this.robot = robot;
        this.godDriver = new OmniscientDriver(room, robot, stats, strategy);
        this.reactiveDriver = new ReactiveDriver(room, robot, stats);
        this.active = godDriver;
    }

    // --- Mod ---

    public SimulationMode mode() {
        return mode;
    }

    public void setMode(SimulationMode mode) {
        this.mode = mode;
        if (mode == SimulationMode.REALISTIC) {
            this.active = reactiveDriver; // KnownMap ReactiveDriver.resetToStation'da kurulur
        } else {
            this.active = godDriver;
            robot.memory().clear();       // God modunda belief map yok
            robot.telemetry().reset();
        }
        active.resetToStation();
    }

    // --- Delege edilen kontrol ---

    public void step(double dt) {
        active.step(dt);
    }

    public void resetToStation() {
        active.resetToStation();
    }

    public void beginCleaning() {
        active.beginCleaning();
    }

    public void requestReturnToStation() {
        active.requestReturnToStation();
    }

    public Direction currentDirection() {
        return active.currentDirection();
    }

    /** Hız çarpanı her iki sürücüye de uygulanır. */
    public void setSpeedMultiplier(double speedMultiplier) {
        godDriver.setSpeedMultiplier(speedMultiplier);
        reactiveDriver.setSpeedMultiplier(speedMultiplier);
    }

    /** Strateji yalnızca Tanrı Modu için anlamlıdır. */
    public void setStrategy(CleaningStrategy strategy) {
        godDriver.setStrategy(strategy);
    }
}
