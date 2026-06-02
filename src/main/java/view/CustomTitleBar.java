package view;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import util.SimConstants;

/**
 * UNDECORATED pencere icin ozel baslik cubugu: surukle-tasi + simge durumu
 * + kapat. Modern, koyu temali gorunum icin sistem cercevesi kaldirildi.
 */
public class CustomTitleBar extends HBox {

    private double dragOffsetX;
    private double dragOffsetY;

    public CustomTitleBar(Stage stage) {
        setPrefHeight(SimConstants.TITLE_BAR_HEIGHT);
        getStyleClass().add("title-bar");
        setSpacing(8);

        Label title = new Label("🤖  Robot Süpürge Simülasyonu");
        title.getStyleClass().add("title-bar-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimizeBtn = makeButton("—", "title-btn");
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        Button closeBtn = makeButton("✕", "title-btn-close");
        closeBtn.setOnAction(e -> stage.close());

        getChildren().addAll(title, spacer, minimizeBtn, closeBtn);

        // Pencereyi baslik cubugundan surukleyerek tasima
        setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
    }

    private Button makeButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        return btn;
    }
}
