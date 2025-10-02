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
        Scene scene = stage.getScene();
        Parent currentRoot = scene.getRoot();

        // captures a frame of the gameplay so crossfade doesn't seem to jumpy 
        WritableImage shot = currentRoot.snapshot(new SnapshotParameters(), null);
        ImageView overlay = new ImageView(shot);
        overlay.setPreserveRatio(false);
        overlay.fitWidthProperty().bind(scene.widthProperty());
        overlay.fitHeightProperty().bind(scene.heightProperty());
        overlay.setOpacity(1.0);

        // show nextRoot underneath the frozen overlay
        StackPane container = new StackPane(nextRoot, overlay);
        scene.setRoot(container);

        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(overlay.opacityProperty(), 1.0)),
            new KeyFrame(duration,       new KeyValue(overlay.opacityProperty(), 0.0))
        );
        tl.setOnFinished(e -> scene.setRoot(nextRoot));
        tl.play();
    }
}
