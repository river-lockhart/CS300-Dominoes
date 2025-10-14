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

    // builds winner overlay and wires buttons
    public Winner(Stage stage) {
        // make overlay layer
        overlay = new StackPane();
        overlay.setVisible(false);
        overlay.setPickOnBounds(true);

        // add dim background
        Rectangle dim = new Rectangle();
        dim.setFill(Color.rgb(0, 0, 0, 0.55));
        dim.widthProperty().bind(overlay.widthProperty());
        dim.heightProperty().bind(overlay.heightProperty());

        // set title style
        title.setFill(Color.WHITE);
        title.setStyle("-fx-font-weight: bold;");

        // scale title with overlay height
        overlay.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            double h = newHeight == null ? 800 : newHeight.doubleValue();
            title.setFont(Font.font(Math.max(28, h * 0.08)));
        });

        // build buttons row
        HBox buttonRow = new HBox(25, menuButton, exitButton);
        buttonRow.setAlignment(Pos.CENTER);

        // build panel container
        panel = new VBox(30, title, buttonRow);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(24));
        panel.setBackground(new Background(
                new BackgroundFill(Color.BLACK, new CornerRadii(10), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
                Color.WHITE, BorderStrokeStyle.SOLID,
                new CornerRadii(10), new BorderWidths(2))));
        panel.setFillWidth(false);

        // size panel by overlay size
        panel.prefWidthProperty().bind(overlay.widthProperty().multiply(0.40));
        panel.prefHeightProperty().bind(overlay.heightProperty().multiply(0.40));
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // style buttons to match pause menu
        for (Button button : new Button[]{menuButton, exitButton}) {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");
            button.setFocusTraversable(false);

            // size buttons by panel size
            button.prefWidthProperty().bind(panel.widthProperty().multiply(0.40));
            button.prefHeightProperty().bind(panel.heightProperty().multiply(0.22));
            button.setMinHeight(32);
            button.setMaxWidth(Region.USE_PREF_SIZE);
            button.setMaxHeight(Region.USE_PREF_SIZE);

            // scale font with height
            button.heightProperty().addListener((obs, oldHeight, newHeight) ->
                    button.setFont(Font.font(newHeight.doubleValue() * 0.25)));
        }

        // wire button actions
        menuButton.setOnAction(e -> {
            hide();
            Parent menuRoot = new MainMenu(stage).createRoot();
            SceneTransition.fadeIntoScene(stage, menuRoot, Duration.millis(600));
        });
        exitButton.setOnAction(e -> stage.close());

        // mount panel into overlay
        overlay.getChildren().addAll(dim, panel);
        StackPane.setAlignment(panel, Pos.CENTER);
    }

    // returns the overlay node
    public StackPane getView() { return overlay; }

    // shows overlay with given winner text
    public void show(String text) {
        title.setText(text == null ? "" : text);
        overlay.setVisible(true);
        overlay.toFront();
        overlay.requestFocus();
    }

    // hides the overlay
    public void hide() { overlay.setVisible(false); }
}
