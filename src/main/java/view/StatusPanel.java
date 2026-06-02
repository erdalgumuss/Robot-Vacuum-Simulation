package view;

import controller.SimulationManager;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Room;
import model.SimulationStats;
import util.SimConstants;

/**
 * Alt istatistik seridi. Gercek zamanli olcumleri gosterir: toplam alan,
 * temizlenen (kapsanan) alan ve yuzdesi, kalan alan, gecen sure ve batarya.
 * Her karede {@link #update} ile tazelenir.
 */
public class StatusPanel extends HBox {

    private final Label totalArea = new Label("—");
    private final Label cleanedArea = new Label("0%");
    private final Label remainingArea = new Label("—");
    private final Label elapsed = new Label("00:00");
    private final Label dirtRemaining = new Label("0");
    private final Label unreachable = new Label("0");
    private final Label battery = new Label("100%");

    public StatusPanel() {
        setPrefHeight(SimConstants.STATUS_BAR_HEIGHT);
        setSpacing(10);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("status-bar");

        getChildren().addAll(
                metric("Toplam Alan", totalArea),
                metric("Temizlenen Alan", cleanedArea),
                metric("Kalan Alan", remainingArea),
                metric("Kalan Kir", dirtRemaining),
                metric("Ulaşılamaz", unreachable),
                metric("Geçen Süre", elapsed),
                metric("Batarya", battery)
        );
    }

    public void update(SimulationManager sim) {
        Room room = sim.room();
        SimulationStats stats = sim.stats();

        int total = room.totalFloorCells();
        int visited = room.visitedFloorCells();
        int remaining = Math.max(0, total - visited);
        double coverage = total == 0 ? 0 : 100.0 * visited / total;

        totalArea.setText(total + " m²");
        cleanedArea.setText(String.format("%d m² (%.0f%%)", visited, coverage));
        remainingArea.setText(remaining + " m²");
        dirtRemaining.setText(String.valueOf(room.dirtyCellCount()));
        unreachable.setText(String.valueOf(sim.unreachableDirtCount()));
        elapsed.setText(stats.elapsedFormatted());
        battery.setText(String.format("%.0f%%", sim.robot().battery()));
    }

    private VBox metric(String title, Label valueLabel) {
        Label t = new Label(title);
        t.getStyleClass().add("metric-title");
        valueLabel.getStyleClass().add("metric-value");
        VBox box = new VBox(2, t, valueLabel);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }
}
