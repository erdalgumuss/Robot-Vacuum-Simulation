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

    // Odometri: robotun KENDİ tahmin ettiği konum (zamanla saparak gerçeklikten ayrışır)
    private double estimatedX;
    private double estimatedY;
    private double poseConfidence = 1.0; // 1 = tam emin, 0 = kayıp

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

    public double estimatedX() { return estimatedX; }
    public double estimatedY() { return estimatedY; }
    public double poseConfidence() { return poseConfidence; }

    public void setPoseEstimate(double x, double y, double confidence) {
        this.estimatedX = x;
        this.estimatedY = y;
        this.poseConfidence = confidence;
    }

    public void reset() {
        setMotor(0, 0, 0);
        estimatedX = 0;
        estimatedY = 0;
        poseConfidence = 1.0;
    }
}
