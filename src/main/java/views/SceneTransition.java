package views;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class SceneTransition {
    private SceneTransition() {}

    public static void fadeIntoScene(Stage stage, Parent nextRoot, Duration duration) {
        Scene currentScene = stage.getScene();

        // if there is no scene yet, just set one and bail
        if (currentScene == null) {
            stage.setScene(new Scene(nextRoot));
            return;
        }

        // prevents attempting to animate between two different scenes
        Scene targetScene = nextRoot.getScene();
        if (targetScene != null && targetScene != currentScene) {
            // degrade gracefully: just swap the stage's scene rather than animating
            stage.setScene(targetScene);
            return;
        }

        Parent currentRoot = currentScene.getRoot();

        // capture a snapshot of the current root so the crossfade doesn't jump
        WritableImage shot = currentRoot.snapshot(new SnapshotParameters(), null);
        ImageView overlay = new ImageView(shot);
        overlay.setPreserveRatio(false);
        overlay.fitWidthProperty().bind(currentScene.widthProperty());
        overlay.fitHeightProperty().bind(currentScene.heightProperty());
        overlay.setOpacity(1.0);

        // show nextRoot underneath the frozen overlay
        StackPane container = new StackPane(nextRoot, overlay);
        currentScene.setRoot(container);

        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(overlay.opacityProperty(), 1.0)),
            new KeyFrame(duration,       new KeyValue(overlay.opacityProperty(), 0.0))
        );

        tl.setOnFinished(e -> {
            // adopts nextRoot into the current scene
            Scene scene = stage.getScene();
            if (scene != null && (nextRoot.getScene() == null || nextRoot.getScene() == scene)) {
                scene.setRoot(nextRoot);
            }
        });

        tl.play();
    }
}
