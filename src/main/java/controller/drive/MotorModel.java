package controller.drive;

import model.DirtType;
import util.SimConstants;

/**
 * Robotun motor telemetrisini türeten saf model: zorlanma (yük), voltaj ve
 * tekerlek devri. Gerçek hayattaki gibi davranır — dönmek, halıda gezmek ve
 * inatçı kir temizlemek motoru zorlar, yük arttıkça voltaj düşer (sag).
 * <p>
 * Saf ve yan etkisizdir; hem {@link ReactiveDriver} hem testler kullanır.
 */
public final class MotorModel {

    private MotorModel() { }

    /** Motor yükü/zorlanması 0..1. */
    public static double load(double turnRate, boolean cleaning, double dirtResistance, boolean onCarpet) {
        double l = SimConstants.BASE_LOAD
                + SimConstants.TURN_LOAD_GAIN * clamp01(turnRate / SimConstants.TURN_SPEED)
                + (cleaning ? SimConstants.CLEAN_LOAD_GAIN * clamp01(dirtResistance) : 0.0)
                + (onCarpet ? SimConstants.CARPET_LOAD_GAIN : 0.0);
        return clamp01(l);
    }

    /** Yük altında düşen motor voltajı (V). Batarya seviyesiyle de ölçeklenir. */
    public static double voltage(double load, double battery) {
        double v = SimConstants.MOTOR_VOLTAGE_NOMINAL
                * (1.0 - SimConstants.VOLTAGE_SAG * clamp01(load))
                * clamp01(battery / 100.0);
        return Math.max(0.0, v);
    }

    /** Anlık hıza göre tekerlek devri (RPM). */
    public static double rpm(double effectiveSpeed) {
        double circumference = 2 * Math.PI * SimConstants.WHEEL_RADIUS;
        return Math.max(0.0, (effectiveSpeed / circumference) * 60.0);
    }

    /** Kir tipinin temizleme direnci 0..1 (leke en zoru). */
    public static double dirtResistance(DirtType dirt) {
        if (dirt == null) {
            return 0.0;
        }
        return clamp01(dirt.batteryCost() / DirtType.STAIN.batteryCost());
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }
}
