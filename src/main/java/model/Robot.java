package model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Robot suprge'nin durum modeli. Konumu <b>surekli</b> (piksel tabanli)
 * tutulur: {@code (x, y)} dunya koordinati ve {@code heading} yonelim acisi.
 * Bu sayede robot gercekci bir sekilde akiskan hareket eder; grid yalnizca
 * kir/engel/istasyon icin mantiksal zemindir.
 * <p>
 * Hareketin <i>nasil</i> hesaplanacagi controller katmanindadir; bu sinif
 * yalnizca durumu ve basit yardimcilari (batarya kelepcesi, iz kaydi) tutar.
 */
public class Robot {

    /** Hareket izini cizmek icin saklanacak maksimum nokta sayisi. */
    private static final int MAX_TRAIL = 1500;

    private double x;            // dunya koordinati (piksel)
    private double y;
    private double heading;      // yonelim acisi (radyan); 0 = doguya bakar

    private double battery;      // 0 .. 100
    private RobotState state = RobotState.IDLE;

    /** Robotun gectigi noktalarin izi (view bunu cizgi olarak cizer). */
    private final Deque<double[]> trail = new ArrayDeque<>();

    private final RobotMemory memory = new RobotMemory();
    private final RobotTelemetry telemetry = new RobotTelemetry();

    public Robot(double x, double y, double battery) {
        this.x = x;
        this.y = y;
        this.battery = clampBattery(battery);
    }

    public double x() { return x; }
    public double y() { return y; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double heading() { return heading; }
    public void setHeading(double heading) { this.heading = heading; }

    public double battery() { return battery; }

    public void setBattery(double battery) {
        this.battery = clampBattery(battery);
    }

    /** Bataryayi belirtilen miktarda azaltir (negatife dusmez). */
    public void drainBattery(double amount) {
        this.battery = clampBattery(this.battery - amount);
    }

    public boolean isBatteryEmpty() {
        return battery <= 0.0;
    }

    public RobotState state() { return state; }
    public void setState(RobotState state) { this.state = state; }

    // --- Gercekci mod: bellek + telemetri ---

    public RobotMemory memory() { return memory; }
    public RobotTelemetry telemetry() { return telemetry; }

    public KnownMap knownMap() { return memory.knownMap(); }
    public void setKnownMap(KnownMap knownMap) { memory.setKnownMap(knownMap); }

    public SensorReading lastReading() { return memory.lastReading(); }
    public void setLastReading(SensorReading reading) { memory.setLastReading(reading); }

    public double motorLoad() { return telemetry.motorLoad(); }
    public double motorVoltage() { return telemetry.motorVoltage(); }
    public double wheelRpm() { return telemetry.wheelRpm(); }

    public void setMotorTelemetry(double load, double voltage, double rpm) {
        telemetry.setMotor(load, voltage, rpm);
    }

    // --- Hareket izi ---

    public void recordTrail() {
        trail.addLast(new double[]{x, y});
        if (trail.size() > MAX_TRAIL) {
            trail.removeFirst();
        }
    }

    public Iterable<double[]> trail() {
        return trail;
    }

    public void clearTrail() {
        trail.clear();
    }

    private static double clampBattery(double value) {
        if (value < 0.0) return 0.0;
        if (value > 100.0) return 100.0;
        return value;
    }
}
