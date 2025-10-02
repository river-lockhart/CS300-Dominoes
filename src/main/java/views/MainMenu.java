package views;

import controllers.CPlayer;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.util.Duration;
import models.AvailablePieces;
import models.Hand;

public class MainMenu {
    private final Stage stage;

    public MainMenu(Stage stage) {
        this.stage = stage;
    }

    // root to allow crossfade between scenes
    public Parent createRoot() {
        BorderPane root = new BorderPane();

        // create buttons
        Button playButton = new Button("Play");
        Button exitButton = new Button("Exit");

        // removes focus from buttons (prevent accidental close out...trial and error lol)
        playButton.setFocusTraversable(false);
        exitButton.setFocusTraversable(false);

        // button action
        playButton.setOnAction(e -> {
            // reset game state here with fresh instances
            Hand hand = new Hand();
            CPlayer player = new CPlayer();
            AvailablePieces remainingPieces = new AvailablePieces(hand);

            var tableRoot = new CTable(stage, hand, remainingPieces, player).createRoot();
            SceneTransition.fadeIntoScene(stage, tableRoot, Duration.millis(600));
        });

        exitButton.setOnAction(e -> stage.close());

        // transparent buttons 
        playButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");
        exitButton.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-width: 5px; -fx-border-radius: 10px;");

        // create box for button alignment
        VBox buttonsOnRight = new VBox(60, playButton, exitButton);
        buttonsOnRight.setPadding(new Insets(20));
        buttonsOnRight.setStyle("-fx-alignment: center-right;");
        root.setRight(buttonsOnRight);

        // background image
        var url = getClass().getResource("/assets/menu/mainmenu.jpg");
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

        // ✅ size bindings that work both when inserted into the current Scene
        //    (during cross-fade) and when used to create a fresh Scene.
        // use the live stage scene if present; otherwise fall back to root size
        var scene = stage.getScene();

        // use computed sizing so our pref bindings take effect
        playButton.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        playButton.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        exitButton.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        exitButton.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

        if (scene != null) {
            playButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.25));
            playButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));
            exitButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.15));
            exitButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));
        } else {
            // fallback (e.g., if someone uses createRoot() before attaching a Scene)
            playButton.prefWidthProperty().bind(root.widthProperty().multiply(0.25));
            playButton.prefHeightProperty().bind(root.heightProperty().multiply(0.15));
            exitButton.prefWidthProperty().bind(root.widthProperty().multiply(0.15));
            exitButton.prefHeightProperty().bind(root.heightProperty().multiply(0.15));
        }

        // flex font size based on the height of the buttons
        playButton.heightProperty().addListener((obs, oldVal, newVal) ->
            playButton.setFont(Font.font(newVal.doubleValue() * 0.4))
        );
        exitButton.heightProperty().addListener((obs, oldVal, newVal) ->
            exitButton.setFont(Font.font(newVal.doubleValue() * 0.4))
        );

        return root;
    }

    public Scene createScene() {
        // parent box
        BorderPane root = (BorderPane) createRoot();

        // create scene
        Scene scene = new Scene(root);

        // create buttons
        Button playButton = (Button) ((VBox) root.getRight()).getChildren().get(0);
        Button exitButton = (Button) ((VBox) root.getRight()).getChildren().get(1);

        // set button dimension properties (rebind to this fresh Scene)
        playButton.prefWidthProperty().unbind();
        playButton.prefHeightProperty().unbind();
        exitButton.prefWidthProperty().unbind();
        exitButton.prefHeightProperty().unbind();

        playButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.25));
        playButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));

        exitButton.prefWidthProperty().bind(scene.widthProperty().multiply(0.15));
        exitButton.prefHeightProperty().bind(scene.heightProperty().multiply(0.15));

        return scene;
    }
}
