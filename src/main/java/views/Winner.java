package views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Winner {
    private final StackPane overlay;
    private final VBox panel;
    private final Text title = new Text("");

    private final Button menuButton = new Button("MENU");
    private final Button exitButton = new Button("EXIT");

    public Winner(Stage stage) {
        // root overlay
        overlay = new StackPane();
        overlay.setVisible(false);
        overlay.setPickOnBounds(true);

        // dimmer
        Rectangle dim = new Rectangle();
        dim.setFill(Color.rgb(0, 0, 0, 0.55));
        dim.widthProperty().bind(overlay.widthProperty());
        dim.heightProperty().bind(overlay.heightProperty());

        // title
        title.setFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold;");
        // responsive font size
        overlay.heightProperty().addListener((o, oh, nh) -> {
            double h = nh == null ? 800 : nh.doubleValue();
            title.setFont(Font.font(Math.max(28, h * 0.08)));
        });

        // buttons row or column
        HBox buttons = new HBox(25, menuButton, exitButton);
        buttons.setAlignment(Pos.CENTER);

        // panel
        panel = new VBox(30, title, buttons);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(24));
        panel.setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(10), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
                Color.WHITE, BorderStrokeStyle.SOLID,
                new CornerRadii(10), new BorderWidths(2))));
        panel.setFillWidth(false);

        panel.prefWidthProperty().bind(overlay.widthProperty().multiply(0.40));
        panel.prefHeightProperty().bind(overlay.heightProperty().multiply(0.40));
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // style buttons to match PauseMenu look
        for (Button b : new Button[]{menuButton, exitButton}) {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");
            b.setFocusTraversable(false);

            b.prefWidthProperty().bind(panel.widthProperty().multiply(0.40));
            b.prefHeightProperty().bind(panel.heightProperty().multiply(0.22));
            b.setMinHeight(32);
            b.setMaxWidth(Region.USE_PREF_SIZE);
            b.setMaxHeight(Region.USE_PREF_SIZE);
            b.heightProperty().addListener((obs, oh, nh) -> b.setFont(Font.font(nh.doubleValue() * 0.25)));
        }

        // actions
        menuButton.setOnAction(e -> {
            hide();
            Parent menuRoot = new MainMenu(stage).createRoot();
            SceneTransition.fadeIntoScene(stage, menuRoot, Duration.millis(600));
        });
        exitButton.setOnAction(e -> stage.close());

        overlay.getChildren().addAll(dim, panel);
        StackPane.setAlignment(panel, Pos.CENTER);
    }

    public StackPane getView() { return overlay; }

    public void show(String text) {
        title.setText(text == null ? "" : text);
        overlay.setVisible(true);
        overlay.toFront();
        overlay.requestFocus();
    }

    public void hide() { overlay.setVisible(false); }
}
