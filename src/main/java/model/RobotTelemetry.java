package model;

/**
 * Runtime telemetry derived by the controller/driver layer and displayed by the
 * view. Keeping it separate from core pose/battery state keeps {@link Robot}
 * focused while preserving a simple read API for UI code.
 */
public class RobotTelemetry {

    private double motorLoad;
    private double motorVoltage;
    private double wheelRpm;

    public double motorLoad() {
        return motorLoad;
    }

    public double motorVoltage() {
        return motorVoltage;
    }

    public double wheelRpm() {
        return wheelRpm;
    }

    public void setMotor(double load, double voltage, double rpm) {
        this.motorLoad = load;
        this.motorVoltage = voltage;
        this.wheelRpm = rpm;
    }

    public void reset() {
        setMotor(0, 0, 0);
    }
}
