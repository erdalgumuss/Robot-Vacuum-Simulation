package view;

import controller.SimulationManager;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import model.KnownMap;
import model.Robot;
import model.SensorReading;
import model.SimulationMode;
import util.SimConstants;

/**
 * Sağ telemetri paneli — robotun "kendi gözü". Ana canvas gerçek ortamı
 * gösterirken bu panel <b>yalnızca robotun bildiklerini</b> gösterir:
 * öğrendiği iç harita (mini-map), sensör radarı (ışınların gördüğü) ve motor
 * telemetrisi (voltaj, zorlanma, devir) + halı/kir durumu.
 * <p>
 * Tüm veriyi {@code sim.robot()} getter'larından okur; kontrol panelinden
 * bağımsızdır.
 */
public class TelemetryPanel extends VBox {

    private final Canvas minimap = new Canvas();
    private final Canvas radar = new Canvas(220, 150);

    private final Label modeLabel = new Label("—");
    private final ProgressBar voltageBar = new ProgressBar(0);
    private final ProgressBar loadBar = new ProgressBar(0);
    private final Label voltageLabel = new Label("—");
    private final Label loadLabel = new Label("—");
    private final Label rpmLabel = new Label("—");
    private final Label dirtChip = new Label("Kir Yok");
    private final Label carpetChip = new Label("Sert Zemin");

    public TelemetryPanel(int rows, int cols) {
        setPrefWidth(SimConstants.TELEMETRY_PANEL_WIDTH);
        setSpacing(12);
        getStyleClass().add("telemetry-panel");

        double mmWidth = SimConstants.TELEMETRY_PANEL_WIDTH - 34;
        minimap.setWidth(mmWidth);
        minimap.setHeight(mmWidth * rows / cols);

        getChildren().addAll(
                title("🛰️  Robot Telemetri"),
                modeLabel,
                section("İç Harita (öğrenilen)", minimap),
                section("Sensör Radarı", radar),
                section("Motor", buildMotor()),
                buildChips()
        );
        modeLabel.getStyleClass().add("telemetry-mode");
    }

    private Label title(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("section-title");
        return l;
    }

    private VBox section(String header, javafx.scene.Node body) {
        Label h = new Label(header);
        h.getStyleClass().add("sub-label");
        VBox box = new VBox(6, h, body);
        box.getStyleClass().add("section");
        return box;
    }

    private VBox buildMotor() {
        voltageBar.setMaxWidth(Double.MAX_VALUE);
        loadBar.setMaxWidth(Double.MAX_VALUE);
        voltageBar.getStyleClass().add("telemetry-bar");
        loadBar.getStyleClass().add("telemetry-bar-load");
        return new VBox(6,
                metricRow("Voltaj", voltageLabel), voltageBar,
                metricRow("Zorlanma", loadLabel), loadBar,
                metricRow("Devir", rpmLabel));
    }

    private HBox metricRow(String key, Label value) {
        Label k = new Label(key);
        k.getStyleClass().add("status-key");
        value.getStyleClass().add("status-val");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(k, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildChips() {
        dirtChip.getStyleClass().add("chip");
        carpetChip.getStyleClass().add("chip");
        HBox row = new HBox(8, dirtChip, carpetChip);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    /** Her karede Main tarafından çağrılır. */
    public void update(SimulationManager sim) {
        modeLabel.setText(sim.mode().label());
        Robot robot = sim.robot();
        boolean realistic = sim.mode() == SimulationMode.REALISTIC && robot.knownMap() != null;

        drawMinimap(robot, realistic);
        drawRadar(robot, realistic);

        double v = robot.motorVoltage();
        double load = robot.motorLoad();
        voltageBar.setProgress(v / SimConstants.MOTOR_VOLTAGE_NOMINAL);
        loadBar.setProgress(load);
        voltageLabel.setText(String.format("%.1f V", v));
        loadLabel.setText(String.format("%%%.0f", load * 100));
        rpmLabel.setText(String.format("%.0f RPM", robot.wheelRpm()));

        SensorReading rd = robot.lastReading();
        boolean dirt = realistic && rd != null && rd.dirtDetected();
        boolean carpet = realistic && rd != null && rd.onCarpet();
        dirtChip.setText(dirt ? "Kir Algılandı" : "Kir Yok");
        carpetChip.setText(carpet ? "Halı" : "Sert Zemin");
        toggleChip(dirtChip, dirt);
        toggleChip(carpetChip, carpet);
    }

    private void toggleChip(Label chip, boolean on) {
        chip.getStyleClass().remove("chip-on");
        if (on) {
            chip.getStyleClass().add("chip-on");
        }
    }

    private void drawMinimap(Robot robot, boolean realistic) {
        GraphicsContext g = minimap.getGraphicsContext2D();
        double w = minimap.getWidth(), h = minimap.getHeight();
        g.setFill(Color.web("#070b14"));
        g.fillRect(0, 0, w, h);

        KnownMap map = realistic ? robot.knownMap() : null;
        if (map == null) {
            g.setFill(Color.web("#475569"));
            g.fillText("Tanrı Modu — harita yok", 8, h / 2);
            return;
        }
        double cs = w / map.cols();
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                double x = c * cs, y = r * cs;
                switch (map.at(r, c)) {
                    case UNKNOWN -> g.setFill(Color.web("#0b1020"));
                    case FREE -> g.setFill(map.isCleaned(r, c) ? Color.web("#1e3a5f") : Color.web("#2a3550"));
                    case OBSTACLE -> g.setFill(Color.web("#7f1d1d"));
                }
                g.fillRect(x, y, cs + 0.5, cs + 0.5);
                if (map.isCarpetSeen(r, c)) {
                    g.setFill(Color.web("#3f6f6f", 0.5));
                    g.fillRect(x, y, cs + 0.5, cs + 0.5);
                }
                if (map.isDirtSeen(r, c)) {
                    g.setFill(Color.web("#f59e0b"));
                    g.fillOval(x + cs * 0.3, y + cs * 0.3, cs * 0.4, cs * 0.4);
                }
            }
        }
        // Robot pozu
        double rx = (robot.x() / SimConstants.CELL_SIZE) * cs;
        double ry = (robot.y() / SimConstants.CELL_SIZE) * cs;
        g.setFill(Color.web("#38bdf8"));
        g.fillOval(rx - 3, ry - 3, 6, 6);
        g.setStroke(Color.web("#38bdf8"));
        g.setLineWidth(1.5);
        g.strokeLine(rx, ry, rx + Math.cos(robot.heading()) * 8, ry + Math.sin(robot.heading()) * 8);
    }

    private void drawRadar(Robot robot, boolean realistic) {
        GraphicsContext g = radar.getGraphicsContext2D();
        double w = radar.getWidth(), h = radar.getHeight();
        g.setFill(Color.web("#070b14"));
        g.fillRect(0, 0, w, h);
        double cx = w / 2, cy = h / 2;

        // menzil halkası
        double scale = (Math.min(w, h) / 2 - 8) / SimConstants.SENSOR_RANGE;
        g.setStroke(Color.web("#1e293b"));
        g.setLineWidth(1);
        g.strokeOval(cx - SimConstants.SENSOR_RANGE * scale, cy - SimConstants.SENSOR_RANGE * scale,
                SimConstants.SENSOR_RANGE * scale * 2, SimConstants.SENSOR_RANGE * scale * 2);

        SensorReading rd = realistic ? robot.lastReading() : null;
        if (rd != null && rd.rayCount() > 0) {
            for (int i = 0; i < rd.rayCount(); i++) {
                // robot çerçevesi: ileri = yukarı
                double rel = rd.rayAngles()[i] - robot.heading();
                double dist = rd.rayDistances()[i];
                double len = dist * scale;
                double ex = cx + Math.sin(rel) * len;
                double ey = cy - Math.cos(rel) * len;
                boolean hit = dist < SimConstants.SENSOR_RANGE - 1e-6;
                g.setStroke(hit ? Color.web("#ef4444") : Color.web("#22c55e", 0.7));
                g.setLineWidth(2);
                g.strokeLine(cx, cy, ex, ey);
                if (hit) {
                    g.setFill(Color.web("#ef4444"));
                    g.fillOval(ex - 3, ey - 3, 6, 6);
                }
            }
        }
        // robot (merkez)
        g.setFill(Color.web("#e2e8f0"));
        g.fillOval(cx - 5, cy - 5, 10, 10);
    }
}
