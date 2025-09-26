package views;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainMenu {
    private final Stage stage;

    public MainMenu(Stage stage) {
        this.stage = stage;
    }

    public Scene createScene() {
        Label title = new Label("Dominoes");
        title.setFont(Font.font(36));
        title.setStyle("-fx-text-fill: white;");

        Button playBtn = new Button("Play");
        Button exitBtn = new Button("Exit");
        playBtn.setMaxWidth(Double.MAX_VALUE);
        exitBtn.setMaxWidth(Double.MAX_VALUE);

        playBtn.setOnAction(e -> {/*go to table view*/});
        exitBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(14, title, playBtn, exitBtn);
        root.setPadding(new Insets(24));
        root.setPrefSize(480, 320);
        root.setStyle("-fx-background-color: #1e1e1e; -fx-alignment: center;");

        return new Scene(root);
    }
}
