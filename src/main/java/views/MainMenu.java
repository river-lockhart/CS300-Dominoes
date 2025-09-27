package views;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.util.Duration;

public class MainMenu {
    private final Stage stage;

    public MainMenu(Stage stage) {
        this.stage = stage;
    }

    public Scene createScene() {
        // parent box
        BorderPane root = new BorderPane();

        // create scene
        Scene scene = new Scene(root);

        // create buttons
        Button playButton = new Button("Play");
        Button exitButton = new Button("Exit");

        // button action
        playButton.setOnAction(e -> {
            var nextRoot = new CTable(stage).createScene().getRoot();
            SceneTransition.fadeIntoScene(stage, nextRoot, Duration.millis(1000));
            stage.setMaximized(true);
        });

        exitButton.setOnAction(e -> stage.close());

        // use top-level parent to determine button size
        playButton.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        playButton.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        // transparent buttons 
        playButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");
        exitButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");

        // create box for button alignment
        VBox buttonsOnRight = new VBox(60, playButton, exitButton);
        buttonsOnRight.setPadding(new Insets(20));
        buttonsOnRight.setStyle("-fx-alignment: center-right;");
        root.setRight(buttonsOnRight);

        // background image
        var url = getClass().getResource("/assets/mainmenu.jpg");
        if (url != null) {
            var img = new Image(url.toExternalForm());
            var background = new Background(new BackgroundImage(
                    img,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
            ));
            root.setBackground(background);
        }

        // set button dimension properties
        playButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.25));
        playButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));

        exitButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.15));
        exitButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));

        // flex font size based on the height of the buttons
        playButton.heightProperty().addListener((obs, oldVal, newVal) ->
            playButton.setFont(Font.font(newVal.doubleValue() * 0.4))
        );
        exitButton.heightProperty().addListener((obs, oldVal, newVal) ->
            exitButton.setFont(Font.font(newVal.doubleValue() * 0.4))
        );

        return scene;
    }
}
