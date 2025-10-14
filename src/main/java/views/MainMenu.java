package views;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.geometry.Insets;
import models.AvailablePieces;
import models.Hand;
import controllers.CPlayer;

public class MainMenu {
    private final Stage stage;

    public MainMenu(Stage stage) {
        this.stage = stage;
    }

    // builds the main menu root layout
    public Parent createRoot() {
        // make base layout container
        BorderPane root = new BorderPane();

        // build play and exit buttons
        Button playButton = new Button("Play");
        Button exitButton = new Button("Exit");

        // remove focus to prevent accidental key actions
        playButton.setFocusTraversable(false);
        exitButton.setFocusTraversable(false);

        // set up play button action
        playButton.setOnAction(e -> {
            // reset game state with fresh instances
            Hand hand = new Hand();
            CPlayer player = new CPlayer();
            AvailablePieces remainingPieces = new AvailablePieces(hand);

            // build table view and crossfade to it
            var turnManager = new controllers.TurnManager();
            var tableRoot = new CTable(stage, hand, remainingPieces, player, turnManager).createRoot();
            SceneTransition.fadeIntoScene(stage, tableRoot, Duration.millis(600));
        });

        // set up exit button action
        exitButton.setOnAction(e -> stage.close());

        // style transparent buttons with white borders
        playButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");
        exitButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");

        // place buttons on the right side
        VBox rightButtons = new VBox(60, playButton, exitButton);
        rightButtons.setPadding(new Insets(20));
        rightButtons.setStyle("-fx-alignment: center-right;");
        root.setRight(rightButtons);

        // set background image if available
        var imageUrl = getClass().getResource("/assets/menu/mainmenu.jpg");
        if (imageUrl != null) {
            Image image = new Image(imageUrl.toExternalForm());
            Background menuBackground = new Background(new BackgroundImage(
                    image,
                    BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
            ));
            root.setBackground(menuBackground);
        }

        // use existing scene or fall back to root sizing
        var scene = stage.getScene();

        // use computed size so font scaling works
        playButton.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        playButton.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        exitButton.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        exitButton.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        // bind size to live scene when present
        if (scene != null) {
            playButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.25));
            playButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));
            exitButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.15));
            exitButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));
        } else {
            // fallback bindings when no scene yet
            playButton.prefWidthProperty().bind(root.widthProperty().multiply(0.25));
            playButton.prefHeightProperty().bind(root.heightProperty().multiply(0.15));
            exitButton.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            exitButton.prefHeightProperty().bind(root.heightProperty().multiply(0.15));
        }

        // scale font with button height
        playButton.heightProperty().addListener((obs, oldVal, newVal) ->
                playButton.setFont(Font.font(newVal.doubleValue() * 0.4))
        );
        exitButton.heightProperty().addListener((obs, oldVal, newVal) ->
                exitButton.setFont(Font.font(newVal.doubleValue() * 0.4))
        );

        return root;
    }

    // builds a new scene for the main menu
    public Scene createScene() {
        // build root from helper method
        BorderPane root = (BorderPane) createRoot();

        // create a fresh scene object
        Scene scene = new Scene(root);

        // get buttons from the right side box
        Button playButton = (Button) ((VBox) root.getRight()).getChildren().get(0);
        Button exitButton = (Button) ((VBox) root.getRight()).getChildren().get(1);

        // clear old size bindings from prior scene
        playButton.prefWidthProperty().unbind();
        playButton.prefHeightProperty().unbind();
        exitButton.prefWidthProperty().unbind();
        exitButton.prefHeightProperty().unbind();

        // bind button size to this scene
        playButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.25));
        playButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));
        exitButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.15));
        exitButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));

        return scene;
    }
}
