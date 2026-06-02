package view;

import controller.SimulationManager;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.LayoutType;
import model.Robot;
import model.Room;
import util.SimConstants;

/**
 * Uygulama giris noktasi (JavaFX Application).
 * <p>
 * Faz 1 - Robot beyni: {@link SimulationManager} model+controller'i sahiplenir;
 * burada yalnizca arayuz kurulur ve her karede simulasyon guncellenip cizilir.
 * Kullanici sol panelden kontrol eder, izgaraya tiklayarak kir/mobilya ekler.
 */
public class Main extends Application {

    private SimulationManager sim;
    private RoomCanvas canvas;
    private ControlPanel controlPanel;
    private StatusPanel statusPanel;
    private TelemetryPanel telemetryPanel;

    // Surukleyerek cizimde ayni hucreyi tekrar tekrar islemeyi onler
    private int lastEditRow = -1;
    private int lastEditCol = -1;

    @Override
    public void start(Stage stage) {
        sim = new SimulationManager(SimConstants.DEFAULT_ROWS, SimConstants.DEFAULT_COLS);
        sim.loadLayout(LayoutType.LIVING_ROOM); // varsayilan oda duzeni

        canvas = new RoomCanvas(sim.room().rows(), sim.room().cols());
        canvas.setCellClickHandler(this::handleCellEdit);

        controlPanel = new ControlPanel(sim);
        statusPanel = new StatusPanel();
        telemetryPanel = new TelemetryPanel(sim.room().rows(), sim.room().cols());

        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.getStyleClass().add("canvas-holder");

        BorderPane center = new BorderPane();
        center.setCenter(canvasHolder);
        center.setBottom(statusPanel);

        BorderPane body = new BorderPane();
        body.setLeft(controlPanel);
        body.setCenter(center);
        body.setRight(telemetryPanel);
        body.getStyleClass().add("body");

        CustomTitleBar titleBar = new CustomTitleBar(stage);
        VBox root = new VBox(titleBar, body);
        root.getStyleClass().add("root");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.setTitle("Robot Süpürge Simülasyonu");
        stage.centerOnScreen();
        stage.show();

        startLoop();
    }

    private void handleCellEdit(int row, int col) {
        if (row == lastEditRow && col == lastEditCol) {
            return;
        }
        lastEditRow = row;
        lastEditCol = col;
        switch (controlPanel.activeTool()) {
            case ADD_DIRT -> sim.addDirt(row, col, controlPanel.selectedDirt());
            case ADD_FURNITURE -> sim.placeFurniture(controlPanel.selectedFurniture(), row, col);
            case NONE -> { /* islem yok */ }
        }
    }

    private void startLoop() {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                sim.update(now);

                Room room = sim.room();
                Robot robot = sim.robot();
                boolean realistic = sim.mode() == model.SimulationMode.REALISTIC;
                // Fog yalnız robot çalışırken; duraklatınca gerçek oda görünür (rahat düzenleme).
                boolean fog = realistic && controlPanel.showBelief() && sim.isRunning();
                canvas.render(room, robot, sim.reachableFromStation(),
                        realistic, fog, controlPanel.showRays());
                controlPanel.updateRobotStatus(robot);
                statusPanel.update(sim);
                telemetryPanel.update(sim);
            }
        }.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
