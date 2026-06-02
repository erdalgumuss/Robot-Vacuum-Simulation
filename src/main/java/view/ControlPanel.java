package view;

import controller.SimulationManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import model.AlgorithmType;
import model.DirtType;
import model.Direction;
import model.FurnitureType;
import model.LayoutType;
import model.Robot;
import model.SimulationMode;
import util.SimConstants;

/**
 * Sol kontrol paneli. Araclar, algoritma secimi, hiz, robot durumu ve kontrol
 * butonlarini icerir; dogrudan {@link SimulationManager} ile konusur (View ->
 * Controller). Canvas uzerine cizim icin aktif arac ({@link ToolMode}) ve secili
 * kir turunu de tutar.
 */
public class ControlPanel extends VBox {

    private final SimulationManager sim;

    private ToolMode activeTool = ToolMode.NONE;
    private DirtType selectedDirt = DirtType.DUST;
    private FurnitureType selectedFurniture = FurnitureType.SOFA;

    // Gerçekçi mod görselleştirme anahtarları (varsayılan açık)
    private boolean showBelief = true;
    private boolean showRays = true;
    private boolean soundOn = true;

    // Algoritma radyoları (gerçekçi modda pasifleştirmek için referans tutulur)
    private final java.util.List<RadioButton> algoRadios = new java.util.ArrayList<>();

    private ToggleButton dirtToolBtn;
    private ToggleButton furnitureToolBtn;

    // Robot durumu (canli guncellenen) etiketleri
    private final Label posLabel = new Label("—");
    private final Label dirLabel = new Label("—");
    private final Label batteryLabel = new Label("100%");
    private final Label stateLabel = new Label("Bekliyor");
    private final Slider batterySlider = new Slider(0, 100, 100);

    public ControlPanel(SimulationManager sim) {
        this.sim = sim;
        setPrefWidth(SimConstants.PANEL_WIDTH);
        setSpacing(12);
        getStyleClass().add("control-panel");

        getChildren().addAll(
                section("🧰  Araçlar", buildTools()),
                section("🛋️  Oda Düzeni", buildLayouts()),
                section("🛰️  Mod", buildMode()),
                section("⚙️  Temizlik Algoritması", buildAlgorithms()),
                section("🚀  Robot Hızı", buildSpeed()),
                section("🤖  Robot Durumu", buildRobotStatus()),
                section("🎮  Kontroller", buildControls())
        );
    }

    // --- Disari acilan: canvas araci ---

    public ToolMode activeTool() { return activeTool; }
    public DirtType selectedDirt() { return selectedDirt; }
    public FurnitureType selectedFurniture() { return selectedFurniture; }
    public boolean showBelief() { return showBelief; }
    public boolean showRays() { return showRays; }
    public boolean soundOn() { return soundOn; }

    /** Her karede Main tarafindan cagrilir: robot durum etiketlerini tazeler. */
    public void updateRobotStatus(Robot robot) {
        int col = (int) (robot.x() / SimConstants.CELL_SIZE);
        int row = (int) (robot.y() / SimConstants.CELL_SIZE);
        posLabel.setText("(" + col + ", " + row + ")");
        Direction d = sim.controller().currentDirection();
        dirLabel.setText(d != null ? d.display() : "—");
        batteryLabel.setText(String.format("%.0f%%", robot.battery()));
        stateLabel.setText(robot.state().label());
        if (!batterySlider.isValueChanging()) {
            batterySlider.setValue(robot.battery());
        }
    }

    // --- Bolum yapilari ---

    private VBox section(String title, javafx.scene.Node body) {
        Label header = new Label(title);
        header.getStyleClass().add("section-title");
        VBox box = new VBox(8, header, body);
        box.getStyleClass().add("section");
        return box;
    }

    private VBox buildTools() {
        ToggleGroup toolGroup = new ToggleGroup();

        dirtToolBtn = new ToggleButton("Kir Ekle");
        dirtToolBtn.getStyleClass().add("tool-toggle");
        dirtToolBtn.setMaxWidth(Double.MAX_VALUE);
        dirtToolBtn.setToggleGroup(toolGroup);
        dirtToolBtn.setOnAction(e ->
                activeTool = dirtToolBtn.isSelected() ? ToolMode.ADD_DIRT : ToolMode.NONE);

        furnitureToolBtn = new ToggleButton("Mobilya Ekle");
        furnitureToolBtn.getStyleClass().add("tool-toggle-success");
        furnitureToolBtn.setMaxWidth(Double.MAX_VALUE);
        furnitureToolBtn.setToggleGroup(toolGroup);
        furnitureToolBtn.setOnAction(e ->
                activeTool = furnitureToolBtn.isSelected() ? ToolMode.ADD_FURNITURE : ToolMode.NONE);

        Label dirtLabel = new Label("Kir Türü");
        dirtLabel.getStyleClass().add("sub-label");

        ToggleGroup dirtGroup = new ToggleGroup();
        HBox dirtTypes = new HBox(6);
        for (DirtType type : DirtType.values()) {
            ToggleButton tb = new ToggleButton(type.label());
            tb.setToggleGroup(dirtGroup);
            tb.getStyleClass().add("chip");
            tb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tb, Priority.ALWAYS);
            if (type == DirtType.DUST) {
                tb.setSelected(true);
            }
            tb.setOnAction(e -> {
                if (tb.isSelected()) {
                    selectedDirt = type;
                } else {
                    tb.setSelected(true); // her zaman bir tip secili kalsin
                }
            });
            dirtTypes.getChildren().add(tb);
        }

        Label furnLabel = new Label("Mobilya Türü");
        furnLabel.getStyleClass().add("sub-label");
        ComboBox<FurnitureType> furnitureCombo = new ComboBox<>();
        furnitureCombo.getItems().addAll(FurnitureType.values());
        furnitureCombo.setValue(selectedFurniture);
        furnitureCombo.setMaxWidth(Double.MAX_VALUE);
        furnitureCombo.setConverter(new StringConverter<>() {
            @Override public String toString(FurnitureType t) { return t == null ? "" : t.label(); }
            @Override public FurnitureType fromString(String s) { return null; }
        });
        furnitureCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                selectedFurniture = n;
            }
        });

        Label hint = new Label("İpucu: araç seç, sonra ızgaraya tıkla. Mobilyaya tıklamak onu kaldırır.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);

        return new VBox(8, dirtToolBtn, dirtLabel, dirtTypes, furnitureToolBtn, furnLabel, furnitureCombo, hint);
    }

    private VBox buildLayouts() {
        ComboBox<LayoutType> combo = new ComboBox<>();
        combo.getItems().addAll(LayoutType.values());
        combo.setValue(sim.layout());
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(LayoutType t) { return t == null ? "" : t.label(); }
            @Override public LayoutType fromString(String s) { return null; }
        });
        combo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                sim.loadLayout(n);
            }
        });
        Label hint = new Label("Hazır oda düzenleri arasında geçiş yapar.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        return new VBox(8, combo, hint);
    }

    private VBox buildMode() {
        ToggleGroup group = new ToggleGroup();
        RadioButton god = new RadioButton(SimulationMode.GOD.label());
        RadioButton real = new RadioButton(SimulationMode.REALISTIC.label());
        god.getStyleClass().add("radio");
        real.getStyleClass().add("radio");
        god.setToggleGroup(group);
        real.setToggleGroup(group);
        god.setSelected(sim.mode() == SimulationMode.GOD);
        real.setSelected(sim.mode() == SimulationMode.REALISTIC);

        god.setOnAction(e -> {
            sim.setMode(SimulationMode.GOD);
            setAlgoRadiosEnabled(true);
        });
        real.setOnAction(e -> {
            sim.setMode(SimulationMode.REALISTIC);
            setAlgoRadiosEnabled(false); // robot kendi mantığıyla gezer
        });

        CheckBox beliefBox = new CheckBox("İnanç Haritası (fog-of-war)");
        beliefBox.setSelected(showBelief);
        beliefBox.getStyleClass().add("radio");
        beliefBox.setOnAction(e -> showBelief = beliefBox.isSelected());

        CheckBox raysBox = new CheckBox("Sensör Işınları");
        raysBox.setSelected(showRays);
        raysBox.getStyleClass().add("radio");
        raysBox.setOnAction(e -> showRays = raysBox.isSelected());

        CheckBox soundBox = new CheckBox("Ses Efektleri");
        soundBox.setSelected(soundOn);
        soundBox.getStyleClass().add("radio");
        soundBox.setOnAction(e -> soundOn = soundBox.isSelected());

        Label hint = new Label("Gerçekçi modda robot odayı bilmez; sensörleriyle öğrenir.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);

        return new VBox(6, god, real, beliefBox, raysBox, soundBox, hint);
    }

    private void setAlgoRadiosEnabled(boolean enabled) {
        for (RadioButton rb : algoRadios) {
            rb.setDisable(!enabled);
        }
    }

    private VBox buildAlgorithms() {
        ToggleGroup group = new ToggleGroup();
        VBox box = new VBox(6);
        for (AlgorithmType type : AlgorithmType.values()) {
            RadioButton rb = new RadioButton(type.label());
            rb.setToggleGroup(group);
            rb.getStyleClass().add("radio");
            if (type == sim.algorithm()) {
                rb.setSelected(true);
            }
            rb.setOnAction(e -> sim.setAlgorithm(type));
            algoRadios.add(rb);
            box.getChildren().add(rb);
        }
        return box;
    }

    private VBox buildSpeed() {
        Slider slider = new Slider(0.5, 3.0, 1.5);
        slider.setBlockIncrement(0.5);
        Label value = new Label("1.5x");
        value.getStyleClass().add("speed-value");
        slider.valueProperty().addListener((obs, o, n) -> {
            value.setText(String.format("%.1fx", n.doubleValue()));
            sim.setSpeedMultiplier(n.doubleValue());
        });
        HBox row = new HBox(10, slider, value);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(slider, Priority.ALWAYS);
        return new VBox(row);
    }

    private VBox buildRobotStatus() {
        VBox box = new VBox(6,
                statusRow("Konum (x, y)", posLabel),
                statusRow("Yön", dirLabel),
                statusRow("Durum", stateLabel),
                statusRow("Batarya", batteryLabel));

        Label manual = new Label("Bataryayı elle ayarla");
        manual.getStyleClass().add("sub-label");
        batterySlider.valueProperty().addListener((obs, o, n) -> {
            if (batterySlider.isValueChanging()) {
                sim.setBattery(n.doubleValue());
            }
        });
        box.getChildren().addAll(manual, batterySlider);
        return box;
    }

    private HBox statusRow(String key, Label valueLabel) {
        Label k = new Label(key);
        k.getStyleClass().add("status-key");
        valueLabel.getStyleClass().add("status-val");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(k, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildControls() {
        Button start = styled("▶  Başlat", "btn-primary");
        Button pause = styled("⏸  Duraklat", "btn-neutral");
        Button reset = styled("⏹  Sıfırla", "btn-danger");
        Button dock = styled("🏠  İstasyona Dön", "btn-neutral");

        start.setOnAction(e -> sim.start());
        pause.setOnAction(e -> sim.pause());
        reset.setOnAction(e -> sim.reset());
        dock.setOnAction(e -> sim.returnToStation());

        HBox row1 = new HBox(8, start, pause);
        HBox.setHgrow(start, Priority.ALWAYS);
        HBox.setHgrow(pause, Priority.ALWAYS);
        return new VBox(8, row1, reset, dock);
    }

    private Button styled(String text, String cls) {
        Button b = new Button(text);
        b.getStyleClass().add(cls);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }
}
