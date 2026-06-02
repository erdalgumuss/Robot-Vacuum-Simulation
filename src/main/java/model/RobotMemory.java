package model;

/**
 * Robot-owned perception state. It is separate from {@link Room}: the robot does
 * not own or build the environment, it only keeps the map and latest reading it
 * has learned through sensors.
 */
public class RobotMemory {

    private KnownMap knownMap;
    private SensorReading lastReading = SensorReading.empty();

    public KnownMap knownMap() {
        return knownMap;
    }

    public void setKnownMap(KnownMap knownMap) {
        this.knownMap = knownMap;
    }

    public SensorReading lastReading() {
        return lastReading;
    }

    public void setLastReading(SensorReading lastReading) {
        this.lastReading = lastReading == null ? SensorReading.empty() : lastReading;
    }

    public void clear() {
        knownMap = null;
        lastReading = SensorReading.empty();
    }
}
