package util;

/**
 * Simulasyon genelinde kullanilan sabitler tek noktada toplanir.
 * Boylece dengeleme (batarya tuketimi, hiz, grid boyutu) tek dosyadan yapilir.
 */
public final class SimConstants {

    private SimConstants() { } // ornek olusturulamaz

    // --- Grid (parametrik oda) ---
    public static final int DEFAULT_ROWS = 14;
    public static final int DEFAULT_COLS = 20;
    public static final double CELL_SIZE = 38.0;   // bir hucrenin piksel boyutu

    // --- Robot (surekli hareket) ---
    public static final double ROBOT_RADIUS = 14.0;
    public static final double BASE_SPEED = 90.0;  // piksel/saniye (1.0x hizda)
    public static final double TURN_SPEED = 6.0;   // radyan/saniye (yonelim degisimi)

    // --- Batarya ---
    public static final double BATTERY_FULL = 100.0;
    public static final double BATTERY_MOVE_COST_PER_SEC = 0.6;  // hareket ederken/sn
    public static final double LOW_BATTERY_THRESHOLD = 20.0;     // bu seviyenin altinda istasyona don
    public static final double CHARGE_RATE_PER_SEC = 12.0;       // istasyonda sarj/sn

    // --- Gercekci mod: sensorler ---
    public static final int SENSOR_RAY_COUNT = 7;                 // ileri yaydaki isin sayisi
    public static final double SENSOR_ARC = Math.toRadians(120);  // yay genisligi (radyan)
    public static final double SENSOR_RANGE = 1.4 * CELL_SIZE;    // algilama menzili (kisa gorus -> daha cok carpma, Roomba hissi)
    public static final double RAY_STEP = ROBOT_RADIUS / 2.0;     // raycast adim boyu
    public static final double BUMP_DIST = 3.0;                   // bu mesafenin altinda temas

    // --- Gercekci mod: hareket ---
    public static final double BUMP_JITTER = Math.toRadians(35);  // sekmede rastgele sapma
    public static final double SWEEP_LANE = 2 * ROBOT_RADIUS;     // mekik serit araligi
    public static final double CARPET_SLOW = 0.8;                 // halida hiz carpani

    // --- Gercekci mod: motor telemetrisi ---
    public static final double WHEEL_RADIUS = 3.2;               // cm (devir hesabi icin)
    public static final double MOTOR_VOLTAGE_NOMINAL = 14.4;     // V
    public static final double VOLTAGE_SAG = 0.18;              // yuke gore voltaj dususu
    public static final double MOTOR_TURN_COST_PER_RAD = 0.05;  // donus basina ek batarya
    public static final double BASE_LOAD = 0.25;
    public static final double TURN_LOAD_GAIN = 0.40;
    public static final double CLEAN_LOAD_GAIN = 0.45;
    public static final double CARPET_LOAD_GAIN = 0.20;

    // --- Pencere yerlesimi ---
    public static final double PANEL_WIDTH = 300.0;     // sol kontrol paneli
    public static final double STATUS_BAR_HEIGHT = 70.0; // alt istatistik seridi
    public static final double TITLE_BAR_HEIGHT = 44.0;
    public static final double TELEMETRY_PANEL_WIDTH = 260.0; // sag telemetri paneli

    /** Varsayilan grid icin canvas genisligi. */
    public static double canvasWidth() {
        return DEFAULT_COLS * CELL_SIZE;
    }

    /** Varsayilan grid icin canvas yuksekligi. */
    public static double canvasHeight() {
        return DEFAULT_ROWS * CELL_SIZE;
    }
}
